package neverlose.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.Heightmap;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.PacketEvent;
import neverlose.events.impl.TickEvent;
import neverlose.events.impl.WorldRenderEvent;
import neverlose.modules.impl.render.ambience.WeatherParticle;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.ColorSetting;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.Instance;
import neverlose.util.render.Render3D;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@FieldDefaults(level = AccessLevel.PUBLIC)
public class Ambience extends ModuleStructure {

    public static Ambience getInstance() {
        return Instance.get(Ambience.class);
    }

    public SliderSettings time = new SliderSettings("Время", "Время суток (0-24000)")
            .range(0, 24000)
            .setValue(1000);

    public SelectSetting worldView = new SelectSetting("Вид мира", "Атмосфера мира")
            .value("Нет", "Снежный", "Мокрый дождь")
            .selected("Нет");

    public SliderSettings weatherDensity = new SliderSettings("Плотность", "Количество частиц погоды")
            .range(50.0f, 800.0f)
            .setValue(280.0f)
            .visible(this::hasWeather);

    public SliderSettings weatherRadius = new SliderSettings("Радиус", "Радиус погоды вокруг игрока")
            .range(8.0f, 40.0f)
            .setValue(20.0f)
            .visible(this::hasWeather);

    public BooleanSetting weatherFog = new BooleanSetting("Туман погоды", "Атмосферный туман для выбранного вида мира")
            .setValue(true)
            .visible(this::hasWeather);

    public BooleanSetting snowCover = new BooleanSetting("Снег (весь мир)", "Покрыть снегом весь мир")
            .setValue(true)
            .visible(this::isSnow);

    public SliderSettings snowCoverStrength = new SliderSettings("Сила снега", "Насколько сильно белеет мир")
            .range(0.2f, 1.0f)
            .setValue(0.85f)
            .visible(() -> isSnow() && snowCover.isValue());

    public BooleanSetting groundSnow = new BooleanSetting("Снег на земле", "Плотный снежный покров на всей прогруженной местности")
            .setValue(true)
            .visible(this::isSnow);

    public BooleanSetting winterSky = new BooleanSetting("Зимнее небо", "Зимний градиент неба")
            .setValue(true)
            .visible(this::isSnow);

    public BooleanSetting customFog = new BooleanSetting("Кастомный туман", "Включить кастомный туман");
    public SliderSettings fogStart = new SliderSettings("Начало тумана", "Дистанция начала тумана")
            .range(0, 500)
            .setValue(5)
            .visible(() -> customFog.isValue());
    public SliderSettings fogEnd = new SliderSettings("Конец тумана", "Дистанция конца тумана")
            .range(0, 1000)
            .setValue(150)
            .visible(() -> customFog.isValue());
    public ColorSetting fogColor = new ColorSetting("Цвет тумана", "Цвет кастомного тумана")
            .value(ColorSetting.DEFAULT_COLOR)
            .visible(() -> customFog.isValue());

    private double animatedTime = 1000;

    private final List<WeatherParticle> particles = new ArrayList<>();
    private final List<WeatherParticle> splashes = new ArrayList<>();
    private final Random random = new Random();

    private String appliedView = "Нет";
    private long lastRenderNs = 0L;

    private final java.util.Map<Long, SnowChunk> snowChunks = new java.util.HashMap<>();
    private long lastLocalRebuild = 0L;
    private final BlockPos.Mutable scanPos = new BlockPos.Mutable();

    private static final int SNOW_SCAN_DEPTH = 16;

    private static final class SnowChunk {
        int count;
        int[] rects = new int[0];
    }

    public boolean shouldApplySnow() {
        return isState() && isSnow() && snowCover.isValue();
    }

    public float getSnowCoverStrength() {
        return snowCoverStrength.getValue();
    }

    public Ambience() {
        super("Ambience", "Изменяет время мира и атмосферу", ModuleCategory.RENDER);
        settings(time, worldView, weatherDensity, weatherRadius, weatherFog,
                snowCover, snowCoverStrength, groundSnow, winterSky,
                customFog, fogStart, fogEnd, fogColor);
    }

    public boolean hasWeather() {
        return worldView != null && !"Нет".equals(worldView.getSelected());
    }

    public boolean isSnow() {
        return "Снежный".equals(worldView.getSelected());
    }

    public boolean isRain() {
        return "Мокрый дождь".equals(worldView.getSelected());
    }

    public boolean shouldApplyWeatherFog() {
        return isState() && hasWeather() && weatherFog.isValue() && !customFog.isValue();
    }

    public int getWeatherFogColor() {
        return isSnow() ? rgb(205, 218, 238) : rgb(100, 106, 120);
    }

    public float getWeatherFogStart() {
        return isSnow() ? 14f : 10f;
    }

    public float getWeatherFogEnd() {
        return isSnow() ? 120f : 130f;
    }

    private static int rgb(int r, int g, int b) {
        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    @Override
    public void activate() {
        animatedTime = time.getValue();
    }

    @Override
    public void deactivate() {
        particles.clear();
        splashes.clear();
        appliedView = "Нет";
        lastRenderNs = 0L;
        snowChunks.clear();
        lastLocalRebuild = 0L;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        double targetTime = time.getValue();
        double speed = 150 / 1000.0;
        double diff = targetTime - animatedTime;
        animatedTime += diff * speed;
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
            event.cancel();
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!isState()) return;

        String view = worldView.getSelected();
        if (!appliedView.equals(view)) {
            particles.clear();
            splashes.clear();
            appliedView = view;
            lastRenderNs = 0L;
            snowChunks.clear();
            lastLocalRebuild = 0L;
        }
        if (!hasWeather()) return;

        long now = System.nanoTime();
        float dt = lastRenderNs == 0L ? 0.016f : (float) ((now - lastRenderNs) / 1_000_000_000.0);
        lastRenderNs = now;
        dt = Math.min(dt, 0.05f);

        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
        Vec3d playerPos = mc.player.getEntityPos();
        double cx = playerPos.x;
        double cy = playerPos.y + 1.0;
        double cz = playerPos.z;
        float radius = weatherRadius.getValue();
        int target = (int) weatherDensity.getValue();

        while (particles.size() < target) {
            particles.add(isSnow() ? createSnow(cx, cy, cz, radius) : createRain(cx, cy, cz, radius));
        }
        while (particles.size() > target) {
            particles.removeLast();
        }

        Iterator<WeatherParticle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            WeatherParticle p = iterator.next();
            p.update(dt);

            if (p.kind == WeatherParticle.Kind.RAIN) {
                BlockPos pos = BlockPos.ofFloored(p.x, p.y, p.z);
                if (!mc.world.isAir(pos)) {
                    spawnSplash(p.x, pos.getY() + 1.02, p.z);
                    recycleRain(p, cx, cy, cz, radius);
                    continue;
                }
            }

            if (outOfRange(p, cx, cy, cz, radius)) {
                if (isSnow()) recycleSnow(p, cx, cy, cz, radius);
                else recycleRain(p, cx, cy, cz, radius);
            }
        }

        Iterator<WeatherParticle> splashIterator = splashes.iterator();
        while (splashIterator.hasNext()) {
            WeatherParticle splash = splashIterator.next();
            splash.update(dt);
            if (splash.dead) splashIterator.remove();
        }

        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        MatrixStack matrices = event.getStack();
        float cameraYaw = mc.gameRenderer.getCamera().getYaw();
        float cameraPitch = mc.gameRenderer.getCamera().getPitch();

        for (WeatherParticle p : particles) {
            p.render(matrices, immediate, cameraPos, cameraYaw, cameraPitch);
        }
        for (WeatherParticle splash : splashes) {
            splash.render(matrices, immediate, cameraPos, cameraYaw, cameraPitch);
        }

        immediate.draw();

        if (isSnow() && groundSnow.isValue()) {
            renderGroundSnow(cx, cz);
        }

        if (isSnow() && winterSky.isValue()) {
            queueWinterSky();
        }
    }

    // ------------------------------------------------------------------ ground snow

    private void renderGroundSnow(double px, double pz) {
        int pcx = ((int) Math.floor(px)) >> 4;
        int pcz = ((int) Math.floor(pz)) >> 4;
        int rd = Math.max(2, mc.options.getClampedViewDistance());

        long now = System.nanoTime() / 1_000_000L;
        if (now - lastLocalRebuild > 1000L) {
            lastLocalRebuild = now;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    snowChunks.remove(chunkKey(pcx + dx, pcz + dz));
                }
            }
        }

        int budget = 4;
        for (int ring = 0; ring <= rd && budget > 0; ring++) {
            for (int dx = -ring; dx <= ring && budget > 0; dx++) {
                for (int dz = -ring; dz <= ring && budget > 0; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    int cx = pcx + dx;
                    int cz = pcz + dz;
                    long key = chunkKey(cx, cz);
                    if (snowChunks.containsKey(key)) continue;
                    if (!mc.world.getChunkManager().isChunkLoaded(cx, cz)) continue;
                    snowChunks.put(key, buildSnowChunk(cx, cz));
                    budget--;
                }
            }
        }

        snowChunks.entrySet().removeIf(e -> {
            int cx = (int) (e.getKey() >> 32);
            int cz = (int) (long) e.getKey();
            return !mc.world.getChunkManager().isChunkLoaded(cx, cz);
        });

        for (SnowChunk chunk : snowChunks.values()) {
            int[] rects = chunk.rects;
            for (int i = 0; i < chunk.count; i++) {
                int base = i * 6;
                double x0 = rects[base];
                double z0 = rects[base + 1];
                double x1 = rects[base + 2];
                double z1 = rects[base + 3];
                double top = rects[base + 4] + 0.016;
                int color = rects[base + 5];
                Render3D.drawQuad(
                        new Vec3d(x0, top, z0),
                        new Vec3d(x0, top, z1),
                        new Vec3d(x1, top, z1),
                        new Vec3d(x1, top, z0),
                        color, true);
            }
        }
    }

    private SnowChunk buildSnowChunk(int cx, int cz) {
        SnowChunk chunk = new SnowChunk();
        int baseX = cx << 4;
        int baseZ = cz << 4;
        int bottom = mc.world.getBottomY();

        int[][] surface = new int[16][16];
        int[][] lightGrid = new int[16][16];
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                surface[lx][lz] = -1;
                int x = baseX + lx;
                int z = baseZ + lz;
                int start = mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
                int floor = Math.max(bottom, start - SNOW_SCAN_DEPTH);
                for (int y = start; y >= floor; y--) {
                    scanPos.set(x, y, z);
                    BlockState state = mc.world.getBlockState(scanPos);
                    if (state.isAir()) continue;
                    if (state.isOf(Blocks.SNOW) || state.isOf(Blocks.SNOW_BLOCK) || state.isOf(Blocks.POWDER_SNOW)) break;

                    VoxelShape shape = state.getCollisionShape(mc.world, scanPos);
                    if (shape.isEmpty()) continue;

                    double maxY = shape.getMax(Direction.Axis.Y);
                    if (maxY < 0.99 || maxY > 1.01) continue;

                    surface[lx][lz] = y + 1;
                    lightGrid[lx][lz] = mc.world.getLightLevel(scanPos);
                    break;
                }
            }
        }

        greedySnow(chunk, surface, lightGrid, baseX, baseZ);
        return chunk;
    }

    private void greedySnow(SnowChunk chunk, int[][] surface, int[][] lightGrid, int baseX, int baseZ) {
        boolean[][] used = new boolean[16][16];
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int h = surface[lx][lz];
                if (h < 0 || used[lx][lz]) continue;

                int w = 1;
                while (lx + w < 16 && surface[lx + w][lz] == h && !used[lx + w][lz]) w++;

                int d = 1;
                rows:
                while (lz + d < 16) {
                    for (int i = 0; i < w; i++) {
                        if (surface[lx + i][lz + d] != h || used[lx + i][lz + d]) break rows;
                    }
                    d++;
                }

                for (int i = 0; i < w; i++) {
                    for (int j = 0; j < d; j++) {
                        used[lx + i][lz + j] = true;
                    }
                }

                float shade = 0.80f + 0.20f * (lightGrid[lx][lz] / 15.0f);
                int noise = (hash(lx, h, lz) & 7) - 3;
                int color = argb(255,
                        clampByte((int) (250.0f * shade) + noise),
                        clampByte((int) (252.0f * shade) + noise),
                        clampByte((int) (255.0f * shade) + noise));

                addSnowRect(chunk, baseX + lx, baseZ + lz, baseX + lx + w, baseZ + lz + d, h, color);
            }
        }
    }

    private void addSnowRect(SnowChunk chunk, int x0, int z0, int x1, int z1, int y, int color) {
        if (chunk.count * 6 + 6 > chunk.rects.length) {
            chunk.rects = java.util.Arrays.copyOf(chunk.rects, Math.max(24, (chunk.count + 1) * 6));
        }
        int base = chunk.count * 6;
        chunk.rects[base] = x0;
        chunk.rects[base + 1] = z0;
        chunk.rects[base + 2] = x1;
        chunk.rects[base + 3] = z1;
        chunk.rects[base + 4] = y;
        chunk.rects[base + 5] = color;
        chunk.count++;
    }

    private static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    private static int hash(int x, int y, int z) {
        int h = x * 374761393 + y * 668265263 + z * 2147483647;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    private static int clampByte(int value) {
        return value < 0 ? 0 : Math.min(value, 255);
    }

    // ------------------------------------------------------------------ winter sky

    private void queueWinterSky() {
        Matrix4f proj = Render3D.lastProjMat;
        float denom = proj.m22() + 1.0f;
        if (Math.abs(denom) < 1.0e-6f) return;
        float far = proj.m32() / denom;
        if (!(far > 16.0f) || far > 1.0e6f) far = 260.0f;

        float distance = far * 0.99f;
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();

        double yaw = Math.toRadians(camera.getYaw());
        double pitch = Math.toRadians(camera.getPitch());
        double dx = -Math.sin(yaw) * Math.cos(pitch);
        double dy = -Math.sin(pitch);
        double dz = Math.cos(yaw) * Math.cos(pitch);

        double rx = -dz;
        double rz = dx;
        double rLen = Math.hypot(rx, rz);
        if (rLen < 1.0e-6) {
            rx = 1.0;
            rz = 0.0;
            rLen = 1.0;
        }
        rx /= rLen;
        rz /= rLen;

        double ux = -rz * dy;
        double uy = rz * dx - rx * dz;
        double uz = rx * dy;
        double uLen = Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (uLen < 1.0e-6) return;
        ux /= uLen;
        uy /= uLen;
        uz /= uLen;

        double halfV = distance * Math.tan(Math.toRadians(mc.options.getFov().getValue() * 0.5));
        double aspect = (double) mc.getWindow().getFramebufferWidth() / (double) mc.getWindow().getFramebufferHeight();
        double halfH = halfV * aspect;

        Vec3d center = cameraPos.add(dx * distance, dy * distance, dz * distance);
        Vec3d right = new Vec3d(rx, 0.0, rz);
        Vec3d up = new Vec3d(ux, uy, uz);

        Vec3d bl = center.add(right.multiply(-halfH)).add(up.multiply(-halfV));
        Vec3d br = center.add(right.multiply(halfH)).add(up.multiply(-halfV));
        Vec3d tr = center.add(right.multiply(halfH)).add(up.multiply(halfV));
        Vec3d tl = center.add(right.multiply(-halfH)).add(up.multiply(halfV));

        int top = argb(210, 122, 168, 232);
        int bottom = argb(0, 236, 243, 255);
        Render3D.drawGradientQuad(bl, br, tr, tl, bottom, bottom, top, top, true);
    }

    private static int argb(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    // ------------------------------------------------------------------ spawn

    private void randomColumn(WeatherParticle p, double cx, double cz, float radius) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double dist = Math.sqrt(random.nextDouble()) * radius;
        p.x = cx + Math.cos(angle) * dist;
        p.z = cz + Math.sin(angle) * dist;
    }

    private WeatherParticle createSnow(double cx, double cy, double cz, float radius) {
        WeatherParticle p = new WeatherParticle(WeatherParticle.Kind.SNOW);
        randomColumn(p, cx, cz, radius);
        p.y = cy - 6.0 + random.nextDouble() * 20.0;
        p.vx = (random.nextDouble() - 0.5) * 0.35;
        p.vz = (random.nextDouble() - 0.5) * 0.35;
        p.vy = -(0.8 + random.nextDouble() * 1.4);
        p.size = 0.06f + random.nextFloat() * 0.10f;
        p.rotation = random.nextFloat() * 360f;
        p.rotSpeed = (random.nextFloat() - 0.5f) * 70f;
        return p;
    }

    private WeatherParticle createRain(double cx, double cy, double cz, float radius) {
        WeatherParticle p = new WeatherParticle(WeatherParticle.Kind.RAIN);
        randomColumn(p, cx, cz, radius);
        p.y = cy + 12.0 + random.nextDouble() * 6.0;
        p.vx = 0.5 + (random.nextDouble() - 0.5) * 0.5;
        p.vz = (random.nextDouble() - 0.5) * 0.5;
        p.vy = -(16.0 + random.nextDouble() * 8.0);
        p.size = 0.42f + random.nextFloat() * 0.35f;
        return p;
    }

    private void recycleSnow(WeatherParticle p, double cx, double cy, double cz, float radius) {
        randomColumn(p, cx, cz, radius);
        p.y = cy + 12.0 + random.nextDouble() * 5.0;
        p.vx = (random.nextDouble() - 0.5) * 0.35;
        p.vz = (random.nextDouble() - 0.5) * 0.35;
        p.vy = -(0.8 + random.nextDouble() * 1.4);
        p.size = 0.06f + random.nextFloat() * 0.10f;
        p.rotation = random.nextFloat() * 360f;
    }

    private void recycleRain(WeatherParticle p, double cx, double cy, double cz, float radius) {
        randomColumn(p, cx, cz, radius);
        p.y = cy + 12.0 + random.nextDouble() * 6.0;
        p.vx = 0.5 + (random.nextDouble() - 0.5) * 0.5;
        p.vz = (random.nextDouble() - 0.5) * 0.5;
        p.vy = -(16.0 + random.nextDouble() * 8.0);
        p.size = 0.42f + random.nextFloat() * 0.35f;
    }

    private void spawnSplash(double x, double y, double z) {
        if (splashes.size() > 400) return;

        WeatherParticle splash = new WeatherParticle(WeatherParticle.Kind.SPLASH);
        splash.x = x;
        splash.y = y;
        splash.z = z;
        splash.size = 0.13f + random.nextFloat() * 0.06f;
        splash.maxLife = 0.4f + random.nextFloat() * 0.15f;
        splashes.add(splash);
    }

    private boolean outOfRange(WeatherParticle p, double cx, double cy, double cz, float radius) {
        double dx = p.x - cx;
        double dz = p.z - cz;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        return horizontal > radius + 3.0 || p.y < cy - 8.0 || p.y > cy + 20.0;
    }

    public long getCustomTime() {
        return (long) animatedTime;
    }
}

package neverlose.modules.impl.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.DrawEvent;
import neverlose.events.impl.WorldRenderEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.Instance;
import neverlose.util.render.Render2D;
import neverlose.util.render.Render3D;
import neverlose.util.render.font.Fonts;
import neverlose.util.render.item.ItemRender;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FireWorkESP extends ModuleStructure {

    public static FireWorkESP getInstance() {
        return Instance.get(FireWorkESP.class);
    }

    private final SliderSettings interval = new SliderSettings("Interval", "Trail point interval in ms")
            .range(10.0f, 1000.0f)
            .setValue(100.0f);

    private final SliderSettings lifetime = new SliderSettings("Lifetime", "Trail point lifetime in ms")
            .range(100.0f, 5000.0f)
            .setValue(1000.0f);

    private final Map<Integer, FireworkData> fireworks = new HashMap<>();
    private static final ItemStack ICON = new ItemStack(Items.FIREWORK_ROCKET);

    public FireWorkESP() {
        super("FireWorkESP", "Shows fireworks with age tags", ModuleCategory.RENDER);
        settings(interval, lifetime);
    }

    @Override
    public void deactivate() {
        fireworks.clear();
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        float tickDelta = event.getPartialTicks();

        Iterator<Map.Entry<Integer, FireworkData>> iterator = fireworks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, FireworkData> entry = iterator.next();
            Entity entity = mc.world.getEntityById(entry.getKey());
            FireworkData data = entry.getValue();
            data.points.removeIf(point -> now - point.timestamp > lifetime.getValue());
            if ((entity == null || !entity.isAlive()) && data.points.isEmpty()) {
                iterator.remove();
            }
        }

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof FireworkRocketEntity) || !entity.isAlive()) {
                continue;
            }

            FireworkData data = fireworks.computeIfAbsent(entity.getId(), id -> new FireworkData());
            if (now - data.lastSpawnTime < interval.getValue()) {
                continue;
            }

            Vec3d pos = new Vec3d(
                    MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX()),
                    MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()) + 0.5,
                    MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ())
            );

            data.points.add(new TrailPoint(pos, now, entity.age / 20.0f));
            data.lastSpawnTime = now;
        }
    }

    @EventHandler
    public void onDraw(DrawEvent event) {
        if (mc.player == null || mc.world == null) return;

        DrawContext context = event.getDrawContext();
        long now = System.currentTimeMillis();

        for (Map.Entry<Integer, FireworkData> entry : new ArrayList<>(fireworks.entrySet())) {
            FireworkData data = entry.getValue();
            for (TrailPoint point : new ArrayList<>(data.points)) {
                float[] screen = worldToScreen(point.pos);
                if (screen == null) continue;

                float progress = 1.0f - (float) (now - point.timestamp) / lifetime.getValue();
                renderTag(context, screen[0], screen[1], MathHelper.clamp(progress, 0.0f, 1.0f),
                        String.format("%.1fs", point.ageSeconds));
            }

            Entity entity = mc.world.getEntityById(entry.getKey());
            if (entity instanceof FireworkRocketEntity && entity.isAlive()) {
                Vec3d pos = new Vec3d(
                        MathHelper.lerp(Render3D.lastTickDelta, entity.lastRenderX, entity.getX()),
                        MathHelper.lerp(Render3D.lastTickDelta, entity.lastRenderY, entity.getY()) + 0.5,
                        MathHelper.lerp(Render3D.lastTickDelta, entity.lastRenderZ, entity.getZ())
                );
                float[] screen = worldToScreen(pos);
                if (screen != null) {
                    renderTag(context, screen[0], screen[1], 1.0f, String.format("%.1fs", entity.age / 20.0f));
                }
            }
        }
    }

    private void renderTag(DrawContext context, float centerX, float centerY, float alpha, String text) {
        if (alpha <= 0.02f) return;

        float scale = 0.35f + 0.65f * alpha;
        float iconScale = 0.5f;
        float iconSize = 16.0f * iconScale;
        float textSize = 5.0f;
        float padding = 3.0f;
        float gap = 2.2f;
        float height = 12.0f;
        float width = padding + iconSize + gap + Fonts.BOLD.getWidth(text, textSize) + padding;
        float x = centerX - width / 2.0f;
        float y = centerY - height / 2.0f;

        int bgAlpha = (int) (190.0f * alpha);
        int fgAlpha = (int) (255.0f * alpha);
        int bg = (bgAlpha << 24) | 0x080A0E;
        int textColor = (fgAlpha << 24) | 0xF0F4FF;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(centerX, centerY);
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(-centerX, -centerY);

        Render2D.rect(x, y, width, height, bg, 2.0f);
        ItemRender.drawItemWithContext(context, ICON, x + padding, y + 2.0f, iconScale, alpha);
        Fonts.BOLD.draw(text, x + padding + iconSize + gap, y + 3.8f, textSize, textColor);

        context.getMatrices().popMatrix();
    }

    private float[] worldToScreen(Vec3d worldPos) {
        try {
            Vec3d camera = Render3D.lastCameraPos;
            Vector4f projected = new Vector4f(
                    (float) (worldPos.x - camera.x),
                    (float) (worldPos.y - camera.y),
                    (float) (worldPos.z - camera.z),
                    1.0f
            );

            projected.mul(Render3D.lastWorldSpaceMatrix).mul(Render3D.lastProjMat);
            if (projected.w <= 0.0f) {
                return null;
            }

            float ndcX = projected.x / projected.w;
            float ndcY = projected.y / projected.w;
            if (ndcX < -1.0f || ndcX > 1.0f || ndcY < -1.0f || ndcY > 1.0f) {
                return null;
            }

            float screenWidth = Render2D.getFixedScaledWidth();
            float screenHeight = Render2D.getFixedScaledHeight();
            return new float[]{
                    (ndcX + 1.0f) / 2.0f * screenWidth,
                    (1.0f - ndcY) / 2.0f * screenHeight
            };
        } catch (Exception ignored) {
            return null;
        }
    }

    private static class FireworkData {
        long lastSpawnTime;
        final List<TrailPoint> points = new ArrayList<>();
    }

    private record TrailPoint(Vec3d pos, long timestamp, float ageSeconds) {}
}

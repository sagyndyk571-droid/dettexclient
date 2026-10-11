package neverlose.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.PacketEvent;
import neverlose.events.impl.WorldChangeEvent;
import neverlose.events.impl.WorldLoadEvent;
import neverlose.events.impl.WorldRenderEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.ColorSetting;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.ColorUtil;
import neverlose.util.Instance;
import neverlose.util.render.Render3D;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TotemAngle extends ModuleStructure {

    public static TotemAngle getInstance() {
        return Instance.get(TotemAngle.class);
    }

    final SelectSetting mode = new SelectSetting("Режим", "Тип эффекта для тотема")
            .value("Angel", "Shatter")
            .selected("Angel");

    final SliderSettings riseHeight = new SliderSettings("Подъём", "Высота подъёма эффекта")
            .range(0.2f, 5.0f)
            .setValue(4.0f);

    final SliderSettings duration = new SliderSettings("Время", "Время жизни эффекта")
            .range(0.2f, 6.0f)
            .setValue(3.0f);

    final BooleanSetting showSelf = new BooleanSetting("Показывать на себе", "Показывать эффект на себе")
            .setValue(false);

    final SliderSettings brightness = new SliderSettings("Яркость", "Усиление альфы эффекта")
            .range(0.2f, 3.0f)
            .setValue(1.0f);

    final SliderSettings transparency = new SliderSettings("Прозрачность", "Общая прозрачность эффекта")
            .range(0.05f, 1.0f)
            .setValue(0.72f);

    final ColorSetting color = new ColorSetting("Цвет", "Цвет эффекта")
            .value(new Color(179, 140, 255, 255).getRGB());

    final Deque<GhostFrame> ghosts = new ArrayDeque<>();
    final Deque<ShatterFrame> shatters = new ArrayDeque<>();

    public TotemAngle() {
        super("TotemAngle", "Totem pop effect", ModuleCategory.RENDER);
        settings(mode, riseHeight, duration, showSelf, brightness, transparency, color);
    }

    @Override
    public void deactivate() {
        clear();
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        clear();
    }

    @EventHandler
    public void onWorldChange(WorldChangeEvent e) {
        clear();
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.getType() != PacketEvent.Type.RECEIVE || mc.world == null) {
            return;
        }

        if (!(e.getPacket() instanceof EntityStatusS2CPacket packet)) {
            return;
        }

        if (packet.getStatus() != 35) {
            return;
        }

        Entity entity = packet.getEntity(mc.world);
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        if (!showSelf.isValue() && mc.player != null && living.getId() == mc.player.getId()) {
            return;
        }

        if (isShatter()) {
            shatters.addLast(createShatterFrame(living));
            while (shatters.size() > 48) {
                shatters.pollFirst();
            }
        } else {
            ghosts.addLast(createGhostFrame(living));
            while (ghosts.size() > 48) {
                ghosts.pollFirst();
            }
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.world == null) {
            clearExpired();
            return;
        }

        long now = System.currentTimeMillis();
        renderGhosts(now);
        renderShatters(now);
    }

    private void clear() {
        ghosts.clear();
        shatters.clear();
    }

    private void clearExpired() {
        ghosts.clear();
        shatters.clear();
    }

    private boolean isShatter() {
        return mode.isSelected("Shatter");
    }

    private void renderGhosts(long now) {
        if (ghosts.isEmpty()) {
            return;
        }

        float lifeMs = duration.getValue() * 1000.0f;
        float rise = riseHeight.getValue();
        float bright = MathHelper.clamp(brightness.getValue(), 0.2f, 3.0f);
        float alphaSetting = MathHelper.clamp(transparency.getValue(), 0.05f, 1.0f);
        float lineWidth = 1.25f;

        Iterator<GhostFrame> iterator = ghosts.iterator();
        while (iterator.hasNext()) {
            GhostFrame frame = iterator.next();
            float t = (now - frame.startTime()) / Math.max(1.0f, lifeMs);
            if (t >= 1.0f) {
                iterator.remove();
                continue;
            }

            double up = rise * easeOut(MathHelper.clamp(t, 0.0f, 0.85f));
            float alpha = (float) MathHelper.clamp(easeOutAlpha(MathHelper.clamp(t, 0.0f, 1.0f)) * bright * alphaSetting, 0.0, 0.95);
            int argb = ColorUtil.multAlpha(color.getColor(), alpha);

            Vec3d origin = frame.pos().add(0.0, up, 0.0);
            drawGhostModel(origin, frame.bodyYaw(), frame.width(), frame.height(), argb, lineWidth);
        }
    }

    private void renderShatters(long now) {
        if (shatters.isEmpty()) {
            return;
        }

        float lifeMs = duration.getValue() * 1000.0f;
        float power = Math.max(0.2f, riseHeight.getValue() * 0.35f);
        float gravity = 5.8f;
        float bright = MathHelper.clamp(brightness.getValue(), 0.2f, 3.0f);
        float alphaSetting = MathHelper.clamp(transparency.getValue(), 0.05f, 1.0f);
        float lineWidth = 1.1f;

        Iterator<ShatterFrame> iterator = shatters.iterator();
        while (iterator.hasNext()) {
            ShatterFrame frame = iterator.next();
            float t = (now - frame.startTime()) / Math.max(1.0f, lifeMs);
            if (t >= 1.0f) {
                iterator.remove();
                continue;
            }

            double elapsed = (now - frame.startTime()) / 1000.0;
            float alpha = (float) MathHelper.clamp(alphaProfile(MathHelper.clamp(t, 0.0f, 1.0f)) * bright * alphaSetting, 0.0, 0.95);
            int argb = ColorUtil.multAlpha(color.getColor(), alpha);

            for (PartMotion part : frame.parts()) {
                double side = part.side() + part.velSide() * elapsed * power;
                double up = part.up() + part.velUp() * elapsed * power - 0.5 * gravity * elapsed * elapsed;
                double front = part.front() + part.velFront() * elapsed * power;
                Vec3d center = transformLocal(frame.pos(), frame.bodyYaw(), side, up, front);
                drawPartBox(center, frame.width(), frame.height(), part.id(), argb, lineWidth);
            }
        }
    }

    private GhostFrame createGhostFrame(LivingEntity entity) {
        float width = Math.max(entity.getWidth(), 0.35f);
        float height = Math.max(entity.getHeight(), 0.6f);
        float bodyYaw = entity.bodyYaw;
        Vec3d pos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
        return new GhostFrame(pos, bodyYaw, width, height, System.currentTimeMillis());
    }

    private ShatterFrame createShatterFrame(LivingEntity entity) {
        float width = Math.max(entity.getWidth(), 0.35f);
        float height = Math.max(entity.getHeight(), 0.6f);
        float bodyYaw = entity.bodyYaw;
        Vec3d pos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
        long startTime = System.currentTimeMillis();

        PartMotion[] parts = new PartMotion[PartId.values().length];
        ThreadLocalRandom random = ThreadLocalRandom.current();

        double wScale = width / 0.6;
        double hScale = height / 1.95;

        parts[PartId.HEAD.ordinal()] = new PartMotion(
                0.0,
                1.60 * hScale,
                0.0,
                randomSigned(random, 0.22),
                0.22 + random.nextDouble(0.05, 0.18),
                randomSigned(random, 0.22),
                PartId.HEAD
        );
        parts[PartId.BODY.ordinal()] = new PartMotion(
                0.0,
                1.08 * hScale,
                0.0,
                randomSigned(random, 0.12),
                0.12 + random.nextDouble(0.04, 0.12),
                randomSigned(random, 0.12),
                PartId.BODY
        );
        parts[PartId.ARM_L.ordinal()] = new PartMotion(
                -0.48 * wScale,
                1.15 * hScale,
                0.0,
                -0.15 - random.nextDouble(0.04, 0.12),
                0.10 + random.nextDouble(0.03, 0.10),
                randomSigned(random, 0.12),
                PartId.ARM_L
        );
        parts[PartId.ARM_R.ordinal()] = new PartMotion(
                0.48 * wScale,
                1.15 * hScale,
                0.0,
                0.15 + random.nextDouble(0.04, 0.12),
                0.10 + random.nextDouble(0.03, 0.10),
                randomSigned(random, 0.12),
                PartId.ARM_R
        );
        parts[PartId.LEG_L.ordinal()] = new PartMotion(
                -0.16 * wScale,
                0.42 * hScale,
                0.0,
                -0.06 - random.nextDouble(0.02, 0.08),
                0.05 + random.nextDouble(0.02, 0.08),
                randomSigned(random, 0.06),
                PartId.LEG_L
        );
        parts[PartId.LEG_R.ordinal()] = new PartMotion(
                0.16 * wScale,
                0.42 * hScale,
                0.0,
                0.06 + random.nextDouble(0.02, 0.08),
                0.05 + random.nextDouble(0.02, 0.08),
                randomSigned(random, 0.06),
                PartId.LEG_R
        );

        return new ShatterFrame(pos, bodyYaw, width, height, startTime, parts);
    }

    private void drawGhostModel(Vec3d origin, float bodyYaw, float width, float height, int color, float lineWidth) {
        double wScale = width / 0.6;
        double hScale = height / 1.95;
        double depthScale = Math.max(0.28, width * 0.40);

        drawLocalBox(origin, bodyYaw, 0.0, 1.70 * hScale, 0.0, 0.50 * wScale, 0.50 * hScale, 0.50 * wScale, color, lineWidth);
        drawLocalBox(origin, bodyYaw, 0.0, 1.18 * hScale, 0.0, 0.60 * wScale, 0.55 * hScale, depthScale, color, lineWidth);
        drawLocalBox(origin, bodyYaw, -0.48 * wScale, 1.18 * hScale, 0.02 * wScale, 0.22 * wScale, 0.50 * hScale, 0.24 * wScale, color, lineWidth);
        drawLocalBox(origin, bodyYaw, 0.48 * wScale, 1.18 * hScale, 0.02 * wScale, 0.22 * wScale, 0.50 * hScale, 0.24 * wScale, color, lineWidth);
        drawLocalBox(origin, bodyYaw, -0.16 * wScale, 0.42 * hScale, 0.0, 0.26 * wScale, 0.90 * hScale, 0.24 * wScale, color, lineWidth);
        drawLocalBox(origin, bodyYaw, 0.16 * wScale, 0.42 * hScale, 0.0, 0.26 * wScale, 0.90 * hScale, 0.24 * wScale, color, lineWidth);
    }

    private void drawPartBox(Vec3d center, float width, float height, PartId partId, int color, float lineWidth) {
        double wScale = width / 0.6;
        double hScale = height / 1.95;
        double boxW;
        double boxH;
        double boxD;

        switch (partId) {
            case HEAD -> {
                boxW = 0.50 * wScale;
                boxH = 0.50 * hScale;
                boxD = 0.50 * wScale;
            }
            case BODY -> {
                boxW = 0.60 * wScale;
                boxH = 0.55 * hScale;
                boxD = Math.max(0.28, width * 0.40);
            }
            case ARM_L, ARM_R -> {
                boxW = 0.22 * wScale;
                boxH = 0.50 * hScale;
                boxD = 0.24 * wScale;
            }
            case LEG_L, LEG_R -> {
                boxW = 0.26 * wScale;
                boxH = 0.90 * hScale;
                boxD = 0.24 * wScale;
            }
            default -> {
                boxW = 0.25 * wScale;
                boxH = 0.25 * hScale;
                boxD = 0.25 * wScale;
            }
        }

        Box box = new Box(
                center.x - boxW * 0.5,
                center.y - boxH * 0.5,
                center.z - boxD * 0.5,
                center.x + boxW * 0.5,
                center.y + boxH * 0.5,
                center.z + boxD * 0.5
        );

        Render3D.drawBox(box, color, lineWidth, true, true, false);
    }

    private void drawLocalBox(Vec3d origin, float bodyYaw, double side, double up, double front, double sizeX, double sizeY, double sizeZ, int color, float lineWidth) {
        Vec3d center = transformLocal(origin, bodyYaw, side, up, front);
        Box box = new Box(
                center.x - sizeX * 0.5,
                center.y - sizeY * 0.5,
                center.z - sizeZ * 0.5,
                center.x + sizeX * 0.5,
                center.y + sizeY * 0.5,
                center.z + sizeZ * 0.5
        );
        Render3D.drawBox(box, color, lineWidth, true, true, false);
    }

    private Vec3d transformLocal(Vec3d origin, float bodyYaw, double side, double up, double front) {
        Vec3d forward = direction(bodyYaw);
        Vec3d right = right(bodyYaw);
        return origin.add(right.multiply(side)).add(0.0, up, 0.0).add(forward.multiply(front));
    }

    private Vec3d direction(float yaw) {
        float radians = (float) Math.toRadians(yaw);
        return new Vec3d(-MathHelper.sin(radians), 0.0, MathHelper.cos(radians));
    }

    private Vec3d right(float yaw) {
        float radians = (float) Math.toRadians(yaw);
        return new Vec3d(MathHelper.cos(radians), 0.0, MathHelper.sin(radians));
    }

    private double easeOut(double t) {
        double inv = 1.0D - t;
        return 1.0D - inv * inv * inv;
    }

    private double easeOutAlpha(double t) {
        double inv = 1.0D - t;
        return 0.75D * (1.0D - inv * inv * inv);
    }

    private double alphaProfile(double t) {
        double in = MathHelper.clamp(t / 0.10, 0.0, 1.0);
        double out = MathHelper.clamp((t - 0.65) / 0.35, 0.0, 1.0);
        double aIn = 1.0 - Math.pow(1.0 - in, 3.0);
        double aOut = 1.0 - out;
        return 0.85 * aIn * aOut;
    }

    private double randomSigned(ThreadLocalRandom random, double range) {
        return (random.nextDouble() - 0.5) * 2.0 * range;
    }

    private record GhostFrame(Vec3d pos, float bodyYaw, float width, float height, long startTime) {
    }

    private record ShatterFrame(Vec3d pos, float bodyYaw, float width, float height, long startTime, PartMotion[] parts) {
    }

    private record PartMotion(double side, double up, double front, double velSide, double velUp, double velFront, PartId id) {
    }

    private enum PartId {
        HEAD,
        BODY,
        ARM_L,
        ARM_R,
        LEG_L,
        LEG_R
    }
}

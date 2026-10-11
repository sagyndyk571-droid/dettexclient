package neverlose.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.WorldRenderEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.ColorSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.ColorUtil;
import neverlose.util.render.Render3D;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Wings extends ModuleStructure {
    static final float DEFAULT_SPREAD = 8.0f;

    static final WingPoint[] SHAPE = {
            new WingPoint(0.08f, 0.10f, 0.88f),
            new WingPoint(0.28f, 0.34f, 0.78f),
            new WingPoint(0.56f, 0.82f, 0.62f),
            new WingPoint(0.86f, 0.30f, 0.52f),
            new WingPoint(1.14f, 0.46f, 0.40f),
            new WingPoint(1.24f, 0.04f, 0.30f),
            new WingPoint(1.02f, -0.18f, 0.28f),
            new WingPoint(1.18f, -0.64f, 0.22f),
            new WingPoint(0.86f, -0.46f, 0.20f),
            new WingPoint(0.80f, -0.98f, 0.14f),
            new WingPoint(0.54f, -0.74f, 0.16f),
            new WingPoint(0.30f, -1.16f, 0.12f),
            new WingPoint(0.10f, -0.54f, 0.18f)
    };

    final BooleanSetting self = new BooleanSetting("На себя", "Показывать крылья на себе")
            .setValue(true);

    final BooleanSetting players = new BooleanSetting("На игроков", "Показывать крылья на игроках")
            .setValue(false);

    final SliderSettings size = new SliderSettings("Размер", "Размер крыльев")
            .range(0.65f, 1.35f)
            .setValue(1.0f);

    final SliderSettings transparency = new SliderSettings("Прозрачность", "Общая прозрачность крыльев")
            .range(0.25f, 1.0f)
            .setValue(0.86f);

    final BooleanSetting throughWalls = new BooleanSetting("Через стены", "Рисовать крылья поверх стен")
            .setValue(false);

    final ColorSetting color = new ColorSetting("Цвет", "Цвет крыльев")
            .value(ColorSetting.DEFAULT_COLOR);

    float selfBodyYaw;
    boolean selfBodyYawInitialized;

    public Wings() {
        super("Wings", "Cosmetic wings", ModuleCategory.RENDER);
        settings(self, players, size, transparency, throughWalls, color);
    }

    @Override
    public void deactivate() {
        selfBodyYawInitialized = false;
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (mc.world == null || mc.player == null) {
            return;
        }

        float tickDelta = event.getPartialTicks();
        if (self.isValue()
                && !mc.options.getPerspective().isFirstPerson()
                && mc.player.isAlive()
                && !hasElytra(mc.player)) {
            renderWings(mc.player, tickDelta);
        }

        if (!players.isValue()) {
            return;
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive() || hasElytra(player)) {
                continue;
            }
            renderWings(player, tickDelta);
        }
    }

    private void renderWings(PlayerEntity player, float tickDelta) {
        WingPose pose = resolvePose(player, tickDelta);
        if (pose == null) {
            return;
        }

        Vec3d origin = player.getLerpedPos(tickDelta);
        float bodyYaw = resolveBodyYaw(player, tickDelta);
        float move = MathHelper.clamp(player.limbAnimator.getAmplitude(tickDelta), 0.0f, 1.0f);
        float flap = MathHelper.sin((player.age + tickDelta) * pose.flapSpeed) * pose.flapAmplitude;
        float open = (DEFAULT_SPREAD + flap + move * pose.motionSpreadBoost) * pose.openMultiplier;
        float wingScale = size.getValue() * pose.scaleMultiplier * (player.isBaby() ? 0.55f : 1.0f);

        int base = color.getColorNoAlpha();
        int glow = ColorUtil.interpolateColor(base, 0xFFFFFFFF, 0.28f);
        int core = ColorUtil.interpolateColor(base, 0xFFFFFFFF, 0.55f);

        renderWingSide(origin, bodyYaw, -1f, open, wingScale, pose, glow, core, base);
        renderWingSide(origin, bodyYaw, 1f, open, wingScale, pose, glow, core, base);
    }

    private void renderWingSide(Vec3d origin, float bodyYaw, float side, float open,
                                float wingScale, WingPose pose, int glow, int core, int base) {
        drawWingLayer(origin, bodyYaw, side, open, wingScale, pose, 1.22f,
                withAlpha(glow, 0.24f), withAlpha(glow, 0.08f));
        drawWingLayer(origin, bodyYaw, side, open, wingScale, pose, 0.84f,
                withAlpha(core, 0.28f), withAlpha(core, 0.10f));
        drawWingLayer(origin, bodyYaw, side, open, wingScale, pose, 1.0f,
                withAlpha(base, 0.76f), withAlpha(base, 0.16f));

        drawWingOutline(origin, bodyYaw, side, open, wingScale, pose, withAlpha(base, 0.72f));
        drawWingRibs(origin, bodyYaw, side, open, wingScale, pose, withAlpha(glow, 0.42f));
    }

    private void drawWingLayer(Vec3d origin, float bodyYaw, float side, float open,
                               float wingScale, WingPose pose, float layerScale,
                               int rootColor, int edgeColor) {
        Vec3d root = wingPoint(origin, bodyYaw, side, open, wingScale, pose, 0f, 0f, layerScale, 0f);

        for (int i = 0; i < SHAPE.length; i++) {
            WingPoint current = SHAPE[i];
            WingPoint next = SHAPE[(i + 1) % SHAPE.length];

            Vec3d currentPoint = wingPoint(origin, bodyYaw, side, open, wingScale, pose,
                    current.x, current.y, layerScale, 0.012f * (1.22f - layerScale));
            Vec3d nextPoint = wingPoint(origin, bodyYaw, side, open, wingScale, pose,
                    next.x, next.y, layerScale, 0.012f * (1.22f - layerScale));

            int currentColor = applyPointAlpha(edgeColor, current.alphaMul);
            int nextColor = applyPointAlpha(edgeColor, next.alphaMul);
            if (side > 0f) {
                drawTriangle(root, nextPoint, currentPoint, rootColor, nextColor, currentColor);
            } else {
                drawTriangle(root, currentPoint, nextPoint, rootColor, currentColor, nextColor);
            }
        }
    }

    private void drawWingOutline(Vec3d origin, float bodyYaw, float side, float open,
                                 float wingScale, WingPose pose, int outlineColor) {
        Vec3d previous = wingPoint(origin, bodyYaw, side, open, wingScale, pose,
                SHAPE[SHAPE.length - 1].x, SHAPE[SHAPE.length - 1].y, 1.0f, 0.018f);

        for (WingPoint point : SHAPE) {
            Vec3d current = wingPoint(origin, bodyYaw, side, open, wingScale, pose,
                    point.x, point.y, 1.0f, 0.018f);
            drawLine(previous, current, outlineColor, applyPointAlpha(outlineColor, point.alphaMul), 1.35f);
            previous = current;
        }
    }

    private void drawWingRibs(Vec3d origin, float bodyYaw, float side, float open,
                              float wingScale, WingPose pose, int ribColor) {
        Vec3d root = wingPoint(origin, bodyYaw, side, open, wingScale, pose, 0f, 0f, 0.96f, 0.02f);
        int[] ribs = {2, 4, 7, 9, 11};

        for (int index : ribs) {
            WingPoint point = SHAPE[index];
            Vec3d end = wingPoint(origin, bodyYaw, side, open, wingScale, pose,
                    point.x, point.y, 0.96f, 0.02f);
            drawLine(root, end, withAlpha(ribColor, 0.18f), applyPointAlpha(ribColor, point.alphaMul), 0.9f);
        }
    }

    private Vec3d wingPoint(Vec3d origin, float bodyYaw, float side, float open,
                            float wingScale, WingPose pose, float x, float y,
                            float layerScale, float layerBackOffset) {
        float roll = (float) Math.toRadians(side * pose.sideRoll);
        float pitch = (float) Math.toRadians(pose.sidePitch);
        float openRad = (float) Math.toRadians(open);

        float scaledX = x * layerScale;
        float scaledY = y * layerScale;
        float rolledX = scaledX * MathHelper.cos(roll) - scaledY * MathHelper.sin(roll) * 0.22f;
        float rolledY = scaledX * MathHelper.sin(roll) * 0.34f + scaledY * MathHelper.cos(roll);

        float localSide = side * (pose.sideOffset + rolledX * MathHelper.cos(openRad));
        float localUp = pose.anchorY + pose.sideYOffset + rolledY * MathHelper.cos(pitch);
        float localBack = pose.anchorBack + pose.sideBackOffset
                + Math.abs(rolledX) * MathHelper.sin(openRad) * 0.35f
                - rolledY * MathHelper.sin(pitch) * 0.12f
                + layerBackOffset;

        float posePitch = (float) Math.toRadians(pose.pitchRotation);
        float poseRoll = (float) Math.toRadians(pose.rollRotation);

        float pitchedUp = localUp * MathHelper.cos(posePitch) - localBack * MathHelper.sin(posePitch);
        float pitchedBack = localUp * MathHelper.sin(posePitch) + localBack * MathHelper.cos(posePitch);
        float rolledSide = localSide * MathHelper.cos(poseRoll) - pitchedUp * MathHelper.sin(poseRoll);
        float rolledUp = localSide * MathHelper.sin(poseRoll) + pitchedUp * MathHelper.cos(poseRoll);

        return transformLocal(origin, bodyYaw,
                rolledSide * wingScale,
                (pose.preTranslateY + rolledUp) * wingScale,
                (pose.preTranslateBack + pitchedBack) * wingScale);
    }

    private Vec3d transformLocal(Vec3d origin, float bodyYaw, float side, float up, float back) {
        float yawRad = (float) Math.toRadians(-bodyYaw + 90.0f);
        double rightX = Math.sin(yawRad);
        double rightZ = Math.cos(yawRad);
        double forwardX = Math.sin(yawRad + Math.PI / 2.0);
        double forwardZ = Math.cos(yawRad + Math.PI / 2.0);

        return origin.add(rightX * side + forwardX * back, up, rightZ * side + forwardZ * back);
    }

    private void drawTriangle(Vec3d root, Vec3d current, Vec3d next, int rootColor, int currentColor, int nextColor) {
        if (throughWalls.isValue()) {
            Render3D.drawGradientQuadNoDepth(root, current, next, root, rootColor, currentColor, nextColor, rootColor);
        } else {
            Render3D.drawGradientQuad(root, current, next, root, rootColor, currentColor, nextColor, rootColor, false);
        }
    }

    private void drawLine(Vec3d start, Vec3d end, int startColor, int endColor, float width) {
        if (throughWalls.isValue()) {
            Render3D.drawLineGradientNoDepth(start, end, startColor, endColor, width);
        } else {
            Render3D.drawLineGradient(start, end, startColor, endColor, width, false);
        }
    }

    private boolean hasElytra(PlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
    }

    private WingPose resolvePose(PlayerEntity player, float tickDelta) {
        float pitch = player.getPitch(tickDelta);

        if (player.isGliding()) {
            float flightProgress = MathHelper.clamp((player.age + tickDelta) / 10f, 0f, 1f);
            float pitchRotation = flightProgress * (-90f - pitch);
            return new WingPose(0.34f, 0.46f, 0f, 0f, pitchRotation, 0f,
                    0.76f, 0.92f, 0.10f, 0.58f, 0.05f, 0.06f, -0.05f, -5f, -2f, 0.13f);
        }

        if (player.isTouchingWater()) {
            return null;
        }

        if (player.isSneaking()) {
            return new WingPose(0f, 0f, 0.96f, 0.10f, 18f, 0f,
                    1f, 1f, 0.18f, 4.5f, 0.06f, 0f, 0.02f, -11f, -4f, 0.12f);
        }

        return new WingPose(0f, 0f, 1.38f, 0.10f, 0f, 0f,
                1f, 1f, 0.18f, 4.5f, 0.06f, 0f, 0.02f, -11f, -4f, 0.12f);
    }

    private float resolveBodyYaw(PlayerEntity player, float tickDelta) {
        float target = MathHelper.lerpAngleDegrees(tickDelta, player.lastBodyYaw, player.bodyYaw);
        if (player != mc.player) {
            return target;
        }

        if (!selfBodyYawInitialized || player.age < 2) {
            selfBodyYaw = target;
            selfBodyYawInitialized = true;
            return selfBodyYaw;
        }

        selfBodyYaw = approachDegrees(selfBodyYaw, target, 14f);
        return selfBodyYaw;
    }

    private float approachDegrees(float current, float target, float maxDelta) {
        float delta = MathHelper.wrapDegrees(target - current);
        delta = MathHelper.clamp(delta, -maxDelta, maxDelta);
        return current + delta;
    }

    private int applyPointAlpha(int value, float multiplier) {
        int alpha = (value >> 24) & 0xFF;
        int resultAlpha = (int) (alpha * MathHelper.clamp(multiplier, 0.28f, 1f));
        return (value & 0x00FFFFFF) | (resultAlpha << 24);
    }

    private int withAlpha(int value, float alphaMultiplier) {
        float settingAlpha = transparency.getValue();
        int alpha = (int) (MathHelper.clamp(alphaMultiplier * settingAlpha, 0f, 1f) * 255f);
        return (value & 0x00FFFFFF) | (alpha << 24);
    }

    private record WingPoint(float x, float y, float alphaMul) {
    }

    private record WingPose(float preTranslateY, float preTranslateBack,
                            float anchorY, float anchorBack,
                            float pitchRotation, float rollRotation,
                            float openMultiplier, float scaleMultiplier,
                            float motionSpreadBoost, float flapAmplitude,
                            float sideOffset, float sideYOffset, float sideBackOffset,
                            float sideRoll, float sidePitch, float flapSpeed) {
    }
}

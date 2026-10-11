package neverlose.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import neverlose.modules.impl.combat.AntiBot;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.ColorSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.ColorUtil;
import neverlose.util.render.сliemtpipeline.ClientPipelines;
import neverlose.util.repository.friend.FriendUtils;
import org.joml.Matrix4f;

import java.awt.Color;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkeletonESP extends ModuleStructure {
    static final float MAX_DISTANCE = 128.0f;
    static SkeletonESP instance;

    final BooleanSetting players = new BooleanSetting("Игроки", "Показывать скелет игроков")
            .setValue(true);

    final BooleanSetting friends = new BooleanSetting("Друзья", "Показывать скелет друзей")
            .setValue(true);

    final BooleanSetting showSelf = new BooleanSetting("Себя", "Показывать скелет своего игрока")
            .setValue(true);

    final BooleanSetting showInvisible = new BooleanSetting("Невидимые", "Показывать невидимых игроков")
            .setValue(false);

    final BooleanSetting ignoreBots = new BooleanSetting("Фильтр ботов", "Не показывать игроков, которых AntiBot считает ботами")
            .setValue(false);

    final SliderSettings lineWidth = new SliderSettings("Толщина", "Толщина линий скелета")
            .range(0.5f, 5.0f)
            .setValue(2.0f);

    final ColorSetting color = new ColorSetting("Цвет", "Цвет скелета игроков")
            .value(ColorSetting.DEFAULT_COLOR);

    final ColorSetting friendColor = new ColorSetting("Цвет друзей", "Цвет скелета друзей")
            .value(new Color(82, 255, 154, 255).getRGB());

    public SkeletonESP() {
        super("SkeletonESP", "Skeleton ESP", ModuleCategory.RENDER);
        instance = this;
        settings(players, friends, showSelf, showInvisible, ignoreBots, lineWidth, color, friendColor);
    }

    public static SkeletonESP getInstance() {
        return instance;
    }

    public void render(MatrixStack matrices, VertexConsumerProvider provider, Vec3d cameraPos, float tickDelta) {
        if (!isState() || mc.world == null || mc.player == null) {
            return;
        }

        VertexConsumer buffer = provider.getBuffer(ClientPipelines.WORLD_DEBUG_LINES_NO_DEPTH);
        AntiBot antiBot = AntiBot.getInstance();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!shouldRender(player, antiBot)) {
                continue;
            }

            renderSkeleton(player, tickDelta, getColor(player), matrices, buffer, cameraPos);
        }
    }

    private boolean shouldRender(PlayerEntity player, AntiBot antiBot) {
        if (player == null || !player.isAlive() || player.age < 3) {
            return false;
        }

        if (player != mc.player && mc.player.distanceTo(player) > MAX_DISTANCE) {
            return false;
        }

        if (player == mc.player) {
            return showSelf.isValue() && !mc.options.getPerspective().isFirstPerson();
        }

        if (player.isInvisible() && !showInvisible.isValue()) {
            return false;
        }

        if (ignoreBots.isValue() && antiBot != null && antiBot.isState() && antiBot.isBot(player)) {
            return false;
        }

        boolean friend = FriendUtils.isFriend(player);
        return friend ? friends.isValue() : players.isValue();
    }

    private void renderSkeleton(PlayerEntity player, float tickDelta, int skeletonColor, MatrixStack matrices, VertexConsumer buffer, Vec3d cameraPos) {
        Vec3d pos = player.getLerpedPos(tickDelta);
        float width = lineWidth.getValue();

        float limbSwing = player.limbAnimator.getAnimationProgress(tickDelta);
        float limbSwingAmount = MathHelper.clamp(player.limbAnimator.getAmplitude(tickDelta), 0.0f, 1.0f);
        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, player.lastBodyYaw, player.bodyYaw);
        float bodyYawRad = (float) Math.toRadians(-bodyYaw + 90.0f);

        double rightX = Math.sin(bodyYawRad);
        double rightZ = Math.cos(bodyYawRad);
        double forwardX = Math.sin(bodyYawRad + Math.PI / 2.0);
        double forwardZ = Math.cos(bodyYawRad + Math.PI / 2.0);

        float sneakOffset = player.isSneaking() ? 0.20f : 0.0f;
        float heightScale = player.isBaby() ? 0.55f : 1.0f;
        double baseY = pos.y;

        Vec3d head = new Vec3d(pos.x, baseY + (1.62f - sneakOffset) * heightScale, pos.z);
        Vec3d neck = new Vec3d(pos.x, baseY + (1.40f - sneakOffset) * heightScale, pos.z);
        Vec3d body = new Vec3d(pos.x, baseY + (0.90f - sneakOffset) * heightScale, pos.z);
        Vec3d pelvis = new Vec3d(pos.x, baseY + (0.60f - sneakOffset) * heightScale, pos.z);

        float rightArmSwing = MathHelper.cos(limbSwing * 0.6662f) * limbSwingAmount;
        float leftArmSwing = MathHelper.cos(limbSwing * 0.6662f + (float) Math.PI) * limbSwingAmount;
        float rightLegSwing = MathHelper.cos(limbSwing * 0.6662f + (float) Math.PI) * 1.4f * limbSwingAmount;
        float leftLegSwing = MathHelper.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;

        if (player.handSwingProgress > 0.0f) {
            rightArmSwing -= MathHelper.sin(player.handSwingProgress * (float) Math.PI) * 0.65f;
        }

        Vec3d rightShoulder = neck.add(rightX * 0.30, -0.10 * heightScale, rightZ * 0.30);
        Vec3d leftShoulder = neck.add(-rightX * 0.30, -0.10 * heightScale, -rightZ * 0.30);
        Vec3d rightElbow = rightShoulder.add(rightX * 0.10 + forwardX * rightArmSwing * 0.30,
                (-0.30 - Math.abs(rightArmSwing) * 0.15) * heightScale,
                rightZ * 0.10 + forwardZ * rightArmSwing * 0.30);
        Vec3d leftElbow = leftShoulder.add(-rightX * 0.10 + forwardX * leftArmSwing * 0.30,
                (-0.30 - Math.abs(leftArmSwing) * 0.15) * heightScale,
                -rightZ * 0.10 + forwardZ * leftArmSwing * 0.30);
        Vec3d rightHand = rightElbow.add(forwardX * rightArmSwing * 0.20,
                (-0.30 - Math.abs(rightArmSwing) * 0.10) * heightScale,
                forwardZ * rightArmSwing * 0.20);
        Vec3d leftHand = leftElbow.add(forwardX * leftArmSwing * 0.20,
                (-0.30 - Math.abs(leftArmSwing) * 0.10) * heightScale,
                forwardZ * leftArmSwing * 0.20);

        Vec3d rightHip = pelvis.add(rightX * 0.15, 0.0, rightZ * 0.15);
        Vec3d leftHip = pelvis.add(-rightX * 0.15, 0.0, -rightZ * 0.15);
        Vec3d rightKnee = rightHip.add(forwardX * rightLegSwing * 0.20,
                (-0.40 + Math.max(0.0f, rightLegSwing) * 0.10) * heightScale,
                forwardZ * rightLegSwing * 0.20);
        Vec3d leftKnee = leftHip.add(forwardX * leftLegSwing * 0.20,
                (-0.40 + Math.max(0.0f, leftLegSwing) * 0.10) * heightScale,
                forwardZ * leftLegSwing * 0.20);
        Vec3d rightFoot = rightKnee.add(forwardX * rightLegSwing * 0.15,
                (-0.40 - Math.max(0.0f, -rightLegSwing) * 0.10) * heightScale,
                forwardZ * rightLegSwing * 0.15);
        Vec3d leftFoot = leftKnee.add(forwardX * leftLegSwing * 0.15,
                (-0.40 - Math.max(0.0f, -leftLegSwing) * 0.10) * heightScale,
                forwardZ * leftLegSwing * 0.15);

        drawBone(matrices, buffer, cameraPos, head, neck, skeletonColor, width);
        drawBone(matrices, buffer, cameraPos, neck, body, skeletonColor, width);
        drawBone(matrices, buffer, cameraPos, body, pelvis, skeletonColor, width);
        drawBone(matrices, buffer, cameraPos, rightShoulder, leftShoulder, skeletonColor, width);
        drawBone(matrices, buffer, cameraPos, rightShoulder, rightElbow, skeletonColor, width);
        drawBone(matrices, buffer, cameraPos, rightElbow, rightHand, skeletonColor, width);
        drawBone(matrices, buffer, cameraPos, leftShoulder, leftElbow, skeletonColor, width);
        drawBone(matrices, buffer, cameraPos, leftElbow, leftHand, skeletonColor, width);
        drawBone(matrices, buffer, cameraPos, rightHip, leftHip, skeletonColor, width);
        drawBone(matrices, buffer, cameraPos, rightHip, rightKnee, skeletonColor, width);
        drawBone(matrices, buffer, cameraPos, rightKnee, rightFoot, skeletonColor, width);
        drawBone(matrices, buffer, cameraPos, leftHip, leftKnee, skeletonColor, width);
        drawBone(matrices, buffer, cameraPos, leftKnee, leftFoot, skeletonColor, width);
    }

    private int getColor(PlayerEntity player) {
        int result = FriendUtils.isFriend(player) ? friendColor.getColor() : color.getColor();
        if (player.isInvisible()) {
            result = ColorUtil.multAlpha(result, 0.55f);
        }
        if (player.hurtTime > 0) {
            float red = MathHelper.clamp(player.hurtTime / 10.0f, 0.0f, 1.0f);
            result = ColorUtil.multRed(result, 1.0f + red * 3.5f);
        }
        return result;
    }

    private void drawBone(MatrixStack matrices, VertexConsumer buffer, Vec3d cameraPos, Vec3d start, Vec3d end, int color, float width) {
        int glowColor = ColorUtil.multAlpha(color, 0.22f);
        drawLine(matrices, buffer, cameraPos, start, end, glowColor, width * 2.35f);
        drawLine(matrices, buffer, cameraPos, start, end, color, width);
    }

    private void drawLine(MatrixStack matrices, VertexConsumer buffer, Vec3d cameraPos, Vec3d start, Vec3d end, int color, float width) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float x1 = (float) (start.x - cameraPos.x);
        float y1 = (float) (start.y - cameraPos.y);
        float z1 = (float) (start.z - cameraPos.z);
        float x2 = (float) (end.x - cameraPos.x);
        float y2 = (float) (end.y - cameraPos.y);
        float z2 = (float) (end.z - cameraPos.z);

        buffer.vertex(matrix, x1, y1, z1).color(color);
        buffer.vertex(matrix, x2, y2, z2).color(color);
    }
}

package neverlose.modules.impl.render;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.TickEvent;
import neverlose.events.impl.WorldRenderEvent;
import neverlose.modules.impl.combat.Aura;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.repository.friend.FriendUtils;
import neverlose.util.render.Render3D;
import neverlose.util.render.сliemtpipeline.ClientPipelines;
import org.joml.Matrix4f;

import java.util.concurrent.ThreadLocalRandom;

public class RichDog extends ModuleStructure {
    private static final Identifier TAKSA_TEXTURE = Identifier.of("neverlose", "textures/richidog/taksa.png");
    private static final Identifier DJ_TEXTURE = Identifier.of("neverlose", "textures/richidog/djekrussel.png");
    private static final float TEX_WIDTH = 60.0f;
    private static final float TEX_HEIGHT = 36.0f;
    private static final float MODEL_SCALE = 1.0f / 16.0f;
    private static final float MODEL_GROUND_Y = 21.0f;
    private static final int LEAP_DURATION = 12;
    private static final double SOFT_LEASH_DISTANCE = 4.8;
    private static final double HARD_LEASH_DISTANCE = 9.5;

    private final SelectSetting variant = new SelectSetting("Variant", "Pet skin variant")
            .value("Taksa", "DJ")
            .selected("Taksa");

    private final SliderSettings followDistance = new SliderSettings("Distance", "Follow distance")
            .range(0.7f, 2.6f)
            .setValue(1.25f);

    private Vec3d pos;
    private Vec3d prevPos;
    private Vec3d velocity = Vec3d.ZERO;
    private float bodyYaw;
    private float ownerYaw;
    private float headYaw;
    private float headPitch;
    private float limbSwing;
    private float limbSwingAmount;
    private float prevLimbSwingAmount;
    private float wanderYaw;
    private Vec3d lastPlayerPos;
    private Vec3d playerDelta = Vec3d.ZERO;
    private int wanderTicks;
    private int idleTicks;
    private int biteTicks;
    private int biteCooldown;
    private int leapTicks;
    private int leapCooldown;
    private int attackSideTicks;
    private int attackSide = 1;
    private float jumpOffset;
    private float prevJumpOffset;

    public RichDog() {
        super("RichDog", "Small pet dog near the player", ModuleCategory.RENDER);
        settings(variant, followDistance);
    }

    @Override
    public void activate() {
        reset();
    }

    @Override
    public void deactivate() {
        reset();
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (!isState() || mc.player == null || mc.world == null) {
            reset();
            return;
        }

        updateOwnerYaw();
        LivingEntity targetEntity = getAuraTarget();
        Vec3d followTarget = getFollowTarget();
        Vec3d target = targetEntity == null ? followTarget : getAttackTarget(targetEntity);

        if (pos == null || pos.distanceTo(mc.player.getEntityPos()) > 14.0 || pos.distanceTo(target) > 18.0) {
            pos = settleOnGround(followTarget);
            prevPos = pos;
            velocity = Vec3d.ZERO;
            bodyYaw = ownerYaw;
            jumpOffset = 0.0f;
            prevJumpOffset = 0.0f;
            return;
        }

        prevPos = pos;
        prevJumpOffset = jumpOffset;
        tickBite(targetEntity);
        if (targetEntity != null && pos.distanceTo(mc.player.getEntityPos()) > SOFT_LEASH_DISTANCE) {
            target = followTarget;
        }

        Vec3d diff = target.subtract(pos);
        double distance = diff.horizontalLength();
        double ownerDistance = pos.distanceTo(mc.player.getEntityPos());
        double catchUp = MathHelper.clamp((ownerDistance - followDistance.getValue()) * 0.055, 0.0, targetEntity == null ? 0.22 : 0.12);
        double maxSpeed = (targetEntity == null ? 0.23 : 0.34) + catchUp + (leapTicks > 0 ? 0.16 : 0.0);
        double pull = targetEntity == null ? 0.72 : 0.9;
        Vec3d desired = distance > 0.06
                ? new Vec3d(diff.x, 0.0, diff.z).normalize().multiply(Math.min(maxSpeed, distance * pull))
                : Vec3d.ZERO;

        velocity = velocity.multiply(0.42, 0.0, 0.42).add(desired.multiply(targetEntity == null ? 0.68 : 0.78));
        pos = moveWithCollision(pos, velocity);
        updateJumpOffset();

        if (pos.distanceTo(mc.player.getEntityPos()) > HARD_LEASH_DISTANCE) {
            snapToOwner(followTarget);
        }

        updatePose(targetEntity);
        updateLimbs();
    }

    @EventHandler
    private void onWorldRender(WorldRenderEvent event) {
        if (!isState() || mc.player == null || mc.world == null || pos == null) {
            return;
        }

        float tickDelta = event.getPartialTicks();
        Vec3d renderPos = prevPos == null ? pos : prevPos.lerp(pos, tickDelta);
        float jump = MathHelper.lerp(tickDelta, prevJumpOffset, jumpOffset);
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer consumer = immediate.getBuffer(ClientPipelines.COSMETIC_MODEL.apply(currentTexture()));
        renderDog(event.getStack(), consumer, renderPos.add(0.0, jump, 0.0), tickDelta);
        immediate.draw();
    }

    private LivingEntity getAuraTarget() {
        LivingEntity target = Aura.target;
        if (!isValidTarget(target, 13.0f)) {
            Aura aura = Aura.getInstance();
            target = aura != null && aura.isState() ? aura.lastTarget : null;
        }
        if (isValidTarget(target, 13.0f)) {
            return target;
        }

        Aura aura = Aura.getInstance();
        if (aura == null || !aura.isState() || mc.world == null) {
            return null;
        }

        LivingEntity nearest = null;
        double nearestDistance = 9.0;
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || !isValidTarget(living, 9.0f)) {
                continue;
            }

            double distance = mc.player.distanceTo(living);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = living;
            }
        }
        return nearest;
    }

    private boolean isValidTarget(LivingEntity target, float maxDistance) {
        if (target == null || target == mc.player || !target.isAlive() || mc.player == null) {
            return false;
        }
        if (target instanceof PlayerEntity player && FriendUtils.isFriend(player)) {
            return false;
        }
        return mc.player.distanceTo(target) <= maxDistance;
    }

    private void updateOwnerYaw() {
        Vec3d currentPlayerPos = mc.player.getEntityPos();
        if (Float.isNaN(ownerYaw)) {
            ownerYaw = mc.player.bodyYaw;
        }

        playerDelta = lastPlayerPos == null ? Vec3d.ZERO : currentPlayerPos.subtract(lastPlayerPos);
        lastPlayerPos = currentPlayerPos;

        if (playerDelta.horizontalLengthSquared() > 0.0006) {
            ownerYaw = (float) Math.toDegrees(Math.atan2(playerDelta.z, playerDelta.x)) - 90.0f;
        }
    }

    private Vec3d getFollowTarget() {
        double distanceToOwner = pos == null ? 999.0 : pos.distanceTo(mc.player.getEntityPos());
        boolean ownerMoving = playerDelta.horizontalLengthSquared() > 0.0006;
        if (ownerMoving || distanceToOwner > followDistance.getValue() + 0.45f) {
            idleTicks = 0;
            return getOwnerFollowSlot();
        }

        if (wanderTicks-- <= 0 || pos == null || distanceToOwner < 0.8) {
            wanderYaw = ownerYaw + ThreadLocalRandom.current().nextFloat(-65.0f, 65.0f);
            wanderTicks = ThreadLocalRandom.current().nextInt(45, 95);
        }

        if (idleTicks++ > 22) {
            float yaw = (float) Math.toRadians(wanderYaw + 180.0f);
            double radius = MathHelper.clamp(followDistance.getValue() * 0.62f, 0.65f, 1.35f);
            return mc.player.getEntityPos().add(-Math.sin(yaw) * radius, 0.0, Math.cos(yaw) * radius);
        }

        return getOwnerFollowSlot();
    }

    private Vec3d getOwnerFollowSlot() {
        float yaw = (float) Math.toRadians(ownerYaw);
        Vec3d back = new Vec3d(Math.sin(yaw), 0.0, -Math.cos(yaw)).multiply(followDistance.getValue());
        Vec3d side = new Vec3d(Math.cos(yaw), 0.0, Math.sin(yaw)).multiply(0.55);
        return mc.player.getEntityPos().add(back).add(side);
    }

    private Vec3d getAttackTarget(LivingEntity target) {
        Vec3d targetPos = target.getEntityPos();
        Vec3d fromTarget = pos == null ? mc.player.getEntityPos().subtract(targetPos) : pos.subtract(targetPos);
        if (fromTarget.horizontalLengthSquared() < 0.01) {
            fromTarget = mc.player.getEntityPos().subtract(targetPos);
        }
        if (fromTarget.horizontalLengthSquared() < 0.01) {
            fromTarget = Vec3d.fromPolar(0.0f, bodyYaw);
        }

        fromTarget = new Vec3d(fromTarget.x, 0.0, fromTarget.z).normalize();
        double distance = pos == null ? 999.0 : pos.distanceTo(targetPos);
        Vec3d side = new Vec3d(-fromTarget.z, 0.0, fromTarget.x).multiply(attackSide * 0.38);
        double radius = leapTicks > 0 ? 0.28 : distance > 1.15 ? 0.48 : 0.68;
        return targetPos.add(fromTarget.multiply(radius)).add(side);
    }

    private void tickBite(LivingEntity target) {
        if (target != null && --attackSideTicks <= 0) {
            attackSide = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
            attackSideTicks = ThreadLocalRandom.current().nextInt(24, 46);
        }
        if (biteCooldown > 0) {
            biteCooldown--;
        }
        if (biteTicks > 0) {
            biteTicks--;
        }
        if (leapCooldown > 0) {
            leapCooldown--;
        }
        if (leapTicks > 0) {
            leapTicks--;
        }

        if (target == null || pos == null) {
            return;
        }

        Vec3d targetPos = target.getEntityPos();
        double distance = pos.distanceTo(targetPos);
        Vec3d lunge = targetPos.subtract(pos);

        if (distance <= 2.7 && leapCooldown <= 0 && lunge.horizontalLengthSquared() > 0.01) {
            leapTicks = LEAP_DURATION;
            leapCooldown = 22;
            velocity = velocity.add(new Vec3d(lunge.x, 0.0, lunge.z).normalize().multiply(0.22));
        }

        if (distance <= 1.05 && biteCooldown <= 0) {
            biteTicks = 9;
            biteCooldown = 18;
            if (lunge.horizontalLengthSquared() > 0.01) {
                velocity = velocity.add(new Vec3d(lunge.x, 0.0, lunge.z).normalize().multiply(0.13));
            }
        }
    }

    private void updateJumpOffset() {
        if (leapTicks > 0) {
            float phase = (LEAP_DURATION - leapTicks) / (float) LEAP_DURATION;
            float arc = MathHelper.sin(phase * (float) Math.PI) * 0.46f;
            jumpOffset += (arc - jumpOffset) * 0.58f;
            return;
        }

        jumpOffset *= 0.48f;
        if (jumpOffset < 0.01f) {
            jumpOffset = 0.0f;
        }
    }

    private void snapToOwner(Vec3d followTarget) {
        pos = settleOnGround(followTarget);
        prevPos = pos;
        velocity = Vec3d.ZERO;
        jumpOffset = 0.0f;
        prevJumpOffset = 0.0f;
    }

    private Vec3d moveWithCollision(Vec3d current, Vec3d motion) {
        Vec3d horizontal = new Vec3d(current.x + motion.x, current.y, current.z + motion.z);
        Vec3d resolved = resolveHorizontal(current, horizontal);
        double ground = findGroundY(resolved);
        double deltaY = ground - current.y;
        double y = deltaY > 0.72 ? current.y : MathHelper.lerp(0.58, current.y, ground);
        if (deltaY < -1.35) {
            y = current.y - 0.18;
        }

        Vec3d next = new Vec3d(resolved.x, y, resolved.z);
        if (!canOccupy(next)) {
            Vec3d stepped = new Vec3d(resolved.x, current.y + 0.55, resolved.z);
            if (canOccupy(stepped)) {
                double stepGround = findGroundY(stepped);
                if (stepGround - current.y <= 0.75) {
                    next = new Vec3d(stepped.x, MathHelper.lerp(0.65, current.y, stepGround), stepped.z);
                }
            }
        }

        if (!canOccupy(next)) {
            velocity = Vec3d.ZERO;
            return current;
        }
        return next;
    }

    private Vec3d resolveHorizontal(Vec3d current, Vec3d next) {
        if (canOccupy(next)) {
            return next;
        }

        Vec3d xOnly = new Vec3d(next.x, current.y, current.z);
        if (canOccupy(xOnly)) {
            return xOnly;
        }

        Vec3d zOnly = new Vec3d(current.x, current.y, next.z);
        if (canOccupy(zOnly)) {
            return zOnly;
        }

        Vec3d step = new Vec3d(next.x, current.y + 0.55, next.z);
        if (canOccupy(step)) {
            return step;
        }

        return current;
    }

    private boolean canOccupy(Vec3d point) {
        if (mc.world == null || mc.player == null) {
            return false;
        }
        return mc.world.isSpaceEmpty(mc.player, dogBox(point));
    }

    private Box dogBox(Vec3d point) {
        return new Box(point.x - 0.28, point.y + 0.02, point.z - 0.28,
                point.x + 0.28, point.y + 0.78, point.z + 0.28);
    }

    private Vec3d settleOnGround(Vec3d sample) {
        return new Vec3d(sample.x, findGroundY(sample), sample.z);
    }

    private double findGroundY(Vec3d sample) {
        if (mc.world == null) {
            return sample.y;
        }

        double startY = sample.y + 0.85;
        for (int i = 0; i < 36; i++) {
            double y = startY - i * 0.12;
            BlockPos blockPos = BlockPos.ofFloored(sample.x, y, sample.z);
            if (!mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty()) {
                return blockPos.getY() + 1.02;
            }
        }
        return sample.y;
    }

    private void updatePose(LivingEntity target) {
        Vec3d look = target == null
                ? mc.player.getEntityPos().add(0.0, mc.player.getEyeHeight(mc.player.getPose()) * 0.72, 0.0)
                : target.getBoundingBox().getCenter();

        if (velocity.horizontalLengthSquared() > 0.0008) {
            float desiredYaw = (float) Math.toDegrees(Math.atan2(velocity.z, velocity.x)) - 90.0f;
            bodyYaw = MathHelper.lerpAngleDegrees(target == null ? 0.28f : 0.44f, bodyYaw, desiredYaw);
        } else if (target != null && pos != null) {
            bodyYaw = MathHelper.lerpAngleDegrees(0.35f, bodyYaw, yawTo(pos, look));
        }

        if (pos != null) {
            float lookYaw = yawTo(pos.add(0.0, 0.42, 0.0), look);
            float lookPitch = pitchTo(pos.add(0.0, 0.42, 0.0), look);
            headYaw = MathHelper.lerpAngleDegrees(0.35f, headYaw, MathHelper.clamp(MathHelper.wrapDegrees(lookYaw - bodyYaw), -55.0f, 55.0f));
            headPitch = MathHelper.lerp(0.35f, headPitch, MathHelper.clamp(lookPitch, -35.0f, 28.0f));
        }
    }

    private void updateLimbs() {
        prevLimbSwingAmount = limbSwingAmount;
        float targetAmount = (float) MathHelper.clamp(velocity.horizontalLength() * 8.0, 0.0, 1.0);
        limbSwingAmount += (targetAmount - limbSwingAmount) * 0.36f;
        limbSwing += limbSwingAmount * 0.88f;
    }

    private static float yawTo(Vec3d from, Vec3d to) {
        return (float) Math.toDegrees(Math.atan2(to.z - from.z, to.x - from.x)) - 90.0f;
    }

    private static float pitchTo(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        return (float) -Math.toDegrees(Math.atan2(dy, horizontal));
    }

    private Identifier currentTexture() {
        return variant.isSelected("DJ") ? DJ_TEXTURE : TAKSA_TEXTURE;
    }

    private void renderDog(MatrixStack stack, VertexConsumer consumer, Vec3d base, float tickDelta) {
        float yaw = (float) Math.toRadians(bodyYaw);
        Vec3d forward = new Vec3d(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3d right = new Vec3d(Math.cos(yaw), 0.0, Math.sin(yaw));
        Vec3d up = new Vec3d(0.0, 1.0, 0.0);
        ModelPose pose = new ModelPose(base, right, up, forward);

        float limbAmount = MathHelper.lerp(tickDelta, prevLimbSwingAmount, limbSwingAmount);
        float frontLeft = MathHelper.cos(limbSwing * 0.6662f) * 1.4f * limbAmount;
        float frontRight = MathHelper.cos(limbSwing * 0.6662f + (float) Math.PI) * 1.4f * limbAmount;
        float backLeft = frontRight;
        float backRight = frontLeft;
        float bite = biteTicks > 0 ? (float) Math.sin((biteTicks / 9.0f) * Math.PI) : 0.0f;
        float animatedHeadPitch = (float) Math.toRadians(headPitch - bite * 18.0f);
        float animatedHeadYaw = (float) Math.toRadians(headYaw);

        drawModelBox(stack, consumer, pose, 0.0f, 10.5f, -6.8f, animatedHeadPitch, animatedHeadYaw, 0.0f,
                -3.0f, -3.0f, -4.0f, 6.0f, 6.0f, 4.0f, 0.0f, 0.0f);
        drawModelBox(stack, consumer, pose, 0.0f, 10.5f, -6.8f, animatedHeadPitch, animatedHeadYaw, 0.0f,
                -1.5f, 0.0f, -7.0f, 3.0f, 3.0f, 3.0f, 21.0f, 0.0f);
        drawModelBox(stack, consumer, pose, 0.0f, 10.5f, -6.8f, animatedHeadPitch, animatedHeadYaw, 0.0f,
                3.0f, -2.0f, -3.5f, 1.0f, 3.0f, 3.0f, 32.0f, 4.0f);
        drawModelBox(stack, consumer, pose, 0.0f, 10.5f, -6.8f, animatedHeadPitch, animatedHeadYaw, 0.0f,
                3.0f, -2.5f, -2.75f, 1.0f, 1.0f, 1.5f, 34.0f, 1.0f);
        drawModelBox(stack, consumer, pose, 0.0f, 10.5f, -6.8f, animatedHeadPitch, animatedHeadYaw, 0.0f,
                -4.0f, -2.0f, -3.5f, 1.0f, 3.0f, 3.0f, 32.0f, 4.0f);
        drawModelBox(stack, consumer, pose, 0.0f, 10.5f, -6.8f, animatedHeadPitch, animatedHeadYaw, 0.0f,
                -4.0f, -2.5f, -2.75f, 1.0f, 1.0f, 1.5f, 34.0f, 1.0f);

        if (bite > 0.05f) {
            drawModelBox(stack, consumer, pose, 0.0f, 10.5f, -6.8f, animatedHeadPitch + bite * 0.22f, animatedHeadYaw, 0.0f,
                    -1.25f, 2.15f, -6.85f, 2.5f, 0.55f, 2.0f, 21.0f, 4.0f);
        }

        drawModelBox(stack, consumer, pose, 0.0f, 10.5f, -5.0f, (float) Math.toRadians(-25.0f), 0.0f, 0.0f,
                -2.95f, -1.0f, -4.0f, 5.9f, 5.0f, 6.0f, 15.0f, 7.0f);
        drawModelBox(stack, consumer, pose, 0.0f, 13.5f, -2.0f, 0.0f, 0.0f, 0.0f,
                -4.0f, -3.5f, -3.0f, 8.0f, 7.0f, 6.0f, 32.0f, 13.0f);
        drawModelBox(stack, consumer, pose, 0.0f, 13.0f, 0.5f, 0.0f, 0.0f, 0.0f,
                -3.0f, -3.0f, -0.5f, 6.0f, 6.0f, 11.0f, 3.0f, 19.0f);

        drawModelBox(stack, consumer, pose, 1.5f, 16.0f, -3.0f, frontLeft, 0.0f, 0.0f,
                -1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f, 42.0f, 0.0f);
        drawModelBox(stack, consumer, pose, -1.5f, 16.0f, -3.0f, frontRight, 0.0f, 0.0f,
                -1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f, 42.0f, 0.0f);
        drawModelBox(stack, consumer, pose, 1.5f, 16.0f, 9.0f, backLeft, 0.0f, 0.0f,
                -1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f, 52.0f, 0.0f);
        drawModelBox(stack, consumer, pose, -1.5f, 16.0f, 9.0f, backRight, 0.0f, 0.0f,
                -1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f, 52.0f, 0.0f);

        float tailWag = (float) Math.cos((mc.player.age + tickDelta) * 0.42f) * 0.38f;
        drawModelBox(stack, consumer, pose, 0.0f, 9.0f, 10.0f, (float) Math.toRadians(biteTicks > 0 ? 55.0f : 26.0f), 0.0f, tailWag,
                -1.0f, 2.0f, -1.0f, 2.0f, 8.0f, 2.0f, 2.0f, 12.0f);
    }

    private void drawModelBox(MatrixStack stack, VertexConsumer consumer, ModelPose pose,
                              float originX, float originY, float originZ, float pitch, float yaw, float roll,
                              float x, float y, float z, float sizeX, float sizeY, float sizeZ,
                              float texU, float texV) {
        Vec3d p000 = toWorld(pose, originX, originY, originZ, x, y, z, pitch, yaw, roll);
        Vec3d p100 = toWorld(pose, originX, originY, originZ, x + sizeX, y, z, pitch, yaw, roll);
        Vec3d p110 = toWorld(pose, originX, originY, originZ, x + sizeX, y + sizeY, z, pitch, yaw, roll);
        Vec3d p010 = toWorld(pose, originX, originY, originZ, x, y + sizeY, z, pitch, yaw, roll);
        Vec3d p001 = toWorld(pose, originX, originY, originZ, x, y, z + sizeZ, pitch, yaw, roll);
        Vec3d p101 = toWorld(pose, originX, originY, originZ, x + sizeX, y, z + sizeZ, pitch, yaw, roll);
        Vec3d p111 = toWorld(pose, originX, originY, originZ, x + sizeX, y + sizeY, z + sizeZ, pitch, yaw, roll);
        Vec3d p011 = toWorld(pose, originX, originY, originZ, x, y + sizeY, z + sizeZ, pitch, yaw, roll);

        Matrix4f matrix = stack.peek().getPositionMatrix();
        Vec3d camera = Render3D.lastCameraPos;
        int color = 0xFFFFFFFF;
        float u0 = texU;
        float u1 = texU + sizeZ;
        float u2 = u1 + sizeX;
        float u3 = u2 + sizeZ;
        float u4 = u3 + sizeX;
        float v0 = texV;
        float v1 = texV + sizeZ;
        float v2 = v1 + sizeY;

        texturedQuad(consumer, matrix, camera, p101, p100, p110, p111, u2, v1, u3, v1, u3, v2, u2, v2, color);
        texturedQuad(consumer, matrix, camera, p000, p001, p011, p010, u0, v1, u1, v1, u1, v2, u0, v2, color);
        texturedQuad(consumer, matrix, camera, p001, p101, p100, p000, u1, v0, u2, v0, u2, v1, u1, v1, color);
        texturedQuad(consumer, matrix, camera, p010, p110, p111, p011, u2, v0, u2 + sizeX, v0, u2 + sizeX, v1, u2, v1, color);
        texturedQuad(consumer, matrix, camera, p100, p000, p010, p110, u1, v1, u2, v1, u2, v2, u1, v2, color);
        texturedQuad(consumer, matrix, camera, p001, p101, p111, p011, u3, v1, u4, v1, u4, v2, u3, v2, color);
    }

    private Vec3d toWorld(ModelPose pose, float originX, float originY, float originZ,
                          float x, float y, float z, float pitch, float yaw, float roll) {
        Vec3d rotated = rotate(new Vec3d(x, y, z), pitch, yaw, roll);
        Vec3d model = new Vec3d(originX, originY, originZ).add(rotated);
        return pose.base
                .add(pose.right.multiply(model.x * MODEL_SCALE))
                .add(pose.up.multiply((MODEL_GROUND_Y - model.y) * MODEL_SCALE))
                .add(pose.forward.multiply(-model.z * MODEL_SCALE));
    }

    private Vec3d rotate(Vec3d point, float pitch, float yaw, float roll) {
        double x = point.x;
        double y = point.y;
        double z = point.z;

        double cos = Math.cos(pitch);
        double sin = Math.sin(pitch);
        double ny = y * cos - z * sin;
        double nz = y * sin + z * cos;
        y = ny;
        z = nz;

        cos = Math.cos(yaw);
        sin = Math.sin(yaw);
        double nx = x * cos + z * sin;
        nz = -x * sin + z * cos;
        x = nx;
        z = nz;

        cos = Math.cos(roll);
        sin = Math.sin(roll);
        nx = x * cos - y * sin;
        ny = x * sin + y * cos;
        return new Vec3d(nx, ny, z);
    }

    private void texturedQuad(VertexConsumer consumer, Matrix4f matrix, Vec3d camera,
                              Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4,
                              float u1, float v1, float u2, float v2, float u3, float v3, float u4, float v4,
                              int color) {
        vertex(consumer, matrix, camera, p1, u1 / TEX_WIDTH, v1 / TEX_HEIGHT, color);
        vertex(consumer, matrix, camera, p2, u2 / TEX_WIDTH, v2 / TEX_HEIGHT, color);
        vertex(consumer, matrix, camera, p3, u3 / TEX_WIDTH, v3 / TEX_HEIGHT, color);
        vertex(consumer, matrix, camera, p4, u4 / TEX_WIDTH, v4 / TEX_HEIGHT, color);
    }

    private void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3d camera, Vec3d p, float u, float v, int color) {
        consumer.vertex(matrix,
                        (float) (p.x - camera.x),
                        (float) (p.y - camera.y),
                        (float) (p.z - camera.z))
                .texture(u, v)
                .color(color);
    }

    private void reset() {
        pos = null;
        prevPos = null;
        velocity = Vec3d.ZERO;
        bodyYaw = 0.0f;
        ownerYaw = Float.NaN;
        headYaw = 0.0f;
        headPitch = 0.0f;
        limbSwing = 0.0f;
        limbSwingAmount = 0.0f;
        prevLimbSwingAmount = 0.0f;
        lastPlayerPos = null;
        playerDelta = Vec3d.ZERO;
        wanderTicks = 0;
        idleTicks = 0;
        biteTicks = 0;
        biteCooldown = 0;
        leapTicks = 0;
        leapCooldown = 0;
        attackSideTicks = 0;
        attackSide = 1;
        jumpOffset = 0.0f;
        prevJumpOffset = 0.0f;
    }

    private record ModelPose(Vec3d base, Vec3d right, Vec3d up, Vec3d forward) {
    }
}

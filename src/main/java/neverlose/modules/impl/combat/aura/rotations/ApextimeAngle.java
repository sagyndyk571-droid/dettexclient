package neverlose.modules.impl.combat.aura.rotations;

import net.minecraft.block.SlabBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import neverlose.debob;
import neverlose.modules.impl.combat.Aura;
import neverlose.modules.impl.combat.aura.Angle;
import neverlose.modules.impl.combat.aura.attack.StrikeManager;
import neverlose.modules.impl.combat.aura.impl.RotateConstructor;
import neverlose.util.math.SensUtil;
import neverlose.util.timer.StopWatch;

import java.util.concurrent.ThreadLocalRandom;

public class ApextimeAngle extends RotateConstructor {

    private static float tick;
    private static int currentPointIndex;
    private static int previousPointIndex;
    private static final StopWatch switchTimer = new StopWatch();
    private static final StopWatch transitionTimer = new StopWatch();
    private static long nextSwitchDelay = ThreadLocalRandom.current().nextLong(1000L, 1600L);
    private static boolean isTransitioning;
    private static final long TRANSITION_DURATION = 1200L;

    private static float[] lastSmoothRandom;
    private static float[] lastSmoothTarget;
    private static long lastSmoothTime;

    public ApextimeAngle() {
        super("Apextime");
    }

    private static Vec3d getBodyPoint(LivingEntity target, BodyPoint point) {
        double x = target.getX();
        double y = target.getY() + (double) (target.getHeight() * point.getHeightMultiplier());
        double z = target.getZ();
        return new Vec3d(x, y, z);
    }

    private static void switchToNextPoint() {
        previousPointIndex = currentPointIndex;
        currentPointIndex = (currentPointIndex + 1) % BodyPoint.values().length;
        nextSwitchDelay = ThreadLocalRandom.current().nextLong(800L, 1400L);
        switchTimer.reset();
        transitionTimer.reset();
        isTransitioning = true;
    }

    private static float randomValue(float min, float max) {
        return min + ThreadLocalRandom.current().nextFloat() * (max - min);
    }

    private static float smoothRandom(float min, float max, float smoothness) {
        long currentTime;
        if (lastSmoothRandom == null) {
            lastSmoothRandom = new float[]{(min + max) / 2.0f};
            lastSmoothTarget = new float[]{randomValue(min, max)};
        }
        if ((double) ((currentTime = System.currentTimeMillis()) - lastSmoothTime) > 250.0 + Math.random() * 300.0) {
            lastSmoothTarget[0] = randomValue(min, max);
            lastSmoothTime = currentTime;
        }
        lastSmoothRandom[0] = lastSmoothRandom[0] + (lastSmoothTarget[0] - lastSmoothRandom[0]) * smoothness;
        return lastSmoothRandom[0];
    }

    private float cooldownFromLastSwing() {
        if (mc.player == null) {
            return 0.0f;
        }
        return MathHelper.clamp(mc.player.getAttackCooldownProgress(0.0f), 0.0f, 1.0f);
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        if (!(entity instanceof LivingEntity target) || mc.player == null) {
            return currentAngle;
        }

        Aura aura = Aura.getInstance();
        StrikeManager attackHandler = debob.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
        boolean isAttack = aura.target != null && aura.target.isAlive() && attackHandler.canAttack(aura.getConfig(), 0);
        float attackDistance = aura.attackrange.getValue();

        float returnPitchSpeed;
        float returnYawSpeed;
        float yawChangeSpeed;
        float pitchChangeSpeed;
        Vec3d targetPoint;

        double distance = mc.player.distanceTo(target);
        float distanceFactor = 1.0f;
        double playerSpeed = Math.hypot(mc.player.getVelocity().x, mc.player.getVelocity().z);
        float speedFactor = 1.0f;
        if (playerSpeed > 0.2) {
            speedFactor = MathHelper.clamp((float) ((float) (1.0 - (playerSpeed - 0.2) * 1.5)), 0.4f, 1.0f);
        }
        float blockHeightFactor = 1.0f;
        if (mc.world != null) {
            BlockPos blockPos = mc.player.getBlockPos();
            var blockState = mc.world.getBlockState(blockPos);
            if (blockState.getBlock() instanceof SlabBlock) {
                blockHeightFactor = 0.5f;
            } else {
                try {
                    VoxelShape shape = blockState.getCollisionShape(mc.world, blockPos);
                    double blockHeight = shape.isEmpty() ? 0.0 : shape.getBoundingBox().maxY;
                    if (blockHeight > 0.0 && blockHeight < 1.0) {
                        blockHeightFactor = MathHelper.clamp((float) ((double) 0.3f + blockHeight * (double) 0.4f), 0.3f, 0.7f);
                    }
                } catch (Exception e) {
                    blockHeightFactor = 1.0f;
                }
            }
        }
        float finalFactor = distanceFactor * speedFactor * blockHeightFactor;
        boolean attack = false;
        boolean isAttackingSoon;
        if (isAttack) {
            tick = 4.0f;
        }
        if (tick > 0.0f) {
            attack = true;
            tick -= 1.0f;
        }
        float cooldownProgress = mc.player.getAttackCooldownProgress(1.5f);
        isAttackingSoon = cooldownProgress >= 0.85f && cooldownProgress < 1.0f;
        float attackAccuracyFactor = 1.0f;
        if (attack || isAttackingSoon) {
            attackAccuracyFactor = 0.3f;
        }
        float finalJitterFactor = finalFactor * attackAccuracyFactor;
        float finalOscillationFactor = finalFactor * (attack || isAttackingSoon ? 0.4f : 1.0f);
        if (switchTimer.finished((double) nextSwitchDelay)) {
            switchToNextPoint();
        }
        if (nextSwitchDelay < 1000L) {
            nextSwitchDelay = ThreadLocalRandom.current().nextLong(1000L, 1600L);
        }
        BodyPoint[] bodyPoints = BodyPoint.values();
        if (isTransitioning && transitionTimer.elapsedTime() < TRANSITION_DURATION) {
            BodyPoint previousPoint = bodyPoints[previousPointIndex];
            BodyPoint currentPoint = bodyPoints[currentPointIndex];
            Vec3d prevPoint = getBodyPoint(target, previousPoint);
            Vec3d currPoint = getBodyPoint(target, currentPoint);
            float progress = MathHelper.clamp((float) ((float) transitionTimer.elapsedTime() / 1200.0f), 0.0f, 1.0f);
            float smoothProgress = progress < 0.5f
                    ? 16.0f * progress * progress * progress * progress * progress
                    : 1.0f - (float) Math.pow(-2.0f * progress + 2.0f, 5.0) / 2.0f;
            targetPoint = new Vec3d(
                    MathHelper.lerp((double) smoothProgress, prevPoint.x, currPoint.x),
                    MathHelper.lerp((double) smoothProgress, prevPoint.y, currPoint.y),
                    MathHelper.lerp((double) smoothProgress, prevPoint.z, currPoint.z));
        } else {
            if (isTransitioning) {
                isTransitioning = false;
            }
            BodyPoint currentPoint = bodyPoints[currentPointIndex];
            targetPoint = getBodyPoint(target, currentPoint);
        }
        Vec3d playerEyePos = mc.player.getEyePos();
        Vec3d attackTargetPoint = targetPoint;
        if (attack || isAttackingSoon) {
            BodyPoint attackPoint = distance > 2.5 ? BodyPoint.CHEST : BodyPoint.HEAD;
            attackTargetPoint = getBodyPoint(target, attackPoint);
        }
        double currentTime = System.currentTimeMillis();
        float oscillY = (float) Math.cos(currentTime / 85.0);
        float oscillY2 = (float) Math.sin(currentTime / 95.0);
        float offsetY = (0.036f * oscillY + 0.032f * oscillY2) * finalOscillationFactor;
        float oscillZ = (float) Math.cos(currentTime / 60.0);
        float oscillZ2 = (float) Math.sin(currentTime / 55.0);
        float offsetZ = (0.044f * oscillZ + 0.04f * oscillZ2) * finalOscillationFactor;
        float timeVar = (float) Math.cos(currentTime / 150.0);
        float timeVar2 = (float) Math.sin(currentTime / 180.0);
        float addyVact = (0.13f * timeVar + 0.11f * timeVar2) * finalOscillationFactor;
        float timeVarZ = (float) Math.cos(currentTime / 200.0);
        float timeVarZ2 = (float) Math.sin(currentTime / 240.0);
        float addyVacZ = (0.13f * timeVarZ + 0.11f * timeVarZ2) * finalOscillationFactor;
        float timeVarX = (float) Math.cos(currentTime / 220.0);
        float timeVarX2 = (float) Math.sin(currentTime / 230.0);
        float addyVacX = (0.16f * timeVarX + 0.13f * timeVarX2) * finalOscillationFactor;
        Vec3d directionVec = attackTargetPoint
                .add((addyVacZ + offsetZ) * finalOscillationFactor, offsetY * 0.15f * finalOscillationFactor, addyVacX * finalOscillationFactor)
                .subtract(mc.player.getEyePos()).normalize();
        float baseYaw = (float) Math.toDegrees(Math.atan2(-directionVec.x, directionVec.z));
        float basePitch = MathHelper.clamp((float) (-Math.toDegrees(Math.atan2(directionVec.y, Math.hypot(directionVec.x, directionVec.z)))), -90.0f, 90.0f);
        double waveA = 50.0 + Math.random() * 60.0;
        double waveB = 55.0 + Math.random() * 65.0;
        double waveC = 60.0 + Math.random() * 70.0;
        double waveD = 65.0 + Math.random() * 75.0;
        double waveE = 58.0 + Math.random() * 68.0;
        double waveF = 62.0 + Math.random() * 72.0;
        float wave1 = (float) Math.cos(currentTime / waveA);
        float wave2 = (float) Math.sin(currentTime / waveB);
        float wave3 = (float) Math.cos(currentTime / waveC);
        float wave4 = (float) Math.sin(currentTime / waveD);
        float wave5 = (float) Math.cos(currentTime / waveE);
        float wave6 = (float) Math.sin(currentTime / waveF);
        float randomAttackShift = 0.0f;
        if (attack) {
            randomAttackShift = ThreadLocalRandom.current().nextLong(-1L, 1L);
        }
        float randomYaw1 = (float) (Math.random() * (double) 2.8f) * finalJitterFactor;
        float randomYaw2 = (float) (Math.random() * 3.0) * finalJitterFactor;
        float randomYaw3 = (float) (Math.random() * (double) 2.9f) * finalJitterFactor;
        float randomYaw4 = (float) (Math.random() * (double) 2.6f) * finalJitterFactor;
        float randomYaw5 = (float) ThreadLocalRandom.current().nextDouble(-1.6, 1.6) * finalJitterFactor;
        float randomPitch1 = (float) (Math.random() * (double) 4.4f) * finalJitterFactor;
        float randomPitch2 = smoothRandom(3.6f, 4.8f, 1.0f) * finalJitterFactor;
        float randomPitch3 = (float) (Math.random() * (double) 4.6f) * finalJitterFactor;
        float randomPitch4 = (float) (Math.random() * 4.5) * finalJitterFactor;
        float randomPitch5 = (float) (Math.random() * (double) 4.3f) * finalJitterFactor;
        float randomPitch6 = (float) (Math.random() * (double) 4.4f) * finalJitterFactor;
        float randomPitch7 = (float) (Math.random() * (double) 4.8f) * finalJitterFactor;
        float randomPitch8 = (float) (Math.random() * (double) 4.2f) * finalJitterFactor;
        float randomPitch9 = (float) ThreadLocalRandom.current().nextDouble(-2.8, 2.8) * finalJitterFactor;
        float randomPitch10 = (float) (Math.random() * (double) 4.3f) * finalJitterFactor;
        float randomPitch11 = (float) ThreadLocalRandom.current().nextDouble(-2.6, 2.6) * finalJitterFactor;
        float yawJitter = (randomYaw1 * wave2 + wave1 * randomYaw2 + wave3 * randomYaw3 + wave4 * randomYaw4 + wave5 * randomYaw5
                + wave6 * (float) (Math.random() * (double) 2.2f) + (float) (Math.random() * (double) 2.1f)
                + (float) ThreadLocalRandom.current().nextDouble(-1.4, 1.4)) * finalJitterFactor * 0.9f;
        float pitchJitter = (randomPitch1 * wave1 + wave2 * randomPitch2 + wave3 * randomPitch3 + wave4 * randomPitch4
                + randomPitch5 * wave5 + randomPitch6 * wave6 + randomPitch7 * wave1 + randomPitch8 * wave2
                + randomPitch9 * wave3 + randomPitch10 * wave4 + randomPitch11 * wave5
                + (float) (Math.random() * (double) 4.8f) - 2.4f
                + (float) ThreadLocalRandom.current().nextDouble(-2.6, 2.6)
                + (float) ThreadLocalRandom.current().nextDouble(-1.8, 1.8)) * finalJitterFactor * 1.2f;
        if (attack || isAttackingSoon) {
            pitchChangeSpeed = randomValue(1.2f, 2.0f);
            yawChangeSpeed = randomValue(12.0f, 18.0f);
        } else {
            pitchChangeSpeed = randomValue(1.8f, 2.8f);
            yawChangeSpeed = randomValue(18.0f, 26.0f);
        }
        float finalPitch = basePitch + pitchJitter + randomAttackShift;
        finalPitch = MathHelper.clamp(finalPitch, -90.0f, 90.0f);
        Angle newRotation = new Angle(baseYaw + yawJitter + randomAttackShift, finalPitch);
        if (attack || isAttackingSoon) {
            returnYawSpeed = randomValue(16.0f, 22.0f);
            returnPitchSpeed = randomValue(8.0f, 13.0f);
        } else {
            returnYawSpeed = randomValue(20.0f, 28.0f);
            returnPitchSpeed = randomValue(11.0f, 16.0f);
        }

        float yawDelta = MathHelper.wrapDegrees(newRotation.getYaw() - currentAngle.getYaw());
        float pitchDelta = newRotation.getPitch() - currentAngle.getPitch();
        float clampedYaw = Math.min(Math.abs(yawDelta), yawChangeSpeed);
        float clampedPitch = Math.min(Math.abs(pitchDelta), pitchChangeSpeed);
        float newYaw = currentAngle.getYaw() + SensUtil.getSens(MathHelper.clamp(yawDelta, -clampedYaw, clampedYaw));
        float newPitch = MathHelper.clamp(
                currentAngle.getPitch() + SensUtil.getSens(MathHelper.clamp(pitchDelta, -clampedPitch, clampedPitch)),
                -90.0f, 90.0f);
        return new Angle(newYaw, newPitch);
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }

    private enum BodyPoint {
        HEAD(0.95f),
        FOREHEAD(0.88f),
        NECK(0.75f),
        SHOULDER(0.65f),
        CHEST(0.55f),
        UPPER_CHEST(0.5f),
        MID_CHEST(0.45f),
        WAIST(0.35f),
        HIPS(0.28f),
        UPPER_LEGS(0.22f),
        KNEES(0.15f),
        LOWER_LEGS(0.1f);

        private final float heightMultiplier;

        BodyPoint(float heightMultiplier) {
            this.heightMultiplier = heightMultiplier;
        }

        public float getHeightMultiplier() {
            return this.heightMultiplier;
        }
    }
}

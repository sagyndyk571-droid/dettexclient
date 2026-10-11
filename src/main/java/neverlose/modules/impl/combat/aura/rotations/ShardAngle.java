package neverlose.modules.impl.combat.aura.rotations;

import neverlose.debob;
import neverlose.modules.impl.combat.Aura;
import neverlose.modules.impl.combat.aura.Angle;
import neverlose.modules.impl.combat.aura.MathAngle;
import neverlose.modules.impl.combat.aura.attack.StrikeManager;
import neverlose.modules.impl.combat.aura.impl.RotateConstructor;
import neverlose.util.timer.StopWatch;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import static net.minecraft.util.math.MathHelper.wrapDegrees;

public class ShardAngle extends RotateConstructor {

    private final SecureRandom random = new SecureRandom();
    private final StopWatch multipointRefreshTimer = new StopWatch();
    private final StopWatch multipointSwitchTimer = new StopWatch();

    private List<Vec3d> multipoints = new ArrayList<>();
    private int multipointIndex;
    private Entity multipointOwner;
    private boolean wideMode;
    private long multipointSwitchDelay = 150L;
    private long multipointRefreshDelay = 800L;

    private float targetJitterYaw;
    private float targetJitterPitch;
    private float currentJitterYaw;
    private float currentJitterPitch;
    private long lastJitterShift = System.currentTimeMillis();
    private long jitterShiftDelay = 45L;

    private Entity reactOwner;
    private long reactUntil;

    private float overshootYaw;
    private float overshootPitch;
    private long nextOvershootTime;

    private float prevAppliedYaw;
    private float prevAppliedPitch;

    private long lastSampleTime = System.currentTimeMillis();

    public ShardAngle() {
        super("Shard");
        currentJitterYaw = (random.nextFloat() - 0.5F) * 2F;
        currentJitterPitch = (random.nextFloat() - 0.5F);
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = debob.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
        boolean attacking = entity != null && attackHandler.canAttack(Aura.getInstance().getConfig(), 0);

        long now = System.currentTimeMillis();
        long tickGap = Math.min(120L, Math.max(1L, now - lastSampleTime));
        lastSampleTime = now;

        if (entity != reactOwner) {
            reactOwner = entity;
            reactUntil = now + 110L + random.nextInt(150);
            overshootYaw = 0F;
            overshootPitch = 0F;
            prevAppliedYaw = 0F;
            prevAppliedPitch = 0F;
            currentJitterYaw = (random.nextFloat() - 0.5F) * 2F;
            currentJitterPitch = (random.nextFloat() - 0.5F);
        }

        if (now < reactUntil) {
            float idle = currentAngle.getYaw() + (random.nextFloat() - 0.5F) * 0.35F;
            float idlePitch = MathHelper.clamp(currentAngle.getPitch() + (random.nextFloat() - 0.5F) * 0.25F, -90F, 90F);
            return new Angle(idle, idlePitch);
        }

        Vec3d aimPoint = resolveMultipoint(entity, !attacking);
        if (aimPoint != null) {
            targetAngle = MathAngle.calculateAngle(aimPoint);
        }

        Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(yawDelta, pitchDelta);

        applyOvershoot(yawDelta, pitchDelta, rotationDifference, now);
        float biasedYawDelta = yawDelta + overshootYaw;
        float biasedPitchDelta = pitchDelta + overshootPitch;

        float smoothSpeed = computeHumanSpeed(rotationDifference, tickGap, attacking);

        updateJitter(attacking);
        float jitterSmoothSpeed = attacking ? 0.35F : 0.2F;
        currentJitterYaw = MathHelper.lerp(jitterSmoothSpeed, currentJitterYaw, targetJitterYaw);
        currentJitterPitch = MathHelper.lerp(jitterSmoothSpeed, currentJitterPitch, targetJitterPitch);

        float appliedYaw = biasedYawDelta * smoothSpeed;
        float appliedPitch = biasedPitchDelta * smoothSpeed;

        float accel = attacking ? 0.82F : 0.6F;
        appliedYaw = MathHelper.lerp(accel, prevAppliedYaw, appliedYaw);
        appliedPitch = MathHelper.lerp(accel, prevAppliedPitch, appliedPitch);
        prevAppliedYaw = appliedYaw;
        prevAppliedPitch = appliedPitch;

        appliedYaw = capStep(appliedYaw, attacking ? 42F : 55F);
        appliedPitch = capStep(appliedPitch, attacking ? 26F : 34F);

        if (attacking && random.nextInt(90) == 0) {
            appliedYaw *= 0.55F;
            appliedPitch *= 0.55F;
        } else if (!attacking && random.nextInt(55) == 0) {
            appliedYaw += (random.nextFloat() - 0.5F) * 2.6F;
            appliedPitch += (random.nextFloat() - 0.5F) * 1.1F;
        }

        float newYaw = currentAngle.getYaw() + appliedYaw;
        float newPitch = currentAngle.getPitch() + appliedPitch;

        newYaw = wrapDegrees(newYaw + currentJitterYaw);
        newPitch = MathHelper.clamp(newPitch + currentJitterPitch, -90F, 90F);

        if (!attacking && random.nextInt(170) == 0) {
            newYaw = wrapDegrees(newYaw + (random.nextFloat() - 0.5F) * 1.2F);
            newPitch = MathHelper.clamp(newPitch + (random.nextFloat() - 0.5F) * 0.8F, -90F, 90F);
        }

        decayOvershoot();

        return new Angle(newYaw, newPitch);
    }

    private void applyOvershoot(float yawDelta, float pitchDelta, float rotationDifference, long now) {
        if (rotationDifference < 6F || rotationDifference > 60F) {
            return;
        }
        if (now < nextOvershootTime) {
            return;
        }
        if (Math.abs(overshootYaw) < 0.35F && Math.abs(overshootPitch) < 0.35F && random.nextFloat() < 0.5F) {
            float amount = 0.05F + random.nextFloat() * 0.08F;
            overshootYaw = Math.signum(yawDelta) * Math.abs(yawDelta) * amount;
            overshootPitch = Math.signum(pitchDelta) * Math.abs(pitchDelta) * amount * 0.5F;
            nextOvershootTime = now + 220L + random.nextInt(430);
        }
    }

    private void decayOvershoot() {
        overshootYaw *= 0.45F;
        overshootPitch *= 0.45F;
        if (Math.abs(overshootYaw) < 0.25F) overshootYaw = 0F;
        if (Math.abs(overshootPitch) < 0.25F) overshootPitch = 0F;
    }

    private float computeHumanSpeed(float rotationDifference, long tickGap, boolean attacking) {
        float gapFactor = MathHelper.clamp(50F / Math.max(20L, tickGap), 0.6F, 1.6F);
        float base;
        if (attacking) {
            base = MathHelper.clamp(rotationDifference / 55F, 0.5F, 0.92F);
        } else {
            base = MathHelper.clamp(rotationDifference / 130F, 0.1F, 0.42F);
        }
        base *= gapFactor;
        return MathHelper.clamp(base + (random.nextFloat() - 0.5F) * 0.08F, 0.08F, 0.95F);
    }

    private float capStep(float value, float max) {
        if (value > max) return max;
        if (value < -max) return -max;
        return value;
    }

    private Vec3d resolveMultipoint(Entity entity, boolean wide) {
        if (!(entity instanceof LivingEntity living)) {
            return null;
        }
        boolean refresh = multipointOwner != entity
                || multipoints.isEmpty()
                || wideMode != wide
                || multipointRefreshTimer.finished(multipointRefreshDelay);
        if (refresh) {
            wideMode = wide;
            buildMultipoints(living);
            multipointOwner = entity;
            multipointIndex = 0;
            multipointRefreshTimer.reset();
            multipointRefreshDelay = 500L + random.nextInt(800);
            multipointSwitchDelay = wide ? 80L + random.nextInt(180) : 140L + random.nextInt(160);
        }
        if (multipointSwitchTimer.finished(multipointSwitchDelay)) {
            multipointIndex = (multipointIndex + 1) % multipoints.size();
            multipointSwitchTimer.reset();
            multipointSwitchDelay = wide ? 80L + random.nextInt(180) : 140L + random.nextInt(160);
        }
        return multipoints.get(multipointIndex);
    }

    private void buildMultipoints(LivingEntity entity) {
        multipoints.clear();
        double width = entity.getWidth();
        double height = entity.getHeight();
        Vec3d pos = entity.getEntityPos();
        if (wideMode) {
            int pointCount = 20 + random.nextInt(9);
            for (int i = 0; i < pointCount; i++) {
                double x = pos.x + (random.nextDouble() - 0.5D) * width * 0.85D;
                double y = pos.y + (0.15D + random.nextDouble() * 0.8D) * height;
                double z = pos.z + (random.nextDouble() - 0.5D) * width * 0.85D;
                multipoints.add(new Vec3d(x, y, z));
            }
        } else {
            double yawRad = Math.toRadians(MathAngle.calculateDelta(
                    MathAngle.cameraAngle(),
                    MathAngle.calculateAngle(pos)).getYaw());
            double sin = Math.sin(yawRad);
            double cos = Math.cos(yawRad);
            for (int i = 0; i < 8; i++) {
                double offset = (random.nextDouble() - 0.5D) * width * 0.5D;
                double forward = (random.nextDouble() - 0.5D) * width * 0.3D;
                double x = pos.x - sin * forward + cos * offset;
                double y = pos.y + (0.42D + random.nextDouble() * 0.34D) * height;
                double z = pos.z + cos * forward + sin * offset;
                multipoints.add(new Vec3d(x, y, z));
            }
        }
    }

    private void updateJitter(boolean attacking) {
        long now = System.currentTimeMillis();
        if (now - lastJitterShift >= jitterShiftDelay) {
            float yawAmplitude = attacking ? 0.45F + random.nextFloat() * 0.6F : 4.5F + random.nextFloat() * 2.5F;
            float pitchAmplitude = attacking ? 0.18F + random.nextFloat() * 0.34F : 2.5F + random.nextFloat() * 1.8F;
            targetJitterYaw = (random.nextFloat() - 0.5F) * 2F * yawAmplitude;
            targetJitterPitch = (random.nextFloat() - 0.5F) * 2F * pitchAmplitude;
            jitterShiftDelay = attacking ? 40L + random.nextInt(35) : 30L + random.nextInt(40);
            lastJitterShift = now;
        }
    }

    public void reset() {
        multipoints.clear();
        multipointOwner = null;
        multipointIndex = 0;
        reactOwner = null;
        reactUntil = 0L;
        overshootYaw = 0F;
        overshootPitch = 0F;
        nextOvershootTime = 0L;
        prevAppliedYaw = 0F;
        prevAppliedPitch = 0F;
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}

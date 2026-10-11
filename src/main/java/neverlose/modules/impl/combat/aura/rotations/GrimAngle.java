package neverlose.modules.impl.combat.aura.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import neverlose.debob;
import neverlose.modules.impl.combat.Aura;
import neverlose.modules.impl.combat.aura.Angle;
import neverlose.modules.impl.combat.aura.MathAngle;
import neverlose.modules.impl.combat.aura.attack.StrikeManager;
import neverlose.modules.impl.combat.aura.impl.RotateConstructor;

import java.security.SecureRandom;

public class GrimAngle extends RotateConstructor {

    private final SecureRandom random = new SecureRandom();

    private Vec3d aimOffset = Vec3d.ZERO;
    private long lastAimShift = 0L;
    private long aimShiftDelay = 90L;

    private float yawVelocity;
    private float pitchVelocity;

    private float overshootYaw;
    private float overshootPitch;

    public GrimAngle() {
        super("Grim AI");
    }

    public void reset() {
        aimOffset = Vec3d.ZERO;
        lastAimShift = 0L;
        aimShiftDelay = 90L;
        yawVelocity = 0.0f;
        pitchVelocity = 0.0f;
        overshootYaw = 0.0f;
        overshootPitch = 0.0f;
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        if (mc.player == null) {
            return currentAngle;
        }

        StrikeManager attackHandler = debob.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
        boolean attacking = entity != null && attackHandler.canAttack(Aura.getInstance().getConfig(), 0);

        if (entity != null) {
            updateAimOffset(entity, attacking);
            Vec3d point = entity.getBoundingBox().getCenter().add(aimOffset);
            targetAngle = MathAngle.calculateAngle(point);
        }

        Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(yawDelta, pitchDelta);

        // While attacking the view must already be on the hitbox within the same tick, otherwise
        // the strike raycast misses. Attacking therefore uses a fast, high-cap follow; out of
        // combat the view eases in slowly so it does not look robotic.
        float maxSpeed = attacking ? 180.0f : 24.0f;
        float smooth = attacking
                ? MathHelper.clamp(0.60f + rotationDifference / 60.0f, 0.60f, 0.98f)
                : MathHelper.clamp(rotationDifference / 90.0f, 0.30f, 0.55f);

        float targetYawVelocity = MathHelper.clamp(yawDelta, -maxSpeed, maxSpeed);
        float targetPitchVelocity = MathHelper.clamp(pitchDelta, -maxSpeed, maxSpeed);

        yawVelocity = MathHelper.lerp(smooth, yawVelocity, targetYawVelocity);
        pitchVelocity = MathHelper.lerp(smooth, pitchVelocity, targetPitchVelocity);

        if (attacking && rotationDifference > 2.0f && random.nextInt(24) == 0) {
            overshootYaw = (random.nextFloat() - 0.5f) * 0.6f;
            overshootPitch = (random.nextFloat() - 0.5f) * 0.4f;
        }
        overshootYaw *= 0.72f;
        overshootPitch *= 0.72f;

        // Sub-gcd jitter is applied BEFORE snapping so the final angle stays on the sensitivity
        // grid. Adding it after the snap (as before) shifted the delta off the grid and got the
        // rotation flagged by Grim's aim check.
        float jitterYaw = (random.nextFloat() - 0.5f) * 0.04f;
        float jitterPitch = (random.nextFloat() - 0.5f) * 0.04f;

        float newYaw = currentAngle.getYaw() + yawVelocity + overshootYaw + jitterYaw;
        float newPitch = MathHelper.clamp(
                currentAngle.getPitch() + pitchVelocity + overshootPitch + jitterPitch, -89.5f, 89.5f);

        // GCD snapping: force every delta onto the mouse-sensitivity grid. This is the core of a
        // Grim-style rotation - the aim check flags deltas that are not multiples of the gcd.
        return new Angle(newYaw, newPitch).adjustSensitivity();
    }

    private void updateAimOffset(Entity entity, boolean attacking) {
        long now = System.currentTimeMillis();
        if (now - lastAimShift < aimShiftDelay) {
            return;
        }
        double width = entity.getWidth();
        double height = entity.getHeight();
        double spread = attacking ? 0.16 : 0.36;
        aimOffset = new Vec3d(
                (random.nextDouble() - 0.5) * width * spread,
                (random.nextDouble() - 0.5) * height * spread,
                (random.nextDouble() - 0.5) * width * spread);
        aimShiftDelay = 80L + random.nextInt(130);
        lastAimShift = now;
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}

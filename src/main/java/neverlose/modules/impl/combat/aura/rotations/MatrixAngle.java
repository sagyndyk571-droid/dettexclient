package neverlose.modules.impl.combat.aura.rotations;

import neverlose.debob;
import neverlose.modules.impl.combat.Aura;
import neverlose.modules.impl.combat.aura.Angle;
import neverlose.modules.impl.combat.aura.MathAngle;
import neverlose.modules.impl.combat.aura.attack.StrikeManager;
import neverlose.modules.impl.combat.aura.impl.RotateConstructor;
import neverlose.modules.impl.combat.aura.target.RaycastAngle;
import neverlose.util.move.MoveUtil;
import neverlose.util.timer.StopWatch;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class MatrixAngle extends RotateConstructor {
    private float yawVelocity = 0f;
    private float pitchVelocity = 0f;
    private int lastEntityId = -1;

    public MatrixAngle() {
        super("Matrix");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = debob.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        StopWatch attackTimer = attackHandler.getAttackTimer();

        if (entity == null || !aura.isState()) {
            lastEntityId = -1;
            yawVelocity *= 0.35f;
            pitchVelocity *= 0.35f;
            return returnToAngle(currentAngle, targetAngle, attackTimer);
        }

        if (lastEntityId != entity.getId()) {
            lastEntityId = entity.getId();
            yawVelocity = 0f;
            pitchVelocity = 0f;
        }

        boolean canAttack = attackHandler.canAttack(aura.getConfig(), 0);

        Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        if (rotationDifference < 0.015f) {
            yawVelocity *= 0.45f;
            pitchVelocity *= 0.45f;
            return new Angle(currentAngle.getYaw(), MathHelper.clamp(currentAngle.getPitch(), -90f, 90f));
        }

        boolean lookingAtHitbox = RaycastAngle.rayTrace(
                currentAngle.toVector(),
                4.0,
                entity.getBoundingBox()
        );

        float distanceFactor = MathHelper.clamp(rotationDifference / 54f, 0.48f, 1f);
        float yawLimit = canAttack ? 76f : lookingAtHitbox ? 22f : 48f * distanceFactor;
        float pitchLimit = canAttack ? 34f : lookingAtHitbox ? 12f : 21f * distanceFactor;

        float targetYawStep = MathHelper.clamp(yawDelta, -yawLimit, yawLimit);
        float targetPitchStep = MathHelper.clamp(pitchDelta, -pitchLimit, pitchLimit);

        float smooth = canAttack ? 0.88f : lookingAtHitbox ? 0.72f : 0.74f;
        yawVelocity = MathHelper.lerp(smooth, yawVelocity, targetYawStep);
        pitchVelocity = MathHelper.lerp(smooth, pitchVelocity, targetPitchStep);

        if (!canAttack && !lookingAtHitbox && aura.isRandomizationEnabled() && MoveUtil.hasPlayerMovement()) {
            long time = System.currentTimeMillis();
            yawVelocity += (float) Math.sin(time / 185D) * 0.22f;
            pitchVelocity += (float) Math.cos(time / 230D) * 0.08f;
        }

        if (Math.abs(yawVelocity) < 0.10f && Math.abs(yawDelta) > 0.08f) {
            yawVelocity = Math.copySign(0.10f, yawDelta);
        }
        if (Math.abs(pitchVelocity) < 0.06f && Math.abs(pitchDelta) > 0.06f) {
            pitchVelocity = Math.copySign(0.06f, pitchDelta);
        }

        if (Math.abs(yawVelocity) > Math.abs(yawDelta)) {
            yawVelocity = yawDelta;
        }
        if (Math.abs(pitchVelocity) > Math.abs(pitchDelta)) {
            pitchVelocity = pitchDelta;
        }

        float newYaw = currentAngle.getYaw() + yawVelocity;
        float newPitch = MathHelper.clamp(currentAngle.getPitch() + pitchVelocity, -89.5f, 89.5f);
        return new Angle(newYaw, newPitch);
    }

    private Angle returnToAngle(Angle currentAngle, Angle targetAngle, StopWatch attackTimer) {
        Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        if (rotationDifference < 0.015f) {
            return new Angle(currentAngle.getYaw(), MathHelper.clamp(currentAngle.getPitch(), -90f, 90f));
        }

        float speed = attackTimer.finished(320) ? 0.72f : 0.42f;
        float yawStep = MathHelper.clamp(yawDelta, -48f, 48f);
        float pitchStep = MathHelper.clamp(pitchDelta, -24f, 24f);

        return new Angle(
                currentAngle.getYaw() + yawStep * speed,
                MathHelper.clamp(currentAngle.getPitch() + pitchStep * speed, -89.5f, 89.5f)
        );
    }

    private float randomLerp(float min, float max) {
        // Only apply randomization if enabled
        Aura aura = Aura.getInstance();
        if (aura.isRandomizationEnabled()) {
            return MathHelper.lerp(new java.util.Random().nextFloat(), min, max);
        }
        // Return the midpoint when randomization is disabled
        return (min + max) / 2.0f;
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0, 0, 0);
    }
}

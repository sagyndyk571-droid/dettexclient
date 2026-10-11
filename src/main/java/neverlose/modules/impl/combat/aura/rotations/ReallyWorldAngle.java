package neverlose.modules.impl.combat.aura.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import neverlose.debob;
import neverlose.modules.impl.combat.Aura;
import neverlose.modules.impl.combat.aura.Angle;
import neverlose.modules.impl.combat.aura.AngleConnection;
import neverlose.modules.impl.combat.aura.MathAngle;
import neverlose.modules.impl.combat.aura.attack.StrikeManager;
import neverlose.modules.impl.combat.aura.impl.RotateConstructor;
import neverlose.modules.impl.combat.aura.target.RaycastAngle;
import neverlose.modules.impl.combat.aura.target.Vector;

public class ReallyWorldAngle extends RotateConstructor {
    private float yawVelocity = 0f;
    private float pitchVelocity = 0f;
    private int lastEntityId = -1;

    public ReallyWorldAngle() {
        super("ReallyWorld");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = debob.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();

        if (entity == null || !aura.isState()) {
            lastEntityId = -1;
            yawVelocity *= 0.35f;
            pitchVelocity *= 0.35f;
            return approach(currentAngle, targetAngle, 0.24f, 16f, 7f);
        }

        if (lastEntityId != entity.getId()) {
            lastEntityId = entity.getId();
            yawVelocity = 0f;
            pitchVelocity = 0f;
        }

        boolean canAttack = attackHandler.canAttack(aura.getConfig(), 0);
        if (canAttack) {
            Vec3d aimPoint = Vector.hitbox(entity, 1.35F, entity.isOnGround() ? 1.05F : 1.24F, 1.35F, 2.0F);
            targetAngle = MathAngle.calculateAngle(aimPoint);
        }

        Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        if (rotationDifference < 0.015f) {
            yawVelocity *= 0.4f;
            pitchVelocity *= 0.4f;
            return new Angle(currentAngle.getYaw(), MathHelper.clamp(currentAngle.getPitch(), -89.5f, 89.5f));
        }

        boolean lookingAtHitbox = RaycastAngle.rayTrace(
                AngleConnection.INSTANCE.getRotation().toVector(),
                3.65,
                entity.getBoundingBox()
        );

        float distanceFactor = MathHelper.clamp(rotationDifference / 70f, 0.28f, 1f);
        float yawLimit = canAttack ? 30f : lookingAtHitbox ? 3.2f : 18f * distanceFactor;
        float pitchLimit = canAttack ? 10f : lookingAtHitbox ? 1.9f : 7.5f * distanceFactor;

        float targetYawStep = MathHelper.clamp(yawDelta, -yawLimit, yawLimit);
        float targetPitchStep = MathHelper.clamp(pitchDelta, -pitchLimit, pitchLimit);

        float smooth = canAttack ? 0.42f : lookingAtHitbox ? 0.2f : 0.32f;
        yawVelocity = MathHelper.lerp(smooth, yawVelocity, targetYawStep);
        pitchVelocity = MathHelper.lerp(smooth, pitchVelocity, targetPitchStep);

        if (Math.abs(yawVelocity) < 0.018f && Math.abs(yawDelta) > 0.08f) {
            yawVelocity = Math.copySign(0.018f, yawDelta);
        }
        if (Math.abs(pitchVelocity) < 0.012f && Math.abs(pitchDelta) > 0.06f) {
            pitchVelocity = Math.copySign(0.012f, pitchDelta);
        }

        return new Angle(
                currentAngle.getYaw() + yawVelocity,
                MathHelper.clamp(currentAngle.getPitch() + pitchVelocity, -89.5f, 89.5f)
        );
    }

    private Angle approach(Angle currentAngle, Angle targetAngle, float smooth, float yawLimit, float pitchLimit) {
        Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        return new Angle(
                currentAngle.getYaw() + MathHelper.clamp(angleDelta.getYaw(), -yawLimit, yawLimit) * smooth,
                MathHelper.clamp(currentAngle.getPitch() + MathHelper.clamp(angleDelta.getPitch(), -pitchLimit, pitchLimit) * smooth, -89.5f, 89.5f)
        );
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}

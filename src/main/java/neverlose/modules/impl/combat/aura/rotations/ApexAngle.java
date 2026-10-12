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

public class ApexAngle extends RotateConstructor {

    private final SecureRandom random = new SecureRandom();

    public ApexAngle() {
        super("Apex");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        if (rotationDifference < 1.0E-4f) {
            rotationDifference = 1.0f;
        }

        Aura aura = Aura.getInstance();
        boolean attacking = lolikbypass(debob.getInstance().getManager().getAttackPerpetrator().getAttackHandler(), 0);
        boolean onTarget = aura.target != null && MathAngle.rayTrace(
                currentAngle.getYaw(),
                currentAngle.getPitch(),
                aura.attackrange.getValue(),
                aura.getLookRange().getValue(),
                aura.target);

        float yaw = 0;
        float pitch = 0;
        if (aura.target != null && !attacking) {
            double degrees = 10.0;
            double period = 10.0;
            double maxAmp = 10.0;
            double rads = Math.toRadians(degrees);
            double t = System.currentTimeMillis() / (period * 55);
            double amp = maxAmp * Math.sin(t * Math.PI * 2);

            yaw = (float) (amp * Math.cos(rads));
            pitch = (float) (amp * Math.sin(rads));
            pitch = MathHelper.clamp(pitch, -90.0f, 90.0f);
        }

        float speed = attacking ? 0.45F
                : (lolikbypass(debob.getInstance().getManager().getAttackPerpetrator().getAttackHandler(), 0) ? 1F : 0.3F);
        if (attacking && !onTarget) {
            speed = 0.45F;
        }

        float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
        float linePitch = (Math.abs(pitchDelta) * 180);
        float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);
        float targetYaw = currentAngle.getYaw() + moveYaw;

        Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
        moveAngle.setYaw(MathHelper.lerp(
                MathHelper.clamp(randomLerp(speed, speed + 0.2F), 0f, 1f),
                currentAngle.getYaw(),
                targetYaw) + yaw);

        float calculatedPitch = MathHelper.lerp(
                MathHelper.clamp(randomLerp(speed, speed + 0.2F), 0f, 1f),
                currentAngle.getPitch(),
                currentAngle.getPitch() + movePitch
        ) + pitch;

        moveAngle.setPitch(MathHelper.clamp(calculatedPitch, -90.0f, 90.0f));
        return moveAngle;
    }

    private boolean lolikbypass(StrikeManager strikeManager, int ticks) {
        Aura aura = Aura.getInstance();
        return aura.target != null && aura.target.isAlive() && strikeManager.canAttack(aura.getConfig(), ticks);
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(random.nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}

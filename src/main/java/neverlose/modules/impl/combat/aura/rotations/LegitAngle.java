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

import java.security.SecureRandom;


public class LegitAngle extends RotateConstructor {

    private final SecureRandom random = new SecureRandom();

    private float jitterYaw   = 0f;
    private float jitterPitch = 0f;
    private float targetJitterYaw   = 0f;
    private float targetJitterPitch = 0f;

    private double phase = 0.0;
    private long lastAttackMs = 0;

    public LegitAngle() {
        super("Legit");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = debob.getInstance().getManager()
                .getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        boolean randomize = aura.isRandomizationEnabled();

        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);
        boolean lookingAtHitbox = entity != null && RaycastAngle.rayTrace(
                AngleConnection.INSTANCE.getRotation().toVector(),
                aura.getAttackrange().getValue(),
                entity.getBoundingBox()
        );

        Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta   = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float rotDiff    = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        if (rotDiff < 0.01f) rotDiff = 1f;

        if (canAttack && entity != null) {
            lastAttackMs = System.currentTimeMillis();
            Vec3d aimPoint = Vector.hitbox(entity, 1f, entity.isOnGround() ? 0.9f : 1.3f, 1f, 2f);
            Angle exactAngle = MathAngle.calculateAngle(aimPoint);
            Angle snapDelta  = MathAngle.calculateDelta(currentAngle, exactAngle);
            float lineYaw    = Math.abs(snapDelta.getYaw()   / rotDiff) * 360f;
            float linePitch  = Math.abs(snapDelta.getPitch() / rotDiff) * 180f;
            float newYaw     = currentAngle.getYaw()   + MathHelper.clamp(snapDelta.getYaw(),   -lineYaw,   lineYaw);
            float newPitch   = currentAngle.getPitch() + MathHelper.clamp(snapDelta.getPitch(), -linePitch, linePitch);
            jitterYaw = 0f; jitterPitch = 0f;
            targetJitterYaw = 0f; targetJitterPitch = 0f;
            return new Angle(newYaw, MathHelper.clamp(newPitch, -90f, 90f));
        }

        long msSinceAttack = System.currentTimeMillis() - lastAttackMs;
        float speed;
        if (msSinceAttack < 80) {
            speed = 0.35f;
        } else if (lookingAtHitbox) {
            speed = randomize ? randomLerp(0.04f, 0.09f) : 0.06f;
        } else {
            float distanceFactor = MathHelper.clamp(rotDiff / 45f, 0.1f, 1f);
            speed = (randomize ? randomLerp(0.25f, 0.45f) : 0.35f) * distanceFactor;
        }

        if (randomize) {
            phase += 0.048;
            if (phase > Math.PI * 2) phase -= Math.PI * 2;

            float jitterAmp  = randomLerp(0.6f, 1.4f);
            float jitterFade = msSinceAttack < 150 ? (float) msSinceAttack / 150f : 1f;
            jitterAmp *= jitterFade;

            targetJitterYaw   = (float) (Math.sin(phase * 1.3)  * jitterAmp);
            targetJitterPitch = (float) (Math.cos(phase * 2.1)  * jitterAmp * 0.35f);
        } else {
            targetJitterYaw = 0f;
            targetJitterPitch = 0f;
        }

        jitterYaw   += (targetJitterYaw   - jitterYaw)   * 0.18f;
        jitterPitch += (targetJitterPitch - jitterPitch) * 0.18f;

        float lineYaw   = Math.abs(yawDelta / rotDiff) * 360f;
        float linePitch = Math.abs(pitchDelta / rotDiff) * 180f;

        float newYaw   = MathHelper.lerp(speed, currentAngle.getYaw(),   currentAngle.getYaw()   + MathHelper.clamp(yawDelta,   -lineYaw,   lineYaw))   + jitterYaw;
        float newPitch = MathHelper.lerp(speed, currentAngle.getPitch(), currentAngle.getPitch() + MathHelper.clamp(pitchDelta, -linePitch, linePitch)) + jitterPitch;

        if (entity == null) { jitterYaw *= 0.85f; jitterPitch *= 0.85f; }

        return new Angle(newYaw, MathHelper.clamp(newPitch, -90f, 90f));
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(random.nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}

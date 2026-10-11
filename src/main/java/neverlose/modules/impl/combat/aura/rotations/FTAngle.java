package neverlose.modules.impl.combat.aura.rotations;

import neverlose.debob;
import neverlose.modules.impl.combat.Aura;
import neverlose.modules.impl.combat.aura.Angle;
import neverlose.modules.impl.combat.aura.MathAngle;
import neverlose.modules.impl.combat.aura.attack.StrikeManager;
import neverlose.modules.impl.combat.aura.impl.RotateConstructor;
import neverlose.util.timer.StopWatch;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;

public class FTAngle extends RotateConstructor {

    private final SecureRandom random = new SecureRandom();

    private static int lastCount = -1;
    private static int hitsAfterMiss = 0;
    private static long missEndTime = 0;
    private static int swingsDone = 0;

    private float currentJitterYaw = 0;
    private float currentJitterPitch = 0;
    private float targetJitterYaw = 0;
    private float targetJitterPitch = 0;

    public FTAngle() {
        super("FunTime");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = debob.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
        StopWatch attackTimer = attackHandler.getAttackTimer();
        int count = attackHandler.getCount();
        Aura aura = Aura.getInstance();

        long now = System.currentTimeMillis();

        if (count != lastCount) {
            hitsAfterMiss++;
            lastCount = count;
        }

        if (hitsAfterMiss >= 40 && missEndTime == 0) {
            missEndTime = now + 350;
            hitsAfterMiss = 0;
            swingsDone = 0;
        }

        if (missEndTime != 0) {
            if (now < missEndTime) {
                long elapsed = now - (missEndTime - 350);
                if (swingsDone == 0 && elapsed >= 50) {
                    mc.player.swingHand(Hand.MAIN_HAND);
                    swingsDone = 1;
                } else if (swingsDone == 1 && elapsed >= 180) {
                    mc.player.swingHand(Hand.MAIN_HAND);
                    swingsDone = 2;
                }
                float missYaw = currentAngle.getYaw() + random.nextFloat() * 2.4f - 1.2f;
                float missPitch = MathHelper.lerp(0.24f, currentAngle.getPitch(), -80f);
                return new Angle(missYaw, MathHelper.clamp(missPitch, -90, 90));
            } else {
                missEndTime = 0;
            }
        }

        Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        if (rotationDifference < 0.01f) rotationDifference = 1;

        int suck = count % 3;
        float timeRandom = attackTimer.elapsedTime() / 80F + (count % 6);

        Angle randomAngle = switch (suck) {
            case 0 -> new Angle((float) Math.cos(timeRandom), (float) Math.sin(timeRandom));
            case 1 -> new Angle((float) Math.sin(timeRandom), (float) Math.cos(timeRandom));
            case 2 -> new Angle((float) Math.sin(timeRandom), (float) -Math.cos(timeRandom));
            default -> new Angle((float) -Math.cos(timeRandom), (float) Math.sin(timeRandom));
        };

        targetJitterYaw = randomLerp(7, 13) * randomAngle.getYaw();
        targetJitterPitch = randomLerp(0.8f, 3.4f) * randomAngle.getPitch() + randomLerp(1.4f, 0.7f) * (float) Math.cos(System.currentTimeMillis() / 8000.0);

        float jitterSmoothSpeed = 0.22f;
        currentJitterYaw += (targetJitterYaw - currentJitterYaw) * jitterSmoothSpeed;
        currentJitterPitch += (targetJitterPitch - currentJitterPitch) * jitterSmoothSpeed;

        if (entity != null) {
            float speed = attackHandler.canAttack(aura.getConfig(), 0) ? 0.72f : 0.16F;

            float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
            float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);

            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

            float lerpSpeed = MathHelper.clamp(randomLerp(speed, speed + 0.22F), 0.04f, 0.82f);

            float newYaw = MathHelper.lerp(lerpSpeed, currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + currentJitterYaw;
            float newPitch = MathHelper.lerp(lerpSpeed, currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + currentJitterPitch;

            return new Angle(newYaw, MathHelper.clamp(newPitch, -90, 90));

        } else {
            float speed = attackTimer.finished(650) ? (random.nextBoolean() ? 0.55F : 0.22F) : -0.12F;

            float yawJitter = !attackTimer.finished(2000) ? currentJitterYaw : 0;
            float pitchJitter = !attackTimer.finished(2000) ? currentJitterPitch : 0;

            float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
            float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);

            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);

            float lerpSpeed = MathHelper.clamp(randomLerp(speed, speed + 0.16F), 0, 0.7F);

            float newYaw = MathHelper.lerp(lerpSpeed, currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + yawJitter;
            float newPitch = MathHelper.lerp(lerpSpeed, currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + pitchJitter;

            return new Angle(newYaw, MathHelper.clamp(newPitch, -90, 90));
        }
    }

    private float randomLerp(float min, float max) {
        return MathHelper.lerp(random.nextFloat(), min, max);
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}

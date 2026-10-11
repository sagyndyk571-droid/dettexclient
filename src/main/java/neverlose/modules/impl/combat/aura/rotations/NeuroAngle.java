package neverlose.modules.impl.combat.aura.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import neverlose.IMinecraft;
import neverlose.command.impl.NeiroCommand;
import neverlose.debob;
import neverlose.modules.impl.combat.Aura;
import neverlose.modules.impl.combat.aura.Angle;
import neverlose.modules.impl.combat.aura.MathAngle;
import neverlose.modules.impl.combat.aura.impl.RotateConstructor;
import neverlose.modules.impl.combat.aura.target.RaycastAngle;

import java.util.List;

public class NeuroAngle extends RotateConstructor implements IMinecraft {

    private int traceIndex = 0;
    private int lastEntityId = -1;
    private float styleYaw = 0;
    private float stylePitch = 0;
    private float offsetYaw = 0;
    private float offsetPitch = 0;
    private float yawVelocity = 0;
    private float pitchVelocity = 0;
    private float headMemoryYaw = 0;
    private float headMemoryPitch = 0;
    private float targetMemoryYaw = 0;
    private float targetMemoryPitch = 0;
    private float movementMemoryYaw = 0;
    private float lastYawDelta = 0;
    private float lastPitchDelta = 0;

    public NeuroAngle() {
        super("Neuro");
    }

    public void reset() {
        traceIndex = 0;
        lastEntityId = -1;
        styleYaw = 0;
        stylePitch = 0;
        offsetYaw = 0;
        offsetPitch = 0;
        yawVelocity = 0;
        pitchVelocity = 0;
        headMemoryYaw = 0;
        headMemoryPitch = 0;
        targetMemoryYaw = 0;
        targetMemoryPitch = 0;
        movementMemoryYaw = 0;
        lastYawDelta = 0;
        lastPitchDelta = 0;
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        if (NeiroCommand.isRecording) {
            return currentAngle;
        }

        if (mc.player == null || entity == null) {
            return approach(currentAngle, targetAngle, 0.36f, 32f, 14f);
        }

        if (lastEntityId != entity.getId()) {
            reset();
            lastEntityId = entity.getId();
        }

        Aura aura = Aura.getInstance();
        boolean canAttack = debob.getInstance().getManager().getAttackPerpetrator().getAttackHandler()
                .canAttack(aura.getConfig(), 0);

        float profileSpeed = 0;
        float attackFrame = 0;
        float recordedMovement = 0;
        List<float[]> trace = getSelectedTrace(aura);
        if (trace != null && !trace.isEmpty()) {
            float[] frame = nextFrame(trace, canAttack);
            boolean richFrame = frame.length >= 13;

            float recordedStyleYaw = frame.length > 0 ? frame[0] : 0;
            float recordedStylePitch = frame.length > 1 ? frame[1] : 0;
            float rawYaw = frame.length > 2 ? frame[2] : recordedStyleYaw;
            float rawPitch = frame.length > 3 ? frame[3] : recordedStylePitch;
            float recordedOffsetYaw = richFrame && frame[12] > 0.5f ? frame[4] : 0;
            float recordedOffsetPitch = richFrame && frame[12] > 0.5f ? frame[5] : 0;
            float recordedTargetYaw = frame.length > 6 ? frame[6] : 0;
            float recordedTargetPitch = frame.length > 7 ? frame[7] : 0;
            attackFrame = frame.length > 8 ? frame[8] : 0;
            float forwardSpeed = frame.length > 9 ? frame[9] : 0;
            float sidewaysSpeed = frame.length > 10 ? frame[10] : 0;
            float recordedHeadYaw = frame.length > 13 ? frame[13] : rawYaw - recordedTargetYaw;
            float recordedBodyYaw = frame.length > 14 ? frame[14] : 0;
            float recordedHeadBodyOffset = frame.length > 15 ? frame[15] : 0;
            float recordedCameraHeadOffset = frame.length > 16 ? frame[16] : 0;
            float recordedHeadPitch = rawPitch - recordedTargetPitch;
            profileSpeed = (float) Math.hypot(rawYaw, rawPitch);
            recordedMovement = MathHelper.clamp((float) Math.hypot(forwardSpeed, sidewaysSpeed), 0f, 1f);

            float maxOffsetYaw = canAttack ? 7.0f : 12.0f;
            float maxOffsetPitch = canAttack ? 4.8f : 7.5f;
            offsetYaw = MathHelper.lerp(0.42f, offsetYaw, MathHelper.clamp(recordedOffsetYaw, -maxOffsetYaw, maxOffsetYaw));
            offsetPitch = MathHelper.lerp(0.42f, offsetPitch, MathHelper.clamp(recordedOffsetPitch, -maxOffsetPitch, maxOffsetPitch));

            float styleScale = canAttack || attackFrame > 0.5f ? 0.78f : 0.56f;
            styleYaw = MathHelper.lerp(0.48f, styleYaw, MathHelper.clamp(recordedStyleYaw, -7.0f, 7.0f) * styleScale);
            stylePitch = MathHelper.lerp(0.48f, stylePitch, MathHelper.clamp(recordedStylePitch, -4.0f, 4.0f) * styleScale);

            targetMemoryYaw = MathHelper.lerp(0.34f, targetMemoryYaw, MathHelper.clamp(recordedTargetYaw, -4.0f, 4.0f));
            targetMemoryPitch = MathHelper.lerp(0.34f, targetMemoryPitch, MathHelper.clamp(recordedTargetPitch, -2.5f, 2.5f));

            float headYawTarget = recordedHeadYaw
                    + recordedBodyYaw * 0.18f
                    + recordedHeadBodyOffset * 0.06f
                    + recordedCameraHeadOffset * 0.10f;
            headMemoryYaw = MathHelper.lerp(0.38f, headMemoryYaw, MathHelper.clamp(headYawTarget, -5.5f, 5.5f));
            headMemoryPitch = MathHelper.lerp(0.36f, headMemoryPitch, MathHelper.clamp(recordedHeadPitch, -3.2f, 3.2f));

            float movementYawTarget = MathHelper.clamp(sidewaysSpeed * 0.45f + forwardSpeed * 0.10f, -0.65f, 0.65f);
            movementMemoryYaw = MathHelper.lerp(0.25f, movementMemoryYaw, movementYawTarget * (canAttack ? 0.35f : 1.0f));
        } else {
            styleYaw *= 0.62f;
            stylePitch *= 0.62f;
            offsetYaw *= 0.55f;
            offsetPitch *= 0.55f;
            headMemoryYaw *= 0.68f;
            headMemoryPitch *= 0.68f;
            targetMemoryYaw *= 0.70f;
            targetMemoryPitch *= 0.70f;
            movementMemoryYaw *= 0.70f;
        }

        Angle profiledTarget = new Angle(
                targetAngle.getYaw() + offsetYaw,
                MathHelper.clamp(targetAngle.getPitch() + offsetPitch, -89.5f, 89.5f)
        );

        Angle angleDelta = MathAngle.calculateDelta(currentAngle, profiledTarget);
        float yawDelta = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();

        float distance = MathHelper.clamp((float) mc.player.distanceTo(entity), 1.0f, 6.0f);
        boolean lookingAtHitbox = RaycastAngle.rayTrace(
                currentAngle.toVector(),
                Math.max(3.0, distance + 0.65f),
                entity.getBoundingBox()
        );
        float speedBoost = MathHelper.clamp(profileSpeed / 55f, 0f, 0.22f);
        float attackBoost = attackFrame > 0.5f ? 0.08f : 0f;
        float yawLimit = canAttack ? 62f : lookingAtHitbox ? 24f : MathHelper.clamp(24f + distance * 3.0f + profileSpeed * 0.35f, 28f, 44f);
        float pitchLimit = canAttack ? 24f : lookingAtHitbox ? 10f : MathHelper.clamp(10f + distance * 1.3f + profileSpeed * 0.16f, 11f, 18f);
        float smooth = canAttack ? 0.76f + speedBoost : lookingAtHitbox ? 0.42f + speedBoost : 0.50f + speedBoost + attackBoost;

        if (Math.signum(yawDelta) != Math.signum(lastYawDelta) && Math.abs(yawDelta) < 5.0f) {
            yawVelocity *= 0.58f;
        }
        if (Math.signum(pitchDelta) != Math.signum(lastPitchDelta) && Math.abs(pitchDelta) < 3.0f) {
            pitchVelocity *= 0.58f;
        }

        yawVelocity = MathHelper.lerp(smooth, yawVelocity, MathHelper.clamp(yawDelta, -yawLimit, yawLimit));
        pitchVelocity = MathHelper.lerp(smooth, pitchVelocity, MathHelper.clamp(pitchDelta, -pitchLimit, pitchLimit));

        float safeStyleYaw = Math.abs(yawDelta) > 1.5f ? styleYaw : styleYaw * 0.25f;
        float safeStylePitch = Math.abs(pitchDelta) > 0.9f ? stylePitch : stylePitch * 0.25f;
        float memoryScale = canAttack ? 0.34f : lookingAtHitbox ? 0.42f : 0.68f;
        float humanMemoryYaw = MathHelper.clamp(
                (headMemoryYaw + targetMemoryYaw * 0.18f + movementMemoryYaw) * memoryScale,
                -2.6f,
                2.6f
        );
        float humanMemoryPitch = MathHelper.clamp(
                (headMemoryPitch + targetMemoryPitch * 0.12f) * memoryScale,
                -1.8f,
                1.8f
        );

        if (!canAttack && recordedMovement > 0.05f) {
            float idleSway = (float) Math.sin((mc.player.age + traceIndex) * 0.37f) * 0.06f * recordedMovement;
            humanMemoryYaw += idleSway;
        }

        lastYawDelta = yawDelta;
        lastPitchDelta = pitchDelta;

        return new Angle(
                currentAngle.getYaw() + yawVelocity + safeStyleYaw + humanMemoryYaw,
                MathHelper.clamp(currentAngle.getPitch() + pitchVelocity + safeStylePitch + humanMemoryPitch, -89.5f, 89.5f)
        );
    }

    private float[] nextFrame(List<float[]> trace, boolean canAttack) {
        int size = trace.size();
        if (traceIndex >= size) {
            traceIndex = 0;
        }

        if (canAttack) {
            int search = Math.min(size, 12);
            for (int i = 0; i < search; i++) {
                int index = (traceIndex + i) % size;
                float[] candidate = trace.get(index);
                if (candidate.length > 8 && candidate[8] > 0.5f) {
                    traceIndex = (index + 1) % size;
                    return candidate;
                }
            }
        }

        float[] frame = trace.get(traceIndex);
        traceIndex = (traceIndex + 1) % size;
        return frame;
    }

    private List<float[]> getSelectedTrace(Aura aura) {
        String selectedProfile = aura.neuroProfile.getSelected();
        if (selectedProfile == null || selectedProfile.equals("Нет профилей")) {
            return null;
        }
        return NeiroCommand.getTraceSilently(selectedProfile);
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

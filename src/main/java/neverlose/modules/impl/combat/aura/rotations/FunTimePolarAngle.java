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
import neverlose.modules.impl.combat.aura.attack.StrikeManager;
import neverlose.modules.impl.combat.aura.impl.RotateConstructor;
import neverlose.modules.impl.combat.aura.target.RaycastAngle;
import neverlose.util.move.MoveUtil;

import java.util.List;

public class FunTimePolarAngle extends RotateConstructor implements IMinecraft {
    private int lastEntityId = -1;
    private int traceIndex = 0;
    private float yawVelocity = 0.0f;
    private float pitchVelocity = 0.0f;
    private float humanYaw = 0.0f;
    private float humanPitch = 0.0f;
    private float snapMemory = 0.0f;
    private float lastYawDelta = 0.0f;
    private float lastPitchDelta = 0.0f;

    public FunTimePolarAngle() {
        super("FunTime Polar");
    }

    public void reset() {
        lastEntityId = -1;
        traceIndex = 0;
        yawVelocity = 0.0f;
        pitchVelocity = 0.0f;
        humanYaw = 0.0f;
        humanPitch = 0.0f;
        snapMemory = 0.0f;
        lastYawDelta = 0.0f;
        lastPitchDelta = 0.0f;
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        if (mc.player == null || entity == null) {
            reset();
            return approach(currentAngle, targetAngle, 0.42f, 42.0f, 18.0f);
        }

        if (lastEntityId != entity.getId()) {
            reset();
            lastEntityId = entity.getId();
        }

        Aura aura = Aura.getInstance();
        StrikeManager attackHandler = debob.getInstance().getManager().getAttackPerpetrator().getAttackHandler();
        boolean canAttack = attackHandler.canAttack(aura.getConfig(), 0);

        Angle profiledTarget = applyHumanProfile(aura, targetAngle, canAttack);
        Angle delta = MathAngle.calculateDelta(currentAngle, profiledTarget);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();

        float distance = MathHelper.clamp((float) mc.player.distanceTo(entity), 1.0f, 6.0f);
        boolean lookingAtHitbox = RaycastAngle.rayTrace(
                currentAngle.toVector(),
                Math.max(3.0, distance + 0.55f),
                entity.getBoundingBox()
        );

        float distanceScale = MathHelper.clamp(distance / 4.5f, 0.42f, 1.0f);
        float yawLimit = canAttack ? 58.0f : lookingAtHitbox ? 16.0f : 30.0f * distanceScale;
        float pitchLimit = canAttack ? 23.0f : lookingAtHitbox ? 7.5f : 12.5f * distanceScale;
        float smooth = canAttack ? 0.74f : lookingAtHitbox ? 0.36f : 0.52f;

        snapMemory = MathHelper.lerp(canAttack ? 0.42f : 0.18f, snapMemory, canAttack ? 1.0f : 0.0f);
        if (snapMemory > 0.08f) {
            yawLimit += 14.0f * snapMemory;
            pitchLimit += 5.0f * snapMemory;
            smooth += 0.08f * snapMemory;
        }

        if (Math.signum(yawDelta) != Math.signum(lastYawDelta) && Math.abs(yawDelta) < 4.0f) {
            yawVelocity *= 0.58f;
        }
        if (Math.signum(pitchDelta) != Math.signum(lastPitchDelta) && Math.abs(pitchDelta) < 2.5f) {
            pitchVelocity *= 0.58f;
        }

        float targetYawStep = MathHelper.clamp(yawDelta, -yawLimit, yawLimit);
        float targetPitchStep = MathHelper.clamp(pitchDelta, -pitchLimit, pitchLimit);
        yawVelocity = MathHelper.lerp(smooth, yawVelocity, targetYawStep);
        pitchVelocity = MathHelper.lerp(smooth, pitchVelocity, targetPitchStep);

        if (Math.abs(yawVelocity) > Math.abs(yawDelta)) {
            yawVelocity = yawDelta;
        }
        if (Math.abs(pitchVelocity) > Math.abs(pitchDelta)) {
            pitchVelocity = pitchDelta;
        }

        float humanScale = canAttack ? 0.18f : lookingAtHitbox ? 0.28f : 0.55f;
        if (!MoveUtil.hasPlayerMovement()) {
            humanScale *= 0.62f;
        }

        float idleYaw = !canAttack && !lookingAtHitbox
                ? (float) Math.sin((mc.player.age + traceIndex) * 0.31f) * 0.10f
                : 0.0f;
        float idlePitch = !canAttack && !lookingAtHitbox
                ? (float) Math.cos((mc.player.age + traceIndex) * 0.23f) * 0.035f
                : 0.0f;

        lastYawDelta = yawDelta;
        lastPitchDelta = pitchDelta;

        return new Angle(
                currentAngle.getYaw() + yawVelocity + humanYaw * humanScale + idleYaw,
                MathHelper.clamp(currentAngle.getPitch() + pitchVelocity + humanPitch * humanScale + idlePitch, -89.5f, 89.5f)
        );
    }

    private Angle applyHumanProfile(Aura aura, Angle targetAngle, boolean canAttack) {
        List<float[]> trace = getSelectedTrace(aura);
        if (trace == null || trace.isEmpty()) {
            humanYaw *= 0.68f;
            humanPitch *= 0.68f;
            return targetAngle;
        }

        float[] frame = nextFrame(trace, canAttack);
        float recordedHumanYaw = frame.length > 0 ? frame[0] : 0.0f;
        float recordedHumanPitch = frame.length > 1 ? frame[1] : 0.0f;
        float aimOffsetYaw = frame.length > 4 ? frame[4] : 0.0f;
        float aimOffsetPitch = frame.length > 5 ? frame[5] : 0.0f;
        float targetDeltaYaw = frame.length > 6 ? frame[6] : 0.0f;
        float targetDeltaPitch = frame.length > 7 ? frame[7] : 0.0f;
        float forward = frame.length > 9 ? frame[9] : 0.0f;
        float sideways = frame.length > 10 ? frame[10] : 0.0f;
        float headDelta = frame.length > 13 ? frame[13] : 0.0f;
        float headBodyOffset = frame.length > 15 ? frame[15] : 0.0f;

        float movement = MathHelper.clamp((float) Math.hypot(forward, sideways), 0.0f, 1.0f);
        float targetYaw = recordedHumanYaw + targetDeltaYaw * 0.20f + sideways * 0.35f + headDelta * 0.12f + headBodyOffset * 0.08f;
        float targetPitch = recordedHumanPitch + targetDeltaPitch * 0.12f + forward * 0.05f;

        float maxYaw = canAttack ? 4.2f : 7.5f + movement * 2.0f;
        float maxPitch = canAttack ? 2.4f : 4.4f + movement;
        humanYaw = MathHelper.lerp(0.36f, humanYaw, MathHelper.clamp(targetYaw, -maxYaw, maxYaw));
        humanPitch = MathHelper.lerp(0.34f, humanPitch, MathHelper.clamp(targetPitch, -maxPitch, maxPitch));

        float offsetScale = canAttack ? 0.22f : 0.42f;
        float safeOffsetYaw = MathHelper.clamp(aimOffsetYaw, -5.0f, 5.0f) * offsetScale;
        float safeOffsetPitch = MathHelper.clamp(aimOffsetPitch, -3.0f, 3.0f) * offsetScale;
        return new Angle(
                targetAngle.getYaw() + safeOffsetYaw,
                MathHelper.clamp(targetAngle.getPitch() + safeOffsetPitch, -89.5f, 89.5f)
        );
    }

    private float[] nextFrame(List<float[]> trace, boolean canAttack) {
        int size = trace.size();
        if (traceIndex >= size) {
            traceIndex = 0;
        }

        if (canAttack) {
            int search = Math.min(size, 10);
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
        if (aura == null || aura.neuroProfile == null) {
            return null;
        }
        String selectedProfile = aura.neuroProfile.getSelected();
        if (selectedProfile == null || selectedProfile.isBlank()) {
            return null;
        }
        return NeiroCommand.getTraceSilently(selectedProfile);
    }

    private Angle approach(Angle currentAngle, Angle targetAngle, float smooth, float yawLimit, float pitchLimit) {
        Angle delta = MathAngle.calculateDelta(currentAngle, targetAngle);
        return new Angle(
                currentAngle.getYaw() + MathHelper.clamp(delta.getYaw(), -yawLimit, yawLimit) * smooth,
                MathHelper.clamp(currentAngle.getPitch() + MathHelper.clamp(delta.getPitch(), -pitchLimit, pitchLimit) * smooth, -89.5f, 89.5f)
        );
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}

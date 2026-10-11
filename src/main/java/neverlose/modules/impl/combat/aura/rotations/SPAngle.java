package neverlose.modules.impl.combat.aura.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import neverlose.IMinecraft;
import neverlose.debob;
import neverlose.modules.impl.combat.Aura;
import neverlose.modules.impl.combat.aura.Angle;
import neverlose.modules.impl.combat.aura.MathAngle;
import neverlose.modules.impl.combat.aura.impl.RotateConstructor;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class SPAngle extends RotateConstructor implements IMinecraft {

    private final SecureRandom random = new SecureRandom();
    private float yawVelocity   = 0;
    private float pitchVelocity = 0;
    private float styleYaw      = 0;
    private float stylePitch    = 0;
    private float offsetYaw     = 0;
    private float offsetPitch   = 0;
    private static final int POINT_HISTORY_SIZE = 6;
    private static final int LOCK_TICKS_MIN     = 4;
    private static final int LOCK_TICKS_MAX     = 9;

    private final List<Vec3d> recentPoints  = new ArrayList<>();
    private Vec3d lockedOffset              = null;
    private int   lockedTicksLeft           = 0;
    private int   attackHoldTicksLeft       = 0;
    private int   acquireBoostTicks         = 0;
    private boolean prevCanAttack           = false;
    private float stylePhase     = 0;
    private float stylePhaseRate = 0;
    private int lastEntityId = -1;

    public SPAngle() {
        super("SpookyTime");
    }

    private void reset() {
        yawVelocity     = 0;
        pitchVelocity   = 0;
        styleYaw        = 0;
        stylePitch      = 0;
        offsetYaw       = 0;
        offsetPitch     = 0;
        lockedOffset      = null;
        lockedTicksLeft   = 0;
        attackHoldTicksLeft = 0;
        acquireBoostTicks = 4;
        prevCanAttack   = false;
        recentPoints.clear();
        stylePhase     = random.nextFloat() * (float) (Math.PI * 2);
        stylePhaseRate = 0.04f + random.nextFloat() * 0.06f;
    }

    private List<Vec3d> generatePoints(Box bb, int count, double spread) {
        List<Vec3d> pts = new ArrayList<>(count);
        double w  = bb.maxX - bb.minX;
        double d  = bb.maxZ - bb.minZ;
        double h  = bb.maxY - bb.minY;
        double cx = (bb.minX + bb.maxX) / 2.0;
        double cz = (bb.minZ + bb.maxZ) / 2.0;
        for (int i = 0; i < count; i++) {
            double rx = (random.nextDouble() * 2.0 - 1.0) * (w * spread);
            double rz = (random.nextDouble() * 2.0 - 1.0) * (d * spread);
            double ry = normalHeight(h);
            pts.add(new Vec3d(cx + rx, bb.minY + ry, cz + rz));
        }
        return pts;
    }
    private Vec3d pointFromOffset(Box bb) {
        Vec3d c = bb.getCenter();
        double w = bb.maxX - bb.minX;
        double h = bb.maxY - bb.minY;
        double d = bb.maxZ - bb.minZ;
        double x = MathHelper.clamp(c.x + lockedOffset.x, bb.minX + w * 0.1, bb.maxX - w * 0.1);
        double y = MathHelper.clamp(c.y + lockedOffset.y, bb.minY + h * 0.1, bb.maxY - h * 0.1);
        double z = MathHelper.clamp(c.z + lockedOffset.z, bb.minZ + d * 0.1, bb.maxZ - d * 0.1);
        return new Vec3d(x, y, z);
    }

    private void lockTo(Vec3d best, Box bb) {
        pushHistory(best);
        lockedOffset = best.subtract(bb.getCenter());
    }

    private double normalHeight(double h) {
        double s = 0;
        for (int i = 0; i < 4; i++) s += random.nextDouble();
        double n = (s / 4.0 - 0.5) * 0.65 + 0.5;
        return h * Math.max(0.05, Math.min(0.95, n));
    }

    private float recentPenalty(Vec3d c) {
        float penalty = 0f;
        int   size    = recentPoints.size();
        for (int i = 0; i < size; i++) {
            Vec3d old = recentPoints.get(i);
            double dx   = c.x - old.x;
            double dy   = c.y - old.y;
            double dz   = c.z - old.z;
            double dist = Math.sqrt(dx * dx + dy * dy * 0.5 + dz * dz);
            float recencyW = (float) (i + 1) / size;
            float prox     = (float) Math.max(0, 1.0 - dist / 0.5);
            penalty += prox * recencyW * 2.5f;
        }
        return penalty;
    }

    private Vec3d pickBest(List<Vec3d> candidates, Angle currentAngle) {
        Vec3d best  = null;
        float bestS = Float.MAX_VALUE;
        for (Vec3d c : candidates) {
            Angle ca    = MathAngle.calculateAngle(c);
            Angle delta = MathAngle.calculateDelta(currentAngle, ca);
            float adist = (float) Math.hypot(delta.getYaw(), delta.getPitch());
            float score = adist / 180f + recentPenalty(c);
            if (score < bestS) {
                bestS = score;
                best  = c;
            }
        }
        return best;
    }

    private void pushHistory(Vec3d p) {
        recentPoints.add(p);
        if (recentPoints.size() > POINT_HISTORY_SIZE)
            recentPoints.remove(0);
    }

    private void tickStyle(boolean canAttack) {
        stylePhase += stylePhaseRate;
        if (stylePhase > Math.PI * 2) stylePhase -= (float) (Math.PI * 2);

        float rawSY = (float) (Math.sin(stylePhase) * 1.8 + Math.cos(stylePhase * 2.7f) * 0.6f);
        float rawSP = (float) (Math.cos(stylePhase * 1.3f) * 0.9f + Math.sin(stylePhase * 3.1f) * 0.35f);

        float scale = canAttack ? 0.78f : 0.56f;
        styleYaw   = MathHelper.lerp(0.48f, styleYaw,   MathHelper.clamp(rawSY, -7.0f, 7.0f)  * scale);
        stylePitch = MathHelper.lerp(0.48f, stylePitch, MathHelper.clamp(rawSP, -4.0f, 4.0f)  * scale);
    }

    private void tickOffset(boolean canAttack) {
        if (random.nextInt(20) == 0) {
            float maxOY = canAttack ? 7.0f : 12.0f;
            float maxOP = canAttack ? 4.8f :  7.5f;
            float rawOY = (float) (random.nextGaussian() * maxOY * 0.4);
            float rawOP = (float) (random.nextGaussian() * maxOP * 0.4);
            offsetYaw   = MathHelper.lerp(0.42f, offsetYaw,   MathHelper.clamp(rawOY, -maxOY, maxOY));
            offsetPitch = MathHelper.lerp(0.42f, offsetPitch, MathHelper.clamp(rawOP, -maxOP, maxOP));
        } else {
            offsetYaw   *= 0.97f;
            offsetPitch *= 0.97f;
        }
    }
    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle,
                                  Vec3d vec3d, Entity entity) {

        if (mc.player == null || entity == null) {
            return approach(currentAngle, targetAngle, 0.36f, 32f, 14f);
        }

        if (lastEntityId != entity.getId()) {
            reset();
            lastEntityId = entity.getId();
        }

        Aura    aura      = Aura.getInstance();
        boolean canAttack = debob.getInstance().getManager()
                .getAttackPerpetrator().getAttackHandler()
                .canAttack(aura.getConfig(), 0);
        boolean attackStateChanged = (canAttack != prevCanAttack);
        prevCanAttack = canAttack;

        Box bb = entity.getBoundingBox();

        if (canAttack) {
            // Держим стабильную точку у центра несколько тиков, а не прыгаем каждый тик.
            if (lockedOffset == null || attackHoldTicksLeft <= 0 || attackStateChanged) {
                List<Vec3d> candidates = generatePoints(bb, 12, 0.22);
                Vec3d       best       = pickBest(candidates, currentAngle);
                if (best != null) {
                    lockTo(best, bb);
                }
                attackHoldTicksLeft = 2 + random.nextInt(3);
            } else {
                attackHoldTicksLeft--;
            }
            lockedTicksLeft = 0;
        } else {
            if (lockedOffset != null && lockedTicksLeft > 0 && !attackStateChanged) {
                lockedTicksLeft--;
            } else {
                List<Vec3d> candidates = generatePoints(bb, 16, 0.40);
                Vec3d       best       = pickBest(candidates, currentAngle);
                if (best != null) {
                    lockTo(best, bb);
                    lockedTicksLeft = LOCK_TICKS_MIN +
                            random.nextInt(LOCK_TICKS_MAX - LOCK_TICKS_MIN + 1);
                }
            }
        }

        if (lockedOffset != null) {
            targetAngle = MathAngle.calculateAngle(pointFromOffset(bb));
        }
        tickStyle(canAttack);
        tickOffset(canAttack);
        float appliedOffsetYaw   = canAttack ? offsetYaw   * 0.15f : offsetYaw;
        float appliedOffsetPitch = canAttack ? offsetPitch * 0.15f : offsetPitch;

        Angle profiledTarget = new Angle(
                targetAngle.getYaw()   + appliedOffsetYaw,
                MathHelper.clamp(targetAngle.getPitch() + appliedOffsetPitch, -89.5f, 89.5f)
        );

        Angle angleDelta = MathAngle.calculateDelta(currentAngle, profiledTarget);
        float yawDelta   = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float distance   = MathHelper.clamp((float) mc.player.distanceTo(entity), 1.0f, 6.0f);
        float rotDist    = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
        float speedBoost = MathHelper.clamp(rotDist / 55f, 0f, 0.22f);
        float yawLimit   = canAttack
                ? 54f
                : MathHelper.clamp(24f + distance * 3.0f + rotDist * 0.35f, 28f, 44f);
        float pitchLimit = canAttack
                ? 20f
                : MathHelper.clamp(10f + distance * 1.3f + rotDist * 0.16f, 11f, 18f);
        float smooth     = canAttack
                ? 0.70f + speedBoost
                : 0.48f + speedBoost;
        if (acquireBoostTicks > 0) {
            smooth = Math.min(0.95f, smooth + 0.28f);
            acquireBoostTicks--;
        }
        yawVelocity   = MathHelper.lerp(smooth, yawVelocity,
                MathHelper.clamp(yawDelta,   -yawLimit,   yawLimit));
        pitchVelocity = MathHelper.lerp(smooth, pitchVelocity,
                MathHelper.clamp(pitchDelta, -pitchLimit, pitchLimit));
        float styleScale     = canAttack ? 0.1f : 1.0f;
        float safeStyleYaw   = (Math.abs(yawDelta)   > 1.5f ? styleYaw   : styleYaw   * 0.25f) * styleScale;
        float safeStylePitch = (Math.abs(pitchDelta)  > 0.9f ? stylePitch : stylePitch * 0.25f) * styleScale;
        return new Angle(
                currentAngle.getYaw()   + yawVelocity   + safeStyleYaw,
                MathHelper.clamp(
                        currentAngle.getPitch() + pitchVelocity + safeStylePitch,
                        -89.5f, 89.5f)
        );
    }

    private Angle approach(Angle current, Angle target,
                           float smooth, float yawLim, float pitchLim) {
        Angle d = MathAngle.calculateDelta(current, target);
        return new Angle(
                current.getYaw()   + MathHelper.clamp(d.getYaw(),   -yawLim,   yawLim)   * smooth,
                MathHelper.clamp(
                        current.getPitch() + MathHelper.clamp(d.getPitch(), -pitchLim, pitchLim) * smooth,
                        -89.5f, 89.5f)
        );
    }

    @Override
    public Vec3d randomValue() {
        return Vec3d.ZERO;
    }
}
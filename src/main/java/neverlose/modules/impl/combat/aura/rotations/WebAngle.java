package neverlose.modules.impl.combat.aura.rotations;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
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
import neverlose.util.move.MoveUtil;
import neverlose.util.timer.StopWatch;

import java.util.Random;

public class WebAngle extends RotateConstructor {

    private static final float SCAN_STEP = 0.05f;


    private final Random rng = new Random();


    private float currentResolve1 = 0f;
    private float currentResolve2 = 0f;
    private boolean wasAttacking   = false;


    private float noiseYaw   = 0f;
    private float noisePitch = 0f;
    private long  lastNoiseTick = 0;

    public WebAngle() {
        super("Modern");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
        StrikeManager attackHandler = debob.getInstance().getManager()
                .getAttackPerpetrator().getAttackHandler();
        Aura aura = Aura.getInstance();
        StopWatch attackTimer = attackHandler.getAttackTimer();
        boolean canAttack = entity != null && attackHandler.canAttack(aura.getConfig(), 0);
        boolean randomize = aura.isRandomizationEnabled();


        if (randomize && canAttack && !wasAttacking) {
            currentResolve1 = 10f + rng.nextFloat() * 7f;
            currentResolve2 = 5f  + rng.nextFloat() * 6f;
        } else if (!randomize) {
            currentResolve1 = 0f;
            currentResolve2 = 0f;
        }
        wasAttacking = canAttack;

        if (entity != null && canAttack) {
            if (isEntityPartiallyInWeb(entity)) {
                Vec3d freePoint = findBestFreeAimPoint(entity);
                targetAngle = MathAngle.calculateAngle(freePoint);
            } else {
                Vec3d aimPoint = Vector.hitbox(entity, 1, entity.isOnGround() ? 0.9F : 1.4F, 1, 2);
                targetAngle = MathAngle.calculateAngle(aimPoint);
            }
        }

        Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta   = angleDelta.getYaw();
        float pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        boolean lookingAtHitbox = false;
        if (entity != null && !canAttack
                && RaycastAngle.rayTrace(AngleConnection.INSTANCE.getRotation().toVector(), 4.0, entity.getBoundingBox())) {
            lookingAtHitbox = true;
        }

        float speed;
        if (canAttack) {

            speed = randomize ? 0.75f + rng.nextFloat() * 0.17f : 0.82f;
        } else if (lookingAtHitbox) {

            speed = randomize ? 0.04f + rng.nextFloat() * 0.04f : 0.06f;
        } else {

            speed = randomize ? 0.15f + rng.nextFloat() * 0.30f : 0.3f;
        }

        float lineYaw   = Math.abs(yawDelta   / rotationDifference) * 360;
        float linePitch = Math.abs(pitchDelta  / rotationDifference) * 180;


        float jitterYaw   = 0;
        float jitterPitch = 0;
        if (randomize && !canAttack && MoveUtil.hasPlayerMovement()) {
            long now = System.currentTimeMillis();

            if (now - lastNoiseTick > 60 + (long)(rng.nextFloat() * 80)) {

                noiseYaw   += (rng.nextFloat() - 0.5f) * 4.5f;
                noisePitch += (rng.nextFloat() - 0.5f) * 1.8f;

                noiseYaw   = MathHelper.clamp(noiseYaw,   -5f, 5f);
                noisePitch = MathHelper.clamp(noisePitch, -2f, 2f);
                lastNoiseTick = now;
            }
            jitterYaw   = noiseYaw;
            jitterPitch = noisePitch;
        } else if (canAttack) {

            noiseYaw   = 0;
            noisePitch = 0;
        }

        float resolve1 = canAttack ? 0 : currentResolve1;
        float resolve2 = canAttack ? 0 : currentResolve2;

        if (!aura.isState() || entity == null) {
            float speedFactor = MathHelper.clamp(1f - (rotationDifference / 180.0f), 0.1f, 1.0f);
            speed     = !attackTimer.finished(550) ? (randomize ? 0.04f + rng.nextFloat() * 0.02f : 0.05f) : (randomize ? 0.75f + rng.nextFloat() * 0.1f : 0.8f) * speedFactor;
            jitterYaw   = 0;
            jitterPitch = 0;
            resolve1    = 0;
            resolve2    = 0;
        }

        float moveYaw   = MathHelper.clamp(yawDelta,   -lineYaw,   lineYaw)   + resolve1;
        float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch) + resolve2;


        float actualSpeed = randomize ? MathHelper.lerp(rng.nextFloat(), speed * 0.85f, speed) : speed;

        Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
        moveAngle.setYaw(  MathHelper.lerp(actualSpeed, currentAngle.getYaw(),
                currentAngle.getYaw()   + moveYaw)   + jitterYaw);
        moveAngle.setPitch(MathHelper.lerp(actualSpeed, currentAngle.getPitch(),
                currentAngle.getPitch() + movePitch) + jitterPitch);

        return moveAngle;
    }

    private boolean isEntityPartiallyInWeb(Entity entity) {
        if (mc.world == null) return false;
        double x    = entity.getX();
        double z    = entity.getZ();
        double minY = entity.getY();
        double maxY = entity.getY() + entity.getHeight();

        for (double y = minY; y <= maxY; y += SCAN_STEP) {
            if (mc.world.getBlockState(BlockPos.ofFloored(x, y, z)).getBlock() == Blocks.COBWEB) {
                return true;
            }
        }
        return false;
    }

    private Vec3d findBestFreeAimPoint(Entity entity) {
        double x    = entity.getX();
        double z    = entity.getZ();
        double minY = entity.getY();
        double maxY = entity.getY() + entity.getHeight();

        double bestZoneStart  = -1;
        double bestZoneEnd    = -1;
        double bestZoneHeight = 0;
        double zoneStart      = -1;

        for (double y = minY; y <= maxY + SCAN_STEP; y += SCAN_STEP) {
            boolean inWeb = (y <= maxY) && mc.world != null
                    && mc.world.getBlockState(BlockPos.ofFloored(x, y, z)).getBlock() == Blocks.COBWEB;

            if (!inWeb && y <= maxY) {
                if (zoneStart < 0) zoneStart = y;
            } else {
                if (zoneStart >= 0) {
                    double zoneEnd    = Math.min(y, maxY);
                    double zoneHeight = zoneEnd - zoneStart;
                    if (zoneHeight > bestZoneHeight) {
                        bestZoneHeight = zoneHeight;
                        bestZoneStart  = zoneStart;
                        bestZoneEnd    = zoneEnd;
                    }
                    zoneStart = -1;
                }
            }
        }

        if (bestZoneStart >= 0) {
            double centerY = (bestZoneStart + bestZoneEnd) / 2.0;
            return new Vec3d(x, centerY, z);
        }

        return new Vec3d(x, maxY - SCAN_STEP, z);
    }

    @Override
    public Vec3d randomValue() {
        return new Vec3d(0, 0, 0);
    }
}

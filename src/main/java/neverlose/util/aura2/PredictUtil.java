package neverlose.util.aura2;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import neverlose.IMinecraft;

@UtilityClass
public class PredictUtil implements IMinecraft {

    public static Vec3d predict(LivingEntity target, double ticksAhead) {
        double dx = target.getX() - target.lastX;
        double dy = target.getY() - target.lastY;
        double dz = target.getZ() - target.lastZ;

        if (Math.hypot(dx, dz) * 20 <= 1 && Math.abs(dy) <= 1) {
            return target.getEntityPos().add(0, target.getHeight() / 2.0, 0);
        }

        float prevPitch = target.lastPitch;
        float prevYaw   = target.lastYaw;
        float curPitch  = target.getPitch();
        float curYaw    = target.getYaw();

        float extraPitch = curPitch + (curPitch - prevPitch);
        float extraYaw   = curYaw   + (curYaw   - prevYaw);

        Vec3d delta = new Vec3d(dx, dy, dz);
        Vec3d forward = Vec3d.fromPolar(extraPitch, extraYaw)
                .multiply(delta.length() * ticksAhead);

        Vec3d vec3d = target.getRotationVector(extraPitch, extraYaw);

        float f = extraPitch * 0.017453292F;
        double d = Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z);
        double e = forward.horizontalLength();
        boolean bl = target.getVelocity().y <= 0.0;
        double g = bl && target.hasStatusEffect(StatusEffects.SLOW_FALLING)
                ? Math.min(target.getFinalGravity(), 0.01)
                : target.getFinalGravity();
        double h = MathHelper.square(Math.cos((double) f));

        forward = forward.add(0.0, g * (-1.0 + h * 0.75), 0.0);

        double i;
        if (forward.y < 0.0 && d > 0.0) {
            i = forward.y * -0.1 * h;
            forward = forward.add(vec3d.x * i / d, i, vec3d.z * i / d);
        }

        if (f < 0.0F && d > 0.0) {
            i = e * (-MathHelper.sin(f)) * 0.04;
            forward = forward.add(-vec3d.x * i / d, i * 2.2f, -vec3d.z * i / d);
        }

        if (d > 0.0) {
            forward = forward.add(
                    (vec3d.x / d * e - forward.x) * 0.1,
                    0.0,
                    (vec3d.z / d * e - forward.z) * 0.1
            );
        }

        return target.getEntityPos().add(forward);
    }
}
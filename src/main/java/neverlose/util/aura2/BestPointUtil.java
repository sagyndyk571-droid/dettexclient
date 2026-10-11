package neverlose.util.aura2;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import neverlose.IMinecraft;

@UtilityClass
public class BestPointUtil implements IMinecraft {

    public Vec3d getNearestPoint(Entity entity) {
        Box box = entity.getBoundingBox();
        double step = 0.1;
        Vec3d best = null;
        double closest = Double.MAX_VALUE;

        for (double x = box.minX; x <= box.maxX; x += step) {
            for (double y = box.minY; y <= box.maxY; y += step) {
                for (double z = box.minZ; z <= box.maxZ; z += step) {
                    Vec3d sample = new Vec3d(x, y, z);
                    double dist = mc.player.getEyePos().distanceTo(sample);
                    if (dist < closest) {
                        closest = dist;
                        best = sample;
                    }
                }
            }
        }
        return best != null ? best : entity.getBoundingBox().getCenter();
    }

    public Vec3d getNearestVisiblePoint(Entity entity, Vec3d fallback, double range) {
        Box box = entity.getBoundingBox();
        double step = 0.2;
        Vec3d best = null;
        double closest = Double.MAX_VALUE;
        Vec3d eye = mc.player.getEyePos();

        for (double x = box.minX; x <= box.maxX; x += step) {
            for (double y = box.minY; y <= box.maxY; y += step) {
                for (double z = box.minZ; z <= box.maxZ; z += step) {
                    Vec3d sample = new Vec3d(x, y, z);
                    double dist = eye.distanceTo(sample);
                    if (dist < closest && dist <= range) {
                        HitResult hit = mc.world.raycast(new RaycastContext(
                                eye, sample,
                                RaycastContext.ShapeType.COLLIDER,
                                RaycastContext.FluidHandling.NONE,
                                mc.player));
                        if (hit.getType() == HitResult.Type.MISS) {
                            closest = dist;
                            best = sample;
                        }
                    }
                }
            }
        }
        return best != null ? best : fallback;
    }
}

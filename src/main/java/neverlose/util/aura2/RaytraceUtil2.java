package neverlose.util.aura2;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import neverlose.IMinecraft;

@UtilityClass
public class RaytraceUtil2 implements IMinecraft {

    public boolean rayTrace(Vec3d clientVec, double range, Box box) {
        Vec3d cameraVec = mc.player.getEyePos();
        return box.contains(cameraVec)
                || box.raycast(cameraVec, cameraVec.add(clientVec.multiply(range))).isPresent();
    }
}

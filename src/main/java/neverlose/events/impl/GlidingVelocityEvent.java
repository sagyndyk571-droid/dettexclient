package neverlose.events.impl;

import net.minecraft.util.math.Vec3d;
import neverlose.events.api.events.Event;

public class GlidingVelocityEvent implements Event {
    private Vec3d velocity;

    public GlidingVelocityEvent(Vec3d velocity) {
        this.velocity = velocity;
    }

    public Vec3d getVelocity() {
        return velocity;
    }

    public void setVelocity(Vec3d velocity) {
        this.velocity = velocity;
    }
}

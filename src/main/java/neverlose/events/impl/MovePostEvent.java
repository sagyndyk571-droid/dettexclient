package neverlose.events.impl;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.math.Vec3d;
import neverlose.events.api.events.Event;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MovePostEvent implements Event {
    float speed;
    Vec3d movementInput;
}

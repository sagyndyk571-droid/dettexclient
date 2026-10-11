package neverlose.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import neverlose.events.api.events.Event;

@Getter
@AllArgsConstructor
public class RotationUpdateEvent implements Event {
    byte type;
}

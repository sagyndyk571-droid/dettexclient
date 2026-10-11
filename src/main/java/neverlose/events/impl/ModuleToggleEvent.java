package neverlose.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import neverlose.events.api.events.Event;
import neverlose.modules.module.ModuleStructure;

@Getter
@AllArgsConstructor
public class ModuleToggleEvent implements Event {
    private final ModuleStructure module;
    private final boolean enabled;
}
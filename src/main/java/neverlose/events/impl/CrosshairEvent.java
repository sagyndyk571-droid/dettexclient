package neverlose.events.impl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import neverlose.events.api.events.callables.EventCancellable;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CrosshairEvent extends EventCancellable {
    DrawContext drawContext;
    float partialTicks;
}

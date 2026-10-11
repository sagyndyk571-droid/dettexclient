package neverlose.screens.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Общий контракт для основного ClickGUI и отдельного меню в стиле WexSide (оверлей рендерится из миксина).
 */
public interface ClickGuiOverlay {

    void renderOverlay(DrawContext context, RenderTickCounter tickCounter);

    boolean isClosing();
}

package neverlose.screens.clickgui.impl.module;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import neverlose.IMinecraft;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.setting.Setting;
import neverlose.modules.module.setting.SettingComponentAdder;
import neverlose.util.interfaces.AbstractSettingComponent;
import neverlose.util.render.Render2D;
import neverlose.util.render.font.Fonts;
import neverlose.util.render.shader.Scissor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Отдельное перетаскиваемое окно полноценных настроек модуля.
 * Открывается по ПКМ по карточке модуля (не по шестерёнке).
 */
@Getter
public class SettingsPopupWindow implements IMinecraft {

    private static final float POPUP_W = 238f;
    private static final float POPUP_H = 208f;
    private static final float HEADER_H = 28f;
    private static final float PAD = 10f;
    private static final float CLOSE_SIZE = 14f;
    private static final float RADIUS = 12f;
    private static final float ROW_H = 14f;
    private static final float ROW_GAP = 10f;

    private ModuleStructure module = null;
    private float popupX = 0;
    private float popupY = 0;
    private final List<AbstractSettingComponent> components = new ArrayList<>();

    private boolean dragging = false;
    private double dragStartMouseX = 0;
    private double dragStartMouseY = 0;
    private float dragStartPopupX = 0;
    private float dragStartPopupY = 0;

    private double settingsScroll = 0;

    public List<AbstractSettingComponent> getComponents() {
        return components;
    }

    public boolean isOpen() {
        return module != null;
    }

    public void open(ModuleStructure mod, float x, float y) {
        module = mod;
        popupX = x - POPUP_W / 2f;
        popupY = y - POPUP_H / 2f;
        components.clear();

        List<Setting> settings = mod.settings();
        if (settings != null && !settings.isEmpty()) {
            new SettingComponentAdder().addSettingComponent(settings, components);
        }
        for (AbstractSettingComponent component : components) {
            component.setWexsideStyle(true);
        }

        dragging = false;
        settingsScroll = 0;
    }

    public void close() {
        module = null;
        components.clear();
        dragging = false;
        settingsScroll = 0;
    }

    public void updateDrag(double mouseX, double mouseY) {
        if (!dragging) return;
        if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            dragging = false;
            return;
        }
        popupX = dragStartPopupX + (float) (mouseX - dragStartMouseX);
        popupY = dragStartPopupY + (float) (mouseY - dragStartMouseY);
    }

    public boolean isInHeader(double mouseX, double mouseY) {
        return mouseX >= popupX && mouseX <= popupX + POPUP_W &&
                mouseY >= popupY && mouseY <= popupY + HEADER_H;
    }

    public boolean isCloseClicked(double mouseX, double mouseY) {
        float closeX = popupX + POPUP_W - PAD - CLOSE_SIZE;
        float closeY = popupY + (HEADER_H - CLOSE_SIZE) / 2f;
        return mouseX >= closeX && mouseX <= closeX + CLOSE_SIZE &&
                mouseY >= closeY && mouseY <= closeY + CLOSE_SIZE;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= popupX && mouseX <= popupX + POPUP_W &&
                mouseY >= popupY && mouseY <= popupY + POPUP_H;
    }

    public void render(DrawContext context, float mouseX, float mouseY, float delta, int guiScale, float alphaMultiplier) {
        if (module == null) return;

        updateDrag(mouseX, mouseY);

        int alpha = (int) (255 * alphaMultiplier);
        Color bg = new Color(13, 14, 22, (int) (246 * alphaMultiplier));
        Color border = new Color(54, 56, 72, (int) (150 * alphaMultiplier));
        Color headerBg = new Color(15, 16, 25, (int) (245 * alphaMultiplier));

        Render2D.blur(popupX, popupY, POPUP_W, POPUP_H, 5f, 0, new Color(0, 0, 0, 0).getRGB());
        Render2D.rect(popupX, popupY, POPUP_W, POPUP_H, bg.getRGB(), RADIUS);
        Render2D.outline(popupX, popupY, POPUP_W, POPUP_H, 0.5f, border.getRGB(), RADIUS);
        Render2D.rect(popupX, popupY, POPUP_W, HEADER_H, headerBg.getRGB(), RADIUS, RADIUS, 0f, 0f);

        float iconX = popupX + PAD;
        float textY = popupY + 8f;
        Fonts.INTER_SEMIBOLD.draw(module.getName(), iconX, textY, 8.5f, new Color(244, 245, 250, alpha).getRGB());
        Fonts.INTER_SEMIBOLD.draw("Settings", iconX, textY + 11f, 4.8f, new Color(120, 124, 138, (int) (210 * alphaMultiplier)).getRGB());

        float closeX = popupX + POPUP_W - PAD - CLOSE_SIZE;
        float closeY = popupY + (HEADER_H - CLOSE_SIZE) / 2f;
        boolean closeHover = mouseX >= closeX && mouseX <= closeX + CLOSE_SIZE && mouseY >= closeY && mouseY <= closeY + CLOSE_SIZE;
        int closeColor = closeHover ? new Color(255, 255, 255, alpha).getRGB() : new Color(200, 200, 210, alpha).getRGB();
        Fonts.BOLD.draw("x", closeX + 3f, closeY + 1f, 8f, closeColor);

        float contentX = popupX + PAD;
        float contentY = popupY + HEADER_H + 8f;
        float contentW = POPUP_W - PAD * 2f;
        float contentH = POPUP_H - HEADER_H - 18f;

        Render2D.rect(contentX, contentY, contentW, contentH,
                new Color(18, 17, 27, (int) (230 * alphaMultiplier)).getRGB(), 10f);
        Render2D.outline(contentX, contentY, contentW, contentH, 0.5f,
                new Color(48, 49, 62, (int) (105 * alphaMultiplier)).getRGB(), 10f);

        float yOffset = (float) settingsScroll;
        Scissor.enable(contentX, contentY, contentW, contentH, guiScale);

        for (AbstractSettingComponent c : components) {
            if (!c.getSetting().isVisible()) continue;

            float h = ROW_H;
            if (c instanceof neverlose.screens.clickgui.impl.settingsrender.SelectComponent sc) {
                h = sc.getTotalHeight();
            } else if (c instanceof neverlose.screens.clickgui.impl.settingsrender.MultiSelectComponent mc) {
                h = mc.getTotalHeight();
            } else if (c instanceof neverlose.screens.clickgui.impl.settingsrender.ColorComponent cc) {
                h = cc.getTotalHeight();
            }

            float itemY = contentY + 12f + yOffset;

            if (itemY + h >= contentY && itemY <= contentY + contentH) {
                c.position(contentX + 10f, itemY);
                c.size(contentW - 20f, ROW_H);
                c.setAlphaMultiplier(alphaMultiplier);
                c.render(context, (int) mouseX, (int) mouseY, delta);
                Render2D.rect(contentX + 10f, itemY + h + 5f, contentW - 20f, 0.35f,
                        new Color(36, 37, 48, (int) (140 * alphaMultiplier)).getRGB(), 0);
            }

            yOffset += h + ROW_GAP;
        }
        Scissor.disable();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (module == null) return false;
        if (isCloseClicked(mouseX, mouseY) && button == 0) {
            close();
            return true;
        }
        if (button == 0 && isInHeader(mouseX, mouseY) && !isCloseClicked(mouseX, mouseY)) {
            dragging = true;
            dragStartMouseX = mouseX;
            dragStartMouseY = mouseY;
            dragStartPopupX = popupX;
            dragStartPopupY = popupY;
            return true;
        }
        if (contains(mouseX, mouseY)) {
            for (AbstractSettingComponent c : components) {
                if (c.getSetting().isVisible() && c.mouseClicked((int) mouseX, (int) mouseY, button)) return true;
            }
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (module == null) return;
        if (button == 0) dragging = false;
        for (AbstractSettingComponent c : components) {
            if (c.getSetting().isVisible()) c.mouseReleased((int) mouseX, (int) mouseY, button);
        }
    }

    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (module == null) return false;
        for (AbstractSettingComponent c : components) {
            if (c.getSetting().isVisible() && c.keyPressed(key, scancode, modifiers)) return true;
        }
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (module == null) return false;
        for (AbstractSettingComponent c : components) {
            if (c.getSetting().isVisible() && c.charTyped(chr, modifiers)) return true;
        }
        return false;
    }

    public void mouseScrolled(double vertical) {
        if (module == null) return;
        settingsScroll += vertical * -8f;
        settingsScroll = Math.min(0, settingsScroll);
    }
}


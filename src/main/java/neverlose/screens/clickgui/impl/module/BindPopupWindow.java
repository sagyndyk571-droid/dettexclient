package neverlose.screens.clickgui.impl.module;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import neverlose.IMinecraft;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.setting.implement.ModuleKeyBindSetting;
import neverlose.screens.clickgui.impl.settingsrender.BindComponent;
import neverlose.util.interfaces.AbstractSettingComponent;
import neverlose.util.render.Render2D;
import neverlose.util.render.font.Fonts;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@Getter
public class BindPopupWindow implements IMinecraft {

    private static final float POPUP_W = 112f;
    private static final float HEADER_H = 18f;
    private static final float PAD = 7f;
    private static final float CLOSE_SIZE = 12f;
    private static final float ITEM_H = 16f;
    private static final float ROW_GAP = 4f;
    private static final float BOTTOM_PAD = 8f;

    private ModuleStructure module = null;
    private float popupX = 0;
    private float popupY = 0;
    private float popupHeight = 49f;
    private final List<AbstractSettingComponent> components = new ArrayList<>();

    private boolean dragging = false;
    private double dragStartMouseX = 0;
    private double dragStartMouseY = 0;
    private float dragStartPopupX = 0;
    private float dragStartPopupY = 0;

    private float animProgress = 0f;
    private long lastAnimUpdateTime = System.currentTimeMillis();

    public boolean isOpen() {
        return module != null;
    }

    public void open(ModuleStructure mod, float x, float y) {
        module = mod;
        components.clear();

        BindComponent bind = new BindComponent(new ModuleKeyBindSetting(mod));
        bind.setWexsideStyle(true);
        bind.setBindPopupCompact(true);
        components.add(bind);

        int n = components.size();
        popupHeight = HEADER_H + PAD + n * ITEM_H + (n > 0 ? (n - 1) * ROW_GAP : 0) + BOTTOM_PAD;
        popupX = x - POPUP_W + 23f;
        popupY = y - 10f;
        dragging = false;

        animProgress = 0f;
        lastAnimUpdateTime = System.currentTimeMillis();
    }

    public void close() {
        module = null;
        components.clear();
        dragging = false;
        animProgress = 0f;
    }

    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastAnimUpdateTime) / 1000f, 0.1f);
        lastAnimUpdateTime = currentTime;
        animProgress += (1f - animProgress) * Math.min(1f, 12f * deltaTime);
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

    private float getCurrentTopY() {
        return popupY + (1f - animProgress) * 6f;
    }

    private float getCurrentHeight() {
        return popupHeight * Math.max(0.2f, animProgress);
    }

    public boolean isInHeader(double mouseX, double mouseY) {
        float topY = getCurrentTopY();
        return mouseX >= popupX && mouseX <= popupX + POPUP_W &&
                mouseY >= topY && mouseY <= topY + HEADER_H;
    }

    public boolean isCloseClicked(double mouseX, double mouseY) {
        float topY = getCurrentTopY();
        float closeX = popupX + POPUP_W - PAD - CLOSE_SIZE;
        float closeY = topY + 3.5f;
        return mouseX >= closeX && mouseX <= closeX + CLOSE_SIZE &&
                mouseY >= closeY && mouseY <= closeY + CLOSE_SIZE;
    }

    public boolean contains(double mouseX, double mouseY) {
        float topY = getCurrentTopY();
        return module != null && mouseX >= popupX && mouseX <= popupX + POPUP_W &&
                mouseY >= topY && mouseY <= topY + popupHeight;
    }

    public void render(DrawContext context, float mouseX, float mouseY, float delta, int guiScale, float alphaMultiplier) {
        if (module == null) return;

        updateAnimation();
        updateDrag(mouseX, mouseY);

        float progress = Math.max(0.01f, animProgress);
        int alpha = (int) (255 * alphaMultiplier * progress);
        float topY = getCurrentTopY();
        float h = getCurrentHeight();

        Render2D.gradientRect(popupX, topY, POPUP_W, h,
                new int[]{
                        new Color(10, 16, 20, alpha).getRGB(),
                        new Color(10, 17, 22, alpha).getRGB(),
                        new Color(7, 12, 15, alpha).getRGB(),
                        new Color(7, 12, 15, alpha).getRGB()
                }, 7f);
        Render2D.outline(popupX, topY, POPUP_W, h, 0.55f,
                new Color(45, 62, 75, (int) (140 * alphaMultiplier * progress)).getRGB(), 7f);

        Fonts.INTER_SEMIBOLD.draw("BIND", popupX + PAD, topY + 6.1f, 4.6f,
                new Color(112, 120, 132, (int) (205 * alphaMultiplier * progress)).getRGB());
        Fonts.INTER_MEDIUM.draw(trimText(module.getName(), POPUP_W - 48f, 4.4f),
                popupX + PAD + 25f, topY + 6.2f, 4.4f,
                new Color(190, 198, 212, (int) (220 * alphaMultiplier * progress)).getRGB());

        float closeX = popupX + POPUP_W - PAD - CLOSE_SIZE;
        float closeY = topY + 3.5f;
        Fonts.INTER_SEMIBOLD.draw("x", closeX + 3.5f, closeY + 2.1f, 5.8f,
                new Color(200, 205, 214, alpha).getRGB());

        Render2D.rect(popupX + PAD, topY + HEADER_H, POPUP_W - PAD * 2f, 0.4f,
                new Color(34, 45, 55, (int) (110 * alphaMultiplier * progress)).getRGB(), 0);

        float contentY = topY + HEADER_H + PAD;
        float itemW = POPUP_W - PAD * 2f;
        for (int i = 0; i < components.size(); i++) {
            AbstractSettingComponent c = components.get(i);
            float itemY = contentY + i * (ITEM_H + ROW_GAP);
            c.position(popupX + PAD, itemY);
            c.size(itemW, ITEM_H);
            c.setAlphaMultiplier(alphaMultiplier * progress);
            c.render(context, (int) mouseX, (int) mouseY, delta);
        }
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
                if (c.getSetting().isVisible() && c.mouseClicked(mouseX, mouseY, button)) return true;
            }
            return true;
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (module == null) return;
        if (button == 0) dragging = false;
        for (AbstractSettingComponent c : components) {
            if (c.getSetting().isVisible()) c.mouseReleased(mouseX, mouseY, button);
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

    private String trimText(String text, float maxWidth, float size) {
        if (text == null) return "";
        if (Fonts.INTER_MEDIUM.getWidth(text, size) <= maxWidth) return text;
        String value = text;
        while (value.length() > 1 && Fonts.INTER_MEDIUM.getWidth(value + "..", size) > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        return value + "..";
    }
}

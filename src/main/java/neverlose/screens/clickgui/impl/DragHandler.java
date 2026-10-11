package neverlose.screens.clickgui.impl;

import lombok.Getter;
import lombok.Setter;
import org.lwjgl.glfw.GLFW;
import neverlose.IMinecraft;

@Getter
@Setter
public class DragHandler implements IMinecraft {
    private float offsetX = 0;
    private float offsetY = 0;
    private float targetOffsetX = 0;
    private float targetOffsetY = 0;

    private boolean dragging = false;
    private int dragButton = GLFW.GLFW_MOUSE_BUTTON_LEFT;
    private double dragStartX = 0;
    private double dragStartY = 0;
    private float dragStartOffsetX = 0;
    private float dragStartOffsetY = 0;

    private static final float ANIMATION_SPEED = 10f;
    private long lastUpdateTime = System.currentTimeMillis();

    public void update(double mouseX, double mouseY) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastUpdateTime) / 1000f, 0.1f);
        lastUpdateTime = currentTime;

        if (dragging) {
            if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), dragButton) != GLFW.GLFW_PRESS) {
                dragging = false;
            } else {
                targetOffsetX = dragStartOffsetX + (float)(mouseX - dragStartX);
                targetOffsetY = dragStartOffsetY + (float)(mouseY - dragStartY);
                offsetX = targetOffsetX;
                offsetY = targetOffsetY;
            }
        }

        float diffX = targetOffsetX - offsetX;
        float diffY = targetOffsetY - offsetY;

        if (Math.abs(diffX) > 0.01f) {
            offsetX += diffX * ANIMATION_SPEED * deltaTime;
        } else {
            offsetX = targetOffsetX;
        }

        if (Math.abs(diffY) > 0.01f) {
            offsetY += diffY * ANIMATION_SPEED * deltaTime;
        } else {
            offsetY = targetOffsetY;
        }
    }

    /** Начать перетаскивание (левая или средняя кнопка). */
    public boolean startDrag(double mouseX, double mouseY, float bgX, float bgY, int bgWidth, int bgHeight) {
        return startDragWithButton(mouseX, mouseY, bgX, bgY, bgWidth, bgHeight, GLFW.GLFW_MOUSE_BUTTON_LEFT);
    }

    public boolean startDragWithButton(double mouseX, double mouseY, float bgX, float bgY, int bgWidth, int bgHeight, int button) {
        if (mouseX >= bgX && mouseX <= bgX + bgWidth && mouseY >= bgY && mouseY <= bgY + bgHeight) {
            dragging = true;
            dragButton = button;
            dragStartX = mouseX;
            dragStartY = mouseY;
            dragStartOffsetX = targetOffsetX;
            dragStartOffsetY = targetOffsetY;
            return true;
        }
        return false;
    }

    public void reset() {
        targetOffsetX = 0;
        targetOffsetY = 0;
    }

    public void stopDrag() {
        dragging = false;
    }

    public boolean isResetNeeded(int key, int mods) {
        boolean ctrlMod = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean altMod = (mods & GLFW.GLFW_MOD_ALT) != 0;
        boolean isCtrlKey = key == GLFW.GLFW_KEY_LEFT_CONTROL || key == GLFW.GLFW_KEY_RIGHT_CONTROL;
        boolean isAltKey = key == GLFW.GLFW_KEY_LEFT_ALT || key == GLFW.GLFW_KEY_RIGHT_ALT;
        return (isCtrlKey && altMod) || (isAltKey && ctrlMod);
    }
}
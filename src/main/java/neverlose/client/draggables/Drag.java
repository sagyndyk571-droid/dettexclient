package neverlose.client.draggables;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import neverlose.debob;
import neverlose.modules.impl.render.Hud;
import neverlose.util.ColorUtil;
import neverlose.util.animations.SweepAnim;
import neverlose.util.config.impl.drag.DragConfig;
import neverlose.util.render.Render2D;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Drag {

    private static final float OUTLINE_OFFSET = 3.0f;
    private static final float OUTLINE_THICKNESS = 1.0f;
    private static final int OUTLINE_COLOR = ColorUtil.rgba(255, 255, 255, 255);

    private static final Set<String> EXCLUDED_ELEMENTS = Set.of("Notifications", "Info");
    private static final Set<String> EXCLUDED_FROM_RESIZE = Set.of("Notifications");
    private static final float HANDLE_SIZE = 8.0f;
    private static final int HANDLE_COLOR = ColorUtil.rgba(255, 255, 255, 235);
    private static final int HANDLE_ACCENT = ColorUtil.rgba(90, 150, 255, 255);

    private static HudElement draggingElement;
    private static int startX, startY;

    private static HudElement resizingElement;
    private static float resizeStartScale = 1.0f;
    private static float resizeStartDistance = 1.0f;

    private static final Map<HudElement, SweepAnim> sweepAnimations = new HashMap<>();
    private static final Map<HudElement, Boolean> wasHovered = new HashMap<>();

    public static void onDraw(DrawContext context, int mouseX, int mouseY, float delta, boolean isChatScreen) {
        HudManager hudManager = getHudManager();
        if (hudManager == null) return;

        Hud hud = Hud.getInstance();
        if (hud == null || !hud.isState()) return;

        if (!isChatScreen) {
            if (draggingElement != null) {
                DragConfig.getInstance().save();
                draggingElement = null;
            }
            if (resizingElement != null) {
                DragConfig.getInstance().save();
                resizingElement = null;
            }
            sweepAnimations.clear();
            wasHovered.clear();
        }

        if (isChatScreen && draggingElement != null) {
            draggingElement.setX(mouseX - startX);
            draggingElement.setY(mouseY - startY);
        }

        if (isChatScreen && resizingElement != null) {
            HudElement element = resizingElement;
            double dx = mouseX - element.getX();
            double dy = mouseY - element.getY();
            float distance = (float) Math.max(1.0, Math.sqrt(dx * dx + dy * dy));
            element.setHudScale(resizeStartScale * (distance / resizeStartDistance));
        }

        hudManager.render(context, delta, mouseX, mouseY);

        if (isChatScreen) {
            for (HudElement element : hudManager.getEnabledElements()) {
                if (!element.visible()) {
                    sweepAnimations.remove(element);
                    wasHovered.remove(element);
                    continue;
                }

                if (EXCLUDED_ELEMENTS.contains(element.getName())) {
                    continue;
                }

                boolean isHovered = isHovered(element, mouseX, mouseY);
                boolean previouslyHovered = wasHovered.getOrDefault(element, false);

                float scaledWidth = scaledWidth(element);
                float scaledHeight = scaledHeight(element);

                float rounding = element.getRoundingRadius();
                float offset = OUTLINE_OFFSET;
                float outlineX = element.getX() - offset;
                float outlineY = element.getY() - offset;
                float outlineWidth = scaledWidth + offset * 2;
                float outlineHeight = scaledHeight + offset * 2;
                float outlineRounding = Math.max(0, rounding + offset);

                SweepAnim anim = sweepAnimations.computeIfAbsent(element, e -> new SweepAnim(0.05f));

                if (isHovered && !previouslyHovered) {
                    anim.start();
                } else if (!isHovered && previouslyHovered) {
                    anim.reset();
                }

                wasHovered.put(element, isHovered);

                anim.update();
                float progress = anim.getProgress();

                if (isHovered || anim.isActive()) {
                    float baseAlpha = 0.3f;
                    Render2D.glowOutline(outlineX, outlineY, outlineWidth, outlineHeight,
                            OUTLINE_THICKNESS, OUTLINE_COLOR, outlineRounding, progress, baseAlpha);
                }

                if (isResizable(element) && (isHovered || resizingElement == element)) {
                    drawResizeHandle(element);
                }

                if (!isHovered && anim.isCompleted()) {
                    sweepAnimations.remove(element);
                    wasHovered.remove(element);
                }
            }
        }
    }

    public static void onMouseClick(Click click) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!(mc.currentScreen instanceof ChatScreen)) return;

        if (click.button() == 0) {
            HudManager hudManager = getHudManager();
            if (hudManager == null) return;

            double mouseX = click.x();
            double mouseY = click.y();

            HudElement handleElement = getResizeHandleAt(mouseX, mouseY);
            if (handleElement != null) {
                resizingElement = handleElement;
                resizeStartScale = handleElement.getHudScale();
                double dx = mouseX - handleElement.getX();
                double dy = mouseY - handleElement.getY();
                resizeStartDistance = (float) Math.max(1.0, Math.sqrt(dx * dx + dy * dy));
                return;
            }

            HudElement element = hudManager.getElementAt(mouseX, mouseY);
            if (element != null) {
                if (element instanceof AbstractHudElement abstractElement && abstractElement.isDraggable()) {
                    draggingElement = element;
                    startX = (int) mouseX - element.getX();
                    startY = (int) mouseY - element.getY();
                }
            }
        }
    }

    public static void onMouseRelease(Click click) {
        if (click.button() == 0) {
            if (draggingElement != null) {
                DragConfig.getInstance().save();
                draggingElement = null;
            }
            if (resizingElement != null) {
                DragConfig.getInstance().save();
                resizingElement = null;
            }
        }
    }

    public static void resetDragging() {
        if (draggingElement != null) {
            DragConfig.getInstance().save();
            draggingElement = null;
        }
        if (resizingElement != null) {
            DragConfig.getInstance().save();
            resizingElement = null;
        }
        sweepAnimations.clear();
        wasHovered.clear();
    }

    public static boolean isDragging() {
        return draggingElement != null;
    }

    private static boolean isHovered(HudElement element, double mouseX, double mouseY) {
        int x = element.getX();
        int y = element.getY();
        float width = scaledWidth(element);
        float height = scaledHeight(element);
        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height;
    }

    private static float scaledWidth(HudElement element) {
        return element.getWidth() * element.getHudScale();
    }

    private static float scaledHeight(HudElement element) {
        return element.getHeight() * element.getHudScale();
    }

    private static boolean isResizable(HudElement element) {
        return element instanceof AbstractHudElement abstractElement
                && abstractElement.isDraggable()
                && !EXCLUDED_FROM_RESIZE.contains(element.getName());
    }

    private static void drawResizeHandle(HudElement element) {
        float handleX = element.getX() + scaledWidth(element) - HANDLE_SIZE;
        float handleY = element.getY() + scaledHeight(element) - HANDLE_SIZE;
        Render2D.rect(handleX, handleY, HANDLE_SIZE, HANDLE_SIZE, HANDLE_COLOR, 2.0f);
        Render2D.rect(handleX + 2.0f, handleY + 2.0f, HANDLE_SIZE - 4.0f, HANDLE_SIZE - 4.0f, HANDLE_ACCENT, 1.0f);
    }

    private static HudElement getResizeHandleAt(double mouseX, double mouseY) {
        HudManager hudManager = getHudManager();
        if (hudManager == null) return null;

        for (HudElement element : hudManager.getEnabledElements()) {
            if (!isResizable(element)) continue;

            float handleX = element.getX() + scaledWidth(element) - HANDLE_SIZE;
            float handleY = element.getY() + scaledHeight(element) - HANDLE_SIZE;
            if (mouseX >= handleX && mouseX <= handleX + HANDLE_SIZE &&
                    mouseY >= handleY && mouseY <= handleY + HANDLE_SIZE) {
                return element;
            }
        }
        return null;
    }

    private static HudManager getHudManager() {
        if (debob.getInstance() == null) return null;
        if (debob.getInstance().getManager() == null) return null;
        return debob.getInstance().getManager().getHudManager();
    }

    public static void tick() {
        HudManager hudManager = getHudManager();
        if (hudManager != null) {
            hudManager.tick();
        }
    }
}
package neverlose.screens.hud;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import neverlose.debob;
import neverlose.client.draggables.AbstractHudElement;
import neverlose.modules.impl.movement.ElytraTarget;
import neverlose.modules.module.ModuleStructure;
import neverlose.util.animations.Direction;
import neverlose.util.render.Render2D;
import neverlose.util.render.shader.Scissor;
import neverlose.util.render.font.Fonts;
import neverlose.util.string.KeyHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class HotKeys extends AbstractHudElement {

    private List<ModuleStructure> keysList = new ArrayList<>();
    private boolean showElytraForward = false;
    private int elytraForwardKey = GLFW.GLFW_KEY_UNKNOWN;
    private float animatedWidth = 80;
    private float animatedHeight = 23;
    private long lastUpdateTime = System.currentTimeMillis();

    private static final float ANIMATION_SPEED = 8.0f;

    public HotKeys() {
        super("HotKeys", 300, 40, 80, 23, true);
        stopAnimation();
    }

    @Override
    public boolean visible() {
        return !scaleAnimation.isFinished(Direction.BACKWARDS);
    }

    @Override
    public void tick() {
        if (debob.getInstance() == null ||
                debob.getInstance().getManager() == null ||
                debob.getInstance().getManager().getModuleProvider() == null) {
            return;
        }

        keysList = debob.getInstance().getManager().getModuleProvider().getModuleStructures().stream()
                .filter(module -> module.isState()
                        && module.getKey() != GLFW.GLFW_KEY_UNKNOWN)
                .toList();

        boolean inChat = isChat(mc.currentScreen);
        updateElytraForwardRow(inChat);
        boolean hasActiveKeys = !keysList.isEmpty() || showElytraForward;

        if (hasActiveKeys || inChat) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }

    private void updateElytraForwardRow(boolean inChat) {
        showElytraForward = false;
        elytraForwardKey = GLFW.GLFW_KEY_UNKNOWN;

        ElytraTarget elytraTarget = ElytraTarget.getInstance();
        if (elytraTarget == null || elytraTarget.getForwardBind() == null) {
            return;
        }

        elytraForwardKey = elytraTarget.getForwardBind().getKey();
        showElytraForward = ElytraTarget.shouldElytraTarget
                || inChat
                || (elytraTarget.isState() && elytraForwardKey != GLFW.GLFW_KEY_UNKNOWN);
    }

    private float lerp(float current, float target, float deltaTime) {
        float factor = (float) (1.0 - Math.pow(0.001, deltaTime * ANIMATION_SPEED));
        return current + (target - current) * factor;
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) return;

        float alphaFactor = alpha / 255.0f;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;

        deltaTime = Math.min(deltaTime, 0.1f);

        float x = getX();
        float y = getY();

        boolean hasVisibleRows = !keysList.isEmpty() || showElytraForward;
        boolean showExample = !hasVisibleRows && isChat(mc.currentScreen);

        float headerHeight = 15;
        float itemHeight = 11;
        float itemSpacing = 1.5f;
        float padding = 8;

        float targetWidth = 80;
        float targetHeight = headerHeight + 3;


        float headerTextWidth = Fonts.FONT67.getWidth("KeyBinds", 6);
        float iconWidth = 10;
        targetWidth = Math.max(headerTextWidth + iconWidth + padding * 2 + 10, targetWidth);

        if (showExample) {
            String exampleName = "Example Module";
            String exampleBind = "X";
            float bindWidth = Fonts.FONT67.getWidth(exampleBind, 6);
            float nameWidth = Fonts.FONT67.getWidth(exampleName, 6);
            targetWidth = Math.max(nameWidth + bindWidth + padding * 2 + 15, targetWidth);
            targetHeight += itemHeight + itemSpacing + 3;
        } else {
            for (ModuleStructure module : keysList) {
                String bind = KeyHelper.getKeyName(module.getKey());
                float bindWidth = Fonts.FONT67.getWidth(bind, 6);
                float nameWidth = Fonts.FONT67.getWidth(module.getName(), 6);
                targetWidth = Math.max(nameWidth + bindWidth + padding * 2 + 15, targetWidth);
                targetHeight += itemHeight + itemSpacing;
            }
            if (showElytraForward) {
                String name = "Перегон";
                String bind = formatKeyName(elytraForwardKey);
                String state = ElytraTarget.shouldElytraTarget ? "ON" : "OFF";
                float nameWidth = Fonts.FONT67.getWidth(name, 6);
                float bindWidth = Fonts.FONT67.getWidth(bind, 5.5f);
                float stateWidth = Fonts.FONT67.getWidth(state, 5.5f);
                targetWidth = Math.max(nameWidth + bindWidth + stateWidth + padding * 2 + 20, targetWidth);
                targetHeight += itemHeight + itemSpacing;
            }
            if (!keysList.isEmpty() || showElytraForward) {
                targetHeight += 3;
            }
        }

        animatedWidth = lerp(animatedWidth, targetWidth, deltaTime);
        animatedHeight = lerp(animatedHeight, targetHeight, deltaTime);

        if (Math.abs(animatedWidth - targetWidth) < 0.3f) {
            animatedWidth = targetWidth;
        }
        if (Math.abs(animatedHeight - targetHeight) < 0.3f) {
            animatedHeight = targetHeight;
        }

        setWidth((int) Math.ceil(animatedWidth));
        setHeight((int) Math.ceil(animatedHeight));

        int bgAlpha = (int) (240 * alphaFactor);
        int panelAlpha = HudTheme.backgroundAlpha(bgAlpha);
        int Color67 = new Color(20, 15, 23, panelAlpha).getRGB();
        int Color677 = new Color(29, 23, 33, panelAlpha).getRGB();

        Render2D.gradientRect(x , y, animatedWidth, animatedHeight,
                new int[]{
                        Color67,
                        Color67,
                        Color67,
                        Color677
                },
                8);


        Fonts.FONT67.draw("KeyBinds", x + padding, y + headerHeight / 2 - 3 - 0.5f + 2, 6,
                HudTheme.text(bgAlpha));

//        Render2D.rect(x + animatedWidth - padding - 12.3f, y + headerHeight / 2 - 4 + 2 - 2, 13, 13, new Color(0, 0, 0,255).getRGB(), 3);
        //Render2D.outline(x + animatedWidth - padding - 12.3f, y + headerHeight / 2 - 4 + 2 - 2, 13, 13,1,new Color(255, 255, 255,35).getRGB(),3);
        Fonts.iconsnew.draw("L", x + animatedWidth - padding - 10, y + headerHeight / 2 - 4 + 2 - 3, 12,
                HudTheme.accent(bgAlpha));

        Scissor.enable(x, y, animatedWidth, animatedHeight, 2);

        float currentY = y + headerHeight + 3;

        if (showExample) {
            String name = "Example Module";
            String bind = "X";


            Fonts.FONT67.draw(name, x + padding, currentY + itemHeight / 2 - 3 - 0.5f, 6,
                    HudTheme.text(bgAlpha));


            float bindWidth = Fonts.FONT67.getWidth(bind, 6);
            Fonts.FONT67.draw(bind, x + animatedWidth - padding - bindWidth - 3, currentY + itemHeight / 2 - 3 - 0.5f, 6,
                    HudTheme.accent(bgAlpha));
        } else {
            for (ModuleStructure module : keysList) {
                String bind = KeyHelper.getKeyName(module.getKey());


                Fonts.FONT67.draw(module.getName(), x + padding, currentY + itemHeight / 2 - 3 - 0.5f, 6,
                        HudTheme.text(bgAlpha));


                float bindWidth = Fonts.FONT67.getWidth(bind, 6);
                Fonts.FONT67.draw(bind, x + animatedWidth - padding - bindWidth - 3, currentY + itemHeight / 2 - 3 - 0.5f, 6,
                        HudTheme.accent(bgAlpha));

                currentY += itemHeight + itemSpacing;
            }
            if (showElytraForward) {
                String name = "Перегон";
                String bind = formatKeyName(elytraForwardKey);
                String state = ElytraTarget.shouldElytraTarget ? "ON" : "OFF";


                Fonts.FONT67.draw(name, x + padding, currentY + itemHeight / 2 - 3 - 0.5f, 6,
                        HudTheme.text(bgAlpha));


                float bindWidth = Fonts.FONT67.getWidth(bind, 5.5f);
                float stateWidth = Fonts.FONT67.getWidth(state, 5.5f);

                Fonts.FONT67.draw(bind, x + animatedWidth - padding - bindWidth - stateWidth - 10, currentY + itemHeight / 2 - 2.8f, 5.5f,
                        HudTheme.muted(bgAlpha));

                int stateColor = ElytraTarget.shouldElytraTarget ? HudTheme.accent(bgAlpha) : HudTheme.muted(bgAlpha);
                Fonts.FONT67.draw(state, x + animatedWidth - padding - stateWidth - 3, currentY + itemHeight / 2 - 2.8f, 5.5f,
                        stateColor);
            }
        }

        Scissor.disable();
    }

    private String formatKeyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN || key == -1) return "NONE";
        String name = KeyHelper.getKeyName(key);
        return name == null || name.isEmpty() ? "KEY" + key : name;
    }
}

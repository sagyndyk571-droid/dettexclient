package neverlose.screens.clickgui.impl.module.render;

import net.minecraft.client.gui.DrawContext;
import neverlose.modules.module.ModuleStructure;
import neverlose.screens.clickgui.ClickGuiStylePreference;
import neverlose.screens.clickgui.impl.module.handler.ModuleAnimationHandler;
import neverlose.screens.clickgui.impl.module.handler.ModuleScrollHandler;
import neverlose.screens.clickgui.impl.settingsrender.CheckboxComponent;
import neverlose.screens.clickgui.impl.settingsrender.ColorComponent;
import neverlose.screens.clickgui.impl.settingsrender.MultiSelectComponent;
import neverlose.screens.clickgui.impl.settingsrender.SelectComponent;
import neverlose.util.interfaces.AbstractSettingComponent;
import neverlose.util.render.Render2D;
import neverlose.util.render.shader.Scissor;
import neverlose.util.render.font.Fonts;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SettingsPanelRenderer {

    private static final float SETTINGS_PANEL_CORNER_RADIUS = 7f;
    private static final float CORNER_INSET = 3f;
    private static final int SETTING_HEIGHT = 13;
    private static final int SETTING_SPACING = 5;

    private final ModuleAnimationHandler animationHandler;

    private static int clampU8(float value) {
        int v = Math.round(value);
        return Math.max(0, Math.min(255, v));
    }

    public SettingsPanelRenderer(ModuleAnimationHandler animationHandler) {
        this.animationHandler = animationHandler;
    }

    public void render(DrawContext context, ModuleStructure selectedModule, List<AbstractSettingComponent> settingComponents,
                       float x, float y, float width, float height, float mouseX, float mouseY, float delta,
                       int guiScale, float alphaMultiplier, ModuleScrollHandler scrollHandler, ModuleAnimationHandler animHandler,
                       boolean wexsideStyle) {

        animHandler.updateVisibilityAnimations(settingComponents);

        if (wexsideStyle) {
            if (ClickGuiStylePreference.isGlassStyle()) {
int bgA = clampU8(222 * alphaMultiplier);
                Render2D.blur(x, y, width, height, 5f, 6f, new Color(0, 0, 0, 0).getRGB());
                Render2D.rect(x, y, width, height, new Color(9, 15, 20, bgA).getRGB(), 6f);
                Render2D.outline(x, y, width, height, 0.5f, new Color(58, 82, 112, clampU8(105 * alphaMultiplier)).getRGB(), 6f);
            } else {
                int bgA = clampU8(242 * alphaMultiplier);
                Render2D.rect(x, y, width, height, new Color(10, 16, 19, bgA).getRGB(), 6f);
                Render2D.outline(x, y, width, height, 0.5f, new Color(38, 50, 60, clampU8(135 * alphaMultiplier)).getRGB(), 6f);
            }
        } else {
            int panelAlpha = clampU8(15 * alphaMultiplier);
            int outlineAlpha = clampU8(215 * alphaMultiplier);
            Render2D.rect(x, y, width, height, new Color(64, 64, 64, panelAlpha).getRGB(), SETTINGS_PANEL_CORNER_RADIUS);
            Render2D.outline(x, y, width, height, 0.5f, new Color(55, 55, 55, outlineAlpha).getRGB(), SETTINGS_PANEL_CORNER_RADIUS);
        }

        if (selectedModule == null) {
            String text = "Select a module";
            float textSize = 6f;
            var emptyF = wexsideStyle ? Fonts.INTER_SEMIBOLD : Fonts.INTER_MEDIUM;
            float textWidth = emptyF.getWidth(text, textSize);
            float textHeight = emptyF.getHeight(textSize);
            float centerX = x + (width - textWidth) / 2f;
            float centerY = y + (height - textHeight) / 2f;
            emptyF.draw(text, centerX, centerY, textSize, new Color(100, 100, 100, clampU8(150 * alphaMultiplier)).getRGB());
            return;
        }

        if (wexsideStyle) {
            int titleC = new Color(224, 226, 235, clampU8(255 * alphaMultiplier)).getRGB();
            Fonts.INTER_SEMIBOLD.draw(selectedModule.getName(), x + 8, y + 7, 6.5f, titleC);
            String desc = selectedModule.getDescription();
            if (desc != null && !desc.isEmpty()) {
                String d = desc.length() > 48 ? desc.substring(0, 48) + "..." : desc;
                Fonts.INTER_SEMIBOLD.draw(d, x + 8, y + 17f, 4.5f, new Color(112, 120, 132, clampU8(220 * alphaMultiplier)).getRGB());
            }
            Render2D.rect(x + 8, y + 28, width - 16, 0.5f,
                    (ClickGuiStylePreference.isGlassStyle()
                            ? new Color(50, 68, 86, clampU8(105 * alphaMultiplier))
                            : new Color(31, 42, 48, clampU8(145 * alphaMultiplier))).getRGB(), 0);
        } else {
            Fonts.INTER_SEMIBOLD.draw(selectedModule.getName(), x + 8, y + 8, 7, new Color(255, 255, 255, clampU8(200 * alphaMultiplier)).getRGB());
            String desc = selectedModule.getDescription();
            if (desc != null && !desc.isEmpty()) {
                Fonts.INTER_MEDIUM.draw(desc.length() > 52 ? desc.substring(0, 55) + "..." : desc, x + 15, y + 20, 5, new Color(128, 128, 128, clampU8(150 * alphaMultiplier)).getRGB());
                Fonts.GUI_ICONS.draw("C", x + 8, y + 20, 6, new Color(128, 128, 128, clampU8(150 * alphaMultiplier)).getRGB());
            }
            Render2D.rect(x + 8, y + 30, width - 16, 1.25f, new Color(64, 64, 64, clampU8(64 * alphaMultiplier)).getRGB(), 10);
        }

        float sideInset = CORNER_INSET;
        float bottomInset = CORNER_INSET + 3;

        float clipY = y + (wexsideStyle ? 30f : 31f);
        float clipH = height - (wexsideStyle ? 25 : 26) - bottomInset;

        float clipX = x + sideInset;
        float clipW = width - sideInset * 2;

        Scissor.enable(clipX, clipY, clipW, clipH, guiScale);

        List<Float> finalYPositions = new ArrayList<>();
        List<Float> animatedHeights = new ArrayList<>();
        float posY = y + (wexsideStyle ? 35f : 38f) + (float) scrollHandler.getSettingDisplayScroll();

        for (AbstractSettingComponent c : settingComponents) {
            float heightAnim = animHandler.getHeightAnimations().getOrDefault(c, c.getSetting().isVisible() ? 1f : 0f);

            if (heightAnim <= 0.001f) {
                finalYPositions.add(null);
                animatedHeights.add(0f);
                continue;
            }

            finalYPositions.add(posY);

            float baseHeight = getComponentBaseHeight(c);
            float layoutHeight = baseHeight * heightAnim;
            animatedHeights.add(layoutHeight);
            posY += layoutHeight + getComponentSpacing(c) * heightAnim;
        }

        float visibleTop = clipY;
        float visibleBottom = clipY + clipH;

        for (int i = 0; i < settingComponents.size(); i++) {
            AbstractSettingComponent c = settingComponents.get(i);
            Float startY = finalYPositions.get(i);

            if (startY == null) continue;

            float visAnim = animHandler.getVisibilityAnimations().getOrDefault(c, c.getSetting().isVisible() ? 1f : 0f);
            float heightAnim = animHandler.getHeightAnimations().getOrDefault(c, c.getSetting().isVisible() ? 1f : 0f);

            if (visAnim <= 0.001f && heightAnim <= 0.001f) continue;

            float animatedHeight = animatedHeights.get(i);

            float componentAlpha = visAnim * alphaMultiplier;

            c.position(x + 8, startY);
            c.size(width - 16f, SETTING_HEIGHT);
            c.setAlphaMultiplier(componentAlpha);

            if (startY + animatedHeight >= visibleTop && startY <= visibleBottom && componentAlpha > 0.01f) {
                float itemClipTop = Math.max(startY, visibleTop);
                float itemClipBottom = Math.min(startY + animatedHeight, visibleBottom);
                float itemClipHeight = itemClipBottom - itemClipTop;

                if (itemClipHeight > 0.5f) {
                    boolean isExpandable = (c instanceof ColorComponent || c instanceof SelectComponent || c instanceof MultiSelectComponent);
                    if (isExpandable) {
                        context.getMatrices().pushMatrix();
                        c.render(context, (int) mouseX, (int) mouseY, delta);
                        context.getMatrices().popMatrix();
                    } else {
                        Scissor.enable(clipX, itemClipTop, clipW, itemClipHeight, guiScale);
                        context.getMatrices().pushMatrix();
                        c.render(context, (int) mouseX, (int) mouseY, delta);
                        context.getMatrices().popMatrix();
                        Scissor.disable();
                    }
                }
            }
        }

        Scissor.disable();

        boolean hasVisibleSettings = false;
        for (AbstractSettingComponent c : settingComponents) {
            float visAnim = animHandler.getVisibilityAnimations().getOrDefault(c, 0f);
            if (visAnim > 0.01f) {
                hasVisibleSettings = true;
                break;
            }
        }

        if (!hasVisibleSettings) {
            String text = "This module doesn't have settings";
            float textSize = 5f;
            var emptyF = wexsideStyle ? Fonts.INTER_SEMIBOLD : Fonts.INTER_MEDIUM;
            float textWidth = emptyF.getWidth(text, textSize);
            float textHeight = emptyF.getHeight(textSize);
            float centerX = x + (width - textWidth) / 2f;
            float centerY = y + (height - textHeight) / 2f + 2f;
            emptyF.draw(text, centerX, centerY, textSize, new Color(100, 100, 100, clampU8(150 * alphaMultiplier)).getRGB());
        }

        renderScrollFade(x + sideInset, clipY, width - sideInset * 2, clipH,
                scrollHandler.getSettingScrollTopFade() * alphaMultiplier,
                scrollHandler.getSettingScrollBottomFade() * alphaMultiplier, 60, 12);
    }

    /**
     * Только список настроек внутри карточки Wex (без внешней панели и заголовка модуля).
     */
    public void renderCardInline(DrawContext context, List<AbstractSettingComponent> settingComponents,
                                 float x, float y, float width, float height,
                                 float mouseX, float mouseY, float delta,
                                 int guiScale, float alphaMultiplier, ModuleScrollHandler scrollHandler,
                                 ModuleAnimationHandler animHandler) {

        animHandler.updateVisibilityAnimations(settingComponents);

        float clipY = y;
        float clipH = height;
        float clipX = x;
        float clipW = width;

        Scissor.enable(clipX, clipY, clipW, clipH, guiScale);

        List<Float> finalYPositions = new ArrayList<>();
        List<Float> animatedHeights = new ArrayList<>();
        // Убираем скролл для inline рендера - настройки просто обрезаются по высоте карточки
        float posY = y + 1f;

        for (AbstractSettingComponent c : settingComponents) {
            float heightAnim = animHandler.getHeightAnimations().getOrDefault(c, c.getSetting().isVisible() ? 1f : 0f);

            if (heightAnim <= 0.001f) {
                finalYPositions.add(null);
                animatedHeights.add(0f);
                continue;
            }

            finalYPositions.add(posY);

            float baseHeight = getComponentBaseHeight(c);
            float layoutHeight = baseHeight * heightAnim;
            animatedHeights.add(layoutHeight);
            posY += layoutHeight + getComponentSpacing(c) * heightAnim;
        }

        float visibleTop = clipY;
        float visibleBottom = clipY + clipH;

        for (int i = 0; i < settingComponents.size(); i++) {
            AbstractSettingComponent c = settingComponents.get(i);
            Float startY = finalYPositions.get(i);

            if (startY == null) continue;

            float visAnim = animHandler.getVisibilityAnimations().getOrDefault(c, c.getSetting().isVisible() ? 1f : 0f);
            float heightAnim = animHandler.getHeightAnimations().getOrDefault(c, c.getSetting().isVisible() ? 1f : 0f);

            if (visAnim <= 0.001f && heightAnim <= 0.001f) continue;

            float animatedHeight = animatedHeights.get(i);

            float componentAlpha = visAnim * alphaMultiplier;

            c.position(x, startY);
            c.size(width, SETTING_HEIGHT);
            c.setAlphaMultiplier(componentAlpha);

            if (startY + animatedHeight >= visibleTop && startY <= visibleBottom && componentAlpha > 0.01f) {

                context.getMatrices().pushMatrix();
                c.render(context, (int) mouseX, (int) mouseY, delta);
                context.getMatrices().popMatrix();

            }
        }

        for (int i = 0; i < settingComponents.size() - 1; i++) {
            Float startY = finalYPositions.get(i);
            Float nextY = finalYPositions.get(i + 1);
            if (startY == null || nextY == null) continue;

            float heightAnim = animHandler.getHeightAnimations().getOrDefault(settingComponents.get(i),
                    settingComponents.get(i).getSetting().isVisible() ? 1f : 0f);
            if (heightAnim <= 0.001f) continue;

            float lineY = startY + animatedHeights.get(i) + SETTING_SPACING * 0.5f - 1f;
            if (lineY >= visibleTop && lineY <= visibleBottom) {
                Render2D.rect(x, lineY, width, 0.25f,
                        new Color(40, 52, 58, clampU8(58 * alphaMultiplier)).getRGB(), 0);
            }
        }

        Scissor.disable();

        boolean hasVisibleSettings = false;
        for (AbstractSettingComponent c : settingComponents) {
            float visAnim = animHandler.getVisibilityAnimations().getOrDefault(c, 0f);
            if (visAnim > 0.01f) {
                hasVisibleSettings = true;
                break;
            }
        }

        if (!hasVisibleSettings) {
            String text = "Нет настроек";
            float textSize = 5f;
            float textWidth = Fonts.INTER_SEMIBOLD.getWidth(text, textSize);
            float textHeight = Fonts.INTER_SEMIBOLD.getHeight(textSize);
            float centerX = x + (width - textWidth) / 2f;
            float centerY = y + (height - textHeight) / 2f + 2f;
            Fonts.INTER_SEMIBOLD.draw(text, centerX, centerY, textSize,
                    new Color(120, 120, 120, clampU8(180 * alphaMultiplier)).getRGB());
        }

        renderScrollFade(clipX, clipY, clipW, clipH,
                scrollHandler.getSettingScrollTopFade() * alphaMultiplier,
                scrollHandler.getSettingScrollBottomFade() * alphaMultiplier, 60, 12);
    }

    public float calculateTotalHeight(List<AbstractSettingComponent> settingComponents, ModuleAnimationHandler animHandler) {
        float total = 0;
        int lastVisibleIndex = -1;
        for (int i = 0; i < settingComponents.size(); i++) {
            AbstractSettingComponent c = settingComponents.get(i);
            float heightAnim = animHandler.getHeightAnimations().getOrDefault(c, c.getSetting().isVisible() ? 1f : 0f);
            if (heightAnim > 0.001f) {
                lastVisibleIndex = i;
            }
        }

        for (int i = 0; i < settingComponents.size(); i++) {
            AbstractSettingComponent c = settingComponents.get(i);
            float heightAnim = animHandler.getHeightAnimations().getOrDefault(c, c.getSetting().isVisible() ? 1f : 0f);
            if (heightAnim <= 0.001f) continue;

            float baseHeight = getComponentBaseHeight(c);
            total += baseHeight * heightAnim;
            if (i != lastVisibleIndex) {
                total += getComponentSpacing(c) * heightAnim;
            }
        }
        return total;
    }

    private float getComponentBaseHeight(AbstractSettingComponent c) {
        if (c instanceof SelectComponent) return ((SelectComponent) c).getTotalHeight();
        if (c instanceof MultiSelectComponent) return ((MultiSelectComponent) c).getTotalHeight();
        if (c instanceof CheckboxComponent) return ((CheckboxComponent) c).getTotalHeight();
        if (c instanceof ColorComponent) return ((ColorComponent) c).getTotalHeight();
        return SETTING_HEIGHT;
    }

    private float getComponentSpacing(AbstractSettingComponent c) {
        if (c instanceof CheckboxComponent) return 4f;
        return SETTING_SPACING;
    }

    private void renderScrollFade(float x, float y, float w, float h, float topFade, float bottomFade, int alpha, int size) {
        if (topFade > 0.01f) {
            for (int i = 0; i < size; i++) {
                float fadeAlpha = alpha * topFade * (1f - i / (float) size);
                Render2D.rect(x, y + i, w, 1, new Color(8, 12, 15, clampU8(fadeAlpha)).getRGB(), 0);
            }
        }
        if (bottomFade > 0.01f) {
            for (int i = 0; i < size; i++) {
                float fadeAlpha = alpha * bottomFade * (i / (float) size);
                Render2D.rect(x, y + h - size + i, w, 1, new Color(8, 12, 15, clampU8(fadeAlpha)).getRGB(), 0);
            }
        }
    }
}

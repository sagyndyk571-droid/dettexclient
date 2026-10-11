package neverlose.screens.clickgui.impl.settingsrender;

import net.minecraft.client.gui.DrawContext;
import neverlose.modules.module.setting.implement.MultiSelectSetting;
import neverlose.util.interfaces.AbstractSettingComponent;
import neverlose.util.render.Render2D;
import neverlose.util.render.shader.Scissor;
import neverlose.util.render.font.Fonts;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class MultiSelectComponent extends AbstractSettingComponent {
    private final MultiSelectSetting multiSelectSetting;
    private boolean expanded = false;
    private float expandAnimation = 0f;
    private float hoverAnimation = 0f;
    private float scrollOffset = 0f;
    private float scrollOffsetAnimated = 0f;
    private boolean scrollingRight = true;
    private long scrollPauseTime = 0;

    private float descScrollOffset = 0f;
    private boolean descScrollingRight = true;
    private long descScrollPauseTime = 0;

    private float arrowRotation = 0f;

    private final Map<String, Float> optionHoverAnimations = new HashMap<>();
    private final Map<String, Float> checkAnimations = new HashMap<>();
    private final Map<String, Float> itemAlphaAnimations = new HashMap<>();
    private final Map<String, Float> itemXPositions = new HashMap<>();
    private final Map<String, Float> itemTargetPositions = new HashMap<>();
    private final Set<String> previousSelected = new HashSet<>();

    private float noneAlphaAnimation = 0f;

    private long lastUpdateTime = System.currentTimeMillis();

    private static final float ANIMATION_SPEED = 8f;
    private static final float COLLAPSE_SPEED = 15f;
    private static final long SCROLL_PAUSE_DURATION = 2000;
    private static final float BOX_WIDTH = 65f;
    private static final float OPTION_HEIGHT = 14f;
    private static final float SCROLL_PIXELS_PER_SECOND = 20f;
    private static final float DESC_PADDING = 8f;
    private static final float ITEM_ANIMATION_SPEED = 10f;
    private static final float POSITION_ANIMATION_SPEED = 8f;

    public MultiSelectComponent(MultiSelectSetting setting) {
        super(setting);
        this.multiSelectSetting = setting;
        for (String option : setting.getList()) {
            checkAnimations.put(option, setting.isSelected(option) ? 1f : 0f);
            // Ensure the option participates in the rendering animations even if it's not selected initially.
            itemAlphaAnimations.put(option, setting.isSelected(option) ? 1f : 0f);
            optionHoverAnimations.put(option, 0f);
        }
        previousSelected.addAll(setting.getSelected());

        float initX = 0;
        for (String item : setting.getList()) {
            if (setting.isSelected(item)) {
                itemAlphaAnimations.put(item, 1f);
                itemXPositions.put(item, initX);
                itemTargetPositions.put(item, initX);

                String displayText = item + ", ";
                initX += fontMed().getWidth(displayText, 5);
            }
        }

        noneAlphaAnimation = setting.getSelected().isEmpty() ? 1f : 0f;
    }

    private float getDeltaTime() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastUpdateTime) / 1000f, 0.1f);
        lastUpdateTime = currentTime;
        return deltaTime;
    }

    private float lerp(float current, float target, float speed) {
        float diff = target - current;
        if (Math.abs(diff) < 0.001f) {
            return target;
        }
        return current + diff * Math.min(speed, 1f);
    }

    private void updateItemAnimations(float deltaTime) {
        Set<String> currentSelected = new HashSet<>(multiSelectSetting.getSelected());

        for (String item : currentSelected) {
            if (!itemAlphaAnimations.containsKey(item)) {
                itemAlphaAnimations.put(item, 0f);

                float lastPos = 0;
                for (String existingItem : multiSelectSetting.getList()) {
                    if (itemXPositions.containsKey(existingItem)) {
                        float pos = itemXPositions.get(existingItem);
                        String text = existingItem + ", ";
                        float endPos = pos + fontMed().getWidth(text, 5);
                        if (endPos > lastPos) {
                            lastPos = endPos;
                        }
                    }
                }
                itemXPositions.put(item, lastPos);
                itemTargetPositions.put(item, lastPos);
            }
        }

        for (String item : itemAlphaAnimations.keySet()) {
            boolean isSelected = currentSelected.contains(item);
            float currentAlpha = itemAlphaAnimations.get(item);
            float targetAlpha = isSelected ? 1f : 0f;
            float newAlpha = lerp(currentAlpha, targetAlpha, deltaTime * ITEM_ANIMATION_SPEED);
            itemAlphaAnimations.put(item, newAlpha);
        }

        List<String> allItems = multiSelectSetting.getList();
        List<String> visibleItems = new ArrayList<>();

        for (String item : allItems) {
            if (itemAlphaAnimations.containsKey(item) && itemAlphaAnimations.get(item) > 0.001f) {
                visibleItems.add(item);
            }
        }

        float currentTargetX = 0;
        for (int i = 0; i < visibleItems.size(); i++) {
            String item = visibleItems.get(i);
            float itemAlpha = itemAlphaAnimations.getOrDefault(item, 0f);

            itemTargetPositions.put(item, currentTargetX);

            String displayText = item;
            if (i < visibleItems.size() - 1) {
                displayText += ", ";
            }

            float textWidth = fontMed().getWidth(displayText, 5);
            currentTargetX += textWidth * itemAlpha;
        }

        for (String item : visibleItems) {
            float targetX = itemTargetPositions.getOrDefault(item, 0f);
            float currentX = itemXPositions.getOrDefault(item, targetX);
            currentX = lerp(currentX, targetX, deltaTime * POSITION_ANIMATION_SPEED);
            itemXPositions.put(item, currentX);
        }

        List<String> toRemove = new ArrayList<>();
        for (String item : itemAlphaAnimations.keySet()) {
            boolean isSelected = currentSelected.contains(item);
            float alpha = itemAlphaAnimations.get(item);
            if (!isSelected && alpha < 0.001f) {
                toRemove.add(item);
            }
        }

        for (String item : toRemove) {
            // Keep entries stable to avoid "disappearing" text for 1+ frames.
            itemAlphaAnimations.put(item, 0f);
        }

        boolean hasVisibleItems = false;
        for (Float alpha : itemAlphaAnimations.values()) {
            if (alpha > 0.001f) {
                hasVisibleItems = true;
                break;
            }
        }

        float noneTarget = (!hasVisibleItems && currentSelected.isEmpty()) ? 1f : 0f;
        noneAlphaAnimation = lerp(noneAlphaAnimation, noneTarget, deltaTime * ITEM_ANIMATION_SPEED);

        previousSelected.clear();
        previousSelected.addAll(currentSelected);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float deltaTime = getDeltaTime();

        updateItemAnimations(deltaTime);

        boolean mainHovered = isMainHover(mouseX, mouseY);
        hoverAnimation = lerp(hoverAnimation, mainHovered ? 1f : 0f, deltaTime * ANIMATION_SPEED);

        float expandSpeed = expanded ? ANIMATION_SPEED : COLLAPSE_SPEED;
        expandAnimation = lerp(expandAnimation, expanded ? 1f : 0f, deltaTime * expandSpeed);

        float targetRotation = expanded ? 90f : 0f;
        arrowRotation = lerp(arrowRotation, targetRotation, deltaTime * ANIMATION_SPEED);

        if (isWexsideStyle()) {
            renderWexMultiSelect(context, mouseX, mouseY, deltaTime);
            return;
        }

        Fonts.GUI_ICONS.draw("I", x - 0.5f, y + height / 2 - 8.5f, 9, applyAlpha(new Color(210, 210, 220, 200)).getRGB());

        fontSemi().draw(multiSelectSetting.getName(), x + 9.5f, y + height / 2 - 7.5f, 6, applyAlpha(new Color(210, 210, 220, 200)).getRGB());

        String description = multiSelectSetting.getDescription();
        if (description != null && !description.isEmpty()) {
            renderScrollingDescription(description, deltaTime);
        }

        float boxX = x + width - BOX_WIDTH - 2;
        float boxY = y + height / 2 - 5;
        float boxHeight = 10f;

        int bgAlpha = 25 + (int)(hoverAnimation * 15);
        Render2D.rect(boxX, boxY, BOX_WIDTH, boxHeight, applyAlpha(new Color(55, 55, 55, bgAlpha)).getRGB(), 3f);

        int outlineAlpha = 60 + (int)(hoverAnimation * 40);
        Render2D.outline(boxX, boxY, BOX_WIDTH, boxHeight, 0.6f, applyAlpha(new Color(155, 155, 155, outlineAlpha)).getRGB(), 3f);

        renderSelectedText(boxX, boxY, BOX_WIDTH, boxHeight, deltaTime);

        // Стрелка ">" справа
        float arrowX = boxX + BOX_WIDTH - 10f;
        float arrowY = boxY + boxHeight / 2 - 3f;
        int arrowAlpha = (int)(180 * alphaMultiplier);
        Fonts.INTER_SEMIBOLD.draw(" ", arrowX, arrowY, 6f, new Color(150, 160, 180, arrowAlpha).getRGB());

        if (expandAnimation > 0.01f) {
            renderExpandedOptions(context, mouseX, mouseY, boxX, boxY + boxHeight + 2, deltaTime, BOX_WIDTH);
        }
    }

    private boolean wexHasDesc() {
        String d = multiSelectSetting.getDescription();
        return d != null && !d.isEmpty();
    }

    private float getWexBoxW() {
        return Math.min(BOX_WIDTH, Math.max(36f, width - 4f));
    }

    private float getWexBoxY() {
        // Dropdown всегда на одной линии с названием
        return y;
    }

    private String truncateWexLine(String text, float maxW) {
        String s = text;
        while (s.length() > 1 && fontMed().getWidth(s, 4.5f) > maxW) {
            s = s.substring(0, s.length() - 1);
        }
        if (!s.equals(text)) s = s + "..";
        return s;
    }

    private String truncateWexName(String text, float maxW) {
        if (fontSemi().getWidth(text, 6f) <= maxW) return text;
        String s = text;
        while (s.length() > 1 && fontSemi().getWidth(s + "..", 6f) > maxW) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "..";
    }

    private void renderWexMultiSelect(DrawContext context, int mouseX, int mouseY, float deltaTime) {
        float boxW = getWexBoxW();
        float boxX = x + width - boxW - 2f;
        float boxY = getWexBoxY();
        float boxHeight = 10f;

        float nameMax = width - boxW - 10f;
        
        fontSemi().draw(truncateWexName(multiSelectSetting.getName(), nameMax), x + 1f, y + 2f, 6f,
                applyAlpha(new Color(209, 209, 209, 220)).getRGB());
        // Убрали описание в wexside стиле

        int bgAlpha = 25 + (int) (hoverAnimation * 15);
        Render2D.rect(boxX, boxY, boxW, boxHeight, applyAlpha(new Color(24, 26, 38, 230)).getRGB(), 3f);

        int outlineAlpha = 60 + (int) (hoverAnimation * 40);
        Render2D.outline(boxX, boxY, boxW, boxHeight, 0.5f, applyAlpha(new Color(58, 64, 84, outlineAlpha)).getRGB(), 3f);

        renderSelectedText(boxX, boxY, boxW, boxHeight, deltaTime);

        // Стрелка ">" справа
        float arrowX = boxX + boxW - 10f;
        float arrowY = boxY + boxHeight / 2 - 3f;
        int arrowAlpha = (int)(180 * alphaMultiplier);
        Fonts.INTER_SEMIBOLD.draw("", arrowX + 1f, arrowY - 0.3f, 5.4f, new Color(165, 173, 190, arrowAlpha).getRGB());

        if (expandAnimation > 0.01f) {
            renderExpandedOptions(context, mouseX, mouseY, boxX, boxY + boxHeight + 2, deltaTime, boxW);
        }
    }

    private void renderArrowIcon(float iconX, float iconY) {
        int arrowAlpha = 120 + (int)(hoverAnimation * 60);

        float centerX = iconX + 4f;
        float centerY = iconY + 4f;

        float rad = (float) Math.toRadians(arrowRotation);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float offsetX = -4f;
        float offsetY = -4f;

        float rotatedX = centerX + (offsetX * cos - offsetY * sin);
        float rotatedY = centerY + (offsetX * sin + offsetY * cos);

//        Fonts.GUI_ICONS.draw("W", rotatedX, rotatedY, 8, applyAlpha(new Color(180, 180, 185, arrowAlpha)).getRGB());
    }

    private void renderScrollingDescription(String description, float deltaTime) {
        float descY = y + height / 2 + 0.5f;
        float boxX = x + width - BOX_WIDTH - 2;
        float availableWidth = boxX - x - DESC_PADDING;
        float descWidth = fontMed().getWidth(description, 5);

        if (descWidth <= availableWidth) {
            descScrollOffset = 0;
            fontMed().draw(description, x + 0.5f, descY, 5, applyAlpha(new Color(128, 128, 128, 128)).getRGB());
        } else {
            updateDescScrollAnimation(deltaTime, descWidth, availableWidth);

            float maxScroll = descWidth - availableWidth + 5;
            float currentScroll = descScrollOffset * maxScroll;

            Scissor.enable(x, descY - 2, availableWidth, 10, 2);
            fontMed().draw(description, x + 0.5f - currentScroll, descY, 5, applyAlpha(new Color(128, 128, 128, 128)).getRGB());
            Scissor.disable();
        }
    }

    private void updateDescScrollAnimation(float deltaTime, float textWidth, float availableWidth) {
        long currentTime = System.currentTimeMillis();

        if (descScrollPauseTime > 0) {
            if (currentTime - descScrollPauseTime < SCROLL_PAUSE_DURATION) {
                return;
            }
            descScrollPauseTime = 0;
        }

        float scrollDistance = textWidth - availableWidth + 5;
        if (scrollDistance <= 0) {
            descScrollOffset = 0;
            return;
        }

        float scrollSpeed = SCROLL_PIXELS_PER_SECOND / scrollDistance;

        if (descScrollingRight) {
            descScrollOffset += deltaTime * scrollSpeed;
            if (descScrollOffset >= 1f) {
                descScrollOffset = 1f;
                descScrollingRight = false;
                descScrollPauseTime = currentTime;
            }
        } else {
            descScrollOffset -= deltaTime * scrollSpeed;
            if (descScrollOffset <= 0f) {
                descScrollOffset = 0f;
                descScrollingRight = true;
                descScrollPauseTime = currentTime;
            }
        }
    }

    private void renderSelectedText(float boxX, float boxY, float boxWidth, float boxHeight, float deltaTime) {
        float textY = boxY + boxHeight / 2 - 2.5f;
        float availableWidth = boxWidth - 4;
        float baseX = boxX + 4;

        Scissor.enable(boxX + 1, boxY, availableWidth + 2, boxHeight,2 );

        if (noneAlphaAnimation > 0.01f) {
            int noneAlpha = (int)(200 * noneAlphaAnimation * alphaMultiplier);
            fontMed().draw("None", baseX, textY, 5, new Color(160, 160, 165, noneAlpha).getRGB());
        }

        List<String> allItems = multiSelectSetting.getList();
        List<String> visibleItems = new ArrayList<>();

        for (String item : allItems) {
            if (itemAlphaAnimations.containsKey(item) && itemAlphaAnimations.get(item) > 0.001f) {
                visibleItems.add(item);
            }
        }

        if (visibleItems.isEmpty()) {
            Scissor.disable();
            return;
        }

        float totalWidth = 0;
        for (int i = 0; i < visibleItems.size(); i++) {
            String item = visibleItems.get(i);
            float itemAlpha = itemAlphaAnimations.getOrDefault(item, 0f);

            String displayText = item;
            if (i < visibleItems.size() - 1) {
                displayText += ", ";
            }
            totalWidth += fontMed().getWidth(displayText, 5) * itemAlpha;
        }

        if (totalWidth <= availableWidth) {
            scrollOffset = 0;
            scrollOffsetAnimated = lerp(scrollOffsetAnimated, 0, deltaTime * POSITION_ANIMATION_SPEED);
        } else {
            updateScrollAnimation(deltaTime, totalWidth, availableWidth);
            scrollOffsetAnimated = lerp(scrollOffsetAnimated, scrollOffset, deltaTime * POSITION_ANIMATION_SPEED);
        }

        float maxScroll = Math.max(0, totalWidth - availableWidth + 5);
        float currentScroll = scrollOffsetAnimated * maxScroll;

        for (int i = 0; i < visibleItems.size(); i++) {
            String item = visibleItems.get(i);
            float itemAlpha = itemAlphaAnimations.getOrDefault(item, 0f);
            float itemX = itemXPositions.getOrDefault(item, 0f);

            String displayText = item;
            if (i < visibleItems.size() - 1) {
                displayText += ", ";
            }

            float renderX = baseX + itemX - currentScroll;

            int alpha = (int)(200 * itemAlpha * alphaMultiplier);
            if (alpha > 0) {
                fontMed().draw(displayText, renderX, textY, 5, new Color(160, 160, 165, alpha).getRGB());
            }
        }

        Scissor.disable();
    }

    private void updateScrollAnimation(float deltaTime, float textWidth, float availableWidth) {
        long currentTime = System.currentTimeMillis();

        if (scrollPauseTime > 0) {
            if (currentTime - scrollPauseTime < SCROLL_PAUSE_DURATION) {
                return;
            }
            scrollPauseTime = 0;
        }

        float scrollDistance = textWidth - availableWidth + 5;
        if (scrollDistance <= 0) {
            scrollOffset = 0;
            return;
        }

        float scrollSpeed = SCROLL_PIXELS_PER_SECOND / scrollDistance;

        if (scrollingRight) {
            scrollOffset += deltaTime * scrollSpeed;
            if (scrollOffset >= 1f) {
                scrollOffset = 1f;
                scrollingRight = false;
                scrollPauseTime = currentTime;
            }
        } else {
            scrollOffset -= deltaTime * scrollSpeed;
            if (scrollOffset <= 0f) {
                scrollOffset = 0f;
                scrollingRight = true;
                scrollPauseTime = currentTime;
            }
        }
    }

    private void renderExpandedOptions(DrawContext context, int mouseX, int mouseY, float boxX, float startY, float deltaTime, float panelW) {
        List<String> options = multiSelectSetting.getList();

        float fullPanelHeight = options.size() * OPTION_HEIGHT;
        float visibleHeight = fullPanelHeight * expandAnimation;

        float panelAlpha = expandAnimation * alphaMultiplier;

        int panelBgAlpha = (int)(200 * panelAlpha);
        Render2D.rect(boxX, startY, panelW, visibleHeight, new Color(23, 28, 43, panelBgAlpha).getRGB(), 3f);

        int panelOutlineAlpha = (int)(100 * panelAlpha);
        Render2D.outline(boxX, startY, panelW, visibleHeight, 0.5f, new Color(80, 80, 85, panelOutlineAlpha).getRGB(), 3f);

        if (visibleHeight < 1f) return;

        Scissor.enable(boxX, startY, panelW, visibleHeight,2);

        float optionY = startY;

        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);

            boolean optionHovered = mouseX >= boxX && mouseX <= boxX + panelW &&
                    mouseY >= optionY && mouseY <= optionY + OPTION_HEIGHT &&
                    expandAnimation > 0.8f;

            float hoverAnim = optionHoverAnimations.getOrDefault(option, 0f);
            hoverAnim = lerp(hoverAnim, optionHovered ? 1f : 0f, deltaTime * ANIMATION_SPEED);
            optionHoverAnimations.put(option, hoverAnim);

            boolean isSelected = multiSelectSetting.isSelected(option);
            float checkAnim = checkAnimations.getOrDefault(option, 0f);
            checkAnim = lerp(checkAnim, isSelected ? 1f : 0f, deltaTime * 10f);
            checkAnimations.put(option, checkAnim);

            if (hoverAnim > 0.01f) {
                int hoverBgAlpha = (int)(30 * hoverAnim * panelAlpha);
                Render2D.rect(boxX + 2, optionY + 1, panelW - 4, OPTION_HEIGHT - 2,
                        new Color(100, 100, 105, hoverBgAlpha).getRGB(), 2f);
            }

            if (checkAnim > 0.01f) {
                Render2D.rect(boxX + 2, optionY + 1, panelW - 4, OPTION_HEIGHT - 2,
                        new Color(55, 55, 60, (int) (42 * checkAnim * panelAlpha)).getRGB(), 2f);
            }

            float textX = boxX + 6f;
            float textY = optionY + OPTION_HEIGHT / 2 - 2.5f;

            float availableTextWidth = panelW - 12f;
            String displayOption = option;
            float optionTextWidth = fontMed().getWidth(option, 5);

            if (optionTextWidth > availableTextWidth) {
                while (fontMed().getWidth(displayOption + "..", 5) > availableTextWidth && displayOption.length() > 1) {
                    displayOption = displayOption.substring(0, displayOption.length() - 1);
                }
                displayOption += "..";
            }

            int textGray = (int)(140 + checkAnim * 40 + hoverAnim * 20);
            int textAlpha = (int)(200 * panelAlpha);
            fontMed().draw(displayOption, textX, textY, 5, new Color(textGray, textGray, textGray + 5, textAlpha).getRGB());

            optionY += OPTION_HEIGHT;
        }

        Scissor.disable();
    }

    private boolean isMainHover(double mouseX, double mouseY) {
        float boxW = isWexsideStyle() ? getWexBoxW() : BOX_WIDTH;
        float boxX = x + width - boxW - 2;
        float boxY = isWexsideStyle() ? getWexBoxY() : y + height / 2 - 5;
        float boxHeight = 10f;
        return mouseX >= boxX && mouseX <= boxX + boxW && mouseY >= boxY && mouseY <= boxY + boxHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            float boxW = isWexsideStyle() ? getWexBoxW() : BOX_WIDTH;
            float boxX = x + width - boxW - 2;
            float boxY = isWexsideStyle() ? getWexBoxY() : y + height / 2 - 5;

            if (isMainHover(mouseX, mouseY)) {
                expanded = !expanded;
                return true;
            }

            if (expanded && expandAnimation > 0.8f) {
                float startY = boxY + 10f + 2;

                float optionY = startY;
                for (String option : multiSelectSetting.getList()) {
                    if (mouseX >= boxX && mouseX <= boxX + boxW &&
                            mouseY >= optionY && mouseY <= optionY + OPTION_HEIGHT) {
                        if (multiSelectSetting.isSelected(option)) {
                            multiSelectSetting.getSelected().remove(option);
                        } else {
                            multiSelectSetting.getSelected().add(option);
                        }
                        return true;
                    }
                    optionY += OPTION_HEIGHT;
                }
            }
        }
        return false;
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        float h = getTotalHeight();
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + h;
    }

    public float getTotalHeight() {
        float baseHeight = isWexsideStyle() ? 14f : height;
        float expandedHeight = multiSelectSetting.getList().size() * OPTION_HEIGHT * expandAnimation;
        return baseHeight + expandedHeight;
    }
}

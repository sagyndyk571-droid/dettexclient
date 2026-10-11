package neverlose.screens.clickgui.impl.settingsrender;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import neverlose.screens.clickgui.WexKnobMetrics;
import neverlose.screens.clickgui.WexTrackMetrics;
import neverlose.util.interfaces.AbstractSettingComponent;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.render.Render2D;

import java.awt.*;

public class SliderComponent extends AbstractSettingComponent {
    private final SliderSettings sliderSettings;
    private boolean dragging = false;
    private float animatedPercentage = 0f;
    private float knobAnimation = 0f;

    private boolean inputMode = false;
    private String inputText = "";
    private int cursorPosition = 0;

    private float inputAnimation = 0f;
    private float hoverAnimation = 0f;
    private float unitsAlpha = 1f;
    private float valueOffsetX = 0f;
    private float backgroundAlpha = 0f;

    private long lastUpdateTime = System.currentTimeMillis();

    private static final float ANIMATION_SPEED = 8f;
    private static final float FAST_ANIMATION_SPEED = 12f;

    public SliderComponent(SliderSettings setting) {
        super(setting);
        this.sliderSettings = setting;
        float range = sliderSettings.getMax() - sliderSettings.getMin();
        if (range > 0) {
            this.animatedPercentage = (sliderSettings.getValue() - sliderSettings.getMin()) / range;
        }
    }

    private int clampAlpha(float alpha) {
        return Math.max(0, Math.min(255, (int) alpha));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (dragging) {
            updateValue(mouseX);
        }

        float deltaTime = getDeltaTime();
        updateAnimations(mouseX, mouseY, deltaTime);

        float range = sliderSettings.getMax() - sliderSettings.getMin();
        float targetPercentage = range > 0 ? (sliderSettings.getValue() - sliderSettings.getMin()) / range : 0f;
        animatedPercentage += (targetPercentage - animatedPercentage) * 0.25f;
    
        float knobTarget = dragging ? 1f : 0f;
        knobAnimation += (knobTarget - knobAnimation) * 0.25f;
        knobAnimation = Math.max(0f, Math.min(1f, knobAnimation));

        String valueText = sliderSettings.isInteger()
                ? String.valueOf((int) sliderSettings.getValue())
                : String.format("%.1f", sliderSettings.getValue());

        if (isWexsideStyle()) {
            renderWexSlider(valueText);
            return;
        }

        if (!isWexsideStyle()) {
            float nameWidth = fontSemi().getWidth(sliderSettings.getName(), 6);
            float valueWidth = fontSliderValue().getWidth(valueText, 5);
            float totalWidth = nameWidth + valueWidth + 4;
            float textX = x + 1f;
            float textY = y - 6.5f;  // 8 * 0.8
            fontSemi().draw(sliderSettings.getName(), textX, textY, 5f, applyAlpha(new Color(210, 210, 220, 200)).getRGB());  // 6 * 0.8
            fontSliderValue().draw(valueText, textX + nameWidth + 2, textY, 4f, applyAlpha(new Color(160, 160, 165, 200)).getRGB());  // 5 * 0.8
        } else {
            float nameWidth = fontSemi().getWidth(sliderSettings.getName(), 5f);  // 6 * 0.8
            float valueWidth = fontSliderValue().getWidth(valueText, 4f);  // 5 * 0.8
            float totalWidth = nameWidth + valueWidth + 4;
            float textX = x + 1f;
            float textY = y - 6.5f;  // 8 * 0.8
            fontSemi().draw(sliderSettings.getName(), textX, textY, 5f, applyAlpha(new Color(209, 209, 209, 255)).getRGB());  // 6 * 0.8
            fontSliderValue().draw(valueText, textX + nameWidth + 2, textY, 4f, applyAlpha(new Color(160, 160, 165, 200)).getRGB());  // 5 * 0.8
        }

        renderSlider();
    }

    private void renderWexSlider(String valueText) {
        fontSemi().draw(sliderSettings.getName(), x + 1f, y + 2f, 6f,
                applyAlpha(new Color(209, 209, 209, 230)).getRGB());

        float valueW = Math.max(18f, fontSliderValue().getWidth(valueText, 5f) + 9f);
        float valueH = 12f;
        float valueX = x + width - valueW - 1f;
        float valueY = y - 1f;
        Render2D.rect(valueX, valueY, valueW, valueH,
                applyAlpha(new Color(30, 31, 43, 230)).getRGB(), 4f);
        Render2D.outline(valueX, valueY, valueW, valueH, 0.45f,
                applyAlpha(new Color(55, 59, 76, 190)).getRGB(), 4f);
        fontSliderValue().draw(valueText, valueX + (valueW - fontSliderValue().getWidth(valueText, 5f)) / 2f,
                valueY + 3.2f, 5f, applyAlpha(new Color(205, 208, 220, 230)).getRGB());

        float trackX = x + width - valueW - 74f;
        float trackW = 64f;
        float trackY = y + 4.5f;
        float trackH = 2.4f;
        Render2D.rect(trackX, trackY, trackW, trackH,
                applyAlpha(new Color(35, 35, 45, 255)).getRGB(), trackH / 2f);
        float filled = trackW * animatedPercentage;
        if (filled > 0.5f) {
            Render2D.rect(trackX, trackY, filled, trackH,
                    applyAlpha(new Color(92, 130, 255, 255)).getRGB(), trackH / 2f);
        }
        float knobSize = 7f + knobAnimation;
        float knobX = trackX + filled - knobSize / 2f;
        float knobY = trackY + trackH / 2f - knobSize / 2f;
        Render2D.rect(knobX, knobY, knobSize, knobSize,
                applyAlpha(new Color(245, 247, 255, 255)).getRGB(), knobSize / 2f);
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

    private void updateAnimations(int mouseX, int mouseY, float deltaTime) {
        float inputTarget = inputMode ? 1f : 0f;
        inputAnimation = lerp(inputAnimation, inputTarget, deltaTime * FAST_ANIMATION_SPEED);

        boolean isHovered = isValueHover(mouseX, mouseY) && !inputMode;
        float hoverTarget = isHovered ? 1f : 0f;
        hoverAnimation = lerp(hoverAnimation, hoverTarget, deltaTime * ANIMATION_SPEED);

        float unitsTarget = inputMode ? 0f : 1f;
        unitsAlpha = lerp(unitsAlpha, unitsTarget, deltaTime * ANIMATION_SPEED);

        float offsetTarget = inputMode ? 1f : 0f;
        valueOffsetX = lerp(valueOffsetX, offsetTarget, deltaTime * ANIMATION_SPEED);

        float bgTarget = inputMode ? 1f : 0f;
        backgroundAlpha = lerp(backgroundAlpha, bgTarget, deltaTime * ANIMATION_SPEED);
    }

    private void renderValueInput(int mouseX, int mouseY) {
        String valueText = sliderSettings.isInteger()
                ? String.valueOf((int) sliderSettings.getValue())
                : String.format("%.1f", sliderSettings.getValue());
        String fullText = valueText;

        float fullTextWidth = fontSliderValue().getWidth(fullText, 5);
        float valueTextWidth = fontSliderValue().getWidth(valueText, 5);

        float baseX = x + width - fullTextWidth - 4;
        float textY = y + 2f;

        float currentValueX = baseX;

        float inputBoxX = baseX - 3;
        float inputBoxY = textY - 1;
        float inputBoxWidth = fullTextWidth + 6;
        float inputBoxHeight = 8;

        if (backgroundAlpha > 0.01f) {
            int bgAlpha = clampAlpha(200 * backgroundAlpha * alphaMultiplier);
            Render2D.rect(inputBoxX, inputBoxY, inputBoxWidth, inputBoxHeight,
                    new Color(40, 40, 45, bgAlpha).getRGB(), 2f);
        }

        float combinedOutlineAlpha = Math.max(hoverAnimation * 0.4f, inputAnimation);
        if (combinedOutlineAlpha > 0.01f) {
            int outlineAlpha = clampAlpha(180 * combinedOutlineAlpha * alphaMultiplier);

        }

        if (inputMode && inputAnimation > 0.5f) {
            String displayText = inputText;
            float displayTextWidth = fontSliderValue().getWidth(displayText, 5);
            float centeredX = inputBoxX + (inputBoxWidth - displayTextWidth) / 2f;

            int textAlpha = clampAlpha(220 * Math.min(1f, (inputAnimation - 0.5f) * 2f) * alphaMultiplier);
            fontSliderValue().draw(displayText, centeredX, textY, 5,
                    new Color(230, 230, 235, textAlpha).getRGB());

            long currentTime = System.currentTimeMillis();
            if ((currentTime % 1000) < 500) {
                String beforeCursor = inputText.substring(0, cursorPosition);
                float cursorX = centeredX + fontSliderValue().getWidth(beforeCursor, 5);
                int cursorAlpha = clampAlpha(255 * inputAnimation * alphaMultiplier);
                Render2D.rect(cursorX, inputBoxY + 2, 0.5f, inputBoxHeight - 4,
                        new Color(180, 180, 180, cursorAlpha).getRGB(), 0f);
            }
        } else {
            float valueAlpha = 1f - (inputAnimation * 0.5f);
            int valueAlphaInt = clampAlpha(160 * valueAlpha * alphaMultiplier);

            if (valueAlphaInt > 0) {
                fontSliderValue().draw(valueText, currentValueX, textY, 5,
                        new Color(100, 100, 105, valueAlphaInt).getRGB());
            }
        }
    }

    private void renderSlider() {
        float sliderPadding = 1f;
        float sliderTrackWidth = width - 2;

        if (isWexsideStyle()) {
            float sliderY = y + WexTrackMetrics.SLIDER_TRACK_Y_OFFSET;
            float trackH = WexTrackMetrics.SLIDER_TRACK_H;
            float trackR = Math.max(0.5f, trackH * 0.5f);
            int trackOff = applyAlpha(new Color(WexTrackMetrics.SLIDER_TRACK_BG_R, WexTrackMetrics.SLIDER_TRACK_BG_G, WexTrackMetrics.SLIDER_TRACK_BG_B, 255)).getRGB();
            int fillCol = applyAlpha(new Color(70, 130, 220, 255)).getRGB();
            int knobCol = applyAlpha(new Color(100, 150, 255, 255)).getRGB();

            Render2D.rect(x + sliderPadding, sliderY, sliderTrackWidth, trackH, trackOff, trackR);

            float filledWidth = sliderTrackWidth * animatedPercentage;
            if (filledWidth > 0.5f) {
                Render2D.rect(x + sliderPadding, sliderY, filledWidth, trackH, fillCol, trackR);
            }

            float kw = WexKnobMetrics.KNOB_W;
            float kh = WexKnobMetrics.KNOB_H;
            float knobX = x + sliderPadding + sliderTrackWidth * animatedPercentage - kw * 0.5f;
            float knobY = sliderY + trackH * 0.5f - kh * 0.5f;
            knobX = Math.max(x + sliderPadding - kw * 0.5f + 1f,
                    Math.min(knobX, x + sliderPadding + sliderTrackWidth - kw * 0.5f - 1f));
            float kR = Math.max(0.25f, kh * 0.5f);
            Render2D.rect(knobX, knobY, kw, kh, knobCol, kR);
            return;
        }

        float baseY = y + 10.5f;
        float boxH = 9f;

        Render2D.rect(x + 3, baseY, width - 6, boxH,
                applyAlpha(new Color(40, 39, 39, 255)).getRGB(), 2f);

        float filledWidth = (width - 8) * animatedPercentage;
        if (filledWidth > 0.5f) {
            Render2D.rect(x + 3, baseY + 5.5f, filledWidth, 1.5f,
                    applyAlpha(new Color(160, 160, 255, 255)).getRGB(), 1f);
        }

        float knobR = 2.6f + knobAnimation * 0.6f;
        float knobCx = x + 5 + filledWidth;
        float knobCy = baseY + 6.2f;
        knobCx = Math.max(x + 5, Math.min(knobCx, x + width - 5));
        Render2D.rect(knobCx - knobR, knobCy - knobR, knobR * 2, knobR * 2,
                applyAlpha(new Color(160, 160, 255, 255)).getRGB(), knobR);
    }

    private boolean isValueHover(double mouseX, double mouseY) {
        String valueText = sliderSettings.isInteger()
                ? String.valueOf((int) sliderSettings.getValue())
                : String.format("%.1f", sliderSettings.getValue());
        String fullText = valueText;

        float fullTextWidth = fontSliderValue().getWidth(fullText, 5);
        float boxX = x + width - fullTextWidth - 7;
        float boxY = y;

        return mouseX >= boxX && mouseX <= boxX + fullTextWidth + 10 &&
                mouseY >= boxY && mouseY <= boxY + 10;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isValueHover(mouseX, mouseY) && !inputMode) {
                inputMode = true;
                String currentValue = sliderSettings.isInteger()
                        ? String.valueOf((int) sliderSettings.getValue())
                        : String.format("%.1f", sliderSettings.getValue());
                inputText = currentValue;
                cursorPosition = inputText.length();
                return true;
            }

            if (inputMode && !isValueHover(mouseX, mouseY)) {
                applyInputValue();
                inputMode = false;
                inputText = "";
                return true;
            }

            if (isSliderHover(mouseX, mouseY) && !inputMode) {
                dragging = true;
                updateValue(mouseX);
                return true;
            }
        }
        return false;
    }

    private boolean isSliderHover(double mouseX, double mouseY) {
        if (isWexsideStyle()) {
            String valueText = sliderSettings.isInteger()
                    ? String.valueOf((int) sliderSettings.getValue())
                    : String.format("%.1f", sliderSettings.getValue());
            float valueW = Math.max(18f, fontSliderValue().getWidth(valueText, 5f) + 9f);
            float trackX = x + width - valueW - 74f;
            float trackW = 64f;
            return mouseX >= trackX - 4f && mouseX <= trackX + trackW + 4f && mouseY >= y - 3f && mouseY <= y + 13f;
        }
        float sliderY = y + 9f;
        float sliderHeight = 12f;
        return mouseX >= x && mouseX <= x + width && mouseY >= sliderY && mouseY <= sliderY + sliderHeight;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            updateValue(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!inputMode) return false;

        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                applyInputValue();
                inputMode = false;
                inputText = "";
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                inputMode = false;
                inputText = "";
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (cursorPosition > 0) {
                    inputText = inputText.substring(0, cursorPosition - 1) + inputText.substring(cursorPosition);
                    cursorPosition--;
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (cursorPosition < inputText.length()) {
                    inputText = inputText.substring(0, cursorPosition) + inputText.substring(cursorPosition + 1);
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (cursorPosition > 0) cursorPosition--;
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (cursorPosition < inputText.length()) cursorPosition++;
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                cursorPosition = 0;
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                cursorPosition = inputText.length();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!inputMode) return false;

        if (isValidInputChar(chr)) {
            String newText = inputText.substring(0, cursorPosition) + chr + inputText.substring(cursorPosition);

            if (isValidInputFormat(newText)) {
                inputText = newText;
                cursorPosition++;
            }
            return true;
        }

        return false;
    }

    private boolean isValidInputChar(char chr) {
        return Character.isDigit(chr) || chr == '.' || chr == '-';
    }

    private boolean isValidInputFormat(String text) {
        if (text.isEmpty() || text.equals("-") || text.equals(".") || text.equals("-.")) {
            return true;
        }

        int dotCount = 0;
        int minusCount = 0;
        int digitsAfterDot = 0;
        boolean foundDot = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '-') {
                if (i != 0) return false;
                minusCount++;
                if (minusCount > 1) return false;
            } else if (c == '.') {
                if (sliderSettings.isInteger()) return false;
                dotCount++;
                if (dotCount > 1) return false;
                foundDot = true;
            } else if (Character.isDigit(c)) {
                if (foundDot) {
                    digitsAfterDot++;
                    if (digitsAfterDot > 1) return false;
                }
            } else {
                return false;
            }
        }

        return true;
    }

    private void applyInputValue() {
        if (inputText.isEmpty() || inputText.equals("-") || inputText.equals(".") || inputText.equals("-.")) {
            return;
        }

        try {
            float value;
            if (sliderSettings.isInteger()) {
                value = Integer.parseInt(inputText);
            } else {
                value = Float.parseFloat(inputText);
            }

            value = Math.max(sliderSettings.getMin(), Math.min(sliderSettings.getMax(), value));

            if (sliderSettings.isInteger()) {
                value = Math.round(value);
            }

            sliderSettings.setValue(value);
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateValue(double mouseX) {
        float sliderPadding = 1f;
        float sliderTrackWidth = width - 2;
        if (isWexsideStyle()) {
            String valueText = sliderSettings.isInteger()
                    ? String.valueOf((int) sliderSettings.getValue())
                    : String.format("%.1f", sliderSettings.getValue());
            float valueW = Math.max(18f, fontSliderValue().getWidth(valueText, 5f) + 9f);
            sliderPadding = width - valueW - 74f;
            sliderTrackWidth = 64f;
        }

        float percentage = (float) ((mouseX - x - sliderPadding) / sliderTrackWidth);
        percentage = Math.max(0, Math.min(1, percentage));

        float range = sliderSettings.getMax() - sliderSettings.getMin();
        float newValue = sliderSettings.getMin() + (range * percentage);

        if (sliderSettings.isInteger()) {
            newValue = Math.round(newValue);
        }

        sliderSettings.setValue(newValue);
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isInputMode() {
        return inputMode;
    }
}

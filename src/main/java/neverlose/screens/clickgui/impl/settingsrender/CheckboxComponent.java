package neverlose.screens.clickgui.impl.settingsrender;

import net.minecraft.client.gui.DrawContext;
import neverlose.util.interfaces.AbstractSettingComponent;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.util.render.Render2D;
import neverlose.util.render.font.Fonts;

import java.awt.*;

public class CheckboxComponent extends AbstractSettingComponent {
    private final BooleanSetting booleanSetting;
    private float checkAnimation = 0f;
    private float hoverAnimation = 0f;
    private float stretchAnimation = 0f;
    private float velocity = 0f;

    public CheckboxComponent(BooleanSetting setting) {
        super(setting);
        this.booleanSetting = setting;
        this.checkAnimation = setting.isValue() ? 1f : 0f;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = isHover(mouseX, mouseY);

        float hoverTarget = hovered ? 1f : 0f;
        hoverAnimation += (hoverTarget - hoverAnimation) * 0.2f;
        hoverAnimation = clamp(hoverAnimation, 0f, 1f);

        float target = booleanSetting.isValue() ? 1f : 0f;
        float oldCheck = checkAnimation;

        float speed = 0.35f;
        checkAnimation += (target - checkAnimation) * speed;

        if (Math.abs(target - checkAnimation) < 0.001f) {
            checkAnimation = target;
        }

        velocity = checkAnimation - oldCheck;

        float absVelocity = Math.abs(velocity);
        float targetStretch = absVelocity * 30f;
        targetStretch = clamp(targetStretch, 0f, 1f);

        float stretchSpeed = targetStretch > stretchAnimation ? 0.5f : 0.2f;
        stretchAnimation += (targetStretch - stretchAnimation) * stretchSpeed;
        stretchAnimation = clamp(stretchAnimation, 0f, 1f);

        int iconAlpha = (int)(200 * alphaMultiplier);

        if (isWexsideStyle()) {
            fontSemi().draw(booleanSetting.getName(), x + 1f, y + 2.2f, 6f,
                    applyAlpha(new Color(209, 209, 209, 220)).getRGB());
            // Убрали описание в wexside стиле
        } else {
            Fonts.GUI_ICONS.draw("T", x + 0.5f, y + height / 2 - 11f, 11, new Color(210, 210, 210, iconAlpha).getRGB());
            fontSemi().draw(booleanSetting.getName(), x + 9.5f, y + height / 2 - 7.5f, 6, applyAlpha(new Color(210, 210, 220, 200)).getRGB());
            fontMed().draw(booleanSetting.getDescription(), x + 0.5f, y + height / 2 + 0.5f, 5, applyAlpha(new Color(128, 128, 128, 128)).getRGB());
        }

        float checkboxSize = 10;
        float checkboxWidth = isWexsideStyle() ? 24f : checkboxSize + 6;
        float checkboxX = x + width - checkboxWidth - 2;
        float checkboxY = isWexsideStyle() ? y + 2f : y + height / 2 - checkboxSize / 2;

        Color trackOff = isWexsideStyle() ? new Color(18, 25, 33, 235) : new Color(55, 55, 55, 25);
        Color trackOn = isWexsideStyle() ? new Color(83, 126, 245, 245) : new Color(55, 55, 55, 25);
        Color trackColor = lerpColor(trackOff, trackOn, checkAnimation);
        Render2D.rect(checkboxX, checkboxY, checkboxWidth, checkboxSize, applyAlpha(trackColor).getRGB(), checkboxSize / 2f);

        int outlineAlpha = 60 + (int)(hoverAnimation * 40);
        Render2D.outline(checkboxX, checkboxY, checkboxWidth, checkboxSize, 0.45f, applyAlpha(new Color(58, 70, 88, outlineAlpha)).getRGB(), checkboxSize / 2f);

        float knobBaseSize = checkboxSize - 3;
        float maxStretchExtra = 4f;
        float stretchExtra = stretchAnimation * maxStretchExtra;

        float knobWidth = knobBaseSize + stretchExtra;
        float knobHeight = knobBaseSize - (stretchAnimation * 1f);

        float padding = 1.5f;
        float travelDistance = checkboxWidth - knobBaseSize - (padding * 2);

        float knobBaseX = checkboxX + padding;

        float stretchOffset;
        if (velocity > 0) {
            stretchOffset = -stretchExtra * 0.3f;
        } else if (velocity < 0) {
            stretchOffset = stretchExtra * 0.3f;
        } else {
            stretchOffset = 0;
        }

        float knobX = knobBaseX + (travelDistance * checkAnimation) - (stretchExtra * checkAnimation) + stretchOffset;
        float knobY = checkboxY + (checkboxSize - knobHeight) / 2f;

        Color offColor = isWexsideStyle() ? new Color(118, 127, 139, 235) : new Color(59, 59, 59, 200);
        Color onColor = isWexsideStyle() ? new Color(246, 248, 255, 255) : new Color(159, 159, 159, 200);
        Color knobColor = lerpColor(offColor, onColor, checkAnimation);

        Render2D.rect(knobX, knobY, knobWidth, knobHeight, applyAlpha(knobColor).getRGB(), 4f);
    }

    private String truncateWex(String text, float maxW) {
        String s = text;
        while (s.length() > 1 && fontMed().getWidth(s, 4.5f) > maxW) {
            s = s.substring(0, s.length() - 1);
        }
        if (!s.equals(text)) s = s + "..";
        return s;
    }

    public float getTotalHeight() {
        if (isWexsideStyle()) {
            return 14f;
        }
        return height;
    }

    private Color lerpColor(Color a, Color b, float t) {
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        int al = (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
        return new Color(clamp(r, 0, 255), clamp(g, 0, 255), clamp(bl, 0, 255), clamp(al, 0, 255));
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHover(mouseX, mouseY) && button == 0) {
            booleanSetting.setValue(!booleanSetting.isValue());
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        float h = isWexsideStyle() ? getTotalHeight() : height;
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + h;
    }
}

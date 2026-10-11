package neverlose.screens.clickgui.impl.settingsrender;

import net.minecraft.client.gui.DrawContext;
import neverlose.screens.clickgui.WexKnobMetrics;
import neverlose.screens.clickgui.WexTrackMetrics;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.util.interfaces.AbstractSettingComponent;
import neverlose.util.render.Render2D;
import neverlose.util.render.font.Fonts;

import java.awt.*;

/**
 * Переключатель в стиле GUI (как в ModuleGridRenderer) — pill-shaped, фиолетовый при включении.
 */
public class ToggleComponent extends AbstractSettingComponent {
    private final BooleanSetting booleanSetting;
    private final boolean displayOnly;
    private float knobProgress = 0f;

    /** Ширина зоны клика / дорожки (см. {@link WexTrackMetrics#SETTINGS_TOGGLE_TRACK_W}). */
    private static final float TOGGLE_W = WexTrackMetrics.SETTINGS_TOGGLE_TRACK_W;
    private static final float TOGGLE_H = 11f;
    private static final float TOGGLE_R = 5f;
    private static final Color TOGGLE_OFF_BG = new Color(55, 57, 69);
    private static final Color TOGGLE_ON_BG = new Color(70, 70, 70);
    private static final float TOGGLE_ANIM_SPEED = 12f;

    public ToggleComponent(BooleanSetting setting) {
        this(setting, false);
    }

    /** displayOnly=true — только для вида, клик не меняет значение */
    public ToggleComponent(BooleanSetting setting, boolean displayOnly) {
        super(setting);
        this.booleanSetting = setting;
        this.displayOnly = displayOnly;
        this.knobProgress = setting.isValue() ? 1f : 0f;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float target = booleanSetting.isValue() ? 1f : 0f;
        float dt = Math.max(delta, 1f / 240f);
        float speed = TOGGLE_ANIM_SPEED * Math.min(dt, 0.1f);
        knobProgress += (target - knobProgress) * speed;
        if (Math.abs(knobProgress - target) < 0.001f) knobProgress = target;
        knobProgress = Math.max(0f, Math.min(1f, knobProgress));

        int alpha = (int) (255 * alphaMultiplier);

        String displayName;
        if (getPopupLabelOverride() != null && !getPopupLabelOverride().isEmpty()) {
            displayName = labelForPopupRow();
        } else {
            displayName = booleanSetting.getName().toLowerCase().contains("visible")
                    || booleanSetting.getName().toLowerCase().contains("показ")
                    || booleanSetting.getName().toLowerCase().contains("show") ? "Visible" : booleanSetting.getName();
        }
        if (isWexsideStyle()) {
            fontSemi().draw(trimText(displayName, width - 20f, 6f), x + 1f, y + 2f, 6f,
                    new Color(209, 209, 209, alpha).getRGB());
            
            // Иконка "U" вместо toggle
            float toggleX = x + width - TOGGLE_W - 2f;
            float toggleY = y + (height - TOGGLE_H) / 2f - 1f;
            renderToggle(toggleX, toggleY, alpha);
        } else {
            fontSemi().draw(displayName, x + 1f, y + height / 2 - 6f, 6, new Color(255, 255, 255, alpha).getRGB());
            float toggleX = x + width - TOGGLE_W - 2f;
            float toggleY = y + (height - TOGGLE_H) / 2f - 1f;
            renderToggle(toggleX, toggleY, alpha);
        }
    }

    private String trimText(String text, float maxWidth, float size) {
        if (text == null) return "";
        if (fontSemi().getWidth(text, size) <= maxWidth) return text;
        String value = text;
        while (value.length() > 1 && fontSemi().getWidth(value + "..", size) > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        return value + "..";
    }

    private void renderToggle(float x, float y, int alpha) {
        if (isWexsideStyle()) {
            float tw = WexTrackMetrics.SETTINGS_TOGGLE_TRACK_W;
            float th = WexTrackMetrics.SETTINGS_TOGGLE_TRACK_H;
            float ty = y + (TOGGLE_H - th) / 2f;
            float tx = x;
            float p = knobProgress;
            int tr = (int) (WexTrackMetrics.TRACK_OFF_R + (WexTrackMetrics.TRACK_ON_R - WexTrackMetrics.TRACK_OFF_R) * p);
            int tg = (int) (WexTrackMetrics.TRACK_OFF_G + (WexTrackMetrics.TRACK_ON_G - WexTrackMetrics.TRACK_OFF_G) * p);
            int tb = (int) (WexTrackMetrics.TRACK_OFF_B + (WexTrackMetrics.TRACK_ON_B - WexTrackMetrics.TRACK_OFF_B) * p);
            int track = new Color(tr, tg, tb, alpha).getRGB();
            Render2D.rect(tx, ty, tw, th, track, th * 0.5f);
            float kw = WexKnobMetrics.KNOB_W;
            float kh = WexKnobMetrics.KNOB_H;
            float travel = Math.max(0f, tw - kw - 1f);
            float knobCx = tx + kw * 0.5f + 0.5f + travel * p;
            float knobCy = ty + th * 0.5f;
            int kr = (int) (130 + (220 - 130) * p);
            int kg = (int) (130 + (220 - 130) * p);
            int kb = (int) (132 + (222 - 132) * p);
            int knobCol = new Color(kr, kg, kb, alpha).getRGB();
            float kR = Math.max(0.25f, kh * 0.5f);
            Render2D.rect(knobCx - kw * 0.5f, knobCy - kh * 0.5f, kw, kh, knobCol, kR);
            return;
        }
        boolean on = knobProgress > 0.5f;
        Color trackColor = on ? TOGGLE_ON_BG : TOGGLE_OFF_BG;
        Render2D.rect(x, y + 1f, TOGGLE_W, TOGGLE_H,
                new Color(trackColor.getRed(), trackColor.getGreen(), trackColor.getBlue(), alpha).getRGB(), TOGGLE_R);

        float knobSize = TOGGLE_H - 4.2f;
        float padding = 2f;
        float travelDist = TOGGLE_W - knobSize - padding * 2f;
        float knobX = x + padding + travelDist * knobProgress;
        float knobY = y + 3f;
        int r = Math.max(0, Math.min(255, (int) (150 + (255 - 150) * knobProgress)));
        int g = Math.max(0, Math.min(255, (int) (150 + (255 - 150) * knobProgress)));
        int b = Math.max(0, Math.min(255, (int) (160 + (255 - 160) * knobProgress)));
        Color knobColor = new Color(r, g, b, alpha);

        Render2D.rect(knobX, knobY, knobSize, knobSize, knobColor.getRGB(), knobSize / 2f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (displayOnly) return false;
        if (button != 0) return false;
        
        if (isWexsideStyle()) {
            // Клик по иконке "U"
            float toggleX = x + width - TOGGLE_W - 2f;
            float toggleY = y + (height - TOGGLE_H) / 2f - 1f;
            if (mouseX >= toggleX && mouseX <= toggleX + TOGGLE_W && mouseY >= toggleY && mouseY <= toggleY + TOGGLE_H) {
                booleanSetting.setValue(!booleanSetting.isValue());
                return true;
            }
        } else {
            float toggleX = x + width - TOGGLE_W - 2f;
            float toggleY = y + (height - TOGGLE_H) / 2f - 1f;
            if (mouseX >= toggleX && mouseX <= toggleX + TOGGLE_W && mouseY >= toggleY && mouseY <= toggleY + TOGGLE_H) {
                booleanSetting.setValue(!booleanSetting.isValue());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}

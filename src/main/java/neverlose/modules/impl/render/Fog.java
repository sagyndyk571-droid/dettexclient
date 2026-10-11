package neverlose.modules.impl.render;

import lombok.Getter;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.ColorSetting;
import neverlose.modules.module.setting.implement.SliderSettings;

import java.awt.Color;

public class Fog extends ModuleStructure {
    @Getter
    private static Fog instance;

    private final ColorSetting color = new ColorSetting("Color", "Fog color")
            .value(new Color(112, 142, 175, 210).getRGB());

    private final SliderSettings opacity = new SliderSettings("Opacity", "Fog color blend")
            .range(0.0f, 1.0f)
            .setValue(0.85f);

    private final SliderSettings start = new SliderSettings("Start", "Fog start distance")
            .range(0.0f, 256.0f)
            .setValue(6.0f);

    private final SliderSettings end = new SliderSettings("End", "Fog end distance")
            .range(1.0f, 512.0f)
            .setValue(90.0f);

    private final BooleanSetting fogBlur = new BooleanSetting("FogBlur", "Blur world with fog")
            .setValue(false);

    private final SliderSettings blurRadius = new SliderSettings("Blur Radius", "FogBlur radius")
            .range(0.5f, 8.0f)
            .setValue(1.6f)
            .visible(() -> fogBlur.isValue());

    private final SliderSettings blurStrength = new SliderSettings("Blur Strength", "FogBlur opacity")
            .range(0.0f, 1.0f)
            .setValue(0.22f)
            .visible(() -> fogBlur.isValue());

    public Fog() {
        super("Fog", "Custom world fog", ModuleCategory.RENDER);
        instance = this;
        settings(color, opacity, start, end, fogBlur, blurRadius, blurStrength);
    }

    public boolean shouldApplyFog() {
        return isState();
    }

    public boolean shouldApplyBlur() {
        return isState() && fogBlur.isValue() && blurStrength.getValue() > 0.001f;
    }

    public int getFogColor() {
        return color.getColorNoAlpha();
    }

    public float getColorBlend() {
        return clamp01(opacity.getValue() * color.getAlpha());
    }

    public float getFogStart() {
        return Math.max(0.0f, start.getValue());
    }

    public float getFogEnd() {
        return Math.max(getFogStart() + 1.0f, end.getValue());
    }

    public float getBlurRadius() {
        return Math.max(0.0f, blurRadius.getValue());
    }

    public float getBlurStrength() {
        return clamp01(blurStrength.getValue());
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}

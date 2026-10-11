package neverlose.modules.impl.render;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.GlassHandsRenderEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.ColorSetting;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.render.shader.HandShaderRenderer;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HandShader extends ModuleStructure {

    private static HandShader instance;

    SelectSetting mode = new SelectSetting("Mode", "Hand shader mode")
            .value("Full", "NoFull", "WebShader", "Fire", "Plasma", "Glitch")
            .selected("Full");

    SliderSettings blurRadius = new SliderSettings("Blur Radius", "Shader blur radius")
            .range(1.0f, 30.0f)
            .setValue(12.0f);

    SliderSettings opacity = new SliderSettings("Opacity", "Shader opacity")
            .range(0.0f, 1.0f)
            .setValue(0.6f);

    SliderSettings threshold = new SliderSettings("Threshold", "Hand mask threshold")
            .range(0.001f, 0.1f)
            .setValue(0.01f);

    ColorSetting color = new ColorSetting("Color", "Shader tint")
            .value(ColorSetting.DEFAULT_COLOR);

    public HandShader() {
        super("HandShader", "Shader effect for first person hands", ModuleCategory.RENDER);
        settings(mode, blurRadius, opacity, threshold, color);
        instance = this;
    }

    public static HandShader getInstance() {
        return instance;
    }

    @Override
    public void activate() {
        HandShaderRenderer renderer = HandShaderRenderer.getInstance();
        renderer.invalidate();
        renderer.setEnabled(true);
        updateRendererSettings();
    }

    @Override
    public void deactivate() {
        HandShaderRenderer renderer = HandShaderRenderer.getInstance();
        renderer.setEnabled(false);
    }

    @EventHandler
    public void onGlassHandsRender(GlassHandsRenderEvent event) {
        if (!isState()) return;

        HandShaderRenderer renderer = HandShaderRenderer.getInstance();
        updateRendererSettings();

        if (event.getPhase() == GlassHandsRenderEvent.Phase.PRE) {
            renderer.captureSceneBeforeHands();
        } else if (event.getPhase() == GlassHandsRenderEvent.Phase.POST) {
            renderer.captureSceneAfterHands();
            renderer.renderEffect();
        }
    }

    private void updateRendererSettings() {
        HandShaderRenderer renderer = HandShaderRenderer.getInstance();
        renderer.setBlurRadius(blurRadius.getValue());
        renderer.setOpacity(opacity.getValue());
        renderer.setThreshold(threshold.getValue());
        renderer.setColor(color.getColor());
        renderer.setMode(getModeIndex());
    }

    private int getModeIndex() {
        return switch (mode.getSelected()) {
            case "NoFull" -> 1;
            case "WebShader" -> 2;
            case "Fire" -> 3;
            case "Plasma" -> 4;
            case "Glitch" -> 5;
            default -> 0;
        };
    }
}

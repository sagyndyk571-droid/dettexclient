package neverlose.modules.impl.render;

import lombok.Getter;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.SliderSettings;

public class Saturation extends ModuleStructure {
    @Getter
    private static Saturation instance;

    private final SliderSettings saturation = new SliderSettings("Saturation", "World saturation amount")
            .range(0.0f, 2.5f)
            .setValue(1.25f);

    public Saturation() {
        super("Saturation", "Changes world color saturation", ModuleCategory.RENDER);
        instance = this;
        settings(saturation);
    }

    public float getSaturationValue() {
        return isState() ? Math.max(0.0f, saturation.getValue()) : 1.0f;
    }

    public boolean shouldApply() {
        return isState() && Math.abs(getSaturationValue() - 1.0f) > 0.001f;
    }
}

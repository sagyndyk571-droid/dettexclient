package neverlose.modules.impl.render;

import lombok.Getter;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.modules.module.setting.implement.SliderSettings;

public class AspectRatio extends ModuleStructure {
    @Getter
    private static AspectRatio instance;

    private final SelectSetting mode = new SelectSetting("Соотношение", "Изменяет соотношение сторон экрана")
            .value("4:3", "16:9", "1:1", "16:10", "Кастомное")
            .selected("Кастомное");

    private final SliderSettings aspectRatio = new SliderSettings("Значение", "Кастомное соотношение сторон")
            .range(0.1f, 5.0f)
            .setValue(1.0f)
            .visible(() -> mode.isSelected("Кастомное"));

    public AspectRatio() {
        super("AspectRatio", "Изменяет соотношение сторон экрана", ModuleCategory.RENDER);
        instance = this;
        settings(mode, aspectRatio);
    }

    public float getAspectRatio(float fallback) {
        if (!isState()) {
            return fallback;
        }

        return switch (mode.getSelected()) {
            case "4:3" -> 4.0f / 3.0f;
            case "16:9" -> 16.0f / 9.0f;
            case "1:1" -> 1.0f;
            case "16:10" -> 16.0f / 10.0f;
            default -> aspectRatio.getValue();
        };
    }
}

package neverlose.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomChat extends ModuleStructure {

    public static CustomChat getInstance() {
        return Instance.get(CustomChat.class);
    }

    public BooleanSetting enabled = new BooleanSetting("Кастомный дизайн", "Использовать собственный дизайн чата")
            .setValue(true);

    public SliderSettings backgroundOpacity = new SliderSettings("Прозрачность фона", "Прозрачность фона чата")
            .range(0f, 1f)
            .setValue(0.55f);

    public BooleanSetting blur = new BooleanSetting("Blur фон", "Размытие за фоном чата")
            .setValue(true);

    public BooleanSetting roundedCorners = new BooleanSetting("Скруглённые углы", "Скруглять углы фона чата")
            .setValue(true);

    public SliderSettings inputOpacity = new SliderSettings("Прозрачность инпута", "Прозрачность поля ввода")
            .range(0f, 1f)
            .setValue(0.6f);

    public CustomChat() {
        super("CustomChat", ModuleCategory.RENDER);
        settings(enabled, backgroundOpacity, blur, roundedCorners, inputOpacity);
    }
}
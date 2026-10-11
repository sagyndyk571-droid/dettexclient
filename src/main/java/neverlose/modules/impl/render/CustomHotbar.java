package neverlose.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomHotbar extends ModuleStructure {

    public static CustomHotbar getInstance() {
        return Instance.get(CustomHotbar.class);
    }

    public BooleanSetting enabled = new BooleanSetting("Кастомный дизайн", "Использовать собственный дизайн хотбара")
            .setValue(true);

    public SliderSettings backgroundOpacity = new SliderSettings("Прозрачность фона", "Прозрачность фона хотбара")
            .range(0f, 1f)
            .setValue(0.55f);

    public BooleanSetting blur = new BooleanSetting("Blur фон", "Размытие за фоном хотбара")
            .setValue(true);

    public BooleanSetting showSelector = new BooleanSetting("Селектор слота", "Подсвечивать выбранный слот")
            .setValue(true);

    public BooleanSetting showOffhand = new BooleanSetting("Показывать оффхенд", "Отображать слот левой руки")
            .setValue(true);
    public BooleanSetting hideXpBar = new BooleanSetting("Скрыть XP бар", "Полностью скрыть полоску опыта")
            .setValue(true);

    public SliderSettings statusBarsOffset = new SliderSettings(
            "Смещение статус-баров",
            "Вертикальное смещение HP, брони и голода (отриц. = вверх)"
    ).range(-30f, 10f).setValue(-6f);


    public SliderSettings xpBarOffset = new SliderSettings(
            "Смещение XP-бара",
            "Вертикальное смещение полоски опыта (отриц. = вверх)"
    ).range(-30f, 10f).setValue(-3f);

    public CustomHotbar() {
        super("CustomHotbar", ModuleCategory.RENDER);
        settings(enabled, backgroundOpacity, blur, showSelector, showOffhand,
                 xpBarOffset, hideXpBar);
    }
}
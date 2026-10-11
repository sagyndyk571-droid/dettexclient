package neverlose.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomScoreboard extends ModuleStructure {

    public static CustomScoreboard getInstance() {
        return Instance.get(CustomScoreboard.class);
    }


    public BooleanSetting enabled = new BooleanSetting("Кастомный дизайн", "Заменить дизайн скорборда на свой")
            .setValue(true);

    public BooleanSetting overlayMode = new BooleanSetting("Режим overlay", "Наложить тёмный фон поверх оригинального скорборда")
            .setValue(false);

    public BooleanSetting hideScoreboard = new BooleanSetting("Скрыть скорборд", "Полностью убрать скорборд с экрана")
            .setValue(false);

    public SliderSettings posX = new SliderSettings("Позиция X", "Смещение скорборда по горизонтали")
            .range(-500f, 500f)
            .setValue(0f);

    public SliderSettings posY = new SliderSettings("Позиция Y", "Смещение скорборда по вертикали")
            .range(-500f, 500f)
            .setValue(0f);

    public SliderSettings maxLines = new SliderSettings("Макс. строк", "Сколько строк отображать")
            .range(1f, 15f)
            .setValue(10f);

    public BooleanSetting showTitle = new BooleanSetting("Заголовок", "Показывать заголовок скорборда")
            .setValue(true);

    public SliderSettings backgroundOpacity = new SliderSettings("Прозрачность фона", "0.0 = прозрачный, 1.0 = глухой")
            .range(0f, 1f)
            .setValue(0.55f);

    public BooleanSetting showScores = new BooleanSetting("Показывать очки", "Отображать числовые значения очков справа")
            .setValue(true);

    public CustomScoreboard() {
        super("CustomScoreboard", ModuleCategory.RENDER);
        settings(enabled, overlayMode, hideScoreboard, posX, posY, maxLines, showTitle, backgroundOpacity, showScores);
    }
}
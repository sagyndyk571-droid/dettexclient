package neverlose.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.*;
import neverlose.util.Instance;

import java.awt.Color;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Hud extends ModuleStructure {
    public static Hud getInstance() {
        return Instance.get(Hud.class);
    }

    public MultiSelectSetting interfaceSettings = new MultiSelectSetting("Интерфейс", "Интерфейс")
            .value("Watermark",
                    "HotKeys",
                    "Potions",
                    "Staff",
                    "TargetHud",
                    "Info",
                    "Inventory",
                    "CoolDowns",
                    "Music Info",
                    "ArrayList",
                    "Notifications")

            .selected("Watermark",
                    "HotKeys",
                    "Potions",
                    "Staff",
                    "TargetHud",
                    "Info",
                    "Music Info",
                    "Notifications");

    public SelectSetting hudStyle = new SelectSetting("Стиль HUD", "Выбор оформления HUD")
            .value("HUD 1", "HUD 2")
            .selected("HUD 1");

    public SelectSetting hudTheme = new SelectSetting("HUD Theme", "HUD color theme")
            .value("Blue", "Cyan", "Purple", "Emerald", "Rose", "Amber", "Mono", "Custom")
            .selected("Blue");

    public ColorSetting hudAccent = new ColorSetting("HUD Accent", "Custom HUD accent color")
            .value(new Color(70, 130, 220, 255).getRGB())
            .presets(
                    new Color(70, 130, 220, 255).getRGB(),
                    new Color(65, 195, 230, 255).getRGB(),
                    new Color(145, 105, 245, 255).getRGB(),
                    new Color(65, 190, 135, 255).getRGB(),
                    new Color(235, 90, 135, 255).getRGB(),
                    new Color(235, 170, 70, 255).getRGB()
            )
            .visible(() -> hudTheme.isSelected("Custom"));

    public SliderSettings hudOpacity = new SliderSettings("Прозрачность HUD", "Общая прозрачность HUD элементов")
            .range(0.0f, 100.0f)
            .setValue(100.0f);

    public BooleanSetting hudBlur = new BooleanSetting("Блюр HUD", "Размытие под HUD элементами")
            .setValue(false);

    public SliderSettings hudBlurRadius = new SliderSettings("Сила блюра", "Сила размытия под HUD элементами")
            .range(2.0f, 20.0f)
            .setValue(8.0f)
            .visible(() -> hudBlur.isValue());

    public BooleanSetting showBps = new BooleanSetting("Show BPS", "Показывать блоки в секунду")
            .setValue(true)
            .visible(() -> interfaceSettings.isSelected("Info"));

    public BooleanSetting showTps = new BooleanSetting("Show TPS", "Показывать TPS в Watermark")
            .setValue(true)
            .visible(() -> interfaceSettings.isSelected("Watermark"));

    public MultiSelectSetting watermarkParts = new MultiSelectSetting("Watermark Parts", "Watermark visible sections")
            .value("Name", "FPS", "Ping", "Time", "TPS", "Server")
            .selected("Name", "FPS", "Ping", "Time", "TPS", "Server")
            .visible(() -> interfaceSettings.isSelected("Watermark"));

    public BooleanSetting notifyShield = new BooleanSetting("Ломание щита", "Уведомлять о ломании щита")
            .setValue(false)
            .visible(() -> interfaceSettings.isSelected("Notifications"));

    public BooleanSetting notifySpec = new BooleanSetting("Просьба о наблюдении", "Уведомлять о просьбах наблюдения")
            .setValue(false)
            .visible(() -> interfaceSettings.isSelected("Notifications"));

    public BooleanSetting notifyWarps = new BooleanSetting("Пиар варпов", "Уведомлять о пиаре варпов")
            .setValue(false)
            .visible(() -> interfaceSettings.isSelected("Notifications"));

    public BooleanSetting notifyModule = new BooleanSetting("Состояние модулей/настроек", "Уведомлять о включении и выключении модулей")
            .setValue(true)
            .visible(() -> interfaceSettings.isSelected("Notifications"));

    public BooleanSetting notifyLowStrength = new BooleanSetting("Низкая прочность предметов", "Уведомлять о низкой прочности предметов")
            .setValue(true)
            .visible(() -> interfaceSettings.isSelected("Notifications"));

    public BooleanSetting notifyEffects = new BooleanSetting("Закончились важные зелья", "Уведомлять о важных зельях")
            .setValue(true)
            .visible(() -> interfaceSettings.isSelected("Notifications"));

    public BooleanSetting notifyAlphaBg = new BooleanSetting("Прозрачный фон", "Делать фон уведомлений прозрачнее")
            .setValue(false)
            .visible(() -> interfaceSettings.isSelected("Notifications"));

    public Hud() {
        super("Hud", ModuleCategory.RENDER);
        settings(interfaceSettings, hudStyle, hudTheme, hudAccent, hudOpacity, hudBlur, hudBlurRadius, showBps,
                notifyShield, notifySpec, notifyWarps, notifyModule, notifyLowStrength, notifyEffects, notifyAlphaBg);
    }
}

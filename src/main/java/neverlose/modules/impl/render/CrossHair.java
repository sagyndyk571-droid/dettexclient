package neverlose.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.hit.EntityHitResult;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.CrosshairEvent;
import neverlose.modules.impl.player.FreeCam;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.ColorSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.ColorUtil;
import neverlose.util.Instance;
import neverlose.util.render.Render2D;

import java.awt.Color;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CrossHair extends ModuleStructure {
    SliderSettings gap = new SliderSettings("Зазор", "Расстояние от центра")
            .range(0.0f, 8.0f)
            .setValue(2.0f);

    SliderSettings length = new SliderSettings("Длина", "Длина лучей")
            .range(2.0f, 10.0f)
            .setValue(4.0f);

    SliderSettings thickness = new SliderSettings("Толщина", "Толщина лучей")
            .range(1.0f, 5.0f)
            .setValue(1.5f);

    SliderSettings dynamicAmount = new SliderSettings("Динамика", "Расширение разрыва от кулдауна удара")
            .range(1.0f, 12.0f)
            .setValue(7.0f);

    BooleanSetting dynamicGap = new BooleanSetting("Динамический зазор", "Расширять прицел после удара")
            .setValue(true);

    BooleanSetting showOutline = new BooleanSetting("Обводка", "Рисовать тёмную обводку")
            .setValue(true);

    BooleanSetting showDot = new BooleanSetting("Точка", "Рисовать точку в центре")
            .setValue(true);

    BooleanSetting hoverColor = new BooleanSetting("Цвет при наведении", "Менять цвет при наведении на сущность")
            .setValue(true);

    ColorSetting color = new ColorSetting("Цвет", "Основной цвет прицела")
            .value(new Color(255, 255, 255, 255).getRGB());

    ColorSetting targetColor = new ColorSetting("Цвет цели", "Цвет при наведении на сущность")
            .value(new Color(255, 76, 76, 255).getRGB())
            .visible(hoverColor::isValue);

    public CrossHair() {
        super("CrossHair", "Custom CrossHair", ModuleCategory.RENDER);
        settings(gap, length, thickness, dynamicGap, dynamicAmount, showOutline, showDot, hoverColor, color, targetColor);
    }

    public static CrossHair getInstance() {
        return Instance.get(CrossHair.class);
    }

    @EventHandler
    public void onCrosshair(CrosshairEvent event) {
        if (mc.player == null || mc.world == null || !shouldRender()) {
            return;
        }

        float centerX = mc.getWindow().getScaledWidth() / 2.0f;
        float centerY = mc.getWindow().getScaledHeight() / 2.0f;
        float cooldown = 1.0f - mc.player.getAttackCooldownProgress(event.getPartialTicks());

        Render2D.beginOverlay();
        drawCrosshair(centerX, centerY, Math.max(0.0f, cooldown));
        Render2D.endOverlay();
        event.cancel();
    }

    private boolean shouldRender() {
        FreeCam freeCam = FreeCam.getInstance();
        return mc.options.getPerspective() == Perspective.FIRST_PERSON
                || (freeCam != null && freeCam.isState());
    }

    private void drawCrosshair(float centerX, float centerY, float cooldown) {
        float thicknessValue = thickness.getValue();
        float lengthValue = length.getValue();
        float actualGap = gap.getValue();
        if (dynamicGap.isValue()) {
            actualGap += dynamicAmount.getValue() * cooldown;
        }

        int crosshairColor = getCrosshairColor();
        if (showOutline.isValue()) {
            int outlineColor = ColorUtil.getColor(0, 190);
            float outline = 1.0f;
            drawBars(centerX, centerY, actualGap - outline / 2.0f, lengthValue + outline,
                    thicknessValue + outline, outlineColor, 0.7f);
        }

        drawBars(centerX, centerY, actualGap, lengthValue, thicknessValue, crosshairColor, 0.65f);

        if (showDot.isValue()) {
            float dotSize = Math.max(1.0f, thicknessValue);
            if (showOutline.isValue()) {
                Render2D.rect(centerX - dotSize / 2.0f - 0.5f, centerY - dotSize / 2.0f - 0.5f,
                        dotSize + 1.0f, dotSize + 1.0f, ColorUtil.getColor(0, 190), 0.65f);
            }
            Render2D.rect(centerX - dotSize / 2.0f, centerY - dotSize / 2.0f,
                    dotSize, dotSize, crosshairColor, 0.55f);
        }
    }

    private void drawBars(float centerX, float centerY, float gapValue, float lengthValue,
                          float thicknessValue, int drawColor, float radius) {
        float halfThickness = thicknessValue / 2.0f;

        Render2D.rect(centerX + gapValue, centerY - halfThickness,
                lengthValue, thicknessValue, drawColor, radius);
        Render2D.rect(centerX - gapValue - lengthValue, centerY - halfThickness,
                lengthValue, thicknessValue, drawColor, radius);
        Render2D.rect(centerX - halfThickness, centerY - gapValue - lengthValue,
                thicknessValue, lengthValue, drawColor, radius);
        Render2D.rect(centerX - halfThickness, centerY + gapValue,
                thicknessValue, lengthValue, drawColor, radius);
    }

    private int getCrosshairColor() {
        if (hoverColor.isValue() && mc.crosshairTarget instanceof EntityHitResult) {
            return targetColor.getColor();
        }
        return color.getColor();
    }
}

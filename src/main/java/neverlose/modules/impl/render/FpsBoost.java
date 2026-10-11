package neverlose.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.MultiSelectSetting;
import neverlose.modules.module.setting.implement.SliderSettings;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class FpsBoost extends ModuleStructure {

    static FpsBoost instance;

    final MultiSelectSetting modes = new MultiSelectSetting("Оптимизации", "Какие оптимизации включить")
            .value("Сущности", "Армор-стенды", "Вещи", "Облака")
            .selected("Сущности", "Армор-стенды", "Вещи", "Облака");

    final SliderSettings entityDistance = new SliderSettings("Дистанция сущностей", "Не рендерить мобов/игроков дальше")
            .range(8.0f, 128.0f)
            .setValue(48.0f);

    final SliderSettings armorStandDistance = new SliderSettings("Дистанция армор-стендов", "Не рендерить армор-стенды дальше")
            .range(4.0f, 64.0f)
            .setValue(16.0f);

    final SliderSettings itemDistance = new SliderSettings("Дистанция вещей", "Не рендерить предметы и рамки дальше")
            .range(8.0f, 96.0f)
            .setValue(28.0f);

    public FpsBoost() {
        super("Оптимизация", "FPS Booster: кэширование блюра, отсечение дальних сущностей, отключение облаков", ModuleCategory.RENDER);
        instance = this;
        settings(modes, entityDistance, armorStandDistance, itemDistance);
    }

    public static FpsBoost getInstance() {
        return instance;
    }

    public boolean shouldCullEntity(Entity entity, double squaredDistance) {
        if (!isState()) return false;
        if (squaredDistance < 25.0) return false;

        double distance = Math.sqrt(squaredDistance);

        if (entity instanceof ArmorStandEntity) {
            return modes.isSelected("Армор-стенды") && distance > armorStandDistance.getValue();
        }
        if (entity instanceof LivingEntity && !(entity instanceof PlayerEntity)) {
            return modes.isSelected("Сущности") && distance > entityDistance.getValue();
        }
        if (entity instanceof ItemEntity || entity instanceof ItemFrameEntity) {
            return modes.isSelected("Вещи") && distance > itemDistance.getValue();
        }
        return modes.isSelected("Сущности") && distance > entityDistance.getValue();
    }

    public boolean shouldSkipClouds() {
        return isState() && modes.isSelected("Облака");
    }
}
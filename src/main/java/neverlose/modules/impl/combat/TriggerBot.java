package neverlose.modules.impl.combat;

import com.adl.nativeprotect.Native;
import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import neverlose.debob;
import neverlose.events.api.EventHandler;
import neverlose.events.api.types.EventType;
import neverlose.events.impl.PacketEvent;
import neverlose.events.impl.RotationUpdateEvent;
import neverlose.events.impl.TickEvent;
import neverlose.modules.impl.combat.aura.Angle;
import neverlose.modules.impl.combat.aura.AngleConnection;
import neverlose.modules.impl.combat.aura.MathAngle;
import neverlose.modules.impl.combat.aura.attack.StrikerConstructor;
import neverlose.modules.impl.combat.aura.impl.LinearConstructor;
import neverlose.modules.impl.combat.aura.impl.RotateConstructor;
import neverlose.modules.impl.combat.aura.target.MultiPoint;
import neverlose.modules.impl.combat.aura.target.TargetFinder;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.MultiSelectSetting;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.Instance;
import neverlose.util.string.PlayerInteractionHelper;

import java.util.Objects;

public class TriggerBot extends ModuleStructure {
    private static final float RANGE_MARGIN = 0.253F;
    private final TargetFinder targetSelector = new TargetFinder();
    private final MultiPoint pointFinder = new MultiPoint();
    public LivingEntity target;

    public SliderSettings attackRange = new SliderSettings("Дистанция удара", "Дальность атаки до цели")
            .setValue(3).range(1F, 6F);

    MultiSelectSetting targetType = new MultiSelectSetting("Тип таргета", "Фильтрует список целей по типу")
            .value("Игроки", "Мобы", "Животные", "Стойки для брони")
            .selected("Игроки", "Мобы", "Животные");

    public MultiSelectSetting attackSetting = new MultiSelectSetting("Настройки", "Параметры атаки")
            .value("Только криты", "Рандомизация крита", "Бить сквозь стены")
            .selected("Только криты");

    @Getter
    public SelectSetting sprintReset = new SelectSetting("Сброс спринта", "Выбор сброса спринта перед ударом")
            .value("Grim", "Отключен", "Легитный", "Интенсивный")
            .selected("Grim");

    @Getter
    public BooleanSetting smartCrits = new BooleanSetting("Умные криты", "Атака на земле когда кулдаун готов")
            .setValue(false)
            .visible(() -> attackSetting.isSelected("Только криты"));

    public TriggerBot() {
        super("TriggerBot", "Trigger Bot", ModuleCategory.COMBAT);
        settings(attackRange, targetType, attackSetting, sprintReset, smartCrits);
    }

    public static TriggerBot getInstance() {
        return Instance.get(TriggerBot.class);
    }

    @Override
    public void deactivate() {
        target = null;
    }

    @Native
    private LivingEntity updateTarget() {
        TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(targetType.getSelected());
        float range = attackRange.getValue() + RANGE_MARGIN;
        targetSelector.searchTargets(mc.world.getEntities(), range, 360, attackSetting.isSelected("Бить сквозь стены"));
        targetSelector.validateTarget(filter::isValid);
        return targetSelector.getCurrentTarget();
    }

    @EventHandler
   @Native
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (PlayerInteractionHelper.nullCheck()) return;

        switch (e.getType()) {
            case EventType.PRE -> target = updateTarget();
            case EventType.POST -> {
                if (target != null) {
                    debob.getInstance().getManager().getAttackPerpetrator().performTriggerAttack(getConfig(), this);
                }
            }
        }
    }

   @Native
    public StrikerConstructor.AttackPerpetratorConfigurable getConfig() {
        float baseRange = attackRange.getValue() + RANGE_MARGIN;

        Pair<Vec3d, Box> pointData = pointFinder.computeVector(
                target,
                baseRange,
                AngleConnection.INSTANCE.getRotation(),
                getSmoothMode().randomValue(),
                attackSetting.isSelected("Бить сквозь стены")
        );

        Vec3d computedPoint = pointData.getLeft();
        Box hitbox = pointData.getRight();

        Angle angle = MathAngle.fromVec3d(computedPoint.subtract(Objects.requireNonNull(mc.player).getEyePos()));

        return new StrikerConstructor.AttackPerpetratorConfigurable(
                target,
                angle,
                baseRange,
                attackSetting.getSelected(),
                null,
                hitbox
        );
    }

    public RotateConstructor getSmoothMode() {
        return new LinearConstructor();
    }

    @Native
    public boolean isResetSprintLegit() {
        return sprintReset.isSelected("Легитный");
    }

    @Native
    public boolean isResetSprintPacket() {
        return sprintReset.isSelected("Интенсивный");
    }

    @Native
    public boolean isOnlyCrits() {
        return attackSetting.isSelected("Только криты");
    }

    @Native
    public boolean isRandomizeCrit() {
        return attackSetting.isSelected("Рандомизация крита");
    }

    @EventHandler
    public void tick(TickEvent e) {}

    @EventHandler
    public void onPacket(PacketEvent e) {}
}

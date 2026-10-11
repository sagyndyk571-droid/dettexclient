package neverlose.modules.impl.combat;

import com.adl.nativeprotect.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.LivingEntity;
import neverlose.events.api.EventHandler;
import neverlose.events.api.types.EventType;
import neverlose.events.impl.InputEvent;
import neverlose.events.impl.RotationUpdateEvent;
import neverlose.events.impl.TickEvent;
import neverlose.modules.impl.combat.aura.Angle;
import neverlose.modules.impl.combat.aura.AngleConfig;
import neverlose.modules.impl.combat.aura.AngleConnection;
import neverlose.modules.impl.combat.aura.impl.LinearConstructor;
import neverlose.modules.impl.combat.aura.target.TargetFinder;
import neverlose.modules.impl.combat.macetarget.armor.ArmorSwapHandler;
import neverlose.modules.impl.combat.macetarget.armor.FireworkHandler;
import neverlose.modules.impl.combat.macetarget.attack.AttackHandler;
import neverlose.modules.impl.combat.macetarget.flight.FlightController;
import neverlose.modules.impl.combat.macetarget.prediction.TargetPredictor;
import neverlose.modules.impl.combat.macetarget.stage.StageHandler;
import neverlose.modules.impl.combat.macetarget.state.MaceState.Stage;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.MultiSelectSetting;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.Instance;
import neverlose.util.inventory.InventoryUtils;
import neverlose.util.inventory.SwapSettings;
import neverlose.util.math.TaskPriority;
import neverlose.util.timer.StopWatch;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaceTarget extends ModuleStructure {

    public static MaceTarget getInstance() {
        return Instance.get(MaceTarget.class);
    }

    final SelectSetting serverMode = new SelectSetting("Сервер", "Режим работы под сервер")
            .value("Default", "ReallyWorld")
            .selected("Default");

    final SelectSetting modeSetting = new SelectSetting("Режим", "Способ свапа")
            .value("Silent", "Legit")
            .selected("Silent");

    final SliderSettings height = new SliderSettings("Высота", "Высота полёта над целью")
            .range(20.0f, 60.0f)
            .setValue(30.0f);

    final MultiSelectSetting targetType = new MultiSelectSetting("Цели", "Типы целей")
            .value("Игроки", "Мобы", "Животные")
            .selected("Игроки");

    final BooleanSetting autoEquipChest = new BooleanSetting("Авто-нагрудник", "Одевать нагрудник при выключении")
            .setValue(true);

    final BooleanSetting predictMovement = new BooleanSetting("Предугадывание", "Предугадывать позицию убегающей цели")
            .setValue(true);

    final TargetPredictor predictor = new TargetPredictor();
    final FlightController flightController;
    final ArmorSwapHandler armorSwapHandler;
    final FireworkHandler fireworkHandler;
    final AttackHandler attackHandler = new AttackHandler();
    final StageHandler stageHandler;
    final TargetFinder targetFinder = new TargetFinder();
    final StopWatch fireworkTimer = new StopWatch();

    LivingEntity target;

    public MaceTarget() {
        super("MaceTarget", "Mace Target", ModuleCategory.COMBAT);
        settings(serverMode, modeSetting, height, targetType, autoEquipChest, predictMovement);

        flightController = new FlightController(predictor);
        armorSwapHandler = new ArmorSwapHandler(this::buildSettings);
        fireworkHandler = new FireworkHandler(this::buildSettings);
        stageHandler = new StageHandler(armorSwapHandler, fireworkHandler, attackHandler, fireworkTimer);
    }

    @Native
    private boolean isSilentMode() {
        return modeSetting.getSelected().equals("Silent");
    }

    @Native
    private boolean isReallyWorldMode() {
        return serverMode.getSelected().equals("ReallyWorld");
    }

    private SwapSettings buildSettings() {
        return isSilentMode() ? SwapSettings.instant() : SwapSettings.legit();
    }

    @Native
    private void updateHandlers() {
        stageHandler.setSilentMode(isSilentMode());
        stageHandler.setReallyWorldMode(isReallyWorldMode());
        stageHandler.setHeight(height.getValue());
        flightController.setPredictionEnabled(predictMovement.isValue());
        flightController.setHeight(height.getValue());
    }

    @Override
    @Native
    public void activate() {
        stageHandler.reset();
        target = null;
        attackHandler.reset();
        armorSwapHandler.reset();
        fireworkHandler.reset();
        predictor.reset();
        fireworkTimer.reset();
    }

    @Override
    @Native
    public void deactivate() {
        if (autoEquipChest.isValue() && mc.player != null) {
            equipChestplateOnDisable();
        }

        armorSwapHandler.forceRestore();
        fireworkHandler.forceRestore();
        target = null;
        targetFinder.releaseTarget();
        armorSwapHandler.reset();
        fireworkHandler.reset();
        predictor.reset();
        AngleConnection.INSTANCE.startReturning();
    }

   @Native
    private void equipChestplateOnDisable() {
        if (InventoryUtils.hasElytra()) {
            int slot = InventoryUtils.findChestArmorSlot();
            if (slot != -1) {
                int wrappedSlot = InventoryUtils.wrapSlot(slot);
                InventoryUtils.swap(wrappedSlot, 6);
                InventoryUtils.closeScreen();
            }
        }
    }

    @EventHandler
   @Native
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (event.getType() == EventType.PRE) {
            updateHandlers();

            if (target == null || !target.isAlive()) {
                findTarget();
            }

            if (target == null) return;

            predictor.update(target);

            Stage currentStage = stageHandler.getStage();

            switch (currentStage) {
                case FLYING_UP -> {
                    if (InventoryUtils.hasElytra() && mc.player.isGliding()) {
                        Angle targetAngle = flightController.calculateAngle(target, currentStage);
                        rotateTo(targetAngle);
                    }
                }
                case TARGETTING, ATTACKING -> {
                    Angle targetAngle = flightController.calculateAngle(target, currentStage);
                    rotateTo(targetAngle);
                }
            }
        }

        if (event.getType() == EventType.POST) {
            handlePostRotation();
        }
    }

   @Native
    private void handlePostRotation() {
        if (attackHandler.isPendingAttack()) {
            attackHandler.performAttack(target);
            attackHandler.setPendingAttack(false);

            if (attackHandler.isShouldDisableAfterAttack()) {
                attackHandler.setShouldDisableAfterAttack(false);
                setState(false);
            }
        }
    }

    @EventHandler
   @Native
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            resetAllStates();
            return;
        }

        if (!isSilentMode()) {
            armorSwapHandler.processLoop();
            fireworkHandler.processLoop();
        }

        if (armorSwapHandler.isActive() || fireworkHandler.isActive()) {
            return;
        }

        if (target == null || !target.isAlive()) {
            return;
        }

        processStage();
    }

   @Native
    private void processStage() {
        boolean hasElytra = InventoryUtils.hasElytra();
        Stage currentStage = stageHandler.getStage();

        switch (currentStage) {
            case PREPARE -> stageHandler.handlePrepare(hasElytra);
            case FLYING_UP -> stageHandler.handleFlyingUp(target, hasElytra);
            case TARGETTING -> stageHandler.handleTargetting(target);
            case ATTACKING -> stageHandler.handleAttacking(target, hasElytra);
        }
    }

    @EventHandler
    public void onInput(InputEvent event) {
        if (mc.player == null) return;

        if (armorSwapHandler.getMovement().isBlocked() || fireworkHandler.getMovement().isBlocked()) {
            event.setDirectionalLow(false, false, false, false);
            event.setJumping(false);
        }

        if (target != null && InventoryUtils.hasElytra() && stageHandler.getStage() == Stage.FLYING_UP) {
            if (mc.player.isOnGround()) {
                event.setJumping(true);
            } else if (!mc.player.isGliding() && !mc.player.getAbilities().flying) {
                event.setJumping(mc.player.age % 2 == 0);
            }
        }
    }

    @Native
    private void findTarget() {
        TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(targetType.getSelected());
        targetFinder.searchTargets(mc.world.getEntities(), 128.0f, 360, true);
        targetFinder.validateTarget(filter::isValid);
        target = targetFinder.getCurrentTarget();
    }

    @Native
    private void rotateTo(Angle angle) {
        AngleConfig config = new AngleConfig(new LinearConstructor(), true, false);
        Angle.VecRotation rotation = new Angle.VecRotation(angle, angle.toVector());
        AngleConnection.INSTANCE.rotateTo(rotation, target, 1, config, TaskPriority.HIGH_IMPORTANCE_1, this);
    }

    @Native
    private void resetAllStates() {
        armorSwapHandler.reset();
        fireworkHandler.reset();
        attackHandler.reset();
        predictor.reset();
        target = null;
        stageHandler.reset();
    }
}
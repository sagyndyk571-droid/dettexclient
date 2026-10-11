package neverlose.modules.impl.combat;

import lombok.Getter;
import lombok.experimental.NonFinal;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import neverlose.debob;
import neverlose.events.api.EventHandler;
import neverlose.events.api.types.EventType;
import neverlose.events.impl.InputEvent;
import neverlose.events.impl.RotationUpdateEvent;
import neverlose.events.impl.TickEvent;
import neverlose.modules.impl.combat.aura.Angle;
import neverlose.modules.impl.combat.aura.AngleConfig;
import neverlose.modules.impl.combat.aura.AngleConnection;
import neverlose.modules.impl.combat.aura.MathAngle;
import neverlose.modules.impl.combat.aura.attack.StrikeManager;
import neverlose.modules.impl.combat.aura.attack.StrikerConstructor;
import neverlose.modules.impl.combat.aura.impl.*;
import neverlose.modules.impl.combat.aura.impl.RotateConstructor;
import neverlose.modules.impl.combat.aura.rotations.*;
import neverlose.modules.impl.combat.aura.target.MultiPoint;
import neverlose.modules.impl.combat.aura.target.TargetFinder;
import neverlose.modules.impl.movement.ElytraTarget;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.*;
import neverlose.util.Instance;
import neverlose.util.math.TaskPriority;
import neverlose.util.network.Network;

import java.util.Objects;

public class Aura extends ModuleStructure {


    public static Aura getInstance() {
        return Instance.get(Aura.class);
    }

    public final SelectSetting mode = new SelectSetting("Режим наводки", "Select aim mode")
            .value("Matrix", "ReallyWorld", "FunTime Snap", "FunTime Polar", "Snap", "SpookyTime", "Neuro", "Shard", "Grim AI")
            .selected("Matrix");

    public final SelectSetting neuroProfile = new SelectSetting("Нейро-Профиль", "Select neuro profile")
            .value("Нет профилей")
            .selected("Нет профилей")
            .visible(() -> mode.isSelected("Neuro") || mode.isSelected("FunTime Polar"));

    private final SelectSetting moveFix = new SelectSetting("Коррекция движения", "Select move fix mode")
            .value("Сфокусированная", "Свободная", "Преследование", "Таргет", "Отключена")
            .selected("Сфокусированная");

    @Getter
    public final SliderSettings attackrange = new SliderSettings("Дистанция удара", "Set range value")
            .range(2.0f, 6.0f)
            .setValue(3.0f);

    private final SliderSettings lookrange = new SliderSettings("Дистанция поиска", "Set look range value")
            .range(0.0f, 10.0f)
            .setValue(1.5f);

    public final MultiSelectSetting options = new MultiSelectSetting("Настройки", "Select settings")
            .value("Бить сквозь стены", "Ломать щит", "Рандомизация крита", "Рандомизация вращения", "Не бить если ешь")
            .selected("Бить сквозь стены", "Ломать щит", "Не бить если ешь");

    private final MultiSelectSetting targetType = new MultiSelectSetting("Настройка целей", "Select target settings")
            .value("Игроки", "Мобы", "Животные", "Друзья", "Стойки для брони")
            .selected("Игроки", "Мобы", "Животные");

    @Getter
    private final SelectSetting resetSprintMode = new SelectSetting("Сброс спринта", "Reset sprint mode")
            .value("Grim", "Отключен", "Легитный", "Пакетный")
            .selected("Grim");

    @Getter
    private final BooleanSetting checkCrit = new BooleanSetting("Только криты", "Only critical hits")
            .setValue(true);


    @Getter
    private final BooleanSetting smartCrits = new BooleanSetting("Умные криты",
            "Smart crits - attack on ground when possible")
            .setValue(false)
            .visible(() -> checkCrit.isValue());

    public boolean isRandomizationEnabled() {
        return options.isSelected("Рандомизация вращения");
    }

    @Getter
    private final BooleanSetting rwWallBypass = new BooleanSetting("Обход RW стен", "Обход для ударов через тонкие стены")
            .setValue(false);

    @Getter
    private final BooleanSetting rwWallLookDown = new BooleanSetting("Смотреть вниз", "Держать взгляд ниже пока RW обход ждёт удар")
            .setValue(false)
            .visible(rwWallBypass::isValue);

    public Aura() {
        super("Aura", ModuleCategory.COMBAT);
        settings(mode, neuroProfile, attackrange, lookrange, options, rwWallBypass, rwWallLookDown, targetType, moveFix, resetSprintMode, checkCrit, smartCrits);
    }

    @NonFinal
    public static LivingEntity target;

    @NonFinal
    public LivingEntity lastTarget;

    TargetFinder targetSelector = new TargetFinder();
    MultiPoint pointFinder = new MultiPoint();

    private final MatrixAngle matrixAngle = new MatrixAngle();
    private final ReallyWorldAngle reallyWorldAngle = new ReallyWorldAngle();
    private final FTAngle ftAngle = new FTAngle();
    private final FunTimePolarAngle funTimePolarAngle = new FunTimePolarAngle();
    private final SPAngle spAngle = new SPAngle();
    private final SnapAngle snapAngle = new SnapAngle();
    private final NeuroAngle neuroAngle = new NeuroAngle();
    private final ShardAngle shardAngle = new ShardAngle();
    private final GrimAngle grimAngle = new GrimAngle();
    private final LinearConstructor linearAngle = new LinearConstructor();

    @Override
    public void activate() {
        AngleConnection.INSTANCE.reset();
        neuroAngle.reset();
        funTimePolarAngle.reset();
        grimAngle.reset();
        shardAngle.reset();
    }

    @Override
    public void deactivate() {
        AngleConnection.INSTANCE.reset();
        target = null;
        lastTarget = null;
        neuroAngle.reset();
        funTimePolarAngle.reset();
        grimAngle.reset();
        shardAngle.reset();
    }

    @EventHandler
    private void tick(TickEvent event) {
        // Only check for new profiles once every 3 seconds to avoid main thread IO lag
        if (mc.player != null && mc.player.age % 60 == 0 && (mode.isSelected("Neuro") || mode.isSelected("FunTime Polar"))) {
            java.util.List<String> list = neverlose.command.impl.NeiroCommand.getAvailableProfiles();
            // SelectSetting.value() is now optimized to ignore redundant calls
            neuroProfile.value(list.toArray(new String[0]));
        }
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        switch (e.getType()) {
            case EventType.PRE -> {
                LivingEntity previousTarget = target;
                target = updateTarget();

                if (previousTarget != null && target == null) {
                    debob.getInstance().getManager()
                            .getAttackPerpetrator()
                            .getAttackHandler()
                            .resetPendingState();
                    AngleConnection.INSTANCE.reset();
                    lastTarget = null;
                    neuroAngle.reset();
                    funTimePolarAngle.reset();
                    grimAngle.reset();
                    shardAngle.reset();
                }

                if (target != previousTarget && target != null) {
                    neuroAngle.reset();
                    funTimePolarAngle.reset();
                    grimAngle.reset();
                    shardAngle.reset();
                }

                boolean passed = false;
                if (mode.isSelected("FunTime Snap") || mode.isSelected("HolyWorld")) {
                    passed = true;
                }
                if (target != null && passed && target.distanceTo(mc.player) <= attackrange.getValue() + 0.25F) {
                    rotateToTarget(getConfig());
                    lastTarget = target;
                }
                if (target != null && !passed) {
                    rotateToTarget(getConfig());
                    lastTarget = target;
                }
            }
            case EventType.POST -> {
                if (target != null && !neverlose.command.impl.NeiroCommand.isRecording) {
                    debob.getInstance().getManager().getAttackPerpetrator().performAttack(getConfig());
                }
            }
        }
    }

    public StrikerConstructor.AttackPerpetratorConfigurable getConfig() {
        float baseRange = attackrange.getValue();

        Pair<Vec3d, Box> pointData = pointFinder.computeVector(
                target,
                baseRange,
                AngleConnection.INSTANCE.getRotation(),
                getSmoothMode().randomValue(),
                options.isSelected("Бить сквозь стены"));

        Vec3d computedPoint = pointData.getLeft();
        Box hitbox = pointData.getRight();

        if (mc.player.isGliding() && target.isGliding()) {
            Vec3d targetVelocity = target.getVelocity();
            double targetSpeed = targetVelocity.horizontalLength();

            float leadTicks = 0;
            if (ElytraTarget.shouldElytraTarget && ElytraTarget.getInstance() != null
                    && ElytraTarget.getInstance().isState()) {
                leadTicks = ElytraTarget.getInstance().getPredictionTicks();
            }

            if (targetSpeed > 0.35) {
                Vec3d predictedPos = target.getEntityPos().add(targetVelocity.multiply(leadTicks));
                computedPoint = predictedPos.add(0, target.getHeight() / 2, 0);

                hitbox = new Box(
                        predictedPos.x - target.getWidth() / 2,
                        predictedPos.y,
                        predictedPos.z - target.getWidth() / 2,
                        predictedPos.x + target.getWidth() / 2,
                        predictedPos.y + target.getHeight(),
                        predictedPos.z + target.getWidth() / 2);
            }
        }

        Angle angle = MathAngle.fromVec3d(computedPoint.subtract(Objects.requireNonNull(mc.player).getEyePos()));
        return new StrikerConstructor.AttackPerpetratorConfigurable(
                target,
                angle,
                baseRange,
                options.getSelected(),
                mode,
                hitbox);
    }

    public AngleConfig getRotationConfig() {
        boolean visibleCorrection = !moveFix.isSelected("Отключена");
        boolean freeCorrection = moveFix.isSelected("Свободная");
        return new AngleConfig(getSmoothMode(), visibleCorrection, freeCorrection);
    }

    private void rotateToTarget(StrikerConstructor.AttackPerpetratorConfigurable config) {
        StrikeManager attackHandler = debob.getInstance().getManager().getAttackPerpetrator()
                .getAttackHandler();
        AngleConnection controller = AngleConnection.INSTANCE;
        Angle.VecRotation rotation = new Angle.VecRotation(config.getAngle(), config.getAngle().toVector());
        AngleConfig rotationConfig = getRotationConfig();

        boolean elytraMode = mc.player.isGliding() && ElytraTarget.getInstance() != null
                && ElytraTarget.getInstance().isState();

        switch (mode.getSelected()) {

            case "FunTime Snap" -> {
                if (attackHandler.canAttack(config, 5)) {
                    controller.clear();
                    controller.rotateTo(rotation, target, 60, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
                }
            }

            case "Snap" -> {
                if (attackHandler.canAttack(config, 0)) {
                    controller.rotateTo(rotation, target, 0, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
                }
            }

            case "Matrix", "ReallyWorld", "FunTime Polar", "SpookyTime", "Neuro", "Shard", "Grim AI" -> {
                controller.rotateTo(rotation, target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
            }

        }

        if (elytraMode) {
            controller.rotateTo(rotation, target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
        }
    }

    @EventHandler
    public void onInput(InputEvent event) {
        if (mc.player == null || mc.world == null)
            return;

        PlayerInput input = event.getInput();
        if (input == null)
            return;

        if (!isState())
            return;

        if (target == null || !target.isAlive())
            return;

        boolean w = mc.options.forwardKey.isPressed();
        boolean s = mc.options.backKey.isPressed();
        boolean a = mc.options.leftKey.isPressed();
        boolean d = mc.options.rightKey.isPressed();

        if (moveFix.isSelected("Таргет")) {
            Vec3d playerPos = mc.player.getEntityPos();
            Vec3d targetPos = target.getEntityPos();

            Vec3d moveTarget = new Vec3d(targetPos.x, playerPos.y, targetPos.z);
            Vec3d dir = moveTarget.subtract(playerPos).normalize();

            float yaw = (target != null && mode.isSelected("Neuro")) ?
                    MathAngle.calculateAngle(target.getBoundingBox().getCenter()).getYaw() :
                    AngleConnection.INSTANCE.getRotation().getYaw();
            float moveAngle = (float) Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90F;
            float angleDiff = MathHelper.wrapDegrees(moveAngle - yaw);

            boolean forward = false, back = false, left = false, right = false;

            if (angleDiff >= -22.5 && angleDiff < 22.5) {
                forward = true;
            } else if (angleDiff >= 22.5 && angleDiff < 67.5) {
                forward = true;
                right = true;
            } else if (angleDiff >= 67.5 && angleDiff < 112.5) {
                right = true;
            } else if (angleDiff >= 112.5 && angleDiff < 157.5) {
                back = true;
                right = true;
            } else if (angleDiff >= -67.5 && angleDiff < -22.5) {
                forward = true;
                left = true;
            } else if (angleDiff >= -112.5 && angleDiff < -67.5) {
                left = true;
            } else if (angleDiff >= -157.5 && angleDiff < -112.5) {
                back = true;
                left = true;
            } else {
                back = true;
            }

            event.setDirectionalLow(forward, back, left, right);
            return;
        }

        if (moveFix.isSelected("Преследование")) {
            if (!w && !s && !a && !d)
                return;

            Vec3d playerPos = mc.player.getEntityPos();
            Box targetBox = target.getBoundingBox();
            Vec3d center = targetBox.getCenter();

            float targetYaw = target.getYaw();
            double rad = Math.toRadians(targetYaw);

            Vec3d forwardDir = new Vec3d(-Math.sin(rad), 0, Math.cos(rad)).normalize();
            Vec3d rightDir = new Vec3d(-forwardDir.z, 0, forwardDir.x).normalize();
            Vec3d leftDir = rightDir.multiply(-1);

            double halfWidth = target.getWidth() / 2.0;
            double offset = halfWidth + 0.1;

            Vec3d moveTargetVec = center;
            Vec3d offsetVec = Vec3d.ZERO;

            if (w)
                offsetVec = offsetVec.add(forwardDir);
            if (s)
                offsetVec = offsetVec.add(forwardDir.multiply(-1.0));
            if (a)
                offsetVec = offsetVec.add(leftDir);
            if (d)
                offsetVec = offsetVec.add(rightDir);

            if (offsetVec.lengthSquared() > 0) {
                offsetVec = offsetVec.normalize().multiply(offset);
                moveTargetVec = center.add(offsetVec);
            }

            moveTargetVec = new Vec3d(moveTargetVec.x, playerPos.y, moveTargetVec.z);
            Vec3d dir = moveTargetVec.subtract(playerPos).normalize();

            float yaw = (target != null && mode.isSelected("Neuro")) ?
                    MathAngle.calculateAngle(target.getBoundingBox().getCenter()).getYaw() :
                    AngleConnection.INSTANCE.getRotation().getYaw();
            float moveAngle = (float) Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90F;
            float angleDiff = MathHelper.wrapDegrees(moveAngle - yaw);

            boolean forward = false, back = false, left = false, right = false;

            if (angleDiff >= -22.5 && angleDiff < 22.5) {
                forward = true;
            } else if (angleDiff >= 22.5 && angleDiff < 67.5) {
                forward = true;
                right = true;
            } else if (angleDiff >= 67.5 && angleDiff < 112.5) {
                right = true;
            } else if (angleDiff >= 112.5 && angleDiff < 157.5) {
                back = true;
                right = true;
            } else if (angleDiff >= -67.5 && angleDiff < -22.5) {
                forward = true;
                left = true;
            } else if (angleDiff >= -112.5 && angleDiff < -67.5) {
                left = true;
            } else if (angleDiff >= -157.5 && angleDiff < -112.5) {
                back = true;
                left = true;
            } else {
                back = true;
            }

            event.setDirectionalLow(forward, back, left, right);
        }
    }

    private LivingEntity updateTarget() {
        TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(targetType.getSelected());
        float range = attackrange.getValue() + 0.25F
                + (mc.player.isGliding() && ElytraTarget.getInstance() != null && ElytraTarget.getInstance().isState()
                ? ElytraTarget.getInstance().elytraFindRange.getValue()
                : lookrange.getValue());

        float dynamicFov = 360;

        targetSelector.searchTargets(mc.world.getEntities(), range, dynamicFov,
                options.isSelected("Бить сквозь стены"));
        targetSelector.validateTarget(filter::isValid);
        return targetSelector.getCurrentTarget();
    }

    public RotateConstructor getSmoothMode() {
        if (mc.player.isGliding() && ElytraTarget.getInstance() != null && ElytraTarget.getInstance().isState()) {
            return new LinearConstructor();
        }
        if (Network.isReallyWorld() && mode.isSelected("Matrix")) {
            return reallyWorldAngle;
        }
        return switch (mode.getSelected()) {
            case "FunTime Snap" -> ftAngle;
            case "FunTime Polar" -> funTimePolarAngle;
            case "SpookyTime" -> spAngle;
            case "Snap" -> snapAngle;
            case "Matrix" -> matrixAngle;
            case "ReallyWorld" -> reallyWorldAngle;
            case "Neuro" -> neuroAngle;
            case "Shard" -> shardAngle;
            case "Grim AI" -> grimAngle;
            default -> linearAngle;
        };
    }
}

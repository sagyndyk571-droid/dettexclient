package neverlose.modules.impl.movement;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.InputEvent;
import neverlose.events.impl.MovePostEvent;
import neverlose.events.impl.PacketEvent;
import neverlose.events.impl.PlayerTravelEvent;
import neverlose.events.impl.PostMotionEvent;
import neverlose.events.impl.TickEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.Instance;
import neverlose.util.move.MoveUtil;
import neverlose.util.string.PlayerInteractionHelper;
import neverlose.util.timer.TimerManager;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Speed extends ModuleStructure {
    static final float REALLY_WORLD2_FALL_TIMER = 1.58f;
    static final double REALLY_WORLD2_AIR_BOOST = 0.0055;

    SelectSetting mode = new SelectSetting("Режим", "Выберите режим скорости")
            .value("Vanilla", "Grim", "FunTime", "HolyWorld", "ReallyWorld", "ReallyWorld2")
            .selected("Grim");

    SliderSettings speed = new SliderSettings("Скорость", "Настройка скорости передвижения")
            .range(1.0f, 5.0f)
            .setValue(1.5f)
            .visible(() -> mode.isSelected("Vanilla"));

    @NonFinal
    int ticks;

    @NonFinal
    int groundTicks;

    @NonFinal
    String lastMode = "";

    public Speed() {
        super("Speed", "Speed", ModuleCategory.MOVEMENT);
        settings(mode, speed);
    }

    public static Speed getInstance() {
        return Instance.get(Speed.class);
    }

    public static boolean isReallyWorldSpeedActive() {
        Speed speed = getInstance();
        return speed != null && speed.isState() && speed.isReallyWorldMode();
    }

    public static boolean isReallyWorld2SpeedActive() {
        Speed speed = getInstance();
        return speed != null && speed.isState() && speed.isReallyWorld2Mode();
    }

    public static boolean isAnyReallyWorldSpeedActive() {
        Speed speed = getInstance();
        return speed != null && speed.isState() && (speed.isReallyWorldMode() || speed.isReallyWorld2Mode());
    }

    @Override
    public void activate() {
        ticks = 0;
        groundTicks = 0;
        lastMode = mode.getSelected();
        TimerManager.reset();
    }

    @Override
    public void deactivate() {
        ticks = 0;
        groundTicks = 0;
        lastMode = "";
        TimerManager.reset();
    }

    @EventHandler

    public void onTick(TickEvent event) {
        updateModeState();

        if (!isReallyWorldMode() && !isReallyWorld2Mode()) {
            TimerManager.reset();
        }

        if (isReallyWorldMode() && mc.player != null && !MoveUtil.hasPlayerMovement()) {
            ticks = 0;
            groundTicks = 0;
            TimerManager.reset();
            return;
        }

        if (mode.isSelected("Vanilla")) {
            MoveUtil.setVelocity(speed.getValue() / 3);
        }
        if (isReallyWorld2Mode()) {
            handleReallyWorld2Mode();
        }
    }

    @EventHandler
    public void onMovePost(MovePostEvent event) {
        if (isReallyWorld2Mode()) {
            handleReallyWorld2MovePost();
            return;
        }

        if (!isReallyWorldMode() || mc.player == null || mc.world == null) {
            return;
        }

        if (!MoveUtil.hasPlayerMovement()) {
            ticks = 0;
            groundTicks = 0;
            TimerManager.reset();
            return;
        }

        TimerManager.setTimer(1.7f);

        if (ticks > 3) {
            double boost = 0.03;
            if (ticks % 2 == 0) {
                mc.player.addVelocityInternal(new Vec3d(0.0, 0.03f, 0.0));
                boost = mc.player.isOnGround() ? 0.085 : 0.03;
            }

            float direction = MoveUtil.getdir();
            double x = 0.0;
            double z = 0.0;
            if (direction != -1.0f) {
                double yaw = Math.toRadians(direction);
                x = -Math.sin(yaw);
                z = Math.cos(yaw);
            }

            mc.player.addVelocityInternal(new Vec3d(x * boost, 0.0, z * boost));
        }

        ticks++;
    }

    @EventHandler

    public void onInput(InputEvent event) {
        if (isReallyWorld2Mode()) {
            handleReallyWorld2Input();
            return;
        }

        if (!isReallyWorldMode() || mc.player == null || mc.world == null) {
            return;
        }

        if (mc.player.verticalCollision) {
            groundTicks++;
        } else {
            groundTicks = 0;
        }

        if (groundTicks >= 1) {
            mc.player.jump();
        }
    }

    @EventHandler

    public void onPacket(PacketEvent event) {
        if (!isReallyWorldMode() || mc.player == null || mc.world == null) {
            return;
        }

        if (!event.isSend() && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            if (ticks % 2 == 1) {
                ticks++;
            }
            TimerManager.reset();
        }
    }

    @EventHandler
    public void onMotion(PlayerTravelEvent event) {
        if (mode.isSelected("FunTime")) {
            handleFunTimeMode();
        }
        if (mode.isSelected("Grim") && event.isPre() && MoveUtil.hasPlayerMovement()) {
            handleGrimMode();
        }
        if (mode.isSelected("HolyWorld") && event.isPre() && MoveUtil.hasPlayerMovement()) {
            handleHolyWorldMode();
        }
    }

    @EventHandler
    public void onPostMotion(PostMotionEvent event) {
        if (isReallyWorldMode()) {
            handleReallyWorldPostMotion();
        }
    }

    private void updateModeState() {
        String selected = mode.getSelected();
        if (selected.equals(lastMode)) {
            return;
        }

        ticks = 0;
        groundTicks = 0;
        lastMode = selected;
        TimerManager.reset();
    }

    private boolean isReallyWorldMode() {
        return mode.isSelected("ReallyWorld");
    }

    private boolean isReallyWorld2Mode() {
        return mode.isSelected("ReallyWorld2");
    }

    private void handleReallyWorldPostMotion() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
            return;
        }

        if (!MoveUtil.hasPlayerMovement()) {
            return;
        }

        if (ticks % 2 == 0) {
            TimerManager.setTimer(0.3f);
            PlayerInteractionHelper.sendPacketWithOutEvent(
                    new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING)
            );
        }
    }

    private void handleReallyWorld2Mode() {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (!MoveUtil.hasPlayerMovement()) {
            ticks = 0;
            groundTicks = 0;
            TimerManager.reset();
            return;
        }

        if (mc.player.isOnGround()) {
            TimerManager.setTimer(0.821f);
        } else if (mc.player.fallDistance > 0.1f && mc.player.fallDistance < 0.72f) {
            TimerManager.setTimer(REALLY_WORLD2_FALL_TIMER);
        } else if (mc.player.fallDistance >= 0.72f) {
            TimerManager.setTimer(0.91f);
        } else {
            TimerManager.reset();
        }
    }

    private void handleReallyWorld2Input() {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (!MoveUtil.hasPlayerMovement() || mc.player.isSneaking() || mc.player.isGliding()) {
            return;
        }

        if (mc.player.verticalCollision) {
            groundTicks++;
        } else {
            groundTicks = 0;
        }

        if (groundTicks >= 1 || (mc.player.isOnGround() && mc.player.isTouchingWater())) {
            mc.player.jump();
        }
    }

    private void handleReallyWorld2MovePost() {
        if (mc.player == null || mc.world == null || !MoveUtil.hasPlayerMovement()) {
            return;
        }

        if (ticks % 2 == 0 && !mc.player.isOnGround() && mc.player.fallDistance > 0.1f && mc.player.fallDistance < 0.72f) {
            double[] motion = MoveUtil.forward(REALLY_WORLD2_AIR_BOOST);
            mc.player.addVelocityInternal(new Vec3d(motion[0], 0.0, motion[1]));
        }

        ticks++;
    }

    private void handleFunTimeMode() {
        if (!mc.player.isSwimming() && !mc.player.isGliding() && !mc.player.isSneaking()) {
            if (mc.player.getBoundingBox().maxY - mc.player.getBoundingBox().minY < 1.5f) {
                float motion = mc.player.hasStatusEffect(StatusEffects.SPEED) ? 0.32f : 0.28f;
                MoveUtil.setVelocity(motion);
            }
        }
    }
    private void handleGrimMode() {
        int collisions = 0;

        for (Entity ent : mc.world.getEntities()) {
            if (ent != mc.player
                    && !(ent instanceof ArmorStandEntity)
                    && (ent instanceof LivingEntity || ent instanceof BoatEntity)
                    && mc.player.getBoundingBox().expand(0.5f).intersects(ent.getBoundingBox())) {
                collisions++;
            }
        }
        double[] motion = MoveUtil.forward(0.07 * collisions);
        mc.player.addVelocity(motion[0], 0, motion[1]);
    }
    private void handleHolyWorldMode() {
        int collisions = 0;

        for (Entity ent : mc.world.getEntities()) {
            if (ent != mc.player
                    && !(ent instanceof ArmorStandEntity)
                    && (ent instanceof LivingEntity || ent instanceof BoatEntity)
                    && mc.player.getBoundingBox().expand(0.35f).intersects(ent.getBoundingBox())) {
                collisions++;
            }
        }
        double[] motion = MoveUtil.forward(0.0205 * collisions);
        mc.player.addVelocity(motion[0], 0, motion[1]);
    }
}

package neverlose.modules.impl.movement;

import com.adl.nativeprotect.Native;
import net.minecraft.client.MinecraftClient;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.TickEvent;
import neverlose.modules.impl.combat.Aura;
import neverlose.modules.impl.combat.aura.Angle;
import neverlose.modules.impl.combat.aura.AngleConfig;
import neverlose.modules.impl.combat.aura.AngleConnection;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.Instance;
import neverlose.util.math.TaskPriority;
import neverlose.util.move.MoveUtil;

public class Strafe extends ModuleStructure {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public SelectSetting mode = new SelectSetting("Режим", "Выберите тип стрейфов")
            .value("Matrix", "Grim", "ReallyWorld")
            .selected("Matrix");
    SliderSettings speed = new SliderSettings("Скорость", "Выберите скорость для стрейфа")
            .setValue(0.42F).range(0F, 1F).visible(() -> mode.isSelected("Matrix"));

    private float lastYaw, lastPitch;
    private final Angle rot = new Angle(0, 0);

    public Strafe() {
        super("Strafe", "Strafe", ModuleCategory.MOVEMENT);
        settings(mode, speed);
    }

    public static Strafe getInstance() {
        return Instance.get(Strafe.class);
    }

    @EventHandler
   @Native
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        boolean moving = MoveUtil.hasPlayerMovement();

        float yaw = mc.player.getYaw();

        if (mode.isSelected("Matrix")) {
            handleMatrixMode(moving, yaw);
        } else if (mode.isSelected("Grim")) {
            handleGrimMode(moving, yaw);
        } else if (mode.isSelected("ReallyWorld")) {
            handleReallyWorldMode(moving, yaw);
        }

        lastYaw = yaw;
        lastPitch = 0;
    }

   @Native
    private void handleMatrixMode(boolean moving, float yaw) {
        if (moving) {
            yaw = MoveUtil.moveYaw(mc.player.getYaw());
            double motion = speed.getValue() * 1.5f;
            MoveUtil.setVelocity(motion);
        } else {
            MoveUtil.setVelocity(0);
        }
        mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z);
    }

   @Native
    private void handleGrimMode(boolean moving, float yaw) {
        if (moving) {
            AngleConfig.freeCorrection = true;
            yaw = MoveUtil.moveYaw(mc.player.getYaw());
            rot.setYaw(yaw);
            rot.setPitch(mc.player.getPitch());
            if (Aura.getInstance().target == null) {
                AngleConnection.INSTANCE.rotateTo(rot, AngleConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
            }
        }
    }

    private void handleReallyWorldMode(boolean moving, float yaw) {
        if (!moving) {
            return;
        }

        AngleConfig.freeCorrection = true;
        yaw = MoveUtil.moveYaw(mc.player.getYaw());
        rot.setYaw(yaw);
        rot.setPitch(mc.player.getPitch());

        if (Aura.getInstance().target == null) {
            AngleConnection.INSTANCE.rotateTo(rot, AngleConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
        }

        limitReallyWorldHorizontalSpeed();
    }

    private void limitReallyWorldHorizontalSpeed() {
        if (mc.player == null || mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.isClimbing()
                || mc.player.isGliding() || mc.player.hasVehicle()) {
            return;
        }

        double maxSpeed = Speed.isAnyReallyWorldSpeedActive()
                ? (mc.player.isOnGround() ? 0.36 : 0.30)
                : (mc.player.isOnGround() ? 0.24 : 0.18);
        double x = mc.player.getVelocity().x;
        double z = mc.player.getVelocity().z;
        double horizontal = Math.hypot(x, z);
        if (horizontal > maxSpeed) {
            double scale = maxSpeed / horizontal;
            mc.player.setVelocity(x * scale, mc.player.getVelocity().y, z * scale);
        }
    }

    @Override
    @Native
    public void activate() {
        super.activate();
        lastYaw = mc.player != null ? mc.player.getYaw() : 0;
        lastPitch = mc.player != null ? mc.player.getPitch() : 0;
    }
}

package neverlose.modules.impl.movement;

import com.adl.nativeprotect.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.SwimmingEvent;
import neverlose.events.impl.TickEvent;
import neverlose.modules.impl.combat.aura.AngleConnection;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.move.MoveUtil;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WaterSpeed extends ModuleStructure {

    SelectSetting modeSetting = new SelectSetting("Mode", "WaterSpeed bypass mode")
            .value("FunTime", "ReallyWorld")
            .selected("FunTime");

    BooleanSetting speedSync = new BooleanSetting("Speed sync", "Use ReallyWorld water mode with ReallyWorld Speed")
            .setValue(true);

    SliderSettings waterSpeed = new SliderSettings("Water speed", "Water movement multiplier")
            .range(1.0f, 1.35f)
            .setValue(1.13f);

    SliderSettings verticalSpeed = new SliderSettings("Vertical", "Water up/down speed")
            .range(0.02f, 0.12f)
            .setValue(0.058f);

    BooleanSetting iceBoost = new BooleanSetting("Ice boost", "Extra boost only while the head is under ice")
            .setValue(false);

    SliderSettings iceBoostSpeed = new SliderSettings("Ice speed", "Ice boost multiplier")
            .range(1.0f, 3.0f)
            .setValue(1.5f)
            .visible(() -> iceBoost.isValue());

    public WaterSpeed() {
        super("WaterSpeed", "Water Speed", ModuleCategory.MOVEMENT);
        settings(modeSetting, speedSync, waterSpeed, verticalSpeed, iceBoost, iceBoostSpeed);
    }

    @EventHandler
    @Native
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (shouldUseReallyWorldWater()) {
            handleReallyWorldTick();
        } else if (modeSetting.isSelected("FunTime") && mc.player.isSwimming() && mc.player.isOnGround()) {
            mc.player.jump();
            mc.player.setVelocity(mc.player.getVelocity().x, 0.1, mc.player.getVelocity().z);
        }

        if (iceBoost.isValue() && mc.player.isSwimming() && isHeadUnderIce()) {
            applyIceBoost();
        }
    }

    @Native
    private void applyIceBoost() {
        float speedMultiplier = iceBoostSpeed.getValue();

        double yaw = Math.toRadians(mc.player.getYaw());
        double pitch = Math.toRadians(mc.player.getPitch());

        double baseSpeed = 0.04 * speedMultiplier;
        double horizontalSpeed = Math.cos(pitch) * baseSpeed;
        double motionX = -Math.sin(yaw) * horizontalSpeed;
        double motionZ = Math.cos(yaw) * horizontalSpeed;

        mc.player.setVelocity(
                mc.player.getVelocity().x + motionX,
                mc.player.getVelocity().y,
                mc.player.getVelocity().z + motionZ
        );
    }

    @EventHandler
    @Native
    public void onSwimming(SwimmingEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (shouldUseReallyWorldWater()) {
            processReallyWorldSwimming(e);
        } else if (modeSetting.isSelected("FunTime")) {
            processSwimmingBoost(e);
        }
    }

    @Native
    private boolean shouldUseReallyWorldWater() {
        return modeSetting.isSelected("ReallyWorld") || (speedSync.isValue() && Speed.isAnyReallyWorldSpeedActive());
    }

    @Native
    private void handleReallyWorldTick() {
        if ((!mc.player.isTouchingWater() && !mc.player.isSwimming()) || !MoveUtil.hasPlayerMovement()) {
            return;
        }

        if (mc.player.isSwimming() && mc.player.isOnGround() && mc.options.jumpKey.isPressed()) {
            mc.player.jump();
        }
    }

    @Native
    private void processReallyWorldSwimming(SwimmingEvent e) {
        if (!MoveUtil.hasPlayerMovement()) {
            return;
        }

        double multiplier = getWaterMultiplier();
        e.getVector().x *= multiplier;
        e.getVector().z *= multiplier;

        if (mc.options.jumpKey.isPressed()) {
            e.getVector().y = Math.max(e.getVector().y, verticalSpeed.getValue());
        } else if (mc.options.sneakKey.isPressed()) {
            e.getVector().y = Math.min(e.getVector().y, -verticalSpeed.getValue());
        } else if (!mc.player.isOnGround()) {
            e.getVector().y = MathHelper.clamp(e.getVector().y, -0.02, 0.012);
        }
    }

    @Native
    private double getWaterMultiplier() {
        double multiplier = waterSpeed.getValue();
        if (Speed.isReallyWorld2SpeedActive()) {
            multiplier = Math.min(multiplier, 1.10);
        } else if (Speed.isReallyWorldSpeedActive()) {
            multiplier = Math.min(multiplier, 1.12);
        }
        return multiplier;
    }

    @Native
    private void processSwimmingBoost(SwimmingEvent e) {
        if (mc.options.jumpKey.isPressed()) {
            float pitch = AngleConnection.INSTANCE.getRotation().getPitch();
            float boost = pitch >= 0 ? MathHelper.clamp(pitch / 45, 1, 2) : 0.5F;
            e.getVector().y = 0.1 * boost;
        }

        if (iceBoost.isValue() && isHeadUnderIce()) {
            float speedMultiplier = iceBoostSpeed.getValue();
            e.getVector().x *= speedMultiplier;
            e.getVector().z *= speedMultiplier;
        }
    }

    @Native
    private boolean isHeadUnderIce() {
        if (mc.player == null || mc.world == null) return false;

        BlockPos headPos = mc.player.getBlockPos().up(1);
        BlockPos aboveHeadPos = mc.player.getBlockPos().up(2);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos checkPos = headPos.add(dx, 0, dz);
                BlockPos checkPosAbove = aboveHeadPos.add(dx, 0, dz);

                if (isIceBlock(checkPos) || isIceBlock(checkPosAbove)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isIceBlock(BlockPos pos) {
        var block = mc.world.getBlockState(pos).getBlock();
        return block == Blocks.ICE ||
                block == Blocks.PACKED_ICE ||
                block == Blocks.BLUE_ICE ||
                block == Blocks.FROSTED_ICE;
    }
}

package neverlose.modules.impl.movement;

import com.adl.nativeprotect.Native;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.PacketEvent;
import neverlose.events.impl.TickEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.util.Instance;

public class AutoSprint extends ModuleStructure {
    public static AutoSprint getInstance() {
        return Instance.get(AutoSprint.class);
    }

    private static volatile boolean serverSprintState = false;
    private static volatile long suppressSprintUntil = 0L;
    private static volatile int forcedSprintPackets = 0;

    @Getter
    private final BooleanSetting noReset = new BooleanSetting("Не сбрасывать спринт", "Don't reset sprint for crits")
            .setValue(false);

    public AutoSprint() {
        super("AutoSprint", ModuleCategory.MOVEMENT);
        settings(noReset);
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (!event.isSend() || !(event.getPacket() instanceof ClientCommandC2SPacket packet)) {
            return;
        }

        if (packet.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING) {
            if (forcedSprintPackets > 0) {
                return;
            }
            if (serverSprintState) {
                event.cancel();
            }
        } else if (packet.getMode() == ClientCommandC2SPacket.Mode.STOP_SPRINTING) {
            if (forcedSprintPackets > 0) {
                return;
            }
            if (!serverSprintState) {
                event.cancel();
            }
        }
    }

    public static void trackSentPacket(Packet<?> packet) {
        if (!(packet instanceof ClientCommandC2SPacket command)) {
            return;
        }

        if (command.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING) {
            if (forcedSprintPackets > 0) {
                forcedSprintPackets--;
            }
            serverSprintState = true;
        } else if (command.getMode() == ClientCommandC2SPacket.Mode.STOP_SPRINTING) {
            if (forcedSprintPackets > 0) {
                forcedSprintPackets--;
            }
            serverSprintState = false;
        }
    }

    public static boolean isServerSprinting() {
        return serverSprintState;
    }

    public static void resetServerState() {
        serverSprintState = false;
        suppressSprintUntil = 0L;
        forcedSprintPackets = 0;
    }

    public static void suppressSprintFor(long millis) {
        suppressSprintUntil = Math.max(suppressSprintUntil, System.currentTimeMillis() + millis);
    }

    public static void forceNextSprintPacket() {
        forcedSprintPackets++;
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null)
            return;

        processSprint();
    }


    private void processSprint() {
        boolean horizontal = mc.player.horizontalCollision && !mc.player.collidedSoftly;
        boolean sneaking = mc.player.isSneaking() && !mc.player.isSwimming();
        boolean suppressed = System.currentTimeMillis() < suppressSprintUntil;
        boolean consuming = isConsumingItem();
        boolean canSprint = !horizontal && mc.player.forwardSpeed > 0 && isSafeSprintState();

        if (suppressed || consuming) {
            if (mc.player.isSprinting()) {
                mc.player.setSprinting(false);
            }
            return;
        }

        if (sneaking)
            return;

        if (!canSprint && mc.player.isSprinting() && hasAntiCheatSensitiveState()) {
            mc.player.setSprinting(false);
            return;
        }

        if (canSprint && !mc.player.isSprinting()) {
            mc.player.setSprinting(true);
        }
    }

    private boolean isSafeSprintState() {
        return !hasAntiCheatSensitiveState();
    }

    private boolean hasAntiCheatSensitiveState() {
        return hasBadMovementEffect() || isOnSensitiveSurface();
    }

    private boolean hasBadMovementEffect() {
        return mc.player.hasStatusEffect(StatusEffects.SLOWNESS)
                || mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                || mc.player.hasStatusEffect(StatusEffects.LEVITATION)
                || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);
    }

    private boolean isConsumingItem() {
        if (!mc.player.isUsingItem()) return false;

        var stack = mc.player.getActiveItem();
        if (stack.isEmpty()) return false;

        UseAction useAction = stack.getUseAction();
        return useAction == UseAction.EAT || useAction == UseAction.DRINK;
    }

    private boolean isOnSensitiveSurface() {
        if (mc.world == null) return false;

        BlockPos belowPos = BlockPos.ofFloored(mc.player.getX(), mc.player.getBoundingBox().minY - 0.001, mc.player.getZ());
        BlockState below = mc.world.getBlockState(belowPos);
        BlockState feet = mc.world.getBlockState(mc.player.getBlockPos());

        return isSensitiveSurface(below) || isSensitiveSurface(feet);
    }

    private boolean isSensitiveSurface(BlockState state) {
        Block block = state.getBlock();
        String path = Registries.BLOCK.getId(block).getPath();
        return path.contains("carpet")
                || path.contains("snow")
                || block == Blocks.POWDER_SNOW
                || block == Blocks.HONEY_BLOCK
                || block == Blocks.SOUL_SAND
                || block == Blocks.SLIME_BLOCK
                || block == Blocks.COBWEB;
    }

    @Override
    public void deactivate() {
        resetServerState();
    }
}

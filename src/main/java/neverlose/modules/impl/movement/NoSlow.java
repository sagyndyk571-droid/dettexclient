package neverlose.modules.impl.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import neverlose.events.api.EventHandler;
import neverlose.events.api.types.EventType;
import neverlose.events.impl.TickEvent;
import neverlose.events.impl.UsingItemEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.util.Instance;
import neverlose.util.inventory.script.Script;
import neverlose.util.string.PlayerInteractionHelper;
import neverlose.util.timer.StopWatch;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoSlow extends ModuleStructure {
    public static NoSlow getInstance() {
        return Instance.get(NoSlow.class);
    }

    private final StopWatch notifWatch = new StopWatch();
    private final Script script = new Script();
    private boolean finish;

    public final SelectSetting itemMode = new SelectSetting("Режим предмета", "Выберите режим обхода").value("Grim Old", "ReallyWorld", "SpookyTime", "Funtime");
    public final BooleanSetting speedSync = new BooleanSetting("Синхронизация Speed", "Подстраивает ReallyWorld NoSlow под Speed")
            .setValue(true)
            .visible(() -> itemMode.isSelected("ReallyWorld"));

    public NoSlow() {
        super("NoSlow", "No Slow", ModuleCategory.MOVEMENT);
        settings(itemMode, speedSync);
    }

    private int ticks = 0;
    private int cycleCounter = 0;

    private boolean isOnSnowOrCarpet() {
        if (mc.player == null || mc.world == null) return false;
        BlockPos playerPos = mc.player.getBlockPos();
        var block = mc.world.getBlockState(playerPos).getBlock();
        return block == Blocks.SNOW ||
                block == Blocks.WHITE_CARPET ||
                block == Blocks.ORANGE_CARPET ||
                block == Blocks.MAGENTA_CARPET ||
                block == Blocks.LIGHT_BLUE_CARPET ||
                block == Blocks.YELLOW_CARPET ||
                block == Blocks.LIME_CARPET ||
                block == Blocks.PINK_CARPET ||
                block == Blocks.GRAY_CARPET ||
                block == Blocks.LIGHT_GRAY_CARPET ||
                block == Blocks.CYAN_CARPET ||
                block == Blocks.PURPLE_CARPET ||
                block == Blocks.BLUE_CARPET ||
                block == Blocks.BROWN_CARPET ||
                block == Blocks.GREEN_CARPET ||
                block == Blocks.RED_CARPET ||
                block == Blocks.BLACK_CARPET;
    }

    @EventHandler
    public void onUpdate(TickEvent event) {
        if (mc.player == null) return;

        if (itemMode.isSelected("ReallyWorld") || itemMode.isSelected("SpookyTime")) {
            if (!mc.player.isUsingRiptide()) {
                if (mc.player.isUsingItem()) {
                    ticks++;
                } else {
                    ticks = 0;
                    cycleCounter = 0;
                }
            }
        } else {
            if (mc.player.getActiveHand() == Hand.MAIN_HAND || mc.player.getActiveHand() == Hand.OFF_HAND) {
                ticks++;
            } else {
                ticks = 0;
            }
        }
    }

    @EventHandler
    public void onUsingItem(UsingItemEvent e) {
        if (mc.player == null) return;

        Hand first = mc.player.getActiveHand();
        Hand second = first.equals(Hand.MAIN_HAND) ? Hand.OFF_HAND : Hand.MAIN_HAND;

        switch (e.getType()) {
            case EventType.ON -> {
                handleItemUse(e, first, second);
            }
            case EventType.POST -> {
                while (!script.isFinished()) script.update();
            }
        }
    }

    private void handleItemUse(UsingItemEvent e, Hand first, Hand second) {
        switch (itemMode.getSelected()) {
            case "Grim Old" -> {
                if (mc.player.getOffHandStack().getUseAction().equals(UseAction.NONE) || mc.player.getMainHandStack().getUseAction().equals(UseAction.NONE)) {
                    PlayerInteractionHelper.interactItem(first);
                    PlayerInteractionHelper.interactItem(second);
                    e.cancel();
                }
            }
            case "ReallyWorld" -> {
                int[] thresholds = getReallyWorldThresholds();
                int threshold = thresholds[cycleCounter % thresholds.length];

                if (ticks >= threshold) {
                    e.cancel();
                    ticks = 0;
                    cycleCounter++;
                }
            }
            case "SpookyTime" -> {
                int[] thresholds = new int[]{2, 2, 2};
                int threshold = thresholds[cycleCounter % 2];
                if (ticks >= threshold) {
                    e.cancel();
                    ticks = 0;
                    cycleCounter++;
                }
            }
            case "Funtime" -> {
                if (ticks > 0F && mc.player.getItemUseTime() > 1F) {
                    boolean mainHandCrossbow = mc.player.getMainHandStack().getItem() instanceof CrossbowItem;
                    boolean offHandCrossbow = mc.player.getOffHandStack().getItem() instanceof CrossbowItem;

                    if (mainHandCrossbow || offHandCrossbow) {
                        if (ticks > 0F && mc.player.getItemUseTime() > 1) {
                            e.cancel();
                            ticks = 0;
                        }
                    } else if (mc.player.isOnGround() && isOnSnowOrCarpet()) {
                        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                                PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
                                mc.player.getBlockPos().up(),
                                Direction.DOWN
                        ));
                        mc.player.setVelocity(
                                mc.player.getVelocity().x,
                                mc.player.getVelocity().y,
                                mc.player.getVelocity().z
                        );
                        e.cancel();
                        ticks = 0;
                    }
                }
            }
        }
    }

    private int[] getReallyWorldThresholds() {
        if (speedSync.isValue() && Speed.isReallyWorld2SpeedActive()) {
            return mc.player.isJumping() ? new int[]{2, 2, 3} : new int[]{2, 3, 2};
        }

        if (speedSync.isValue() && Speed.isReallyWorldSpeedActive()) {
            return mc.player.isJumping() ? new int[]{2, 2, 2} : new int[]{2, 2, 3};
        }

        return mc.player.isJumping() ? new int[]{2, 2, 2} : new int[]{2, 3, 3};
    }
}

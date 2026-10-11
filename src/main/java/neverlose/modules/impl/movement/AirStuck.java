package neverlose.modules.impl.movement;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import neverlose.events.api.EventHandler;
import neverlose.events.api.types.EventType;
import neverlose.events.impl.InputEvent;
import neverlose.events.impl.PacketEvent;
import neverlose.events.impl.RotationUpdateEvent;
import neverlose.events.impl.TickEvent;
import neverlose.modules.impl.combat.aura.Angle;
import neverlose.modules.impl.combat.aura.AngleConnection;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.util.Instance;
import neverlose.util.inventory.InventoryUtils;
import neverlose.util.string.PlayerInteractionHelper;

/**
 *  © 2026 Copyright neverloseclient
 *        All Rights Reserved ®
 */

public class AirStuck extends ModuleStructure {

    public static AirStuck getInstance() {
        return Instance.get(AirStuck.class);
    }

    private boolean savedFlying;
    private float   savedYaw;
    private float   savedPitch;
    private float   savedYawOff;
    private boolean restoreElytraOnDisable;

    private double frozenX;
    private double frozenY;
    private double frozenZ;
    private long lastResync;
    private int keepAliveTicks;

    public AirStuck() {
        super("AirStuck", ModuleCategory.MOVEMENT);
    }

    @Override
    public void activate() {
        if (mc.player == null) return;

        savedFlying = mc.player.getAbilities().flying;
        savedYaw    = mc.player.getYaw();
        savedPitch  = mc.player.getPitch();
        savedYawOff = mc.player.bodyYaw;

        frozenX = mc.player.getX();
        frozenY = mc.player.getY();
        frozenZ = mc.player.getZ();

        restoreElytraOnDisable = swapElytraToChestplate();
    }

    @Override
    public void deactivate() {
        if (mc.player == null) return;

        mc.player.getAbilities().flying = savedFlying;
        mc.player.noClip = false;
        if (restoreElytraOnDisable) {
            swapToElytra();
            restoreElytraOnDisable = false;
        }
    }

    @EventHandler
    public void onInput(InputEvent event) {
        event.inputNone();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null) return;

        mc.player.noClip = true;
        mc.player.setOnGround(false);
        mc.player.setVelocity(0, 0, 0);
        mc.player.getAbilities().flying = true;

        mc.player.headYaw = savedYaw;
        mc.player.bodyYaw = savedYawOff;

        if (mc.player.isSprinting()) mc.player.setSprinting(false);

        if (++keepAliveTicks % 3 == 0) {
            syncFrozenPosition();
        }
    }

    @EventHandler
    public void onRotation(RotationUpdateEvent event) {
        if (mc.player == null) return;
        mc.player.headYaw = savedYaw;
        mc.player.bodyYaw = savedYawOff;

        if (event.getType() == EventType.POST) {
            syncFrozenPosition();
        }
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (mc.player == null) return;

        if (!event.isSend()) {
            if (event.getPacket() instanceof GameJoinS2CPacket) {
                setState(false);
                return;
            }

            if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
                event.cancel();
                long now = System.currentTimeMillis();
                if (now - lastResync > 300L) {
                    lastResync = now;
                    syncFrozenPosition();
                }
            }
            return;
        }

        if (event.getPacket() instanceof PlayerMoveC2SPacket) {
            event.cancel();
        }
    }

    private void syncFrozenPosition() {
        if (mc.player == null) return;

        Angle rotation = AngleConnection.INSTANCE.getRotation();
        float yaw = rotation != null ? rotation.getYaw() : mc.player.getYaw();
        float pitch = rotation != null ? rotation.getPitch() : mc.player.getPitch();

        PlayerInteractionHelper.sendPacketWithOutEvent(
                new PlayerMoveC2SPacket.Full(
                        frozenX,
                        frozenY,
                        frozenZ,
                        yaw,
                        pitch,
                        false,
                        false
                )
        );
    }

    private boolean swapElytraToChestplate() {
        if (mc.player == null || !isWearingElytra()) {
            return false;
        }

        int chestSlot = findChestplateSlot();
        if (chestSlot == -1) {
            return false;
        }

        InventoryUtils.swap(InventoryUtils.wrapSlot(chestSlot), 6);
        return true;
    }

    private void swapToElytra() {
        if (mc.player == null || isWearingElytra()) {
            return;
        }

        int elytraSlot = findElytraSlot();
        if (elytraSlot != -1) {
            InventoryUtils.swap(InventoryUtils.wrapSlot(elytraSlot), 6);
        }
    }

    private boolean isWearingElytra() {
        ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        return !chest.isEmpty() && chest.isOf(Items.ELYTRA);
    }

    private int findElytraSlot() {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.ELYTRA)) {
                return i;
            }
        }
        return -1;
    }

    private int findChestplateSlot() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            EquippableComponent component = stack.get(DataComponentTypes.EQUIPPABLE);
            if (component != null && component.slot() == EquipmentSlot.CHEST && !stack.isOf(Items.ELYTRA)) {
                return i;
            }
        }
        return -1;
    }
}
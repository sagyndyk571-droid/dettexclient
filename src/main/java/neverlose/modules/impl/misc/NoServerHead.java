package neverlose.modules.impl.misc;

import com.mojang.datafixers.util.Pair;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.PacketEvent;
import neverlose.events.impl.TickEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.util.timer.TimerUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoServerHead extends ModuleStructure {
    static final int PLAYER_SCREEN_HEAD_SLOT = 5;
    static final int PLAYER_INVENTORY_HEAD_SLOT = 39;
    static final UUID LOCAL_REALLYWORLD_PACK_ID = UUID.nameUUIDFromBytes("neverlose:reallyworld_no_heads_v3".getBytes(StandardCharsets.UTF_8));
    static final String SANITIZED_PACK_NAME = "reallyworld_no_heads_v3.zip";
    static final String EMBEDDED_PACK_RESOURCE = "/assets/neverlose/resourcepacks/" + SANITIZED_PACK_NAME;

    final TimerUtil resourcePackTimer = TimerUtil.create();
    ResourcePackAction resourcePackAction = ResourcePackAction.WAIT;
    UUID pendingResourcePackId;
    boolean localPackApplied;

    BooleanSetting blockResourcePack = new BooleanSetting("ReallyWorld RP", "Load local ReallyWorld RP without player-head body models")
            .setValue(true);

    BooleanSetting blockSkulls = new BooleanSetting("Heads", "Block server skull/player head cosmetics")
            .setValue(true);

    BooleanSetting blockCustomModels = new BooleanSetting("Custom models", "Block custom resource-pack head models")
            .setValue(true);

    BooleanSetting inventorySync = new BooleanSetting("Inventory sync", "Block inventory head-slot replacements too")
            .setValue(true);

    public NoServerHead() {
        super("NoServerHead", "Blocks forced server resource-pack head cosmetics", ModuleCategory.MISC);
        settings(blockResourcePack, blockSkulls, blockCustomModels, inventorySync);
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (event.getType() != PacketEvent.Type.RECEIVE) {
            return;
        }

        if (event.getPacket() instanceof ResourcePackSendS2CPacket packet && blockResourcePack.isValue()) {
            pendingResourcePackId = packet.id();
            resourcePackAction = ResourcePackAction.ACCEPT;
            resourcePackTimer.resetCounter();
            replaceServerPackWithSanitized();
            event.cancel();
            return;
        }

        if (mc.player == null) {
            return;
        }

        if (event.getPacket() instanceof EntityEquipmentUpdateS2CPacket packet) {
            handleEquipmentPacket(event, packet);
            return;
        }

        if (!inventorySync.isValue()) {
            return;
        }

        if (event.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket packet) {
            if (packet.getSyncId() == 0 && isPlayerHeadSlot(packet.getSlot()) && shouldBlockHead(packet.getStack())) {
                event.cancel();
            }
            return;
        }

        if (event.getPacket() instanceof InventoryS2CPacket packet) {
            if (packet.syncId() == 0 && hasBlockedHeadSlot(packet)) {
                event.cancel();
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (!blockResourcePack.isValue()) {
            resourcePackAction = ResourcePackAction.WAIT;
            pendingResourcePackId = null;
            return;
        }

        ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
        if (networkHandler == null || pendingResourcePackId == null) {
            return;
        }

        if (resourcePackAction == ResourcePackAction.ACCEPT) {
            networkHandler.sendPacket(new ResourcePackStatusC2SPacket(
                    pendingResourcePackId,
                    ResourcePackStatusC2SPacket.Status.ACCEPTED));
            resourcePackAction = ResourcePackAction.SEND;
            resourcePackTimer.resetCounter();
        } else if (resourcePackAction == ResourcePackAction.SEND && resourcePackTimer.isReached(300L)) {
            networkHandler.sendPacket(new ResourcePackStatusC2SPacket(
                    pendingResourcePackId,
                    ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
            resourcePackAction = ResourcePackAction.WAIT;
            pendingResourcePackId = null;
        }
    }

    @Override
    public void activate() {
        replaceServerPackWithSanitized();
    }

    @Override
    public void deactivate() {
        resourcePackAction = ResourcePackAction.WAIT;
        pendingResourcePackId = null;
        removeLocalPack();
    }

    private void handleEquipmentPacket(PacketEvent event, EntityEquipmentUpdateS2CPacket packet) {
        for (Pair<EquipmentSlot, ItemStack> entry : packet.getEquipmentList()) {
            if (entry.getFirst() == EquipmentSlot.HEAD && shouldBlockHead(entry.getSecond())) {
                event.cancel();
                return;
            }
        }
    }

    private boolean shouldBlockHead(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        if (blockSkulls.isValue() && isSkull(stack)) {
            return true;
        }

        return blockCustomModels.isValue()
                && isHeadEquippable(stack)
                && hasResourcePackModel(stack);
    }

    private boolean isSkull(ItemStack stack) {
        return stack.isOf(Items.PLAYER_HEAD)
                || stack.isOf(Items.SKELETON_SKULL)
                || stack.isOf(Items.WITHER_SKELETON_SKULL)
                || stack.isOf(Items.ZOMBIE_HEAD)
                || stack.isOf(Items.CREEPER_HEAD)
                || stack.isOf(Items.DRAGON_HEAD)
                || stack.isOf(Items.PIGLIN_HEAD);
    }

    private boolean isHeadEquippable(ItemStack stack) {
        var component = stack.get(DataComponentTypes.EQUIPPABLE);
        return component != null && component.slot() == EquipmentSlot.HEAD;
    }

    private boolean hasResourcePackModel(ItemStack stack) {
        return stack.isOf(Items.CARVED_PUMPKIN)
                || stack.contains(DataComponentTypes.CUSTOM_MODEL_DATA)
                || stack.contains(DataComponentTypes.ITEM_MODEL)
                || stack.contains(DataComponentTypes.CUSTOM_DATA)
                || stack.contains(DataComponentTypes.PROFILE);
    }

    private boolean isPlayerHeadSlot(int slot) {
        return slot == PLAYER_SCREEN_HEAD_SLOT || slot == PLAYER_INVENTORY_HEAD_SLOT;
    }

    private boolean hasBlockedHeadSlot(InventoryS2CPacket packet) {
        return hasBlockedHeadSlot(packet, PLAYER_SCREEN_HEAD_SLOT)
                || hasBlockedHeadSlot(packet, PLAYER_INVENTORY_HEAD_SLOT);
    }

    private boolean hasBlockedHeadSlot(InventoryS2CPacket packet, int slot) {
        return packet.contents().size() > slot && shouldBlockHead(packet.contents().get(slot));
    }

    private void replaceServerPackWithSanitized() {
        if (!blockResourcePack.isValue()) {
            return;
        }

        try {
            if (mc.getServerResourcePackProvider() != null) {
                Path pack = ensureSanitizedPack();
                if (pack == null) {
                    return;
                }
                mc.getServerResourcePackProvider().removeAll();
                installSanitizedPack(pack);
            }
        } catch (Exception ignored) {
        }
    }

    private void installSanitizedPack(Path pack) {
        if (mc.getServerResourcePackProvider() == null) {
            return;
        }

        mc.getServerResourcePackProvider().remove(LOCAL_REALLYWORLD_PACK_ID);
        mc.getServerResourcePackProvider().addResourcePack(LOCAL_REALLYWORLD_PACK_ID, pack);
        mc.getServerResourcePackProvider().acceptAll();
        localPackApplied = true;
        reloadClientResources();
    }

    private void removeLocalPack() {
        if (!localPackApplied || mc.getServerResourcePackProvider() == null) {
            return;
        }

        try {
            mc.getServerResourcePackProvider().remove(LOCAL_REALLYWORLD_PACK_ID);
        } catch (Exception ignored) {
        }
        localPackApplied = false;
        reloadClientResources();
    }

    private void reloadClientResources() {
        try {
            mc.execute(() -> {
                try {
                    mc.reloadResources();
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    private Path ensureSanitizedPack() throws IOException {
        Path target = mc.runDirectory.toPath()
                .resolve("Neverlose")
                .resolve("cache")
                .resolve(SANITIZED_PACK_NAME);

        Files.createDirectories(target.getParent());
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        try (InputStream stream = NoServerHead.class.getResourceAsStream(EMBEDDED_PACK_RESOURCE)) {
            if (stream == null) {
                return null;
            }
            Files.copy(stream, temp, StandardCopyOption.REPLACE_EXISTING);
        }

        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    enum ResourcePackAction {
        ACCEPT, SEND, WAIT
    }
}

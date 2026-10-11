package neverlose.modules.impl.player;

import lombok.Getter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.TickEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.Instance;
import neverlose.util.inventory.InventoryUtils;

@Getter
public class ElytraFlight extends ModuleStructure {

    public static ElytraFlight getInstance() {
        return Instance.get(ElytraFlight.class);
    }

    private final SliderSettings fireworkDelay = new SliderSettings("Задержка фейерверка", "Задержка между пусками фейерверков (мс)")
            .range(200f, 15000f)
            .setValue(800f);

    private final BooleanSetting hotbarOnly = new BooleanSetting("Только хотбар", "Искать элитры только в хотбаре")
            .setValue(false);

    private final BooleanSetting stopOnEat = new BooleanSetting("Стопать при еде", "Не пускать фейерверки при еде")
            .setValue(true);

    private final BooleanSetting useTarget = new BooleanSetting("Таргет на игроков", "Преследовать ближайшего игрока")
            .setValue(true);

    private final SliderSettings targetDistance = new SliderSettings("Дистанция таргета", "Дистанция поиска и торможения")
            .range(5f, 100f)
            .setValue(64f);

    private long lastFireworkTime = 0;
    private boolean firstFireworkFired = false;
    private int jumpTicks = 0;
    private PlayerEntity currentTarget = null;

    public ElytraFlight() {
        super("ElytraFlight", "Elytra Flight", ModuleCategory.MOVEMENT);
        settings(fireworkDelay, hotbarOnly, stopOnEat, useTarget, targetDistance);
    }

    private int findChestArmorSlot(boolean findElytra) {
        if (mc.player == null) return -1;
        int limit = hotbarOnly.isValue() ? 9 : 36;
        for (int i = 0; i < limit; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            EquippableComponent component = stack.get(DataComponentTypes.EQUIPPABLE);
            if (component == null || component.slot() != EquipmentSlot.CHEST) continue;
            if (findElytra) {
                if (stack.isOf(Items.ELYTRA)) return i;
            } else {
                if (!stack.isOf(Items.ELYTRA)) return i;
            }
        }
        return -1;
    }

    private boolean isElytraCurrentlyEquipped() {
        if (mc.player == null) return false;
        ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        return !chest.isEmpty() && chest.isOf(Items.ELYTRA);
    }

    private void performSafeSwap(int invSlot) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (invSlot >= 0 && invSlot <= 8) {
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, 6, invSlot, SlotActionType.SWAP, mc.player);
        } else {
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, invSlot < 9 ? invSlot + 36 : invSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, invSlot < 9 ? invSlot + 36 : invSlot, 0, SlotActionType.PICKUP, mc.player);
        }
    }

    @Override
    public void activate() {
        super.activate();
        if (mc.player != null) {
        }
        if (mc.player == null) return;
        lastFireworkTime = 0;
        firstFireworkFired = false;
        jumpTicks = 0;
        currentTarget = null;
        if (isElytraCurrentlyEquipped()) return;
        int elytraSlot = findChestArmorSlot(true);
        if (elytraSlot == -1) { setState(false); return; }
        performSafeSwap(elytraSlot);
    }

    @Override
    public void deactivate() {
        if (mc.player == null) return;
        currentTarget = null;
        if (!isElytraCurrentlyEquipped()) return;
        int chestSlot = findChestArmorSlot(false);
        if (chestSlot == -1) return;
        performSafeSwap(chestSlot);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isGliding()) {
            handleTargeting();

            if (stopOnEat.isValue() && mc.player.isUsingItem()) return;
            long now = System.currentTimeMillis();
            if (!firstFireworkFired) {
                InventoryUtils.silentUseItem(Items.FIREWORK_ROCKET);
                lastFireworkTime = now;
                firstFireworkFired = true;
            } else if (now - lastFireworkTime >= (long) fireworkDelay.getValue()) {
                InventoryUtils.silentUseItem(Items.FIREWORK_ROCKET);
                lastFireworkTime = now;
            }
            return;
        }

        firstFireworkFired = false;
        currentTarget = null;
        if (isElytraCurrentlyEquipped()) {
            if (mc.player.isOnGround()) {
                mc.player.jump();
                jumpTicks = 0;
            } else {
                jumpTicks++;
                if (jumpTicks == 3 && mc.getNetworkHandler() != null) {
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                }
            }
        }
    }

    private void handleTargeting() {
        if (!useTarget.isValue()) {
            currentTarget = null;
            return;
        }

        PlayerEntity bestTarget = null;
        double minDistance = targetDistance.getValue();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) continue;
            double dist = mc.player.distanceTo(player);
            if (dist < minDistance) {
                minDistance = dist;
                bestTarget = player;
            }
        }

        currentTarget = bestTarget;

        if (currentTarget == null) return;

        Vec3d targetPos = currentTarget.getEntityPos().add(currentTarget.getVelocity().multiply(5));
        if (mc.player.distanceTo(currentTarget) < 4) {
            targetPos = currentTarget.getEntityPos().add(currentTarget.getVelocity().multiply(2));
        }

        double diffX = targetPos.x - mc.player.getX();
        double diffY = (targetPos.y + currentTarget.getStandingEyeHeight() * 0.5) - mc.player.getY();
        double diffZ = targetPos.z - mc.player.getZ();

        float yaw = (float) (MathHelper.atan2(diffZ, diffX) * 57.2957763671875) - 90.0f;
        float pitch = (float) (-(MathHelper.atan2(diffY, MathHelper.sqrt((float) (diffX * diffX + diffZ * diffZ))) * 57.2957763671875));

        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }
}
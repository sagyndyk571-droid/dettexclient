package neverlose.modules.impl.combat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.KeyEvent;
import neverlose.events.impl.PacketEvent;
import neverlose.events.impl.TickEvent;
import neverlose.modules.impl.combat.aura.AngleConfig;
import neverlose.modules.impl.combat.aura.AngleConnection;
import neverlose.modules.impl.combat.aura.MathAngle;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BindSetting;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.screens.clickgui.impl.settingsrender.BindComponent;
import neverlose.util.inventory.InventoryUtils;
import neverlose.util.math.TaskPriority;
import neverlose.util.string.PlayerInteractionHelper;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class AutoExplosion extends ModuleStructure {
    static final double INTERACT_RANGE = 4.5D;
    static final int TARGET_TIMEOUT_TICKS = 12;
    static final int CRYSTAL_TIMEOUT_TICKS = 14;

    final SelectSetting mode = new SelectSetting("Режим взрыва", "Способ запуска цепочки")
            .value("Авто", "По бинду")
            .selected("Авто");

    final BindSetting bind = new BindSetting("Бинд", "Поставить обсидиан и взорвать кристалл")
            .visible(() -> mode.isSelected("По бинду"));

    final BooleanSetting explosionOnRightClick = new BooleanSetting(
            "Взрыв по ПКМ",
            "Ставить и взрывать кристалл при нажатии на обсидиан или бедрок"
    ).setValue(true);

    final BooleanSetting keepCrystal = new BooleanSetting(
            "Оставлять кристалл",
            "Не возвращать выбранный слот после установки кристалла"
    ).setValue(false);

    BlockPos targetPos;
    int targetSlot = -1;
    int oldSlot = -1;
    int targetTicks;
    int crystalTicks;
    boolean needSync;
    boolean blocked;
    boolean internalInteract;
    Box crystalArea;

    public AutoExplosion() {
        super("AutoExplosion", "Автоматически ставит и взрывает кристалл", ModuleCategory.COMBAT);
        settings(mode, bind, explosionOnRightClick, keepCrystal);
    }

    @Override
    public void activate() {
        reset();
    }

    @Override
    public void deactivate() {
        reset();
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) {
            return;
        }
        if (!mode.isSelected("По бинду") || !matchesBind(event)) {
            return;
        }

        placeObsidianByCrosshair();
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (!event.isSend() || event.isCancelled() || internalInteract || mc.player == null || mc.world == null) {
            return;
        }
        if (!(event.getPacket() instanceof PlayerInteractBlockC2SPacket packet)) {
            return;
        }

        BlockHitResult hit = packet.getBlockHitResult();
        BlockPos clickedPos = hit.getBlockPos();
        BlockPos placePos = clickedPos.offset(hit.getSide());

        if (mode.isSelected("Авто")
                && isHoldingObsidian()
                && isInRange(placePos)
                && !isCrystalCoolingDown()) {
            int crystalSlot = findCrystalHotbarSlot();
            if (crystalSlot != -1) {
                startCrystalChain(placePos, crystalSlot);
            }
        }

        if (explosionOnRightClick.isValue() && shouldPlaceByRightClick(clickedPos)) {
            if (placeCrystalFromOffhand(hit, clickedPos)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            reset();
            return;
        }

        if (needSync) {
            needSync = false;
            restoreSelectedSlot();
        }

        processTargetBlock();
        processCrystalArea();
    }

    private void processTargetBlock() {
        if (targetPos == null) {
            return;
        }

        if (--targetTicks <= 0) {
            clearTarget();
            return;
        }

        Block block = mc.world.getBlockState(targetPos).getBlock();
        if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) {
            return;
        }

        if (blocked) {
            blocked = false;
            return;
        }

        tryPlaceCrystalFast(targetPos);
    }

    private void tryPlaceCrystalFast(BlockPos pos) {
        if (targetSlot < 0 || targetSlot > 8 || !canPlaceCrystal(pos)) {
            clearTarget();
            return;
        }

        rotateTo(Vec3d.ofCenter(pos).add(0.0D, 0.5D, 0.0D));

        oldSlot = InventoryUtils.currentSlot();
        InventoryUtils.selectSlot(targetSlot);
        InventoryUtils.selectSlotSilent(targetSlot);

        BlockHitResult result = new BlockHitResult(
                Vec3d.ofCenter(pos).add(0.0D, 0.5D, 0.0D),
                Direction.UP,
                pos,
                false
        );
        sendInteract(Hand.MAIN_HAND, result);
        mc.player.swingHand(Hand.MAIN_HAND);

        needSync = !keepCrystal.isValue();
        crystalArea = boxFromBlock(pos.up()).expand(0.2D);
        crystalTicks = CRYSTAL_TIMEOUT_TICKS;
        clearTarget();
    }

    private void processCrystalArea() {
        if (crystalArea == null) {
            return;
        }

        if (--crystalTicks <= 0) {
            crystalArea = null;
            return;
        }

        for (Entity entity : mc.world.getOtherEntities(null, crystalArea)) {
            if (!(entity instanceof EndCrystalEntity crystal) || !crystal.isAlive()) {
                continue;
            }

            rotateTo(crystal.getBoundingBox().getCenter());
            mc.interactionManager.attackEntity(mc.player, crystal);
            mc.player.swingHand(Hand.MAIN_HAND);
            crystalArea = null;
            return;
        }
    }

    private boolean shouldPlaceByRightClick(BlockPos clickedPos) {
        if (isCrystalCoolingDown() || isHoldingBlockForPlace()) {
            return false;
        }

        Block block = mc.world.getBlockState(clickedPos).getBlock();
        if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) {
            return false;
        }

        return isInRange(clickedPos) && mc.world.getBlockState(clickedPos.up()).isAir();
    }

    private boolean placeCrystalFromOffhand(BlockHitResult hit, BlockPos clickedPos) {
        Slot crystalSlot = InventoryUtils.findSlotAnywhere(Items.END_CRYSTAL);
        boolean alreadyInOffhand = mc.player.getOffHandStack().isOf(Items.END_CRYSTAL);
        if (!alreadyInOffhand && crystalSlot == null) {
            return false;
        }

        if (!alreadyInOffhand) {
            InventoryUtils.swapToOffhand(crystalSlot);
        }

        sendInteract(Hand.OFF_HAND, hit);
        mc.player.swingHand(Hand.OFF_HAND);
        crystalArea = boxFromBlock(clickedPos.up()).expand(0.2D);
        crystalTicks = CRYSTAL_TIMEOUT_TICKS;

        if (!alreadyInOffhand) {
            InventoryUtils.swapToOffhand(crystalSlot);
            InventoryUtils.closeScreen();
        }
        return true;
    }

    private void placeObsidianByCrosshair() {
        Slot obsidianSlot = InventoryUtils.findSlotAnywhere(Items.OBSIDIAN);
        int crystalSlot = findCrystalHotbarSlot();
        if (obsidianSlot == null || crystalSlot == -1) {
            return;
        }
        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos placePos = hit.getBlockPos().offset(hit.getSide());
        if (!isInRange(placePos) || !mc.world.getBlockState(placePos).isAir()) {
            return;
        }

        InventoryUtils.swapToOffhand(obsidianSlot);
        sendInteract(Hand.OFF_HAND, hit);
        mc.player.swingHand(Hand.OFF_HAND);
        InventoryUtils.swapToOffhand(obsidianSlot);
        InventoryUtils.closeScreen();

        startCrystalChain(placePos, crystalSlot);
    }

    private void startCrystalChain(BlockPos pos, int crystalSlot) {
        targetPos = pos.toImmutable();
        targetSlot = crystalSlot;
        targetTicks = TARGET_TIMEOUT_TICKS;
        blocked = true;
    }

    private void sendInteract(Hand hand, BlockHitResult hitResult) {
        internalInteract = true;
        try {
            PlayerInteractionHelper.sendSequencedPacket(sequence ->
                    new PlayerInteractBlockC2SPacket(hand, hitResult, sequence));
        } finally {
            internalInteract = false;
        }
    }

    private void rotateTo(Vec3d vec) {
        AngleConnection.INSTANCE.rotateTo(
                MathAngle.calculateAngle(vec),
                2,
                AngleConfig.DEFAULT,
                TaskPriority.HIGH_IMPORTANCE_2,
                this
        );
    }

    private boolean canPlaceCrystal(BlockPos pos) {
        Block base = mc.world.getBlockState(pos).getBlock();
        if (base != Blocks.OBSIDIAN && base != Blocks.BEDROCK) {
            return false;
        }

        BlockPos up1 = pos.up();
        BlockPos up2 = pos.up(2);
        if (!mc.world.getBlockState(up1).isAir() || !mc.world.getBlockState(up2).isAir()) {
            return false;
        }

        Box box = new Box(
                up1.getX(), up1.getY(), up1.getZ(),
                up1.getX() + 1.0D, up1.getY() + 2.0D, up1.getZ() + 1.0D
        );
        for (Entity entity : mc.world.getOtherEntities(null, box)) {
            if (!(entity instanceof EndCrystalEntity)) {
                return false;
            }
        }
        return true;
    }

    private int findCrystalHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.END_CRYSTAL)) {
                return i;
            }
        }
        return -1;
    }

    private boolean matchesBind(KeyEvent event) {
        if (event.action() != GLFW.GLFW_PRESS) {
            return false;
        }

        int key = bind.getKey();
        if (key == GLFW.GLFW_KEY_UNKNOWN || key == -1) {
            return false;
        }

        if (bind.getType() == 2) {
            return key == BindComponent.MIDDLE_MOUSE_BIND
                    && event.type() == InputUtil.Type.MOUSE
                    && event.key() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
        }
        if (bind.getType() == 0) {
            return event.type() == InputUtil.Type.MOUSE && event.key() == key;
        }
        return event.type() == InputUtil.Type.KEYSYM && event.key() == key;
    }

    private boolean isHoldingObsidian() {
        return mc.player.getMainHandStack().isOf(Items.OBSIDIAN)
                || mc.player.getOffHandStack().isOf(Items.OBSIDIAN);
    }

    private boolean isHoldingBlockForPlace() {
        Item main = mc.player.getMainHandStack().getItem();
        Item off = mc.player.getOffHandStack().getItem();
        return main instanceof BlockItem && main != Items.PLAYER_HEAD
                || off instanceof BlockItem && off != Items.PLAYER_HEAD;
    }

    private boolean isCrystalCoolingDown() {
        return mc.player.getItemCooldownManager().isCoolingDown(Items.END_CRYSTAL.getDefaultStack());
    }

    private boolean isInRange(BlockPos pos) {
        return mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos)) <= INTERACT_RANGE;
    }

    private Box boxFromBlock(BlockPos pos) {
        return new Box(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D
        );
    }

    private void restoreSelectedSlot() {
        if (oldSlot == -1 || mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }

        InventoryUtils.selectSlotSilent(oldSlot);
        InventoryUtils.selectSlot(oldSlot);
        oldSlot = -1;
    }

    private void clearTarget() {
        targetPos = null;
        targetSlot = -1;
        targetTicks = 0;
        blocked = false;
    }

    private void reset() {
        restoreSelectedSlot();
        clearTarget();
        needSync = false;
        internalInteract = false;
        crystalArea = null;
        crystalTicks = 0;
    }
}

package neverlose.modules.impl.combat.aura.attack;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import neverlose.IMinecraft;
import neverlose.events.api.types.EventType;
import neverlose.events.impl.PacketEvent;
import neverlose.events.impl.UsingItemEvent;
import neverlose.modules.impl.combat.Aura;
import neverlose.modules.impl.combat.PacketCriticals;
import neverlose.modules.impl.combat.TriggerBot;
import neverlose.modules.impl.combat.aura.AngleConnection;
import neverlose.modules.impl.combat.aura.target.RaycastAngle;
import neverlose.modules.impl.movement.AutoSprint;
import neverlose.modules.impl.movement.ElytraTarget;
import neverlose.modules.impl.player.NoDelay;
import neverlose.modules.impl.render.NotificationRuntime;
import neverlose.util.network.Network;
import neverlose.util.player.PlayerSimulation;
import neverlose.util.string.PlayerInteractionHelper;
import neverlose.util.timer.StopWatch;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StrikeManager implements IMinecraft {
    private final Pressing clickScheduler = new Pressing();
    private final StopWatch attackTimer = new StopWatch();
    private final StopWatch shieldWatch = new StopWatch();
    private final StopWatch matrixWallWatch = new StopWatch();
    private final StopWatch sprintResetWatch = new StopWatch();

    private int count = 0;
    private int ticksOnBlock = 0;
    private int matrixWallTargetId = -1;
    private int sprintPreparedTargetId = -1;
    private long sprintPreparedUntil = 0L;
    private boolean sprintInputPending;

    void tick() {
        if (mc.player != null && mc.player.isOnGround()) {
            ticksOnBlock++;
        } else {
            ticksOnBlock = 0;
        }
    }

    void onUsingItem(UsingItemEvent e) {
        if (e.getType() == EventType.START && !shieldWatch.finished(50)) {
            e.cancel();
        }
    }

    void onPacket(PacketEvent e) {
        Packet<?> packet = e.getPacket();
        if (e.isSend()) {
                        if (packet instanceof ClientCommandC2SPacket command) {
                String modeName = command.getMode().name();
                if (modeName.contains("SPRINT") || modeName.contains("SHIFT")) {
                    sprintInputPending = true;
                }
            } else if (packet instanceof PlayerMoveC2SPacket) {
                sprintInputPending = false;
            }
        }
        if (packet instanceof HandSwingC2SPacket || packet instanceof UpdateSelectedSlotC2SPacket) {
            clickScheduler.recalculate();
        }
    }

    public void resetPendingState() {
        sprintPreparedUntil = 0L;
        sprintPreparedTargetId = -1;
        sprintInputPending = false;
    }

    private boolean hasAnyMovementInput() {
        if (mc.player == null)
            return false;
        return mc.player.input.playerInput.forward() ||
                mc.player.input.playerInput.backward() ||
                mc.player.input.playerInput.left() ||
                mc.player.input.playerInput.right();
    }

    private boolean isHoldingMace() {
        return clickScheduler.isHoldingMace();
    }

    private boolean isPlayerEating() {
        if (mc.player == null)
            return false;
        if (!mc.player.isUsingItem())
            return false;
        var activeItem = mc.player.getActiveItem();
        if (activeItem.isEmpty())
            return false;
        var useAction = activeItem.getUseAction();
        return useAction == UseAction.EAT || useAction == UseAction.DRINK;
    }

    private boolean shouldWaitForEating() {
        Aura aura = Aura.getInstance();
        return aura.options.isSelected("Не бить если ешь") && isPlayerEating();
    }

    private boolean isInWater() {
        return mc.player != null
                && (mc.player.isTouchingWater() || mc.player.isSubmergedInWater() || mc.player.isSwimming());
    }

    private boolean hasLowCeiling() {
        if (mc.player == null || mc.world == null)
            return false;

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos above1 = playerPos.up(2);
        BlockPos above2 = playerPos.up(3);

        BlockState state1 = mc.world.getBlockState(above1);
        BlockState state2 = mc.world.getBlockState(above2);

        boolean blocked1 = !state1.isAir() && !state1.getCollisionShape(mc.world, above1).isEmpty();
        boolean blocked2 = !state2.isAir() && !state2.getCollisionShape(mc.world, above2).isEmpty();

        return blocked1 || blocked2;
    }

    private boolean hasLowCeilingForJumpCrit() {
        if (mc.player == null || mc.world == null) {
            return false;
        }

        Box box = mc.player.getBoundingBox().contract(0.03D);
        Box headBox = new Box(
                box.minX,
                box.maxY,
                box.minZ,
                box.maxX,
                box.maxY + 0.32D,
                box.maxZ
        );

        for (BlockPos pos : BlockPos.iterate(
                MathHelper.floor(headBox.minX), MathHelper.floor(headBox.minY), MathHelper.floor(headBox.minZ),
                MathHelper.floor(headBox.maxX), MathHelper.floor(headBox.maxY), MathHelper.floor(headBox.maxZ))) {
            BlockState state = mc.world.getBlockState(pos);
            if (!state.isAir() && !state.getCollisionShape(mc.world, pos).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private boolean isNoDelayJumpIntent() {
        NoDelay noDelay = NoDelay.getInstance();
        return noDelay != null
                && noDelay.isState()
                && noDelay.ignoreSetting.isSelected("Прыжок")
                && mc.options != null
                && mc.options.jumpKey.isPressed();
    }

    private boolean canUseNoDelayCriticalWindow() {
        if (mc.player == null || mc.world == null || !isNoDelayJumpIntent()) {
            return false;
        }

        if (hasLowCeilingForJumpCrit()) {
            return !mc.player.isOnGround()
                    && mc.player.getVelocity().y <= 0.01D
                    && !mc.player.isTouchingWater()
                    && !mc.player.isSubmergedInWater()
                    && !mc.player.isInLava()
                    && !mc.player.isClimbing()
                    && !mc.player.hasVehicle()
                    && !mc.player.getAbilities().flying;
        }

        return !mc.player.isOnGround()
                && mc.player.getVelocity().y < 0.0D
                && !mc.player.isTouchingWater()
                && !mc.player.isSubmergedInWater()
                && !mc.player.isInLava()
                && !mc.player.isClimbing()
                && !mc.player.hasVehicle()
                && !mc.player.getAbilities().flying
                && !mc.player.hasStatusEffect(StatusEffects.LEVITATION)
                && !mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
                && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !mc.player.isGliding()
                && !PlayerInteractionHelper.isBoxInBlock(mc.player.getBoundingBox().expand(-1.0E-3), Blocks.COBWEB);
    }

    private boolean isPerfectCrit() {
        if (mc.player == null)
            return false;

        return mc.player.fallDistance > 0.0F
                && !mc.player.isOnGround()
                && !mc.player.isClimbing()
                && !mc.player.isTouchingWater()
                && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !mc.player.hasVehicle()
                && !mc.player.getAbilities().flying;
    }

    private boolean isAscending() {
        if (mc.player == null)
            return false;
        return !mc.player.isOnGround() && mc.player.getVelocity().y > 0.0;
    }

    private boolean isDescending() {
        if (mc.player == null)
            return false;
        return !mc.player.isOnGround() && mc.player.getVelocity().y <= 0.0;
    }

    private boolean willBeCritInTicks(int ticks) {
        if (ticks == 0) {
            return isPerfectCrit();
        }

        PlayerSimulation sim = PlayerSimulation.simulateLocalPlayer(ticks);

        return sim.fallDistance > 0.0F
                && !sim.onGround
                && sim.velocity.y <= 0.0
                && !sim.isClimbing()
                && !sim.player.isTouchingWater()
                && !sim.hasStatusEffect(StatusEffects.BLINDNESS)
                && !sim.player.hasVehicle()
                && !sim.player.getAbilities().flying;
    }

    private boolean hasMovementRestrictions() {
        if (mc.player == null)
            return true;

        if (isInWater())
            return false;
        if (hasBadCombatEffect())
            return true;
        if (isOnSensitiveSurface())
            return true;
        if (hasLowCeiling())
            return true;
        if (mc.player.hasStatusEffect(StatusEffects.BLINDNESS))
            return true;
        if (mc.player.hasStatusEffect(StatusEffects.LEVITATION))
            return true;
        if (PlayerInteractionHelper.isBoxInBlock(mc.player.getBoundingBox().expand(-1e-3), Blocks.COBWEB))
            return true;
        if (mc.player.isInLava())
            return true;
        if (mc.player.isClimbing())
            return true;
        if (!PlayerInteractionHelper.canChangeIntoPose(EntityPose.STANDING, mc.player.getEntityPos()))
            return true;
        if (mc.player.getAbilities().flying)
            return true;

        return false;
    }

    private boolean hasBadCombatEffect() {
        if (mc.player == null)
            return true;

        return mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                || mc.player.hasStatusEffect(StatusEffects.LEVITATION);
    }

    private boolean shouldPauseCriticalAttack() {
        return isOnSensitiveSurface() && !canUseCriticalsBypass() && !isNoDelayJumpIntent();
    }

    private boolean canSkipCriticalRequirement() {
        if (mc.player == null)
            return false;

        if (isNoDelayJumpIntent()) {
            return false;
        }

        return hasLowCeiling()
                || mc.player.isInLava()
                || mc.player.isClimbing()
                || !PlayerInteractionHelper.canChangeIntoPose(EntityPose.STANDING, mc.player.getEntityPos())
                || mc.player.getAbilities().flying;
    }

    private boolean canUseCriticalsBypass() {
        PacketCriticals criticals = PacketCriticals.getInstance();
        return criticals != null && criticals.canBypassCriticalRequirement();
    }

    private boolean isOnSensitiveSurface() {
        if (mc.player == null || mc.world == null)
            return false;

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

    private boolean shouldResetSprintForCrit() {
        Aura aura = Aura.getInstance();
        String mode = aura != null && aura.getResetSprintMode() != null
                ? aura.getResetSprintMode().getSelected()
                : "Grim";
        return shouldResetSprintForCrit(mode);
    }

    private boolean shouldResetSprintForCrit(String mode) {
        if (mc.player == null)
            return false;

        if (mode == null || mode.equals("Отключен")) {
            return false;
        }
        if (isInWater())
            return false;
        if (hasBadCombatEffect())
            return false;
        if (isOnSensitiveSurface())
            return false;
        if (mc.player.isGliding())
            return false;
        AutoSprint autoSprint = AutoSprint.getInstance();
        if (autoSprint != null && autoSprint.getNoReset().isValue())
            return false;
        boolean clientSprinting = mc.player.isSprinting();
        boolean serverSprinting = AutoSprint.isServerSprinting();
        if (!clientSprinting && !serverSprinting)
            return false;
        if (!hasAnyMovementInput())
            return false;
        if (mc.player.isOnGround())
            return false;
        if (!isDescending() || mc.player.fallDistance <= 0.0F)
            return false;
        if (isPacketSprintReset(mode) && !sprintResetWatch.finished(190))
            return false;

        return true;
    }

    private boolean prepareSprintResetForCriticalAttack(String mode) {
        if (mc.player == null)
            return false;
        if (mode == null || mode.equals("Отключен"))
            return false;
        if (!mc.player.isSprinting())
            return false;
        if (!isDescending() || mc.player.fallDistance <= 0.0F)
            return false;
        if (!hasAnyMovementInput())
            return false;
        if (System.currentTimeMillis() < sprintPreparedUntil)
            return false;
        if (!shouldResetSprintForCrit(mode))
            return false;

        sendSprintPacket(ClientCommandC2SPacket.Mode.STOP_SPRINTING);

        sprintPreparedUntil = System.currentTimeMillis() + 70L;
        sprintResetWatch.reset();
        return true;
    }

    private boolean isPacketSprintReset(String mode) {
        return mode != null && (mode.equals("Пакетный") || mode.equals("Интенсивный"));
    }

    private boolean isLegitSprintReset(String mode) {
        return mode != null && mode.equals("Легитный");
    }

    private boolean canUseSprintReset(String mode) {
        if (mc.player == null)
            return false;
        if (mode == null || mode.equals("РћС‚РєР»СЋС‡РµРЅ"))
            return false;
        if (isInWater())
            return false;
        if (hasBadCombatEffect())
            return false;
        if (isOnSensitiveSurface())
            return false;
        if (mc.player.isGliding())
            return false;
        AutoSprint autoSprint = AutoSprint.getInstance();
        if (autoSprint != null && autoSprint.getNoReset().isValue())
            return false;

        boolean clientSprinting = mc.player.isSprinting();
        boolean serverSprinting = AutoSprint.isServerSprinting();
        return (clientSprinting || serverSprinting) && hasAnyMovementInput();
    }

    private boolean prepareSprintResetForCriticalAttack(StrikerConstructor.AttackPerpetratorConfigurable config,
                                                        String mode,
                                                        boolean onlyCriticals) {
        if (!onlyCriticals || !isGrimSprintReset(mode))
            return false;
        if (config.getTarget() == null)
            return false;
        if (shouldPauseCriticalAttack() || canSkipCriticalRequirement())
            return false;
        if (!canUseSprintReset(mode))
            return false;
        if (mc.player.isOnGround())
            return false;
        if (!willBeCritSoon(2))
            return false;

        long now = System.currentTimeMillis();
        if (now < sprintPreparedUntil)
            return false;
        if (!sprintResetWatch.finished(45))
            return false;

        stopSprintingForAttack(155L);
        sprintPreparedUntil = now + 155L;
        sprintPreparedTargetId = config.getTarget().getId();
        sprintResetWatch.reset();
        return true;
    }

    private boolean isSprintResetPrepared(StrikerConstructor.AttackPerpetratorConfigurable config, String mode) {
        return isGrimSprintReset(mode)
                && config.getTarget() != null
                && sprintPreparedTargetId == config.getTarget().getId()
                && System.currentTimeMillis() < sprintPreparedUntil;
    }

    private boolean willBeCritSoon(int ticks) {
        for (int i = 0; i <= ticks; i++) {
            if (willBeCritInTicks(i))
                return true;
        }
        return false;
    }

    private boolean isGrimSprintReset(String mode) {
        return "Grim".equals(mode);
    }

    private void stopSprintingForAttack(long suppressMillis) {
        if (mc.player == null)
            return;

        if (mc.player.isSprinting()) {
            mc.player.setSprinting(false);
        }
        AutoSprint.suppressSprintFor(suppressMillis);
    }

    private void sendSprintPacket(ClientCommandC2SPacket.Mode mode) {
        if (mc.player == null || mc.getNetworkHandler() == null)
            return;

        boolean wantSprint = mode == ClientCommandC2SPacket.Mode.START_SPRINTING;
        if (wantSprint == AutoSprint.isServerSprinting()) {
            return;
        }

        AutoSprint.forceNextSprintPacket();
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, mode));
    }

    private boolean canCritNow() {
        Aura aura = Aura.getInstance();
        boolean checkCritEnabled = aura.getCheckCrit().isValue();
        boolean smartCritsEnabled = aura.getSmartCrits().isValue();

        if (!checkCritEnabled) {
            return true;
        }

        if (canUseCriticalsBypass()) {
            return true;
        }

        if (canUseNoDelayCriticalWindow()) {
            return true;
        }

        if (shouldPauseCriticalAttack()) {
            return false;
        }

        if (canSkipCriticalRequirement()) {
            return true;
        }

        if (isAscending()) {
            return false;
        }

        if (smartCritsEnabled) {
            return isDescending() && mc.player.fallDistance > 0.0F;
        }

        return isPerfectCrit();
    }

    private void executeAttackWithSprintReset(StrikerConstructor.AttackPerpetratorConfigurable config, String sprintMode) {
        boolean grimMode = isGrimSprintReset(sprintMode);
        boolean grimPrepared = isSprintResetPrepared(config, sprintMode);
        boolean shouldReset = shouldResetSprintForCrit(sprintMode);
        boolean packetReset = shouldReset && isPacketSprintReset(sprintMode);
        boolean legitReset = shouldReset && isLegitSprintReset(sprintMode);

        if (grimMode) {
            if (grimPrepared) {
                if (mc.player.isSprinting()) {
                    mc.player.setSprinting(false);
                }
                AutoSprint.suppressSprintFor(95L);
            }
        } else if (shouldReset) {
            stopSprintingForAttack(legitReset ? 130L : 120L);
        }

        executeAttack(config);

        if (grimPrepared) {
            sprintResetWatch.reset();
            sprintPreparedUntil = 0L;
            sprintPreparedTargetId = -1;
            return;
        }

        if (shouldReset) {
            if (packetReset) {
                sendSprintPacket(ClientCommandC2SPacket.Mode.START_SPRINTING);
            }
            sprintResetWatch.reset();
        }
    }

    void handleAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (config.getTarget() == null || !config.getTarget().isAlive()) {
            return;
        }

        if (shouldWaitForEating()) {
            return;
        }

        if (isHoldingMace()) {
            handleMaceAttack(config);
            return;
        }

        boolean elytraMode = checkElytraMode(config);
        if (elytraMode && !checkElytraRaycast(config)) {
            return;
        }

        boolean rwWallAttempt = isRwWallAttempt(config);
        boolean matrixWallAttempt = !rwWallAttempt && isMatrixWallAttempt(config);
        boolean matrixWallBypass = matrixWallAttempt && isMatrixWallBypass(config);
        if (!rwWallAttempt && !matrixWallBypass) {
            matrixWallTargetId = -1;
        }

        if (rwWallAttempt && !isRwWallBypass(config)) {
            return;
        }

        if (matrixWallAttempt && !matrixWallBypass) {
            return;
        }

        if (!rwWallAttempt && !matrixWallAttempt && !RaycastAngle.rayTrace(config)) {
            return;
        }

        if (!isLookingAtTarget(config, rwWallAttempt || matrixWallBypass)) {
            return;
        }

        if ((rwWallAttempt || matrixWallBypass) && !isMatrixWallReady(config)) {
            return;
        }

        if (!clickScheduler.isCooldownComplete(0)) {
            return;
        }

        String sprintMode = Aura.getInstance().getResetSprintMode().getSelected();
        boolean isGrimMode = Aura.getInstance().mode != null && Aura.getInstance().mode.isSelected("Grim AI");
        if (isGrimMode && config.getTarget() != null) {
            double dist = mc.player.distanceTo(config.getTarget());
            float maxDist = Aura.getInstance().attackrange.getValue() - 0.10f;
            if (dist > maxDist) {
                return;
            }
        }
        if (prepareSprintResetForCriticalAttack(config, sprintMode, Aura.getInstance().getCheckCrit().isValue())) {
            return;
        }

        if (sprintInputPending) {
            return;
        }

        if (!isSprintResetPrepared(config, sprintMode) && !canCritNow()) {
            return;
        }

        preAttackEntity(config);
        executeAttackWithSprintReset(config, sprintMode);
    }

    private void preAttackEntity(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (config.isShouldUnPressShield() &&
                mc.player.isUsingItem() &&
                mc.player.getActiveItem().getItem().equals(Items.SHIELD)) {
            mc.interactionManager.stopUsingItem(mc.player);
            shieldWatch.reset();
        }
    }

    private void handleMaceAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (shouldWaitForEating())
            return;
        if (mc.player.distanceTo(config.getTarget()) > Aura.getInstance().getAttackrange().getValue())
            return;
        if (!RaycastAngle.rayTrace(config))
            return;
        if (!isLookingAtTarget(config))
            return;
        if (!clickScheduler.isMaceFastAttack())
            return;
        if (!attackTimer.finished(25))
            return;

        preAttackEntity(config);
        executeAttackWithSprintReset(config, Aura.getInstance().getResetSprintMode().getSelected());
    }

    private boolean checkElytraMode(StrikerConstructor.AttackPerpetratorConfigurable config) {
        return Aura.target != null &&
                Aura.target.isGliding() &&
                mc.player.isGliding() &&
                ElytraTarget.getInstance() != null &&
                ElytraTarget.getInstance().isState();
    }

    private boolean checkElytraRaycast(StrikerConstructor.AttackPerpetratorConfigurable config) {
        Vec3d targetVelocity = config.getTarget().getVelocity();
        float leadTicks = 0;
        if (ElytraTarget.shouldElytraTarget) {
            leadTicks = ElytraTarget.getInstance().elytraForward.getValue();
        }
        Vec3d predictedPos = config.getTarget().getEntityPos().add(targetVelocity.multiply(leadTicks));
        Box predictedBox = new Box(
                predictedPos.x - config.getTarget().getWidth() / 2,
                predictedPos.y,
                predictedPos.z - config.getTarget().getWidth() / 2,
                predictedPos.x + config.getTarget().getWidth() / 2,
                predictedPos.y + config.getTarget().getHeight(),
                predictedPos.z + config.getTarget().getWidth() / 2);
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = AngleConnection.INSTANCE.getRotation().toVector();
        return predictedBox.raycast(eyePos, eyePos.add(lookVec.multiply(config.getMaximumRange()))).isPresent();
    }

    private void executeAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (tryBreakShield(config)) {
            attackTimer.reset();
            count++;
            return;
        }

        tryBreakRwWallBlockPacket(config);
        mc.interactionManager.attackEntity(mc.player, config.getTarget());
        mc.player.swingHand(Hand.MAIN_HAND);
        attackTimer.reset();
        count++;
    }

    private boolean tryBreakShield(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (!config.isShouldBreakShield() || mc.player == null || mc.interactionManager == null || mc.getNetworkHandler() == null) {
            return false;
        }

        LivingEntity target = config.getTarget();
        if (!isShieldBlocking(target)) {
            return false;
        }

        int axeSlot = findAxeHotbarSlot();
        if (axeSlot == -1) {
            return false;
        }

        int previousSlot = mc.player.getInventory().getSelectedSlot();
        selectHotbarSlot(axeSlot);
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        selectHotbarSlot(previousSlot);
        NotificationRuntime.notifyShieldBreak(target.getName().getString());
        return true;
    }

    private boolean isShieldBlocking(LivingEntity target) {
        return target != null
                && target.isUsingItem()
                && !target.getActiveItem().isEmpty()
                && target.getActiveItem().isOf(Items.SHIELD);
    }

    private int findAxeHotbarSlot() {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof AxeItem) {
                return slot;
            }
        }
        return -1;
    }

    private void selectHotbarSlot(int slot) {
        if (slot < 0 || slot > 8 || mc.player.getInventory().getSelectedSlot() == slot) {
            return;
        }

        mc.player.getInventory().setSelectedSlot(slot);
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    void handleTriggerAttack(StrikerConstructor.AttackPerpetratorConfigurable config, TriggerBot triggerBot) {
        if (shouldWaitForEating())
            return;
        if (!RaycastAngle.rayTrace(config))
            return;
        if (!isLookingAtTarget(config))
            return;
        if (!clickScheduler.isCooldownComplete(0))
            return;
        if (prepareSprintResetForCriticalAttack(config, triggerBot.getSprintReset().getSelected(), triggerBot.isOnlyCrits()))
            return;
        if (sprintInputPending)
            return;
        if (!canAttackTrigger(config, triggerBot))
            return;

        preAttackEntity(config);
        executeAttackWithSprintReset(config, triggerBot.getSprintReset().getSelected());
    }

    private boolean canAttackTrigger(StrikerConstructor.AttackPerpetratorConfigurable config, TriggerBot triggerBot) {
        if (shouldWaitForEating())
            return false;
        if (!clickScheduler.isCooldownComplete(0))
            return false;

        boolean checkCritEnabled = triggerBot.isOnlyCrits();
        boolean smartCritsEnabled = triggerBot.getSmartCrits().isValue();

        if (!checkCritEnabled)
            return true;

        if (canUseCriticalsBypass())
            return true;

        if (canUseNoDelayCriticalWindow())
            return true;

        if (shouldPauseCriticalAttack())
            return false;

        if (canSkipCriticalRequirement())
            return true;

        if (isAscending())
            return false;

        if (smartCritsEnabled) {
            return isDescending() && mc.player.fallDistance > 0.0F;
        }

        return isPerfectCrit();
    }

    public boolean shouldResetSprinting(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (Aura.target == null)
            return false;
        if (shouldWaitForEating())
            return false;
        return shouldResetSprintForCrit();
    }

    public boolean shouldResetSprintingForTrigger(StrikerConstructor.AttackPerpetratorConfigurable config,
                                                  TriggerBot triggerBot) {
        if (triggerBot.target == null)
            return false;
        if (shouldWaitForEating())
            return false;
        return shouldResetSprintForCrit(triggerBot.getSprintReset().getSelected());
    }

    public boolean canAttack(StrikerConstructor.AttackPerpetratorConfigurable config, int ticks) {
        if (shouldWaitForEating())
            return false;
        if (isHoldingMace()) {
            return attackTimer.finished(25) && clickScheduler.isMaceFastAttack();
        }

        if (!clickScheduler.isCooldownComplete(0)) {
            return false;
        }

        if (ticks > 0) {
            Aura aura = Aura.getInstance();
            boolean checkCritEnabled = aura.getCheckCrit().isValue();

            if (!checkCritEnabled)
                return true;
            if (canUseCriticalsBypass())
                return true;
            if (canUseNoDelayCriticalWindow())
                return true;
            if (shouldPauseCriticalAttack())
                return false;
            if (canSkipCriticalRequirement())
                return true;

            for (int i = 0; i <= ticks; i++) {
                if (willBeCritInTicks(i))
                    return true;
            }
            return false;
        }

        return clickScheduler.isCooldownComplete(0) && canCritNow();
    }

    public boolean canCrit(StrikerConstructor.AttackPerpetratorConfigurable config, int ticks) {
        if (isHoldingMace())
            return true;

        if (mc.player.isUsingItem()
                && !mc.player.getActiveItem().getItem().equals(Items.SHIELD)
                && config.isEatAndAttack()) {
            return false;
        }

        Aura aura = Aura.getInstance();
        boolean checkCritEnabled = aura.getCheckCrit().isValue();

        if (!checkCritEnabled)
            return true;

        if (canUseCriticalsBypass())
            return true;

        if (canUseNoDelayCriticalWindow())
            return true;

        if (shouldPauseCriticalAttack())
            return false;

        if (canSkipCriticalRequirement())
            return true;

        if (ticks > 0) {
            for (int i = 0; i <= ticks; i++) {
                if (willBeCritInTicks(i))
                    return true;
            }
            return false;
        }

        return canCritNow();
    }

    private boolean isRwWallAttempt(StrikerConstructor.AttackPerpetratorConfigurable config) {
        Aura aura = Aura.getInstance();
        if (aura == null || !aura.getRwWallBypass().isValue()) {
            return false;
        }
        if (mc.player == null || mc.world == null || config.getTarget() == null) {
            return false;
        }
        if (!config.isIgnoreWalls()) {
            return false;
        }
        if (mc.player.distanceTo(config.getTarget()) > Math.min(config.getMaximumRange(), 3.35f)) {
            return false;
        }
        return isTargetBehindWall(config.getTarget());
    }

    private boolean isRwWallBypass(StrikerConstructor.AttackPerpetratorConfigurable config) {
        Box wallBox = getRwWallBox(config).expand(0.06);
        Vec3d eyePos = mc.player.getEyePos();
        if (wallBox.contains(eyePos)) {
            return true;
        }
        Vec3d lookVec = AngleConnection.INSTANCE.getRotation().toVector();
        Vec3d endVec = eyePos.add(lookVec.multiply(config.getMaximumRange() + 0.45f));
        return wallBox.raycast(eyePos, endVec).isPresent();
    }

    private boolean isTargetBehindWall(net.minecraft.entity.LivingEntity entity) {
        if (entity == null || mc.player == null || mc.world == null) {
            return false;
        }
        return !mc.player.canSee(entity) || hasNarrowRwWallGap(entity);
    }

    private boolean hasNarrowRwWallGap(net.minecraft.entity.LivingEntity entity) {
        Aura aura = Aura.getInstance();
        if (entity == null || mc.player == null || mc.world == null || aura == null || !aura.getRwWallBypass().isValue()) {
            return false;
        }

        Vec3d eyePos = mc.player.getEyePos();
        Box box = entity.getBoundingBox();
        double centerX = box.getCenter().x;
        double centerZ = box.getCenter().z;
        Vec3d[] points = new Vec3d[]{
                box.getCenter(),
                getStableBodyPoint(entity),
                new Vec3d(centerX, box.maxY - 0.08, centerZ),
                new Vec3d(centerX, box.minY + 0.12, centerZ),
                new Vec3d(box.minX + 0.04, box.minY + box.getLengthY() * 0.55, centerZ),
                new Vec3d(box.maxX - 0.04, box.minY + box.getLengthY() * 0.55, centerZ),
                new Vec3d(centerX, box.minY + box.getLengthY() * 0.55, box.minZ + 0.04),
                new Vec3d(centerX, box.minY + box.getLengthY() * 0.55, box.maxZ - 0.04)
        };

        int blocked = 0;
        int clear = 0;
        for (Vec3d point : points) {
            BlockHitResult hit = mc.world.raycast(new RaycastContext(
                    eyePos,
                    point,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
            ));

            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                blocked++;
            } else {
                clear++;
            }
        }

        return clear > 0 && blocked >= clear;
    }

    private Vec3d getStableBodyPoint(net.minecraft.entity.LivingEntity entity) {
        Box box = entity.getBoundingBox();
        return new Vec3d(
                box.getCenter().x,
                box.minY + box.getLengthY() * 0.72,
                box.getCenter().z
        );
    }

    private Box getRwWallBox(StrikerConstructor.AttackPerpetratorConfigurable config) {
        Box box = config.getBox();
        return new Box(
                box.minX - 0.38,
                box.minY + 0.05,
                box.minZ - 0.38,
                box.maxX + 0.38,
                box.maxY - 0.03,
                box.maxZ + 0.38
        );
    }

    private void tryBreakRwWallBlockPacket(StrikerConstructor.AttackPerpetratorConfigurable config) {
        Aura aura = Aura.getInstance();
        if (aura == null || !aura.getRwWallBypass().isValue()) return;
        if (config.getTarget() == null || mc.player == null || mc.world == null) return;
        if (mc.player.canSee(config.getTarget())) return;
        if (mc.getNetworkHandler() == null) return;

        Vec3d start = mc.player.getEyePos();
        Vec3d end = getStableBodyPoint(config.getTarget());
        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos blockPos = hit.getBlockPos();
        BlockState state = mc.world.getBlockState(blockPos);
        if (state.isAir() || state.getHardness(mc.world, blockPos) < 0.0f) return;

        Direction direction = hit.getSide() == null ? Direction.UP : hit.getSide();
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
    }

    private boolean isMatrixWallAttempt(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (mc.player == null || mc.world == null || config.getTarget() == null) {
            return false;
        }
        if (Network.isReallyWorld()) {
            return false;
        }
        if (!config.isIgnoreWalls() || config.getAimMode() == null || !config.getAimMode().isSelected("Matrix")) {
            return false;
        }
        if (mc.player.distanceTo(config.getTarget()) > Math.min(config.getMaximumRange(), 3.15f)) {
            return false;
        }

        Vec3d eyePos = mc.player.getEyePos();
        Box wallBox = getMatrixWallBox(config);
        Vec3d targetPoint = wallBox.getCenter();
        HitResult blockHit = RaycastAngle.raycast(eyePos, targetPoint, RaycastContext.ShapeType.COLLIDER);
        return blockHit != null && blockHit.getType() == HitResult.Type.BLOCK;
    }

    private boolean isMatrixWallBypass(StrikerConstructor.AttackPerpetratorConfigurable config) {
        Vec3d eyePos = mc.player.getEyePos();
        Box wallBox = getMatrixWallBox(config).expand(0.04);
        if (wallBox.contains(eyePos)) {
            return true;
        }
        Vec3d lookVec = AngleConnection.INSTANCE.getRotation().toVector();
        Vec3d endVec = eyePos.add(lookVec.multiply(config.getMaximumRange() + 0.2f));
        return wallBox.raycast(eyePos, endVec).isPresent();
    }

    private boolean isMatrixWallReady(StrikerConstructor.AttackPerpetratorConfigurable config) {
        int targetId = config.getTarget().getId();
        if (matrixWallTargetId != targetId) {
            matrixWallTargetId = targetId;
            matrixWallWatch.reset();
            return false;
        }
        return matrixWallWatch.finished(85);
    }

    private Box getMatrixWallBox(StrikerConstructor.AttackPerpetratorConfigurable config) {
        return config.getBox().expand(0.36, 0.12, 0.36);
    }

    private boolean isLookingAtTarget(StrikerConstructor.AttackPerpetratorConfigurable config) {
        return isLookingAtTarget(config, false);
    }

    private boolean isLookingAtTarget(StrikerConstructor.AttackPerpetratorConfigurable config, boolean matrixWallBypass) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = AngleConnection.INSTANCE.getRotation().toVector();
        boolean isGrimMode = Aura.getInstance().mode != null && Aura.getInstance().mode.isSelected("Grim AI");
        double rangeMul = isGrimMode ? config.getMaximumRange() : config.getMaximumRange() + 0.2f;
        double expandAmt = isGrimMode ? 0.015 : 0.08;
        Vec3d endVec = eyePos.add(lookVec.multiply(rangeMul));
        Box hitBox = (matrixWallBypass ? getMatrixWallBox(config) : config.getBox()).expand(expandAmt);
        if (hitBox.contains(eyePos)) {
            return true;
        }
        return hitBox.raycast(eyePos, endVec).isPresent();
    }
}

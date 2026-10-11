package neverlose.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.AttackEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.util.Instance;
import neverlose.util.string.PlayerInteractionHelper;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class PacketCriticals extends ModuleStructure {
    static final int WATER_CRIT_INTENT_TICKS = 8;
    static final int WATER_CRIT_CONTACT_TICKS = 10;
    static final double WATER_CRIT_MIN_UPWARD_VELOCITY = 0.05D;

    final BooleanSetting cobweb = new BooleanSetting(
            "Паутина",
            "Проводить пакетный крит, когда игрок находится в паутине"
    ).setValue(true);

    final BooleanSetting slowFalling = new BooleanSetting(
            "Плавное падение",
            "Разрешать крит в момент падения под эффектом плавного падения"
    ).setValue(true);

    final BooleanSetting levitation = new BooleanSetting(
            "Левитация",
            "Проводить пакетный крит под эффектом левитации"
    ).setValue(true);

    final BooleanSetting water = new BooleanSetting(
            "Вода",
            "Ждать окно крита после прыжка у поверхности воды"
    ).setValue(true);

    final BooleanSetting onlyAura = new BooleanSetting(
            "Только Aura",
            "Не применять PacketCriticals к ручным ударам"
    ).setValue(false);

    int lastWaterContactAge = Integer.MIN_VALUE;
    int lastWaterCritIntentAge = Integer.MIN_VALUE;

    public PacketCriticals() {
        super(
                "PacketCriticals",
                "Критические удары в паутине и под эффектом плавного падения",
                ModuleCategory.COMBAT
        );
        settings(cobweb, slowFalling, levitation, water, onlyAura);
    }

    public static PacketCriticals getInstance() {
        return Instance.get(PacketCriticals.class);
    }

    public boolean canBypassCriticalRequirement() {
        if (!isState() || mc.player == null || mc.world == null) {
            resetWaterState();
            return false;
        }

        updateWaterCritState();

        if (cobweb.isValue() && isInCobweb()) {
            return true;
        }

        if (levitation.isValue() && mc.player.hasStatusEffect(StatusEffects.LEVITATION)) {
            return true;
        }

        if (water.isValue() && isTryingWaterCrit()) {
            return isWaterCritWindow();
        }

        return slowFalling.isValue()
                && mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
                && mc.player.getVelocity().y < 0.0D
                && mc.player.fallDistance > 0.0f;
    }

    @EventHandler
    public void onAttack(AttackEvent event) {
        if (event.getTarget() == null || !shouldHandleAttack(event.getTarget())) {
            return;
        }

        if (cobweb.isValue() && isInCobweb()) {
            spoofCobwebCritical();
        } else if (levitation.isValue() && mc.player.hasStatusEffect(StatusEffects.LEVITATION)) {
            spoofLevitationCritical();
        }
    }

    private boolean shouldHandleAttack(Entity target) {
        if (!isState() || mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
            return false;
        }

        if (onlyAura.isValue() && target != Aura.target) {
            return false;
        }

        return (cobweb.isValue() && isInCobweb())
                || (levitation.isValue() && mc.player.hasStatusEffect(StatusEffects.LEVITATION));
    }

    private void spoofCobwebCritical() {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        boolean horizontalCollision = mc.player.horizontalCollision;

        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                x, y + 0.003D, z, false, horizontalCollision
        ));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                x, y, z, false, horizontalCollision
        ));
    }

    private void spoofLevitationCritical() {
        spoofAirCritical();
    }

    private void spoofAirCritical() {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        boolean horizontalCollision = mc.player.horizontalCollision;

        sendPosition(x, y + 0.0625D, z, horizontalCollision);
        sendPosition(x, y, z, horizontalCollision);
        sendPosition(x, y + 0.000011D, z, horizontalCollision);
        sendPosition(x, y, z, horizontalCollision);
    }

    private void sendPosition(double x, double y, double z, boolean horizontalCollision) {
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                x, y, z, false, horizontalCollision
        ));
    }

    private boolean isInCobweb() {
        return PlayerInteractionHelper.isBoxInBlock(
                mc.player.getBoundingBox().contract(1.0E-3),
                Blocks.COBWEB
        );
    }

    private void updateWaterCritState() {
        if (!isNearWaterSurface()) {
            return;
        }

        lastWaterContactAge = mc.player.age;

        if (isWaterCritIntentState()) {
            lastWaterCritIntentAge = mc.player.age;
        }
    }

    private boolean isWaterCritIntentState() {
        return mc.options != null
                && mc.options.jumpKey.isPressed()
                && !mc.player.isOnGround()
                && !mc.player.isSubmergedInWater()
                && mc.player.getVelocity().y > WATER_CRIT_MIN_UPWARD_VELOCITY;
    }

    private boolean isTryingWaterCrit() {
        return mc.options != null
                && mc.options.jumpKey.isPressed()
                && mc.player.age - lastWaterCritIntentAge <= WATER_CRIT_INTENT_TICKS
                && mc.player.age - lastWaterContactAge <= WATER_CRIT_CONTACT_TICKS;
    }

    private boolean isWaterCritWindow() {
        return !mc.player.isOnGround()
                && !mc.player.isTouchingWater()
                && !mc.player.isSubmergedInWater()
                && mc.player.fallDistance > 0.0f
                && mc.player.getVelocity().y < 0.0D;
    }

    private boolean isNearWaterSurface() {
        BlockPos below = BlockPos.ofFloored(0.0D, -0.4D, 0.0D);
        return mc.player.isTouchingWater()
                || mc.player.isSubmergedInWater()
                || mc.world.getBlockState(below).isOf(Blocks.WATER);
    }

    private void resetWaterState() {
        lastWaterContactAge = Integer.MIN_VALUE;
        lastWaterCritIntentAge = Integer.MIN_VALUE;
    }
}

package neverlose.modules.impl.movement;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.GlidingVelocityEvent;
import neverlose.events.impl.PacketEvent;
import neverlose.events.impl.TickEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.util.string.PlayerInteractionHelper;

public class GrimGlide extends ModuleStructure {
    private int flagTicks;
    private boolean flagged;

    public GrimGlide() {
        super("GrimGlide", "Grim Glide", ModuleCategory.MOVEMENT);
    }

    @Override
    public void activate() {
        flagTicks = 0;
        flagged = false;
    }

    @Override
    public void deactivate() {
        flagTicks = 0;
        flagged = false;
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (mc.player == null) {
            return;
        }

        if (!event.isSend() && event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            flagTicks = 2;
            flagged = true;
            return;
        }

        if (event.isSend() && event.getPacket() instanceof PlayerMoveC2SPacket) {
            if (mc.player.isGliding() && flagTicks == 0 && !flagged) {
                PlayerInteractionHelper.sendPacketWithOutEvent(
                        new PlayerMoveC2SPacket.OnGroundOnly(true, true)
                );
                event.cancel();
            }

            flagged = false;
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (flagTicks > 0) {
            flagTicks--;
        }
    }

    @EventHandler
    public void onGlidingVelocity(GlidingVelocityEvent event) {
        if (mc.player == null || !mc.player.isGliding()) {
            return;
        }

        Vec3d velocity = mc.player.getVelocity();
        Vec3d look = mc.player.getRotationVector();
        float pitch = mc.player.getPitch() * 0.017453292F;
        double lookHorizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        double velocityHorizontal = velocity.horizontalLength();
        boolean falling = velocity.y <= 0.0;
        double gravity = falling && mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
                ? Math.min(mc.player.getFinalGravity(), 0.01)
                : mc.player.getFinalGravity();
        double pitchFactor = MathHelper.square(Math.cos((double) pitch));

        velocity = velocity.add(0.0, gravity * (-1.0 + pitchFactor * 0.75), 0.0);

        if (velocity.y < 0.0 && lookHorizontal > 0.0) {
            double lift = velocity.y * -0.1 * pitchFactor;
            velocity = velocity.add(
                    look.x * lift / lookHorizontal,
                    lift,
                    look.z * lift / lookHorizontal
            );
        }

        if (pitch < 0.0F && lookHorizontal > 0.0) {
            double lift = velocityHorizontal * -MathHelper.sin(pitch) * 0.04F;
            velocity = velocity.add(
                    -look.x * lift / lookHorizontal,
                    lift * 3.2,
                    -look.z * lift / lookHorizontal
            );
        }

        if (lookHorizontal > 0.0) {
            velocity = velocity.add(
                    (look.x / lookHorizontal * velocityHorizontal - velocity.x) * 0.1,
                    0.0,
                    (look.z / lookHorizontal * velocityHorizontal - velocity.z) * 0.1
            );
        }

        if (flagTicks >= 1) {
            double yaw = Math.toRadians(mc.player.getYaw());
            double boostX = -Math.sin(yaw) * 0.09;
            double boostZ = Math.cos(yaw) * 0.09;
            event.setVelocity(velocity
                    .multiply(0.99, 0.9800000190734863, 0.99)
                    .add(boostX, 0.03, boostZ));
        } else {
            event.setVelocity(velocity.multiply(0.3));
        }
    }
}

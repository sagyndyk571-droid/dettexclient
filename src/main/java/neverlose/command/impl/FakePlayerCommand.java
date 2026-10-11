package neverlose.command.impl;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import neverlose.IMinecraft;
import neverlose.command.Command;
import neverlose.events.api.EventHandler;
import neverlose.events.api.EventManager;
import neverlose.events.impl.AttackEvent;

import java.util.UUID;

public class FakePlayerCommand extends Command implements IMinecraft {
    public static OtherClientPlayerEntity fakePlayer;

    public FakePlayerCommand() {
        super("fakeplayer", "Создать фейк плеера для тестов", "fp", "fake");
        EventManager.register(this);
    }

    @Override
    public void execute(String label, String[] args) {
        if (mc.world == null || mc.player == null) return;
        
        if (fakePlayer == null) {
            fakePlayer = new OtherClientPlayerEntity(mc.world, new GameProfile(UUID.randomUUID(), "NeiroTester"));
            fakePlayer.copyPositionAndRotation(mc.player);
            fakePlayer.headYaw = mc.player.headYaw;
            fakePlayer.bodyYaw = mc.player.bodyYaw;
            fakePlayer.setHealth(20.0f);
            fakePlayer.getInventory().clone(mc.player.getInventory());
            mc.world.addEntity(fakePlayer);
            logDirect("Фейк-плеер успешно заспавнен! Бейте его для записи.", Formatting.GREEN);
        } else {
            fakePlayer.discard();
            fakePlayer = null;
            logDirect("Фейк-плеер удален!", Formatting.GREEN);
        }
    }

    @EventHandler
    public void onAttack(AttackEvent event) {
        if (fakePlayer != null && event.getTarget() != null && event.getTarget().getId() == fakePlayer.getId()) {
            float dmg = 2.0f; // Simulate base damage
            if (!mc.player.getMainHandStack().isEmpty()) {
                dmg += 3.0f; 
            }
            fakePlayer.setHealth(fakePlayer.getHealth() - dmg);
            
            mc.world.playSound(mc.player, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(), SoundEvents.ENTITY_PLAYER_HURT, fakePlayer.getSoundCategory(), 1.0F, 1.0F);
            mc.particleManager.addEmitter(fakePlayer, ParticleTypes.DAMAGE_INDICATOR);
            
            fakePlayer.animateDamage(mc.player.getYaw());
            
            if (fakePlayer.getHealth() <= 0) {
                logDirect("Фейк-плеер убит! Возрождение...", Formatting.YELLOW);
                fakePlayer.setHealth(20.0f);
            }
        }
    }
}

package neverlose.modules.impl.misc;

import com.adl.nativeprotect.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.PacketEvent;
import neverlose.events.impl.TickEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.util.timer.TimerUtil;

import java.util.UUID;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServerRPSpoofer extends ModuleStructure {
    private ResourcePackAction currentAction = ResourcePackAction.WAIT;
    private final TimerUtil counter = TimerUtil.create();
    private UUID pendingPackId;

    public ServerRPSpoofer() {
        super("ServerRPSpoof", "Server RP Spoof", ModuleCategory.MISC);
    }

    @EventHandler
    @Native
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof ResourcePackSendS2CPacket packet) {
            pendingPackId = packet.id();
            currentAction = ResourcePackAction.ACCEPT;
            e.cancel();
        }
    }

    @EventHandler
   @Native
    public void onTick(TickEvent e) {
        ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
        if (networkHandler != null) {
            processResourcePackAction(networkHandler);
        }
    }

   @Native
    private void processResourcePackAction(ClientPlayNetworkHandler networkHandler) {
        if (pendingPackId == null) {
            currentAction = ResourcePackAction.WAIT;
            return;
        }

        if (currentAction == ResourcePackAction.ACCEPT) {
            networkHandler.sendPacket(new ResourcePackStatusC2SPacket(pendingPackId, ResourcePackStatusC2SPacket.Status.ACCEPTED));
            currentAction = ResourcePackAction.SEND;
            counter.resetCounter();
        } else if (currentAction == ResourcePackAction.SEND && counter.isReached(300L)) {
            networkHandler.sendPacket(new ResourcePackStatusC2SPacket(pendingPackId, ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
            currentAction = ResourcePackAction.WAIT;
            pendingPackId = null;
        }
    }

    @Override
    @Native
    public void deactivate() {
        currentAction = ResourcePackAction.WAIT;
        pendingPackId = null;
        super.deactivate();
    }

    public enum ResourcePackAction {
        ACCEPT, SEND, WAIT;
    }
}

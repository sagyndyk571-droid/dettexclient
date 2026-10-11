package neverlose.events.api.types;

import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import neverlose.debob;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.PacketEvent;
import neverlose.events.impl.TickEvent;
import neverlose.events.impl.UsingItemEvent;

public class EventListener implements Listener {
    public static boolean serverSprint;
    public static int selectedSlot;

    @EventHandler
    public void onTick(TickEvent e) {
        debob.getInstance().getManager().getAttackPerpetrator().tick();
        if (debob.getInstance().getManager().getHudManager() != null) {
            debob.getInstance().getManager().getHudManager().tick();
        }
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        switch (e.getPacket()) {
            case ClientCommandC2SPacket command -> serverSprint = switch (command.getMode()) {
                case ClientCommandC2SPacket.Mode.START_SPRINTING -> true;
                case ClientCommandC2SPacket.Mode.STOP_SPRINTING -> false;
                default -> serverSprint;
            };
            case UpdateSelectedSlotC2SPacket slot -> selectedSlot = slot.getSelectedSlot();
            default -> {}
        }

        debob.getInstance().getManager().getAttackPerpetrator().onPacket(e);
        debob.getInstance().getManager().getHudManager().onPacket(e);
    }

    @EventHandler
    public void onUsingItemEvent(UsingItemEvent e) {
        debob.getInstance().getManager().getAttackPerpetrator().onUsingItem(e);
    }

}
package neverlose.modules.impl.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.PacketEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.util.Instance;

import java.util.concurrent.ThreadLocalRandom;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItemSwapFix extends ModuleStructure {
    public static ItemSwapFix getInstance() {
        return Instance.get(ItemSwapFix.class);
    }

    private final SelectSetting mode = new SelectSetting("Mode", "Выберите режим фикса")
            .value("Old", "Matrix");

    public ItemSwapFix() {
        super("ItemSwapFix", ModuleCategory.MISC);
        settings(mode);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.getType() != PacketEvent.Type.RECEIVE) return;
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        if (e.getPacket() instanceof UpdateSelectedSlotS2CPacket packet) {
            int serverSlot = packet.slot();
            int clientSlot = mc.player.getInventory().getSelectedSlot();

            String sel = mode.getSelected();

            if (serverSlot != clientSlot) {
                if ("Old".equals(sel)) {
                    e.cancel();
                    if (ThreadLocalRandom.current().nextDouble() < 0.05) return;
                } else if ("Matrix".equals(sel)) {
                    if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                        return;
                    }
                    mc.player.getInventory().setSelectedSlot(serverSlot);
                }
            }
        }
    }
}

package neverlose.modules.impl.player;

import com.adl.nativeprotect.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.DeathScreenEvent;
import neverlose.events.impl.PacketEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.SelectSetting;

@SuppressWarnings("all")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AutoRespawn extends ModuleStructure {

    SelectSetting modeSetting = new SelectSetting("Режим", "Выберите, что будет использоваться").value("Default");

    public AutoRespawn() {
        super("AutoRespawn", "Auto Respawn", ModuleCategory.PLAYER);
        settings(modeSetting);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
    }

    @EventHandler
    @Native
    public void onDeathScreen(DeathScreenEvent e) {
        if (modeSetting.isSelected("Default")) {
            mc.player.requestRespawn();
            mc.setScreen(null);
        }
    }
}
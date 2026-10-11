package neverlose.modules.impl.player;

import com.adl.nativeprotect.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.PacketEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.util.move.MoveUtil;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoFallDamage extends ModuleStructure {

    SelectSetting mode = new SelectSetting("Режим", "Выберите тип")
            .value("SpookyTime")
            .selected("SpookyTime");

    public NoFallDamage() {
        super("NoFallDamage", "No Fall Damage", ModuleCategory.PLAYER);
        settings(mode);
    }

    @EventHandler
   @Native
    public void onPacket(PacketEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.fallDistance > 0 && MoveUtil.getDistanceToGround() > 4) {
            mc.player.setVelocity(0, 0, 0);
        }
    }
}
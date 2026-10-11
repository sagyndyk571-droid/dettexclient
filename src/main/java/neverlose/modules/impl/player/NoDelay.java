package neverlose.modules.impl.player;

import com.adl.nativeprotect.Native;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.TickEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.MultiSelectSetting;
import neverlose.util.Instance;

public class NoDelay extends ModuleStructure {

    public MultiSelectSetting ignoreSetting = new MultiSelectSetting("Тип", "")
            .value("Прыжок", "Правый клик", "Задержка ломания").selected("Прыжок");

    public NoDelay() {
        super("NoDelay", "No Delay", ModuleCategory.PLAYER);
        settings(ignoreSetting);
    }

    public static NoDelay getInstance() {
        return Instance.get(NoDelay.class);
    }

    @EventHandler
   @Native
    public void onTick(TickEvent e) {
        if (mc.player == null) return;
        if (ignoreSetting.isSelected("Задержка ломания")) mc.interactionManager.blockBreakingCooldown = 0;
        if (ignoreSetting.isSelected("Прыжок")) mc.player.jumpingCooldown = 0;
        if (ignoreSetting.isSelected("Правый клик")) mc.itemUseCooldown = 0;
    }
}

package neverlose.modules.impl.misc;

import com.adl.nativeprotect.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.text.Text;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.TickEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.MultiSelectSetting;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.network.Network;
import neverlose.util.repository.friend.FriendUtils;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AutoLeave extends ModuleStructure {
    SelectSetting leaveType = new SelectSetting("Тип выхода", "Позволяет выбрать тип выхода")
            .value("Hub", "Main Menu").selected("Main Menu");

    MultiSelectSetting triggerSetting = new MultiSelectSetting("Триггеры", "Выберите, в каких случаях произойдет выход")
            .value("Players", "Staff").selected("Players", "Staff");

    SliderSettings distanceSetting = new SliderSettings("Максимальная дистанция", "Максимальная дистанция для активации авто-выхода")
            .setValue(10).range(5, 40).visible(() -> triggerSetting.isSelected("Players"));

    public AutoLeave() {
        super("AutoLeave", "Auto Leave", ModuleCategory.MISC);
        settings(leaveType, triggerSetting, distanceSetting);
    }

    @EventHandler
   @Native
    public void onTick(TickEvent e) {
        if (Network.isPvp()) return;

        if (triggerSetting.isSelected("Players"))
            mc.world.getPlayers().stream().filter(p -> mc.player.distanceTo(p) < distanceSetting.getValue() && mc.player != p && !FriendUtils.isFriend(p)).findFirst().ifPresent(p -> leave(p.getName().copy().append(" - Появился рядом " + mc.player.distanceTo(p) + "м")));
    }

   @Native
    public void leave(Text text) {
        switch (leaveType.getSelected()) {
            case "Hub" -> {
                mc.getNetworkHandler().sendChatCommand("hub");
            }
            case "Main Menu" ->
                    mc.getNetworkHandler().getConnection().disconnect(Text.of("[Auto Leave] \n").copy().append(text));
        }
        setState(false);
    }
}
package neverlose.modules.impl.movement;

import com.adl.nativeprotect.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.TickEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.util.Instance;
import neverlose.util.move.MoveUtil;
import neverlose.util.string.PlayerInteractionHelper;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoWeb extends ModuleStructure {
    public static NoWeb getInstance() {
        return Instance.get(NoWeb.class);
    }

    public final SelectSetting webMode = new SelectSetting("Режим", "Выберите режим обхода")
            .value("Grim", "ReallyWorld")
            .selected("Grim");

    public NoWeb() {
        super("NoWeb", "No Web", ModuleCategory.MOVEMENT);
        settings(webMode);
    }

    @EventHandler
   @Native
    public void onTick(TickEvent e) {
        if (PlayerInteractionHelper.isPlayerInBlock(Blocks.COBWEB)) {
            if (webMode.isSelected("ReallyWorld")) {
                handleReallyWorldMode();
            } else {
                double[] speed = MoveUtil.calculateDirection(0.35);
                mc.player.addVelocity(speed[0], 0, speed[1]);
                mc.player.setVelocity(speed[0], mc.options.jumpKey.isPressed() ? 0.65f : mc.options.sneakKey.isPressed() ? -0.65f : 0, speed[1]);
            }
        }
    }

    @Native
    private void handleReallyWorldMode() {
        double horizontal = Speed.isAnyReallyWorldSpeedActive() ? 0.145 : 0.18;
        double[] speed = MoveUtil.calculateDirection(horizontal);
        double y = mc.options.jumpKey.isPressed() ? 0.32f : mc.options.sneakKey.isPressed() ? -0.32f : 0.0;

        if (Speed.isReallyWorld2SpeedActive() && mc.player.age % 2 == 0) {
            horizontal *= 0.8;
            speed = MoveUtil.calculateDirection(horizontal);
        }

        mc.player.setVelocity(speed[0], y, speed[1]);
    }
}

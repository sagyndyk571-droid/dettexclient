package neverlose.modules.impl.movement;

import com.adl.nativeprotect.Native;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.KeyEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BindSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.Instance;
import neverlose.util.sounds.SoundManager;
import org.lwjgl.glfw.GLFW;

public class ElytraTarget extends ModuleStructure {

    public static ElytraTarget getInstance() {
        return Instance.get(ElytraTarget.class);
    }

    public SliderSettings elytraFindRange = new SliderSettings("Find range", "Target search range while gliding")
            .setValue(32).range(6F, 64F);

    public SliderSettings elytraForward = new SliderSettings("Forward lead", "Target prediction lead")
            .setValue(3).range(0F, 6F);

    final BindSetting forward = new BindSetting("Toggle lead", "");

    public static boolean shouldElytraTarget = false;

    public BindSetting getForwardBind() {
        return forward;
    }

    public ElytraTarget() {
        super("ElytraTarget", "Elytra Target", ModuleCategory.COMBAT);
        settings(elytraFindRange, elytraForward, forward);
    }

    @Override
    public void activate() {
        shouldElytraTarget = true;
    }

    @Override
    public void deactivate() {
        shouldElytraTarget = false;
    }

    public float getPredictionTicks() {
        if (!shouldElytraTarget || !isState()) {
            return 0f;
        }
        return elytraForward.getValue();
    }

    @EventHandler
    @Native
    private void onEventKey(KeyEvent e) {
        if (!isState()) {
            return;
        }

        if (forward.getKey() != GLFW.GLFW_KEY_UNKNOWN && e.isKeyDown(forward.getKey())) {
            shouldElytraTarget = !shouldElytraTarget;
            SoundManager.playSound(shouldElytraTarget ? SoundManager.MODULE_ENABLE : SoundManager.MODULE_DISABLE, 1, 1.0f);
        }
    }
}

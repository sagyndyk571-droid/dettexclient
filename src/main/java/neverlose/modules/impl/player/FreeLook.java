package neverlose.modules.impl.player;

import com.adl.nativeprotect.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.CameraEvent;
import neverlose.events.impl.FovEvent;
import neverlose.events.impl.KeyEvent;
import neverlose.events.impl.MouseRotationEvent;
import neverlose.modules.impl.combat.aura.Angle;
import neverlose.modules.impl.combat.aura.MathAngle;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BindSetting;
import neverlose.util.string.PlayerInteractionHelper;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class FreeLook extends ModuleStructure {

    Perspective perspective;
    Angle angle;
    public static BindSetting freeLookSetting = new BindSetting("Свободный обзор", "Клавиша для свободного обзора");

    public FreeLook() {
        super("FreeLook", "Free Look", ModuleCategory.RENDER);
        settings(freeLookSetting);
        angle = null;
    }

    @EventHandler
    @Native
    public void onKey(KeyEvent e) {
        if (e.isKeyDown(freeLookSetting.getKey())) {
            perspective = mc.options.getPerspective();
            if (angle == null) {
                angle = MathAngle.cameraAngle();
            }
        }
    }

    @EventHandler
   @Native
    public void onFov(FovEvent e) {
        if (PlayerInteractionHelper.isKey(freeLookSetting)) {
            handleFreeLookActivation();
        } else if (perspective != null) {
            handleFreeLookDeactivation();
        }
    }

    @Native
    private void handleFreeLookActivation() {
        if (mc.options.getPerspective().isFirstPerson()) mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        if (angle == null) {
            angle = MathAngle.cameraAngle();
        }
    }

    @Native
    private void handleFreeLookDeactivation() {
        mc.options.setPerspective(perspective);
        perspective = null;
        angle = null;
    }

    @EventHandler
   @Native
    public void onMouseRotation(MouseRotationEvent e) {
        if (PlayerInteractionHelper.isKey(freeLookSetting)) {
            if (angle == null) {
                angle = MathAngle.cameraAngle();
            }
            angle.setYaw(angle.getYaw() + e.getCursorDeltaX() * 0.15F);
            angle.setPitch(MathHelper.clamp(angle.getPitch() + e.getCursorDeltaY() * 0.15F, -90F, 90F));
            e.cancel();
        } else {
            angle = null;
        }
    }

    @EventHandler
    public void onCamera(CameraEvent e) {
        if (PlayerInteractionHelper.isKey(freeLookSetting) && angle != null) {
            e.setAngle(angle);
            e.cancel();
        }
    }
}
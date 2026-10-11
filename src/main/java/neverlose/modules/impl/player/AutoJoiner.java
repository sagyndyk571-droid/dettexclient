package neverlose.modules.impl.player;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.TickEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.Instance;

import java.awt.*;
import java.awt.event.InputEvent;

public class AutoJoiner extends ModuleStructure {

    public static AutoJoiner getInstance() {
        return Instance.get(AutoJoiner.class);
    }

    private final SliderSettings targetSlot = new SliderSettings("Гриф", "Номер слота в GUI")
            .range(0, 53)
            .setValue(20);

    private final SliderSettings clickDelay = new SliderSettings("Задержка (мс)", "Задержка между кликами")
            .range(50, 5000)
            .setValue(300);

    private final BooleanSetting autoDisable = new BooleanSetting("Авто-выключение", "Выключиться при входе на сервер")
            .setValue(true);

    private final BooleanSetting backgroundClick = new BooleanSetting("Клик в фоне", "Кликать даже если окно свёрнуто")
            .setValue(true);

    private long lastClickTime = 0L;
    private boolean wasInHub = false;
    private Robot robot = null;

    public AutoJoiner() {
        super("AutoJoiner", "Auto Joiner", ModuleCategory.MISC);
        settings(targetSlot, clickDelay, autoDisable, backgroundClick);
        try {
            robot = new Robot();
        } catch (AWTException ignored) {}
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        if (now - lastClickTime < (long) clickDelay.getValue()) return;

        if (mc.currentScreen instanceof HandledScreen<?> screen) {
            wasInHub = true;

            var handler = screen.getScreenHandler();
            int slotIndex = targetSlot.getInt();

            if (slotIndex >= handler.slots.size()) return;

            Slot s = handler.slots.get(slotIndex);
            if (!s.hasStack()) return;

            if (mc.isWindowFocused()) {
                mc.interactionManager.clickSlot(handler.syncId, s.id, 0, SlotActionType.PICKUP, mc.player);
                lastClickTime = now;
            } else if (backgroundClick.isValue() && robot != null) {
                performBackgroundClick(s);
                lastClickTime = now;
            }

        } else if (wasInHub) {
            wasInHub = false;
            if (autoDisable.isValue()) setState(false);
        }
    }

    private void performBackgroundClick(Slot slot) {
        try {
            var window = mc.getWindow();
            int winX = (int) window.getX();
            int winY = (int) window.getY();
            double scaleFactor = window.getScaleFactor();
            int screenX = winX + (int) ((slot.x + 8) * scaleFactor);
            int screenY = winY + (int) ((slot.y + 8) * scaleFactor);
            robot.mouseMove(screenX, screenY);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.delay(30);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        } catch (Exception ignored) {}
    }
}
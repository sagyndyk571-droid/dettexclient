package neverlose.modules.impl.misc;

import com.adl.nativeprotect.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.TickEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.SliderSettings;

import neverlose.util.inventory.InventoryUtils;
import neverlose.util.timer.StopWatch;

/**
 * AutoMist — автоматически собирает предметы из мистических сундуков.
 * Фильтрует сундук по названию, ждёт снятия cooldown и shift-кликает все слоты.
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AutoMyst extends ModuleStructure {

    // ─── Настройки ────────────────────────────────────────────────────────────

    SliderSettings clickDelay = new SliderSettings("Задержка клика (мс)", "Задержка между shift-click по слотам")
            .setValue(120).range(0, 1000);

    SliderSettings closeDelay = new SliderSettings("Задержка закрытия (мс)", "Пауза после очистки перед закрытием")
            .setValue(300).range(0, 2000);

    private static final String CHEST_FILTER = "";

    BooleanSetting checkCooldown = new BooleanSetting("Cooldown защита", "Не собирать если порошок на перезарядке")
            .setValue(true);

    BooleanSetting autoClose = new BooleanSetting("Авто-закрытие", "Закрыть сундук после сбора всех предметов")
            .setValue(true);

    BooleanSetting notifications = new BooleanSetting("Уведомления", "Выводить сообщения о работе модуля")
            .setValue(true);

    // ─── Состояние ────────────────────────────────────────────────────────────

    private enum Phase { IDLE, STEALING, CLOSING }

    @NonFinal Phase phase = Phase.IDLE;
    @NonFinal int stolenCount = 0;

    StopWatch clickTimer = new StopWatch();
    StopWatch closeTimer = new StopWatch();

    // ─── Конструктор ──────────────────────────────────────────────────────────

    public AutoMyst() {
        super("AutoMist", "Авто-сбор предметов из мистических сундуков", ModuleCategory.MISC);
        settings(clickDelay, closeDelay, checkCooldown, autoClose, notifications);
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void activate() {
        super.activate();
        resetState();
        log("[AutoMist] Включён. Жду открытия мистического сундука...");
    }

    @Override
    public void deactivate() {
        super.deactivate();
        resetState();
        log("[AutoMist] Выключён.");
    }

    private void resetState() {
        phase = Phase.IDLE;
        stolenCount = 0;
        clickTimer.reset();
        closeTimer.reset();
    }

    // ─── Тик ─────────────────────────────────────────────────────────────────

    @EventHandler
    @Native
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        switch (phase) {
            case IDLE     -> handleIdle();
            case STEALING -> handleStealing();
            case CLOSING  -> handleClosing();
        }
    }

    // ─── Фазы ─────────────────────────────────────────────────────────────────

    /** Ожидаем открытия подходящего сундука */
    private void handleIdle() {
        if (!isMysticalChestOpen()) return;
        if (checkCooldown.isValue() && isCooldownActive()) return;

        stolenCount = 0;
        phase = Phase.STEALING;
        log("[AutoMist] Мистический сундук открыт, начинаю сбор...");
    }

    /** Перебираем слоты сундука и shift-кликаем с задержкой */
    @Native
    private void handleStealing() {
        if (!isMysticalChestOpen()) {
            if (stolenCount > 0) log("[AutoMist] Сундук закрыт. Собрано: " + stolenCount);
            resetState();
            return;
        }

        if (checkCooldown.isValue() && isCooldownActive()) return;
        if (!clickTimer.every(clickDelay.getValue())) return;

        Slot target = nextChestSlot();
        if (target != null) {
            InventoryUtils.click(target.id, 0, SlotActionType.QUICK_MOVE);
            stolenCount++;
        } else {
            log("[AutoMist] Готово! Собрано предметов: " + stolenCount);
            if (autoClose.isValue()) {
                closeTimer.reset();
                phase = Phase.CLOSING;
            } else {
                resetState();
            }
        }
    }

    /** Ждём паузу, затем закрываем экран */
    private void handleClosing() {
        if (!closeTimer.finished(closeDelay.getValue())) return;

        if (mc.currentScreen instanceof GenericContainerScreen) {
            mc.player.closeScreen();
        }
        resetState();
    }

    // ─── Утилиты ──────────────────────────────────────────────────────────────

    /** Открыт ли сундук с названием содержащим "мистический" */
    private boolean isMysticalChestOpen() {
        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return false;
        return screen.getTitle().getString().toLowerCase().contains(CHEST_FILTER);
    }

    /** Первый непустой слот сундука (не инвентарь игрока) */
    @Native
    private Slot nextChestSlot() {
        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return null;

        return screen.getScreenHandler().slots.stream()
                .filter(s -> s.hasStack() && !s.inventory.equals(mc.player.getInventory()))
                .findFirst()
                .orElse(null);
    }

    /** Cooldown пороха — признак что событие ещё не закончилось */
    private boolean isCooldownActive() {
        return mc.player.getItemCooldownManager().isCoolingDown(Items.GUNPOWDER.getDefaultStack());
    }

    private void log(String message) {
        if (notifications.isValue() && mc.player != null) {
            mc.player.sendMessage(Text.of(message), false);
        }
    }
}
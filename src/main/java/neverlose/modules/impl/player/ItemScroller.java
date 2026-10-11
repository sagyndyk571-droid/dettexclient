package neverlose.modules.impl.player;

import com.adl.nativeprotect.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.ClickSlotEvent;
import neverlose.events.impl.HandledScreenEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.inventory.InventoryUtils;
import neverlose.util.string.PlayerInteractionHelper;
import neverlose.util.timer.StopWatch;

import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ItemScroller extends ModuleStructure {
    StopWatch stopWatch = new StopWatch();

    SliderSettings scrollerSetting = new SliderSettings("Задержка прокрутки предметов", "Выберите задержку прокрутки предметов")
            .setValue(50).range(0, 200);

    public ItemScroller() {
        super("ItemScroller", "Item Scroller", ModuleCategory.PLAYER);
        settings(scrollerSetting);
    }

    @EventHandler
   @Native
    public void onHandledScreen(HandledScreenEvent e) {
        if (mc.player == null) return;

        Slot hoverSlot = e.getSlotHover();
        SlotActionType actionType = getActionType();

        if (PlayerInteractionHelper.isKey(mc.options.sneakKey)
                && !PlayerInteractionHelper.isKey(mc.options.sprintKey)
                && hoverSlot != null
                && hoverSlot.hasStack()
                && actionType != null
                && stopWatch.every(scrollerSetting.getValue())) {

            InventoryUtils.click(
                    hoverSlot.id,
                    actionType.equals(SlotActionType.THROW) ? 1 : 0,
                    actionType
            );
        }
    }

    @Native
    private SlotActionType getActionType() {
        return PlayerInteractionHelper.isKey(mc.options.dropKey)
                ? SlotActionType.THROW
                : PlayerInteractionHelper.isKey(mc.options.attackKey)
                ? SlotActionType.QUICK_MOVE
                : null;
    }

    @EventHandler
   @Native
    public void onClickSlot(ClickSlotEvent e) {
        if (mc.player == null) return;

        int slotId = e.getSlotId();
        if (slotId < 0 || slotId >= mc.player.currentScreenHandler.slots.size()) return;

        Slot slot = mc.player.currentScreenHandler.getSlot(slotId);
        Item item = slot.getStack().getItem();

        if (item != null
                && PlayerInteractionHelper.isKey(mc.options.sneakKey)
                && PlayerInteractionHelper.isKey(mc.options.sprintKey)
                && stopWatch.every(50)) {

            processSlotClick(slot, item, e);
        }
    }

   @Native
    private void processSlotClick(Slot slot, Item item, ClickSlotEvent e) {
        getSlots()
                .filter(s -> s.getStack().getItem().equals(item) && s.inventory.equals(slot.inventory))
                .forEach(s -> InventoryUtils.click(s.id, 1, e.getActionType()));
    }

    private Stream<Slot> getSlots() {
        return mc.player.currentScreenHandler.slots.stream();
    }
}
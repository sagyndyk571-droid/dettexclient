package neverlose.modules.impl.player;

import com.adl.nativeprotect.Native;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.PlayerCollisionEvent;
import neverlose.events.impl.PushEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.MultiSelectSetting;

public class NoPush extends ModuleStructure {

    MultiSelectSetting ignoreSetting = new MultiSelectSetting("Игнорировать", "")
            .value("Вода", "Блоки", "Коллизию сущностей", "Рыхлый снег", "Ягоды");

    public NoPush() {
        super("AntiPush", "Anti Push", ModuleCategory.PLAYER);
        settings(ignoreSetting);
    }

    @EventHandler
   @Native
    public void onPush(PushEvent e) {
        switch (e.getType()) {
            case PushEvent.Type.COLLISION -> e.setCancelled(ignoreSetting.isSelected("Коллизию сущностей"));
            case PushEvent.Type.WATER -> e.setCancelled(ignoreSetting.isSelected("Вода"));
            case PushEvent.Type.BLOCK -> e.setCancelled(ignoreSetting.isSelected("Блоки"));
        }
    }

    @EventHandler
   @Native
    public void onPlayerCollision(PlayerCollisionEvent e) {
        Block block = e.getBlock();
        if (block.equals(Blocks.POWDER_SNOW)) e.setCancelled(ignoreSetting.isSelected("Рыхлый снег"));
        else if (block.equals(Blocks.SWEET_BERRY_BUSH)) e.setCancelled(ignoreSetting.isSelected("Ягоды"));
    }
}
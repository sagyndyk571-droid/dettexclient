package neverlose.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.TickEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.util.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ItemPhysic extends ModuleStructure {
    public static ItemPhysic getInstance() {
        return Instance.get(ItemPhysic.class);
    }

    public SelectSetting mode = new SelectSetting("Физика", "").value("Обычная").selected("Обычная");

    public ItemPhysic() {
        super("ItemPhysic", "Item Physic", ModuleCategory.RENDER);
//        setup(mode);
    }

    @EventHandler
    public void onTick(TickEvent e) {
    }
}
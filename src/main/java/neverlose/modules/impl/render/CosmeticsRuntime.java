package neverlose.modules.impl.render;

import neverlose.cosmetics.CosmeticManager;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.TickEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;

public class CosmeticsRuntime extends ModuleStructure {
    public CosmeticsRuntime() {
        super("CosmeticsRuntime", "Cosmetics animation runtime", ModuleCategory.RENDER);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        CosmeticManager manager = CosmeticManager.getInstance();
        manager.ensureInitialized();
        if (manager.isEnabled() && manager.hasAnyActive() && mc.player != null) {
            manager.getAnimator().tick(mc.player);
        }
    }
}

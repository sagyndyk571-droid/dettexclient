package neverlose.modules.impl.render;

import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.util.hmi.HandMyItemsRuntime;

public class HandMyItems extends ModuleStructure {
    public HandMyItems() {
        super("HandMyItems", "HoldMyItems embedded runtime", ModuleCategory.RENDER);
    }

    @Override
    public void activate() {
        HandMyItemsRuntime.enable();
    }

    @Override
    public void deactivate() {
        HandMyItemsRuntime.disable();
    }
}

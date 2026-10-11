package neverlose.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.EntityColorEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.ColorUtil;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeeInvisible extends ModuleStructure {

    SliderSettings alphaSetting = new SliderSettings("Прозрачность", "Прозрачность игрока").setValue(0.5f).range(0.1F, 1);

    public SeeInvisible() {
        super("SeeInvisible", "See Invisible", ModuleCategory.RENDER);
        settings(alphaSetting);
    }

    @EventHandler
    public void onEntityColor(EntityColorEvent e) {
        e.setColor(ColorUtil.multAlpha(e.getColor(), alphaSetting.getValue()));
        e.cancel();
    }

}

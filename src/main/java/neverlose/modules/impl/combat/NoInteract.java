package neverlose.modules.impl.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.util.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoInteract extends ModuleStructure {
    public static NoInteract getInstance() {
        return Instance.get(NoInteract.class);
    }

    public NoInteract() {
        super("NoInteract", "No Interact", ModuleCategory.COMBAT);
    }
}

package neverlose.modules.module.category;

import lombok.Getter;

@Getter
public enum ModuleCategory {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    PLAYER("Player"),
    MISC("Misc"),
    AUTOBUY("AutoBuy"),
    THEME("Theme"),
    SCRIPT("Script"),
    COSMETICS("Cosmetics"),
    CONFIG("Config");

    final String readableName;

    ModuleCategory(String readableName) {
        this.readableName = readableName;
    }

    public String getName() {
        return readableName;
    }
}

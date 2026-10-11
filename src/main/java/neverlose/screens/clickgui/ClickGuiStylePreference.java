package neverlose.screens.clickgui;

import neverlose.modules.module.category.ModuleCategory;

public final class ClickGuiStylePreference {

    public enum Mode {
        WEXSIDE
    }

    private static volatile Mode mode = Mode.WEXSIDE;
    private static volatile ModuleCategory lastCategory = ModuleCategory.COMBAT;
    private static volatile boolean glassStyle = false;

    private ClickGuiStylePreference() {}

    public static Mode getMode() {
        return mode;
    }

    public static String getModeLabel() {
        return "WexSide";
    }

    public static boolean isGlassStyle() {
        return glassStyle;
    }

    public static void toggleGlassStyle() {
        glassStyle = !glassStyle;
    }

    public static ModuleCategory getLastCategory() {
        return lastCategory;
    }

    public static void setLastCategory(ModuleCategory category) {
        if (category != null && category != ModuleCategory.AUTOBUY) {
            lastCategory = category;
        }
    }
}

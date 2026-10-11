package neverlose.util.render.font;

import java.util.LinkedHashMap;
import java.util.Map;

public class Fonts {

    private static final Map<String, String> FONT_REGISTRY = new LinkedHashMap<>();

    public static final Font BOLD = register("bold", "bold");
    public static final Font ICONS = register("icons", "icons");
    public static final Font ICONS1 = register("icons1", "icons1");
    public static final Font ICONSTYPETHO = register("iconstypetho", "iconstypetho");
    public static final Font GUI_ICONS = register("guiicons", "guiicons");
    public static final Font HUD_ICONS = register("hudicons", "hudicons");
    public static final Font FONT67 = register("sfuidisplay_heavy67", "sfuidisplay_heavy67");
    public static final Font FONT677 = register("sf6767", "sf6767");
    public static final Font FONT6777 = register("fon6767", "fon6767");
    public static final Font iconsnew = register("font", "font");
    public static final Font CATEGORY_ICONS = register("categoryicons", "categoryicons");
    public static final Font DEFAULT = register("default", "default");
    public static final Font REGULAR = register("regular", "regular");
    public static final Font TEST = register("test", "test");
    public static final Font NLEZ = register("nlez", "nlez");
    public static final Font INTER_SEMIBOLD = register("inter_semibold", "inter_semibold");
    public static final Font INTER_MEDIUM = register("inter_medium", "inter_medium");
    /** Roboto Regular — тот же TTF, что в Celestial (`celestial/font/robotoregular.ttf`), в MSDF. */
    public static final Font CELESTIAL_ROBOTO = register("celestial_roboto", "celestial_roboto");
    public static final Font INTER = register("inter", "inter");
    public static final Font NEVERLOSE = register("neverlose", "neverlose");
    public static final Font ICONS2 = register("icons2", "icons2");
    public static final Font iconswex = register("iconswex", "iconswex");
    public static final Font mntsb = register("mntsb", "mntsb");
    public static final Font mntsr = register("mntsr", "mntsr");
    public static final Font REGULARNEW = register("regularnew", "regularnew");
    public static final Font MAINMENUSCREEN = register("mainmenuicons", "mainmenuicons");
    public static final Font SF_BOLD = register("sf_bold", "sf_bold.ttf");

    private static Font register(String name, String path) {
        FONT_REGISTRY.put(name, path);
        return new Font(name);
    }

    public static Map<String, String> getRegistry() {
        return FONT_REGISTRY;
    }

    private Fonts() {}
}
package neverlose.screens.clickgui.impl.module.render;

/**
 * Константы двухколоночной сетки WexSide. Раскладка и отрисовка — {@link WexsideTwoColumnLayout}, {@link ModuleComponent}.
 */
public final class WexsideTwoColumnRenderer {

    public static final float COL_LEFT = 8f;
    public static final float COL_RIGHT = 202f;
    public static final float COL_W = 186f;
    public static final float ROW_H = 32f;
    /** Высота верхней части карточки (название). */
    public static final float CARD_TOP_H = 12f;    // 15 * 0.8
    /** Высота нижней части карточки (подпись + тумблер). */
    public static final float CARD_BOTTOM_H = 12f; // 15 * 0.8
    /** Вертикальный зазор между карточками модулей в колонке. */
    public static final float ROW_GAP = 8f;
    public static final float CONTENT_TOP = 6f;

    private WexsideTwoColumnRenderer() {}
}

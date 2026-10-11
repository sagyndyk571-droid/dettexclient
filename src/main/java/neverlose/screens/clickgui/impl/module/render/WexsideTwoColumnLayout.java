package neverlose.screens.clickgui.impl.module.render;

import neverlose.modules.module.ModuleStructure;

import java.util.List;

/**
 * Две независимые вертикальные колонки: левая и правая стеки не связаны по высоте — при раскрытии слева справа нет пустоты.
 */
public final class WexsideTwoColumnLayout {

    /** Макс. видимая высота списка настроек внутри карточки (остальное — скролл). */
    public static final float MAX_INLINE_SETTINGS_H = 400f;  // 500 * 0.8

    public enum HitPart {
        HEADER,
        SETTINGS,
        OUTSIDE
    }

    public static final class HitResult {
        public final ModuleStructure module;
        public final HitPart part;
        public final float cardX;
        public final float cardY;
        public final float cardW;
        public final float cardH;

        public HitResult(ModuleStructure module, HitPart part, float cardX, float cardY, float cardW, float cardH) {
            this.module = module;
            this.part = part;
            this.cardX = cardX;
            this.cardY = cardY;
            this.cardW = cardW;
            this.cardH = cardH;
        }
    }

    private WexsideTwoColumnLayout() {}

    public static float inlineExtraHeight(float totalSettingsHeight) {
        if (totalSettingsHeight <= 0) {
            return 12f;
        }
        // Ограничиваем высоту настроек максимальным значением
        float inner = Math.min(totalSettingsHeight, MAX_INLINE_SETTINGS_H);
        return inner;
    }

    public static float cellHeight(ModuleStructure m, ModuleStructure selected, float totalSettingsHeight) {
        // Модули открываются только если у них есть настройки
        if (m.settings() == null || m.settings().isEmpty()) {
            return WexsideTwoColumnRenderer.ROW_H; // Только заголовок, без настроек
        }
        
        // Вычисляем реальную высоту настроек для этого модуля
        // Используем переданную высоту настроек, но ограничиваем максимумом
        float settingsHeight = Math.min(totalSettingsHeight, MAX_INLINE_SETTINGS_H);
        
        // Если настроек нет или высота 0 - возвращаем только заголовок
        if (settingsHeight <= 0) {
            return WexsideTwoColumnRenderer.ROW_H;
        }
        settingsHeight += 1f;
        
        // Возвращаем высоту заголовка + реальная высота настроек + отступы
        return WexsideTwoColumnRenderer.ROW_H + settingsHeight + 4f; // 12 * 0.8 - отступы сверху/снизу
    }

    public static float computeTotalContentHeight(List<ModuleStructure> modules, ModuleStructure selected,
                                                  float totalSettingsHeight) {
        int n = modules.size();
        if (n == 0) return 0;
        float leftY = 0f;
        float rightY = 0f;
        for (int i = 0; i < n; i++) {
            float h = cellHeight(modules.get(i), selected, totalSettingsHeight);
            if (i % 2 == 0) {
                leftY += h;
                if (i + 2 < n) leftY += WexsideTwoColumnRenderer.ROW_GAP;
            } else {
                rightY += h;
                if (i + 2 < n) rightY += WexsideTwoColumnRenderer.ROW_GAP;
            }
        }
        return Math.max(leftY, rightY);
    }

    /**
     * Позиции карточек: чётные индексы — левая колонка, нечётные — правая (независимые стеки по Y).
     */
    public static void computeCellPositions(List<ModuleStructure> modules, ModuleStructure selected,
                                            float totalSettingsHeight, float contentY, float scrollF,
                                            float[] outCellY, float[] outCellH) {
        int n = modules.size();
        float leftY = contentY + scrollF;
        float rightY = contentY + scrollF;
        for (int i = 0; i < n; i++) {
            float h = cellHeight(modules.get(i), selected, totalSettingsHeight);
            if (i % 2 == 0) {
                outCellY[i] = leftY;
                outCellH[i] = h;
                leftY += h;
                if (i + 2 < n) leftY += WexsideTwoColumnRenderer.ROW_GAP;
            } else {
                outCellY[i] = rightY;
                outCellH[i] = h;
                rightY += h;
                if (i + 2 < n) rightY += WexsideTwoColumnRenderer.ROW_GAP;
            }
        }
    }

    public static HitResult hitTest(double mx, double my, float contentY, float contentH, float scrollF,
                                    List<ModuleStructure> modules, ModuleStructure selected,
                                    float[] moduleSettingsHeights, float panelX) {
        if (modules.isEmpty()) return null;
        int n = modules.size();
        float[] cellY = new float[n];
        float[] cellH = new float[n];
        
        // Вычисляем позиции ячеек с учетом высоты настроек каждого модуля
        float leftY = contentY + scrollF;
        float rightY = contentY + scrollF;
        for (int i = 0; i < n; i++) {
            float moduleSettingsHeight = i < moduleSettingsHeights.length ? moduleSettingsHeights[i] : 0f;
            float h = cellHeight(modules.get(i), selected, moduleSettingsHeight);
            if (i % 2 == 0) {
                cellY[i] = leftY;
                cellH[i] = h;
                leftY += h;
                if (i + 2 < n) leftY += WexsideTwoColumnRenderer.ROW_GAP;
            } else {
                cellY[i] = rightY;
                cellH[i] = h;
                rightY += h;
                if (i + 2 < n) rightY += WexsideTwoColumnRenderer.ROW_GAP;
            }
        }

        for (int i = 0; i < n; i++) {
            float x = panelX + (i % 2 == 0 ? WexsideTwoColumnRenderer.COL_LEFT : WexsideTwoColumnRenderer.COL_RIGHT);
            float cy = cellY[i];
            float ch = cellH[i];
            float w = WexsideTwoColumnRenderer.COL_W;
            if (mx < x || mx > x + w || my < cy || my > cy + ch) continue;
            if (my < contentY || my > contentY + contentH) continue;

            ModuleStructure m = modules.get(i);
            // Название модуля занимает 10px сверху (12 * 0.8)
            float nameHeight = 10f;
            // Карточка начинается после названия
            float cardY = cy + nameHeight;
            // Заголовок карточки (Enabled + toggle) занимает примерно 18px (23 * 0.8)
            float headerHeight = 18f;
            float headerBottom = cardY + headerHeight;
            
            // Если клик в области названия или заголовка карточки
            if (my < headerBottom) {
                return new HitResult(m, HitPart.HEADER, x, cardY, w, ch - nameHeight);
            }
            
            // Если клик ниже заголовка и у модуля есть настройки - это клик по настройкам
            if (ch > WexsideTwoColumnRenderer.ROW_H && m.settings() != null && !m.settings().isEmpty()) {
                return new HitResult(m, HitPart.SETTINGS, x, cardY, w, ch - nameHeight);
            }
            
            return new HitResult(m, HitPart.HEADER, x, cardY, w, ch - nameHeight);
        }
        return null;
    }
}

package neverlose.screens.clickgui.impl.module.render;

import net.minecraft.client.gui.DrawContext;
import neverlose.modules.module.ModuleStructure;
import neverlose.screens.clickgui.ClickGuiStylePreference;
import neverlose.util.render.Render2D;
import neverlose.util.render.shader.Scissor;
import neverlose.util.render.font.Fonts;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders modules as a 2-column grid of cards (wexside-style).
 * Each card: title, "Enabled" / "Missing description", toggle on the right.
 */
public class ModuleGridRenderer {

    // отступ от левой границы контента, чтобы карточки модулей были чуть дальше от разделителя
    private static final float CARD_PAD = 10f;
    private static final float CARD_GAP = 9f;
    private static final float CARD_RADIUS = 20;
    private static final float CARD_HEIGHT = 52f;
    // чуть меньше переключатель, как просили
    private static final float TOGGLE_W = 16f;
    private static final float TOGGLE_H = 11f;
    private static final float TOGGLE_R = 5f;
    // Сдвиг тумблера вниз, чтобы был на уровне иконки (кисточки)
    private static final float TOGGLE_OFFSET_Y = 9.5f;
    private static final Color CARD_BG = new Color(32, 34, 46);
    private static final Color CARD_BORDER = new Color(48, 50, 62);
    private static final Color CARD_BG_GLASS = new Color(10, 16, 21);
    private static final Color CARD_BORDER_GLASS = new Color(58, 82, 112);
    private static final Color TOGGLE_OFF_BG = new Color(55, 57, 69);
    private static final Color TOGGLE_ON_BG = new Color(135, 140, 255);
    private static final Color TOGGLE_OFF_BG_GLASS = new Color(38, 49, 60);
    private static final Color TOGGLE_ON_BG_GLASS = new Color(91, 132, 255);
    /** Размер иконки шестерёнки под первой буквой названия при наведении */
    private static final float GEAR_SIZE = 8f;
    private static final float GEAR_OFFSET_Y = 23.5f; // отступ под строкой названия
    private static final float TOGGLE_ANIM_SPEED = 12f;

    private final Map<ModuleStructure, Float> knobAnimations = new HashMap<>();

    private static int clampU8(float value) {
        int v = Math.round(value);
        return Math.max(0, Math.min(255, v));
    }

    public void render(DrawContext context, List<ModuleStructure> modules,
                      float x, float y, float width, float height,
                      float mouseX, float mouseY, float delta, double scrollOffset, int guiScale, float alphaMultiplier) {

        int alpha = clampU8(255 * alphaMultiplier);
        int cols = 2;
        float cardWidth = (width - CARD_PAD * 2 - CARD_GAP) / cols;
        float contentTop = y + CARD_PAD - 12f ;
        float contentH = height - CARD_PAD + 18f ;

        Scissor.enable(x, contentTop, width , contentH, guiScale);

        float rowHeight = CARD_HEIGHT + CARD_GAP;
        int rows = (int) Math.ceil((double) modules.size() / cols);
        float totalHeight = rows * rowHeight - CARD_GAP;
        float startY = contentTop + (float) scrollOffset;

        for (int i = 0; i < modules.size(); i++) {
            ModuleStructure mod = modules.get(i);
            int col = i % cols;
            int row = i / cols;
            float cardX = x + CARD_PAD + col * (cardWidth + CARD_GAP);
            float cardY = startY + row * rowHeight;

            if (cardY + CARD_HEIGHT < contentTop || cardY > contentTop + contentH) continue;

            boolean hovered = mouseX >= cardX && mouseX <= cardX + cardWidth && mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT;
            boolean glass = ClickGuiStylePreference.isGlassStyle();
            Color cardBg = glass ? CARD_BG_GLASS : CARD_BG;
            Color cardBorder = glass ? CARD_BORDER_GLASS : CARD_BORDER;
            int bgAlpha = clampU8((glass ? (hovered ? 150 : 122) : (hovered ? 55 : 45)) * alphaMultiplier);
            int borderAlpha = glass ? clampU8((hovered ? 118 : 78) * alphaMultiplier) : alpha;
            Render2D.rect(cardX, cardY, cardWidth, CARD_HEIGHT, new Color(cardBg.getRed(), cardBg.getGreen(), cardBg.getBlue(), bgAlpha).getRGB(), CARD_RADIUS);
            Render2D.outline(cardX, cardY, cardWidth, CARD_HEIGHT, 0.5f, new Color(cardBorder.getRed(), cardBorder.getGreen(), cardBorder.getBlue(), borderAlpha).getRGB(), CARD_RADIUS);

            float textX = cardX + 8f;
            float titleY = cardY + 10f;
            // название функции чуть крупнее
            Fonts.INTER_SEMIBOLD.draw(mod.getName(), textX, titleY, 8f, new Color(255, 255, 255, alpha).getRGB());

            // иконка бинда (кисточка) появляется при наведении на карточку; по клику открывается меню (Bind, Visible)
            if (!mod.getName().isEmpty() && hovered) {
                float toggleX = cardX + cardWidth - TOGGLE_W - 8f;
                float gearX = toggleX - GEAR_SIZE - 6f;
                float gearY = titleY + GEAR_OFFSET_Y - 2f;
                int gearAlpha = clampU8(200 * alphaMultiplier);

                // вертикальная серая полоска слева от кисточки


                // вместо шестерёнки используем иконку-кисточку из ICONS1
                Fonts.ICONS1.draw("A", gearX, gearY, GEAR_SIZE, new Color(200, 200, 210, gearAlpha).getRGB());
            }

            String status = mod.isState() ? "Enabled" : "Disabled";
            String desc = mod.getDescription() != null && !mod.getDescription().isEmpty() ? mod.getDescription() : "Missing description";
            if (desc.length() > 28) desc = desc.substring(0, 25) + "...";
            Fonts.INTER_MEDIUM.draw(status + ", " + desc, textX, cardY + 26f, 5f, new Color(140, 140, 150, alpha).getRGB());

            float toggleX = cardX + cardWidth - TOGGLE_W - 8f;
            float toggleY = cardY + (CARD_HEIGHT - TOGGLE_H) / 2f + TOGGLE_OFFSET_Y;
            float knobProgress = getKnobProgress(mod, delta);
            renderToggle(toggleX, toggleY, mod.isState(), knobProgress, alphaMultiplier);
        }

        Scissor.disable();
    }

    private float getKnobProgress(ModuleStructure mod, float delta) {
        float target = mod.isState() ? 1f : 0f;
        float current = knobAnimations.getOrDefault(mod, target);
        float speed = TOGGLE_ANIM_SPEED * Math.min(delta, 0.1f);
        float next = current + (target - current) * speed;
        if (Math.abs(next - target) < 0.001f) next = target;
        next = Math.max(0f, Math.min(1f, next));
        knobAnimations.put(mod, next);
        return next;
    }

    private void renderToggle(float x, float y, boolean on, float knobProgress, float alphaMultiplier) {
        int alpha = clampU8(255 * alphaMultiplier);
        boolean glass = ClickGuiStylePreference.isGlassStyle();
        Color trackColor = on
                ? (glass ? TOGGLE_ON_BG_GLASS : TOGGLE_ON_BG)
                : (glass ? TOGGLE_OFF_BG_GLASS : TOGGLE_OFF_BG);
        // y уже рассчитан как центрированный toggleY в render(), без дополнительных смещений
        Render2D.rect(x, y, TOGGLE_W, TOGGLE_H,
                new Color(trackColor.getRed(), trackColor.getGreen(), trackColor.getBlue(), alpha).getRGB(), TOGGLE_R);

        float knobSize = TOGGLE_H - 4.2f;
        float padding = 2f;
        float travelDist = TOGGLE_W - knobSize - padding * 2f;
        float knobX = x + padding + travelDist * knobProgress;
        float knobY = y + (TOGGLE_H - knobSize) / 2f;
        int r = Math.max(0, Math.min(255, (int) (150 + (255 - 150) * knobProgress)));
        int g = Math.max(0, Math.min(255, (int) (150 + (255 - 150) * knobProgress)));
        int b = Math.max(0, Math.min(255, (int) (160 + (255 - 160) * knobProgress)));
        int a = Math.max(0, Math.min(255, alpha));
        Color knobColor = new Color(r, g, b, a);

        Render2D.rect(knobX, knobY, knobSize, knobSize, knobColor.getRGB(), knobSize / 2f);
    }

    public ModuleStructure getModuleAtPosition(List<ModuleStructure> modules, double mouseX, double mouseY,
                                               float x, float y, float width, float height, double scrollOffset) {
        float contentTop = y + CARD_PAD - 12f;
        float contentH = height - CARD_PAD + 18f;
        int cols = 2;
        float cardWidth = (width - CARD_PAD * 2 - CARD_GAP) / cols;
        float rowHeight = CARD_HEIGHT + CARD_GAP;
        float startY = contentTop + (float) scrollOffset;

        for (int i = 0; i < modules.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            float cardX = x + CARD_PAD + col * (cardWidth + CARD_GAP);
            float cardY = startY + row * rowHeight;
            if (mouseX >= cardX && mouseX <= cardX + cardWidth && mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT) {
                return modules.get(i);
            }
        }
        return null;
    }

    public boolean isToggleClicked(double mouseX, double mouseY, float cardX, float cardY, float cardWidth) {
        float toggleX = cardX + cardWidth - TOGGLE_W - 8f;
        float toggleY = cardY + (CARD_HEIGHT - TOGGLE_H) / 2f + TOGGLE_OFFSET_Y;
        return mouseX >= toggleX && mouseX <= toggleX + TOGGLE_W && mouseY >= toggleY && mouseY <= toggleY + TOGGLE_H;
    }

    /**
     * Проверяет, попал ли клик по иконке шестерёнки (под первой буквой названия).
     * Раскладка совпадает с render (contentTop, cardWidth 155f), чтобы клик совпадал с отрисовкой.
     */
    public ModuleStructure getModuleForGearClick(List<ModuleStructure> modules, double mouseX, double mouseY,
                                                 float x, float y, float width, float height, double scrollOffset) {
        int cols = 2;
        float cardWidth = (width - CARD_PAD * 2 - CARD_GAP) / cols;
        float contentTop = y + CARD_PAD - 12f;
        float contentH = height - CARD_PAD + 18f;
        float rowHeight = CARD_HEIGHT + CARD_GAP;
        float startY = contentTop + (float) scrollOffset;

        for (int i = 0; i < modules.size(); i++) {
            ModuleStructure mod = modules.get(i);
            if (mod.getName().isEmpty()) continue;
            int col = i % cols;
            int row = i / cols;
            float cardX = x + CARD_PAD + col * (cardWidth + CARD_GAP);
            float cardY = startY + row * rowHeight;

            if (cardY + CARD_HEIGHT < contentTop || cardY > contentTop + contentH) continue;

            float textX = cardX + 8f;
            float titleY = cardY + 10f;
            float toggleX = cardX + cardWidth - TOGGLE_W - 8f;
            float gearX = toggleX - GEAR_SIZE - 6f;
            float gearY = titleY + GEAR_OFFSET_Y - 2f;

            // хитбокс расширен чуть влево/вправо и по высоте, чтобы клик совпадал с кисточкой и полоской
            if (mouseX >= gearX - 4f && mouseX <= gearX + GEAR_SIZE + 2f
                    && mouseY >= gearY - 2f && mouseY <= gearY + GEAR_SIZE + 2f) {
                return mod;
            }
        }
        return null;
    }

    public static float getCardHeight() {
        return CARD_HEIGHT + CARD_GAP;
    }

    public static int getColumns() {
        return 2;
    }
}

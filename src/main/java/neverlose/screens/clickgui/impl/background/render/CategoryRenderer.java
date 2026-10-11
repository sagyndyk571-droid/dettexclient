package neverlose.screens.clickgui.impl.background.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.screens.clickgui.impl.background.BackgroundComponent;
import neverlose.util.render.Render2D;
import neverlose.util.render.font.Fonts;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class CategoryRenderer {

    // Группы категорий как в Neverlose
    private static final String[] CATEGORY_GROUPS = {"AIMBOT", "COMMON", "EXTENSIONS"};
    
    // AIMBOT группа
    private static final ModuleCategory[] AIMBOT_CATEGORIES = {
            ModuleCategory.COMBAT  // Rage
    };
    private static final String[] AIMBOT_NAMES = {"Rage"};
    private static final String[] AIMBOT_ICONS = {"C"};
    
    // COMMON группа
    private static final ModuleCategory[] COMMON_CATEGORIES = {
            ModuleCategory.RENDER, ModuleCategory.PLAYER, ModuleCategory.MISC
    };
    private static final String[] COMMON_NAMES = {"Visuals", "Inventory", "Miscellaneous"};
    private static final String[] COMMON_ICONS = {"E", "A", "G"};
    
    // EXTENSIONS группа
    private static final ModuleCategory[] EXTENSIONS_CATEGORIES = {
            ModuleCategory.MOVEMENT, ModuleCategory.AUTOBUY, ModuleCategory.COSMETICS
    };
    private static final String[] EXTENSIONS_NAMES = {"Movement", "AutoBuy", "Cosmetics"};
    private static final String[] EXTENSIONS_ICONS = {"U", "B", "S"};

    private final Map<ModuleCategory, Float> categoryAnimations = new HashMap<>();

    private static final float ANIMATION_SPEED = 8f;
    private static final float ITEM_HEIGHT = 16f;  // 20 * 0.8
    private static final float TEXT_SIZE = 5.8f;  // 7 * 0.8
    private static final float ICON_SIZE = 6.4f;  // 8 * 0.8
    private static final float ICON_SPACING = 4f;  // 5 * 0.8
    private static final float SIDEBAR_PAD = 8f;  // 10 * 0.8
    private static final float LOGO_SIZE = 24f;  // 30 * 0.8
    private static final float LOGO_Y = 8f;  // 10 * 0.8
    /** Начальная Y позиция списка категорий (уменьши — сдвинешь вверх, увеличишь — вниз). */
    private static final float CATEGORIES_START_Y = 46.4f;  // 58 * 0.8
    private static final float ITEM_RADIUS = 3.2f;  // 4 * 0.8
    // цвет клиента — только обводка, иконка и название выбранной категории (фон не меняем)
    private static Color CLIENT_COLOR = new Color(91, 132, 255);
    // HSB-составляющие клиентского цвета для Colorpicker
    private static float clientHue = 0f;
    private static float clientSat = 0.7f;
    private static float clientBri = 0.9f;
    // иконка «6 точек» — уменьшенный размер, сетка 2×3 со свечением
    private static final float DOT_GRID_WIDTH = 3.2f;  // 4 * 0.8
    private static final float DOT_GRID_HEIGHT = 4f;  // 5 * 0.8
    private static final float DOT_RADIUS = 0.36f;  // 0.45 * 0.8
    private static final float DOT_SPACING_X = 1.12f;  // 1.4 * 0.8
    private static final float DOT_SPACING_Y = 1.04f;  // 1.3 * 0.8
    private static final float DOT_GLOW_RADIUS = 0.68f;  // 0.85 * 0.8
    private static final Color DOT_COLOR = new Color(160, 165, 175);
    // фон под StabiliZator: RGB (28,31,40) / #1c1f28
    private static final Color FOOTER_BG = new Color(28, 31, 40);

    public CategoryRenderer() {
        for (ModuleCategory cat : AIMBOT_CATEGORIES) {
            categoryAnimations.put(cat, 0f);
        }
        for (ModuleCategory cat : COMMON_CATEGORIES) {
            categoryAnimations.put(cat, 0f);
        }
        for (ModuleCategory cat : EXTENSIONS_CATEGORIES) {
            categoryAnimations.put(cat, 0f);
        }
        // инициализируем HSB из стартового CLIENT_COLOR
        syncHsbFromClientColor();
    }

    public static Color getClientColor() {
        return CLIENT_COLOR;
    }

    private static void syncHsbFromClientColor() {
        float[] hsb = Color.RGBtoHSB(CLIENT_COLOR.getRed(), CLIENT_COLOR.getGreen(), CLIENT_COLOR.getBlue(), null);
        clientHue = hsb[0];
        clientSat = hsb[1];
        clientBri = hsb[2];
    }

    private static void updateClientColorFromHsb() {
        int rgb = Color.HSBtoRGB(clientHue, clientSat, clientBri);
        CLIENT_COLOR = new Color(rgb);
    }

    public void updateAnimations(ModuleCategory selectedCategory, float deltaTime) {
        for (ModuleCategory cat : AIMBOT_CATEGORIES) {
            updateCategoryAnimation(cat, selectedCategory, deltaTime);
        }
        for (ModuleCategory cat : COMMON_CATEGORIES) {
            updateCategoryAnimation(cat, selectedCategory, deltaTime);
        }
        for (ModuleCategory cat : EXTENSIONS_CATEGORIES) {
            updateCategoryAnimation(cat, selectedCategory, deltaTime);
        }
    }

    private void updateCategoryAnimation(ModuleCategory cat, ModuleCategory selected, float deltaTime) {
        float target = cat == selected ? 1f : 0f;
        float current = categoryAnimations.getOrDefault(cat, 0f);
        float diff = target - current;
        float change = diff * ANIMATION_SPEED * deltaTime;
        if (Math.abs(diff) < 0.001f) {
            categoryAnimations.put(cat, target);
        } else {
            categoryAnimations.put(cat, current + change);
        }
    }

    private static final float LOGO_TEXT_GAP = 10f;
    // чуть крупнее, чтобы надпись выглядела шире
    private static final float LOGO_TEXT_SIZE = 12f;

    // состояние UI выбора цвета клиента (квадратик справа в футере)
    private boolean clientPaletteOpen = false;
    private boolean clientPickerOpen = false;
    private float clientPickerOffsetX = 0f;
    private float clientPickerOffsetY = 0f;
    private boolean clientPickerDragging = false;
    private double pickerDragStartMouseX = 0;
    private double pickerDragStartMouseY = 0;
    private float pickerDragStartOffsetX = 0;
    private float pickerDragStartOffsetY = 0;

    // анимация для маленькой палитры (ряд кружков над футером)
    private float clientPaletteAnim = 0f; // 0 = скрыта, 1 = полностью видна
    private boolean clientPaletteOpening = false;
    private boolean clientPaletteClosing = false;
    private long lastPaletteAnimTime = System.currentTimeMillis();
    private static final float CLIENT_PICKER_W = 90.4f;
    private static final float CLIENT_PICKER_H = 118f;
    private static final float CLIENT_PICKER_GAP = 7.2f;

    public void render(DrawContext context, float bgX, float bgY, ModuleCategory selectedCategory, float alphaMultiplier) {
        int alpha = (int) (255 * alphaMultiplier);
        
        // Рендерим верхнюю часть как в Neverlose
        renderHeader(context, bgX, bgY, alphaMultiplier);
        
        renderCategoryGroups(bgX, bgY, selectedCategory, alphaMultiplier);
        renderFooter(context, bgX, bgY, alphaMultiplier);
    }
    
    private void renderHeader(DrawContext context, float bgX, float bgY, float alphaMultiplier) {
        int alpha = (int) (255 * alphaMultiplier);
        
        // Логотип "DC" слева - уменьшенный
        float logoSize = 25.6f;  // 32 * 0.8
        float logoX = bgX + 9.6f;  // 12 * 0.8
        float logoY = bgY + 9.6f;  // 12 * 0.8
        
        // Рендерим логотип с закругленными углами
        Render2D.rect(logoX, logoY, logoSize, logoSize,
                new Color(8, 18, 35, (int) (230 * alphaMultiplier)).getRGB(), 5f);
        Fonts.INTER_SEMIBOLD.drawCentered("DC", logoX + logoSize / 2f, logoY + 8.1f, 9f,
                new Color(235, 245, 255, (int) (235 * alphaMultiplier)).getRGB());
        
        // Текст справа от логотипа
        float textX = logoX + logoSize + 6.4f;  // 8 * 0.8
        float textY = logoY + 3.2f;  // 4 * 0.8
        
        // "DettexClient" - уменьшенный текст
        Fonts.INTER_SEMIBOLD.draw("DettexClient", textX, textY, 8f, new Color(255, 255, 255, alpha).getRGB());  // 10 * 0.8
        
        // "by GGpidor" - мелкий серый текст под ним
        Fonts.INTER_SEMIBOLD.draw("by GGpidor", textX, textY + 9.6f, 4.8f, new Color(140, 145, 155, alpha).getRGB());  // 12 * 0.8, 6 * 0.8
        
        // Разделительная линия под хедером
//        float lineY = bgY + 37.6f;  // 47 * 0.8
//        Render2D.rect(bgX + 8f, lineY, BackgroundComponent.SIDEBAR_WIDTH - 16f, 0.4f,   // 10 * 0.8, 20 * 0.8, 0.5 * 0.8
//                new Color(60, 65, 75, alpha).getRGB(), 0);
    }

    private void renderCategoryGroups(float bgX, float bgY, ModuleCategory selectedCategory, float alphaMultiplier) {
        int alpha = (int) (255 * alphaMultiplier);
        float currentY = bgY + CATEGORIES_START_Y;
        
        // AIMBOT группа
        Fonts.INTER_SEMIBOLD.draw("AIMBOT", bgX + 9.6f, currentY, 4.4f, new Color(120, 125, 135, alpha).getRGB());  // 12 * 0.8, 5.5 * 0.8
        currentY += 14.4f;  // 18 * 0.8
        for (int i = 0; i < AIMBOT_NAMES.length; i++) {
            ModuleCategory cat = AIMBOT_CATEGORIES[i];
            float anim = categoryAnimations.getOrDefault(cat, 0f);
            boolean active = cat == selectedCategory;
            renderCategoryItem(bgX + 4.8f, currentY, BackgroundComponent.SIDEBAR_WIDTH - 9.6f, ITEM_HEIGHT, AIMBOT_NAMES[i], AIMBOT_ICONS[i], active, anim, alphaMultiplier);  // 6 * 0.8, 12 * 0.8
            currentY += ITEM_HEIGHT + 1.6f;  // 2 * 0.8
        }
        currentY += 6.4f;  // 8 * 0.8
        
        // COMMON группа
        Fonts.INTER_SEMIBOLD.draw("COMMON", bgX + 9.6f, currentY, 4.4f, new Color(120, 125, 135, alpha).getRGB());
        currentY += 14.4f;
        for (int i = 0; i < COMMON_NAMES.length; i++) {
            ModuleCategory cat = COMMON_CATEGORIES[i];
            float anim = categoryAnimations.getOrDefault(cat, 0f);
            boolean active = cat == selectedCategory;
            renderCategoryItem(bgX + 4.8f, currentY, BackgroundComponent.SIDEBAR_WIDTH - 9.6f, ITEM_HEIGHT, COMMON_NAMES[i], COMMON_ICONS[i], active, anim, alphaMultiplier);
            currentY += ITEM_HEIGHT + 1.6f;
        }
        currentY += 6.4f;
        
        // EXTENSIONS группа
        Fonts.INTER_SEMIBOLD.draw("EXTENSIONS", bgX + 9.6f, currentY, 4.4f, new Color(120, 125, 135, alpha).getRGB());
        currentY += 14.4f;
        for (int i = 0; i < EXTENSIONS_NAMES.length; i++) {
            ModuleCategory cat = EXTENSIONS_CATEGORIES[i];
            float anim = categoryAnimations.getOrDefault(cat, 0f);
            boolean active = cat == selectedCategory;
            renderCategoryItem(bgX + 4.8f, currentY, BackgroundComponent.SIDEBAR_WIDTH - 9.6f, ITEM_HEIGHT, EXTENSIONS_NAMES[i], EXTENSIONS_ICONS[i], active, anim, alphaMultiplier);
            currentY += ITEM_HEIGHT + 1.6f;
        }
    }

    private void renderCategoryItem(float x, float y, float w, float h, String name, String icon, boolean active, float animation, float alphaMultiplier) {
        int alpha = (int) (255 * alphaMultiplier);
        if (active) {
            Render2D.rect(x, y - 0.4f, w, h - 0.8f,
                    new Color(35, 41, 52, (int) (220 * alphaMultiplier)).getRGB(), ITEM_RADIUS);
            Render2D.rect(x, y + 2.4f, 1.6f, h - 6f,
                    new Color(CLIENT_COLOR.getRed(), CLIENT_COLOR.getGreen(), CLIENT_COLOR.getBlue(), alpha).getRGB(), 1f);
        }

        if (active) {
            // обводка цветом клиента (фон не рисуем)
            Render2D.outline(x, y - 0.4f, w, h - 0.8f, 0.35f, new Color(74, 101, 150, (int) (120 * alphaMultiplier)).getRGB(), ITEM_RADIUS);  // 2 * 0.8, 0.3 * 0.8
            // точки слева от иконки
        }
        int iconColor = active ? new Color(CLIENT_COLOR.getRed(), CLIENT_COLOR.getGreen(), CLIENT_COLOR.getBlue(), alpha).getRGB() : new Color(160, 166, 177, (int) (210 * alphaMultiplier)).getRGB();
        int textColor = active ? new Color(238, 241, 248, alpha).getRGB() : new Color(178, 182, 191, (int) (225 * alphaMultiplier)).getRGB();
        float iconX = x + 7f;
        // Y иконки категории: y + (h - ICON_SIZE) / 2f — меняй ICON_SIZE или добавь смещение (+N / -N)
        Fonts.ICONS1.draw(icon, iconX + 4f, y + (h - ICON_SIZE) / 2f  , ICON_SIZE, iconColor);  // 5 * 0.8
        // Y названия категории: y + (h - 5f) / 2f — меняй 5f под высоту шрифта или добавь смещение
        float textX = iconX + ICON_SIZE + ICON_SPACING + 4.2f;
        Fonts.INTER_SEMIBOLD.draw(trimText(name, x + w - textX - 5f, TEXT_SIZE), textX, y + (h - 5.6f) / 2f - 0.8f, TEXT_SIZE, textColor);  // 4 * 0.8, 7 * 0.8, 1 * 0.8
    }

    private String trimText(String text, float maxWidth, float size) {
        if (text == null) return "";
        if (Fonts.INTER_SEMIBOLD.getWidth(text, size) <= maxWidth) return text;
        String value = text;
        while (value.length() > 1 && Fonts.INTER_SEMIBOLD.getWidth(value + "..", size) > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        return value + "..";
    }

    /** Рисует иконку из 6 точек в сетке 2×3 с мягким свечением (как на референсе). */
    private static void drawSixDotIcon(float leftX, float topY, int color) {
        int glowAlpha = (color >> 24) & 0xFF;
        int glowColor = (glowAlpha / 3 << 24) | (color & 0x00FFFFFF);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 2; col++) {
                float cx = leftX + DOT_RADIUS + col * (DOT_SPACING_X + 2f * DOT_RADIUS);
                float cy = topY + DOT_RADIUS + row * (2f * DOT_RADIUS + DOT_SPACING_Y);
                // свечение — больший полупрозрачный круг
                float glowSize = DOT_GLOW_RADIUS * 2f;
                Render2D.rect(cx - DOT_GLOW_RADIUS, cy - DOT_GLOW_RADIUS, glowSize, glowSize, glowColor, DOT_GLOW_RADIUS);
                // сама точка
                float dotSize = DOT_RADIUS * 2f;
                Render2D.rect(cx - DOT_RADIUS, cy - DOT_RADIUS, dotSize, dotSize, color, DOT_RADIUS);
            }
        }
    }

    private void renderFooter(DrawContext context, float bgX, float bgY, float alphaMultiplier) {
        float footerY = bgY + BackgroundComponent.BG_HEIGHT - 30.4f;  // 38 * 0.8
        int alpha = (int) (255 * alphaMultiplier);
        float footerW = BackgroundComponent.SIDEBAR_WIDTH - 9.6f;  // 12 * 0.8
        float footerX = bgX + 4.8f;  // 6 * 0.8

        Render2D.rect(bgX + 8f, footerY - 6f, BackgroundComponent.SIDEBAR_WIDTH - 16f, 0.45f,
                new Color(34, 45, 55, (int) (105 * alphaMultiplier)).getRGB(), 0);
        Render2D.rect(footerX, footerY - 1f, footerW, 29f,
                new Color(7, 11, 16, (int) (105 * alphaMultiplier)).getRGB(), 6f);

        float leftPad = footerX + 4.8f;  // 6 * 0.8
        float iconSize = 20f;  // 25 * 0.8
        float circleY = footerY + (28f - iconSize) / 2f;  // 35 * 0.8

        Render2D.rect(leftPad, circleY, iconSize, iconSize,
                new Color(18, 24, 32, (int) (220 * alphaMultiplier)).getRGB(), iconSize / 2f);
        Fonts.INTER_SEMIBOLD.drawCentered("DC", leftPad + iconSize / 2f, circleY + iconSize / 2f - 3.6f, 7f,
                new Color(235, 245, 255, (int) (235 * alphaMultiplier)).getRGB());
        Render2D.rect(leftPad + iconSize - 5.6f, circleY + iconSize - 5.6f, 4.4f, 4.4f,
                new Color(64, 220, 118, (int) (245 * alphaMultiplier)).getRGB(), 2.2f);
        float greenDotX = leftPad + 9.6f;  // 12 * 0.8
        float greenDotY = circleY + iconSize - 4.8f;  // 6 * 0.8
        // точка возле аватара всегда чисто зелёная, не зависит от CLIENT_COLOR

        float lineY = bgY + 280f;  // 350 * 0.8
//        Render2D.rect(bgX + 8f, lineY, BackgroundComponent.SIDEBAR_WIDTH - 16f, 0.4f,  // 10 * 0.8, 20 * 0.8, 0.5 * 0.8
//                new Color(60, 65, 75, alpha).getRGB(), 0);

        float textX = leftPad + iconSize + 6.4f;  // 8 * 0.8
        String username = MinecraftClient.getInstance().getSession() != null
                ? MinecraftClient.getInstance().getSession().getUsername()
                : "User";
        float textMaxW = footerX + footerW - textX - 13f;
        Fonts.INTER_SEMIBOLD.draw(trimText(username, textMaxW, 5.2f), textX, footerY + 8f, 5.2f,
                new Color(235, 239, 248, alpha).getRGB());
        Fonts.INTER_SEMIBOLD.draw("Lifetime", textX, footerY + 14.6f, 4.8f,
                new Color(150, 160, 172, alpha).getRGB());

        float rightIconSize = 9.6f;  // 12 * 0.8
        float rightIconX = footerX + footerW - rightIconSize - 3.2f;  // 4 * 0.8
        float rightIconY = footerY + (28f - rightIconSize) / 2f;  // 35 * 0.8

        Color clientColor = CLIENT_COLOR;
        Fonts.INTER_SEMIBOLD.draw("", rightIconX + 1.8f, rightIconY + 1.2f, 6.4f,
                new Color(190, 198, 212, (int) (215 * alphaMultiplier)).getRGB());
        Render2D.rect(
                rightIconX + (rightIconSize - 4.8f) / 1f - 1.4f,  // 6 * 0.8, 1.75 * 0.8
                rightIconY + (rightIconSize - 4.8f) / 1f - 1.6f,  // 6 * 0.8, 2 * 0.8
                2.8f,  // 3.5 * 0.8
                2.8f,  // 3.5 * 0.8
                new Color(clientColor.getRed(), clientColor.getGreen(), clientColor.getBlue(), alpha).getRGB(),
                2.4f  // 3 * 0.8
        );
    }

    private float getClientPickerX(float footerX) {
        return footerX;
    }

    private float getClientPickerY(float footerY) {
        return footerY - CLIENT_PICKER_H - CLIENT_PICKER_GAP;
    }

    private Color[] getClientPresetColors() {
        return new Color[] {
                new Color(91, 132, 255),
                new Color(80, 190, 255),
                new Color(88, 220, 135),
                new Color(255, 204, 84),
                new Color(255, 132, 82),
                new Color(255, 92, 122),
                new Color(166, 120, 255)
        };
    }

    private int withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha))).getRGB();
    }

    private String getClientHex() {
        return String.format("#%02X%02X%02X", CLIENT_COLOR.getRed(), CLIENT_COLOR.getGreen(), CLIENT_COLOR.getBlue());
    }

    private boolean handleCompactClientPickerClick(double mouseX, double mouseY, float footerX, float footerY) {
        float pickerX = getClientPickerX(footerX);
        float pickerY = getClientPickerY(footerY);

        if (mouseX < pickerX || mouseX > pickerX + CLIENT_PICKER_W ||
                mouseY < pickerY || mouseY > pickerY + CLIENT_PICKER_H) {
            return false;
        }

        float areaX = pickerX + 7f;
        float areaY = pickerY + 22f;
        float areaW = CLIENT_PICKER_W - 14f;
        float areaH = 46f;
        if (mouseX >= areaX && mouseX <= areaX + areaW &&
                mouseY >= areaY && mouseY <= areaY + areaH) {
            clientSat = Math.max(0f, Math.min(1f, (float) ((mouseX - areaX) / areaW)));
            clientBri = 1f - Math.max(0f, Math.min(1f, (float) ((mouseY - areaY) / areaH)));
            updateClientColorFromHsb();
            return true;
        }

        float hueY = areaY + areaH + 7f;
        float hueH = 5f;
        if (mouseX >= areaX && mouseX <= areaX + areaW &&
                mouseY >= hueY && mouseY <= hueY + hueH) {
            clientHue = Math.max(0f, Math.min(1f, (float) ((mouseX - areaX) / areaW)));
            updateClientColorFromHsb();
            return true;
        }

        float swatchY = hueY + 13f;
        float swatchSize = 7.4f;
        float swatchGap = 3.7f;
        Color[] colors = getClientPresetColors();
        for (int i = 0; i < colors.length; i++) {
            float swatchX = areaX + 1f + i * (swatchSize + swatchGap);
            if (mouseX >= swatchX && mouseX <= swatchX + swatchSize &&
                    mouseY >= swatchY && mouseY <= swatchY + swatchSize) {
                CLIENT_COLOR = colors[i];
                syncHsbFromClientColor();
                return true;
            }
        }

        return true;
    }

    private void renderCompactClientPicker(float footerX, float footerY, float alphaMultiplier, float animation) {
        float pickerX = getClientPickerX(footerX);
        float pickerY = getClientPickerY(footerY);
        float panelAlpha = alphaMultiplier * animation;
        int alpha = (int) (255 * panelAlpha);
        if (alpha <= 2) return;

        Render2D.gradientRect(pickerX, pickerY, CLIENT_PICKER_W, CLIENT_PICKER_H,
                new int[]{
                        new Color(10, 16, 19, (int) (246 * panelAlpha)).getRGB(),
                        new Color(10, 16, 19, (int) (246 * panelAlpha)).getRGB(),
                        new Color(7, 12, 15, (int) (246 * panelAlpha)).getRGB(),
                        new Color(7, 12, 15, (int) (246 * panelAlpha)).getRGB()
                }, 7f);
        Render2D.outline(pickerX, pickerY, CLIENT_PICKER_W, CLIENT_PICKER_H, 0.5f,
                new Color(45, 62, 75, (int) (150 * panelAlpha)).getRGB(), 7f);

        Color accent = CLIENT_COLOR;
        Render2D.rect(pickerX + 7f, pickerY + 7f, 9f, 9f, withAlpha(accent, alpha), 3f);
        Render2D.outline(pickerX + 7f, pickerY + 7f, 9f, 9f, 0.45f,
                new Color(230, 236, 248, (int) (120 * panelAlpha)).getRGB(), 3f);
        Fonts.INTER_SEMIBOLD.draw("Accent", pickerX + 20f, pickerY + 6.4f, 5.3f,
                new Color(230, 235, 244, alpha).getRGB());
        Fonts.INTER_MEDIUM.draw(getClientHex(), pickerX + CLIENT_PICKER_W - 38f, pickerY + 7f, 4.4f,
                new Color(130, 140, 154, (int) (210 * panelAlpha)).getRGB());

        float areaX = pickerX + 7f;
        float areaY = pickerY + 22f;
        float areaW = CLIENT_PICKER_W - 14f;
        float areaH = 46f;
        int hueColor = Color.HSBtoRGB(clientHue, 1f, 1f);
        Render2D.gradientRect(areaX, areaY, areaW, areaH,
                new int[]{Color.WHITE.getRGB(), hueColor, hueColor, Color.WHITE.getRGB()}, 4f);
        Render2D.gradientRect(areaX, areaY, areaW, areaH,
                new int[]{
                        new Color(0, 0, 0, 0).getRGB(),
                        new Color(0, 0, 0, 0).getRGB(),
                        new Color(0, 0, 0, (int) (235 * panelAlpha)).getRGB(),
                        new Color(0, 0, 0, (int) (235 * panelAlpha)).getRGB()
                }, 4f);
        Render2D.outline(areaX, areaY, areaW, areaH, 0.45f,
                new Color(48, 64, 78, (int) (140 * panelAlpha)).getRGB(), 4f);

        float markerX = areaX + clientSat * areaW;
        float markerY = areaY + (1f - clientBri) * areaH;
        Render2D.rect(markerX - 2.8f, markerY - 2.8f, 5.6f, 5.6f,
                new Color(255, 255, 255, alpha).getRGB(), 2.8f);
        Render2D.rect(markerX - 1.6f, markerY - 1.6f, 3.2f, 3.2f,
                new Color(10, 12, 16, (int) (210 * panelAlpha)).getRGB(), 1.6f);

        float hueY = areaY + areaH + 7f;
        int[] hueColors = {
                Color.HSBtoRGB(0f, 1f, 1f),
                Color.HSBtoRGB(1f / 6f, 1f, 1f),
                Color.HSBtoRGB(2f / 6f, 1f, 1f),
                Color.HSBtoRGB(3f / 6f, 1f, 1f),
                Color.HSBtoRGB(4f / 6f, 1f, 1f),
                Color.HSBtoRGB(5f / 6f, 1f, 1f),
                Color.HSBtoRGB(1f, 1f, 1f)
        };
        Render2D.gradientRect(areaX, hueY, areaW, 5f, hueColors, 2.5f);
        float hueMarkerX = areaX + clientHue * areaW;
        Render2D.rect(hueMarkerX - 1.2f, hueY - 1f, 2.4f, 7f,
                new Color(245, 248, 255, alpha).getRGB(), 1.2f);

        float swatchY = hueY + 13f;
        float swatchSize = 7.4f;
        float swatchGap = 3.7f;
        Color[] colors = getClientPresetColors();
        for (int i = 0; i < colors.length; i++) {
            float swatchX = areaX + 1f + i * (swatchSize + swatchGap);
            Color color = colors[i];
            Render2D.rect(swatchX, swatchY, swatchSize, swatchSize, withAlpha(color, alpha), 2.4f);
            if (Math.abs(color.getRed() - CLIENT_COLOR.getRed()) < 3 &&
                    Math.abs(color.getGreen() - CLIENT_COLOR.getGreen()) < 3 &&
                    Math.abs(color.getBlue() - CLIENT_COLOR.getBlue()) < 3) {
                Render2D.outline(swatchX - 1f, swatchY - 1f, swatchSize + 2f, swatchSize + 2f, 0.45f,
                        new Color(235, 240, 250, (int) (185 * panelAlpha)).getRGB(), 3f);
            }
        }

        float chipY = pickerY + CLIENT_PICKER_H - 16f;
        Render2D.rect(areaX, chipY, areaW, 9f,
                new Color(14, 20, 25, (int) (190 * panelAlpha)).getRGB(), 3f);
        Render2D.rect(areaX + 3f, chipY + 2.2f, 4.6f, 4.6f, withAlpha(accent, alpha), 2.3f);
        Fonts.INTER_MEDIUM.draw("Client accent", areaX + 11f, chipY + 2.3f, 4.3f,
                new Color(145, 154, 170, (int) (215 * panelAlpha)).getRGB());
    }

    /**
     * Обработка кликов по квадратику цвета клиента и по элементам палитр.
     */
    public boolean handleClientColorClick(double mouseX, double mouseY, float bgX, float bgY, int button) {
        float footerY = bgY + BackgroundComponent.BG_HEIGHT - 30.4f;  // 38 * 0.8
        float footerW = BackgroundComponent.SIDEBAR_WIDTH - 9.6f;  // 12 * 0.8
        float footerX = bgX + 4.8f;  // 6 * 0.8

        float rightIconSize = 9.6f;  // 12 * 0.8
        float rightIconX = footerX + footerW - rightIconSize - 3.2f;  // 4 * 0.8
        float rightIconY = footerY + (28f - rightIconSize) / 2f;  // 35 * 0.8

        // ЛКМ по квадратику — открыть/закрыть быструю палитру
        if (button == 0 &&
                mouseX >= rightIconX && mouseX <= rightIconX + rightIconSize &&
                mouseY >= rightIconY && mouseY <= rightIconY + rightIconSize) {
            clientPaletteOpen = !clientPaletteOpen;
            if (clientPaletteOpen) {
                clientPaletteOpening = true;
                clientPaletteClosing = false;
                clientPickerOpen = false;
            } else {
                clientPaletteOpening = false;
                clientPaletteClosing = true;
                clientPickerOpen = false;
            }
            lastPaletteAnimTime = System.currentTimeMillis();
            return true;
        }

        if (!clientPaletteOpen && !clientPickerOpen) return false;

        if (clientPaletteOpen) {
            if (button != 0) {
                return true;
            }

            if (handleCompactClientPickerClick(mouseX, mouseY, footerX, footerY)) {
                return true;
            }

            clientPaletteOpen = false;
            clientPaletteClosing = true;
            clientPaletteOpening = false;
            lastPaletteAnimTime = System.currentTimeMillis();
            return true;
        }

        // область быстрой палитры (ряд кружков и кисточка) — СНИЗУ от футера
        // координаты ДОЛЖНЫ совпадать с renderClientColorUI
        float paletteWidth = 80f;  // 100 * 0.8
        float basePaletteHeight = 14.4f;  // 18 * 0.8
        float paletteHeight = basePaletteHeight * clientPaletteAnim;
        float paletteX = footerX - 0.8f;  // 1 * 0.8
        float paletteY = footerY - 7.2f - paletteHeight;

        if (clientPaletteOpen && button == 0) {
            if (mouseX >= paletteX && mouseX <= paletteX + paletteWidth &&
                    mouseY >= paletteY && mouseY <= paletteY + paletteHeight) {
                // вычисляем индекс слота; последний — кисточка
                int slots = 7; // 6 цветов + кисточка
                // те же параметры, что и в renderClientColorUI
                float slotRadius = 2.8f * clientPaletteAnim;  // 3.5 * 0.8
                float slotSpacing = 4.8f;  // 6 * 0.8
                float startX = paletteX + 7.2f;  // 9 * 0.8
                float centerY = paletteY + paletteHeight / 2f;

                for (int i = 0; i < slots; i++) {
                    float cx = startX + i * (slotRadius * 2f + slotSpacing);
                    float cy = centerY;
                    float dx = (float) mouseX - cx;
                    float dy = (float) mouseY - cy;
                    if (dx * dx + dy * dy <= slotRadius * slotRadius + 1f) {
                        // 6 первых слотов — меняем цвет клиента, последний — кисточка (открыть Colorpicker)
                        if (i < 6) {
                            Color[] swatchColors = new Color[] {
                                    new Color(81, 87, 255),
                                    new Color(255, 196, 0),
                                    new Color(255, 96, 96),
                                    new Color(87, 215, 116),
                                    new Color(120, 120, 255),
                                    new Color(255, 255, 255)
                            };
                            CLIENT_COLOR = swatchColors[i];
                            syncHsbFromClientColor();
                            clientPaletteOpen = false;
                            clientPaletteClosing = true;
                            clientPaletteOpening = false;
                            lastPaletteAnimTime = System.currentTimeMillis();
                        } else {
                            clientPaletteOpen = false;
                            clientPickerOpen = true;
                            clientPaletteClosing = true;
                            clientPaletteOpening = false;
                            lastPaletteAnimTime = System.currentTimeMillis();
                        }
                        return true;
                    }
                }
            } else {
                // клик мимо — просто закрываем быструю палитру
                clientPaletteOpen = false;
                clientPaletteClosing = true;
                clientPaletteOpening = false;
                lastPaletteAnimTime = System.currentTimeMillis();
            }
        }

        // пока большая менюшка только для вида, закрываем по клику вне прямоугольника
        if (clientPickerOpen && button == 0) {
            float pickerW = 168f;  // 210 * 0.8
            float pickerH = 126f;
            float basePickerX = footerX;
            float basePickerY = footerY - pickerH - 7.2f;
            float pickerX = basePickerX + clientPickerOffsetX;
            float pickerY = basePickerY + clientPickerOffsetY;
            // заголовок (для перетаскивания) и крестик
            float headerH = 12.8f;  // 16 * 0.8
            float closeX = pickerX + pickerW - 11.2f;  // 14 * 0.8
            float closeY = pickerY + 3.2f;  // 4 * 0.8
            float closeW = 8f;  // 10 * 0.8
            float closeH = 8f;  // 10 * 0.8

            // клик по кресту — закрыть
            if (mouseX >= closeX && mouseX <= closeX + closeW &&
                    mouseY >= closeY && mouseY <= closeY + closeH) {
                clientPickerOpen = false;
                clientPickerDragging = false;
                return true;
            }

            // клик по заголовку — начать/закончить перетаскивание
            if (mouseX >= pickerX && mouseX <= pickerX + pickerW &&
                    mouseY >= pickerY && mouseY <= pickerY + headerH) {
                clientPickerDragging = true;
                pickerDragStartMouseX = mouseX;
                pickerDragStartMouseY = mouseY;
                pickerDragStartOffsetX = clientPickerOffsetX;
                pickerDragStartOffsetY = clientPickerOffsetY;
                return true;
            }

            // клик в основной области выбора цвета — меняем CLIENT_COLOR
            float areaX = pickerX + 6.4f;  // 8 * 0.8
            float areaY = pickerY + 16f;  // 20 * 0.8
            float areaW = pickerW - 12.8f;  // 16 * 0.8
            float areaH = 48f;  // 60 * 0.8
            if (mouseX >= areaX && mouseX <= areaX + areaW &&
                    mouseY >= areaY && mouseY <= areaY + areaH) {
                float relX = (float) ((mouseX - areaX) / areaW);
                float relY = (float) ((mouseY - areaY) / areaH);
                relX = Math.max(0f, Math.min(1f, relX));
                relY = Math.max(0f, Math.min(1f, relY));
                clientSat = relX;
                clientBri = 1f - relY;
                updateClientColorFromHsb();
                return true;
            }

            // выбор hue по полоске
            float hueY = areaY + areaH + 4.8f;  // 6 * 0.8
            float hueH = 4.8f;  // 6 * 0.8
            if (mouseX >= areaX && mouseX <= areaX + areaW &&
                    mouseY >= hueY && mouseY <= hueY + hueH) {
                float relHue = (float) ((mouseX - areaX) / areaW);
                relHue = Math.max(0f, Math.min(1f, relHue));
                clientHue = relHue;
                updateClientColorFromHsb();
                return true;
            }

            // клики по prepared swatches внизу
            float swatchY = hueY + 9.6f;  // 12 * 0.8
            float swatchSize = 8f;  // 10 * 0.8
            float swatchGap = 2.8f;  // 3.5 * 0.8
            float startX = areaX + 1.6f;  // 2 * 0.8

            Color[] row1 = {
                    new Color(255, 96, 96), new Color(255, 172, 80), new Color(255, 224, 102), new Color(87, 215, 116),
                    new Color(104, 183, 255), new Color(137, 120, 255), new Color(192, 120, 255), new Color(255, 255, 255)
            };
            Color[] row2 = {
                    new Color(196, 67, 67), new Color(196, 133, 67), new Color(196, 180, 67), new Color(67, 170, 96),
                    new Color(67, 133, 196), new Color(96, 96, 196), new Color(170, 96, 196), new Color(210, 210, 215)
            };

            for (int i = 0; i < row1.length; i++) {
                float x1 = startX + i * (swatchSize + swatchGap);
                float x2 = x1;
                float y1 = swatchY;
                float y2 = swatchY + swatchSize + 2.4f;  // 3 * 0.8
                if (mouseX >= x1 && mouseX <= x1 + swatchSize &&
                        mouseY >= y1 && mouseY <= y1 + swatchSize) {
                    CLIENT_COLOR = row1[i];
                    syncHsbFromClientColor();
                    return true;
                }
                if (mouseX >= x2 && mouseX <= x2 + swatchSize &&
                        mouseY >= y2 && mouseY <= y2 + swatchSize) {
                    CLIENT_COLOR = row2[i];
                    syncHsbFromClientColor();
                    return true;
                }
            }

            // клик вне всего пикера — закрыть
            if (!(mouseX >= pickerX && mouseX <= pickerX + pickerW &&
                    mouseY >= pickerY && mouseY <= pickerY + pickerH)) {
                clientPickerOpen = false;
                clientPickerDragging = false;
            }
        }

        return false;
    }

    /**
     * Отрисовка быстрой палитры и большой менюшки Colorpicker, как на референсе.
     */
    public void renderClientColorUI(DrawContext context, float bgX, float bgY, float mouseX, float mouseY, float alphaMultiplier) {
        if (alphaMultiplier <= 0.01f) return;

        // если кнопку мыши уже отпустили — прекращаем перетаскивание
        if (clientPickerDragging) {
            long window = MinecraftClient.getInstance().getWindow().getHandle();
            if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
                clientPickerDragging = false;
            }
        }

        float footerY = bgY + BackgroundComponent.BG_HEIGHT - 30.4f;  // 38 * 0.8
        float footerW = BackgroundComponent.SIDEBAR_WIDTH - 9.6f;  // 12 * 0.8
        float footerX = bgX + 4.8f;  // 6 * 0.8

        // обновление перетаскивания большого Colorpicker
        if (clientPickerOpen && clientPickerDragging) {
            float basePickerX = footerX;
            float basePickerY = footerY - 126f - 7.2f;
            double dx = mouseX - pickerDragStartMouseX;
            double dy = mouseY - pickerDragStartMouseY;
            clientPickerOffsetX = pickerDragStartOffsetX + (float) dx;
            clientPickerOffsetY = pickerDragStartOffsetY + (float) dy;
        }

        // анимация для маленькой палитры (ряд кружков)
        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastPaletteAnimTime) / 1000f, 0.1f);
        lastPaletteAnimTime = now;
        final float PALETTE_SPEED = 10f;
        if (clientPaletteOpening) {
            clientPaletteAnim += PALETTE_SPEED * dt;
            if (clientPaletteAnim >= 1f) {
                clientPaletteAnim = 1f;
                clientPaletteOpening = false;
            }
        } else if (clientPaletteClosing) {
            clientPaletteAnim -= PALETTE_SPEED * dt;
            if (clientPaletteAnim <= 0f) {
                clientPaletteAnim = 0f;
                clientPaletteClosing = false;
            }
        }

        if (clientPaletteAnim > 0.01f) {
            renderCompactClientPicker(footerX, footerY, alphaMultiplier, clientPaletteAnim);
        }

        if (clientPickerOpen && clientPaletteAnim > 0.01f) {
            int alpha = (int) (255 * alphaMultiplier * clientPaletteAnim);
            float paletteWidth = 80f;  // 100 * 0.8
            float basePaletteHeight = 14.4f;  // 18 * 0.8
            float paletteHeight = basePaletteHeight * clientPaletteAnim;
            float paletteX = footerX - 0.8f;  // 1 * 0.8
            float paletteY = footerY - 7.2f - paletteHeight;

            Render2D.rect(paletteX, paletteY, paletteWidth, paletteHeight,
                    new Color(8, 12, 15, (int) (238 * alphaMultiplier * clientPaletteAnim)).getRGB(), 4f);  // 5 * 0.8
            Render2D.outline(paletteX, paletteY, paletteWidth, paletteHeight, 0.4f,  // 0.5 * 0.8
                    new Color(45, 62, 75, alpha).getRGB(), 4f);  // 5 * 0.8

            // кружки цветов
            Color[] swatchColors = new Color[] {
                    new Color(81, 87, 255),
                    new Color(255, 196, 0),
                    new Color(255, 96, 96),
                    new Color(87, 215, 116),
                    new Color(120, 120, 255),
                    new Color(255, 255, 255)
            };
            int slots = swatchColors.length + 1; // плюс кисточка
            float slotRadius = 2.8f * clientPaletteAnim;  // 3.5 * 0.8
            float slotSpacing = 4.8f;  // 6 * 0.8
            float startX = paletteX + 7.2f;  // 9 * 0.8
            float centerY = paletteY + paletteHeight / 2f;

            for (int i = 0; i < swatchColors.length; i++) {
                float cx = startX + i * (slotRadius * 2f + slotSpacing);
                float cy = centerY;
                Render2D.rect(cx - slotRadius, cy - slotRadius, slotRadius * 2f, slotRadius * 2f,
                        swatchColors[i].getRGB(), slotRadius);
            }

            // слот кисточки
            float brushCx = startX + (slots - 1) * (slotRadius * 2f + slotSpacing);
            float brushCy = centerY;
            Render2D.rect(brushCx - slotRadius, brushCy - slotRadius, slotRadius * 2f, slotRadius * 2f,
                    new Color(30, 30, 40, alpha).getRGB(), slotRadius);
            Fonts.ICONS1.draw("I", brushCx , brushCy - 3.2f, 6f * clientPaletteAnim,  // 4 * 0.8, 7.5 * 0.8
                    new Color(200, 200, 210, alpha).getRGB());
        }

        if (clientPickerOpen) {
            int alpha = (int) (255 * alphaMultiplier);
            float pickerW = 168f;  // 210 * 0.8
            float pickerH = 126f;
            float basePickerX = footerX;
            float basePickerY = footerY - pickerH - 7.2f;
            float pickerX = basePickerX + clientPickerOffsetX;
            float pickerY = basePickerY + clientPickerOffsetY;

            Render2D.rect(pickerX, pickerY, pickerW, pickerH,
                    new Color(8, 12, 15, (int) (245 * alphaMultiplier)).getRGB(), 6.4f);  // 8 * 0.8
            Render2D.outline(pickerX, pickerY, pickerW, pickerH, 0.4f,  // 0.5 * 0.8
                    new Color(45, 62, 75, alpha).getRGB(), 6.4f);  // 8 * 0.8

            // заголовок с кисточкой и крестиком
            Fonts.ICONS1.draw("S", pickerX + 6.4f, pickerY + 5.6f, 5.6f,  // 8 * 0.8, 7 * 0.8, 7 * 0.8
                    new Color(200, 200, 210, alpha).getRGB());
            Fonts.INTER_SEMIBOLD.draw("Colorpicker", pickerX + 16f, pickerY + 5.6f, 5.6f,  // 20 * 0.8, 7 * 0.8, 7 * 0.8
                    new Color(230, 230, 235, alpha).getRGB());
            Fonts.BOLD.draw("x", pickerX + pickerW - 9.6f, pickerY + 4.8f, 6.4f,  // 12 * 0.8, 6 * 0.8, 8 * 0.8
                    new Color(180, 180, 190, alpha).getRGB());

            // большая область цвета (упрощённая, без реальной выборки)
            float areaX = pickerX + 6.4f;  // 8 * 0.8
            float areaY = pickerY + 16f;  // 20 * 0.8
            float areaW = pickerW - 12.8f;  // 16 * 0.8
            float areaH = 48f;  // 60 * 0.8
            // панель основного цвета берёт текущий CLIENT_COLOR (а не всегда красный)
            Color panelColor = CLIENT_COLOR;
            Render2D.rect(areaX, areaY, areaW, areaH,
                    new Color(panelColor.getRed(), panelColor.getGreen(), panelColor.getBlue(), 255).getRGB(), 3.2f);  // 4 * 0.8

            // полоска hue под ней — простой радужный градиент
            float hueY = areaY + areaH + 4.8f;  // 6 * 0.8
            int[] hueColors = {
                    Color.HSBtoRGB(0f, 1f, 1f),
                    Color.HSBtoRGB(1f / 6f, 1f, 1f),
                    Color.HSBtoRGB(2f / 6f, 1f, 1f),
                    Color.HSBtoRGB(3f / 6f, 1f, 1f),
                    Color.HSBtoRGB(4f / 6f, 1f, 1f),
                    Color.HSBtoRGB(5f / 6f, 1f, 1f),
                    Color.HSBtoRGB(1f, 1f, 1f)
            };
            Render2D.gradientRect(areaX, hueY, areaW, 4.8f, hueColors, 2.4f);  // 6 * 0.8, 3 * 0.8

            // Prepared swatches — две строки цветных квадратиков
            float swatchY = hueY + 9.6f;  // 12 * 0.8
            float swatchSize = 8f;  // 10 * 0.8
            float swatchGap = 2.8f;  // 3.5 * 0.8
            Color[] row1 = {
                    new Color(255, 96, 96), new Color(255, 172, 80), new Color(255, 224, 102), new Color(87, 215, 116),
                    new Color(104, 183, 255), new Color(137, 120, 255), new Color(192, 120, 255), new Color(255, 255, 255)
            };
            Color[] row2 = {
                    new Color(196, 67, 67), new Color(196, 133, 67), new Color(196, 180, 67), new Color(67, 170, 96),
                    new Color(67, 133, 196), new Color(96, 96, 196), new Color(170, 96, 196), new Color(210, 210, 215)
            };

            float startX = areaX + 1.6f;  // 2 * 0.8
            for (int i = 0; i < row1.length; i++) {
                float x = startX + i * (swatchSize + swatchGap);
                Render2D.rect(x, swatchY, swatchSize, swatchSize, row1[i].getRGB(), 2.4f);  // 3 * 0.8
                Render2D.rect(x, swatchY + swatchSize + 2.4f, swatchSize, swatchSize, row2[i].getRGB(), 2.4f);  // 3 * 0.8, 3 * 0.8
            }

            // подпись Color parts и 4 поля R/G/B/A
            float partsY = swatchY + swatchSize * 2 + 9.6f;  // 12 * 0.8
            Fonts.INTER_SEMIBOLD.draw("Color parts", areaX, partsY, 4.8f,  // 6 * 0.8
                    new Color(170, 175, 185, alpha).getRGB());

            float fieldY = partsY + 6.4f;  // 8 * 0.8
            float fieldW = 38f;
            float fieldH = 12f;
            float fieldGap = 4f;
            String[] labels = {"R", "G", "B", "A"};
            String[] values = {
                    String.valueOf(CLIENT_COLOR.getRed()),
                    String.valueOf(CLIENT_COLOR.getGreen()),
                    String.valueOf(CLIENT_COLOR.getBlue()),
                    "255"
            };

            float fx = areaX;
            for (int i = 0; i < 4; i++) {
                Render2D.rect(fx, fieldY, fieldW, fieldH,
                        new Color(20, 20, 26, 255).getRGB(), 3f);
                Render2D.outline(fx, fieldY, fieldW, fieldH, 0.5f,
                        new Color(45, 62, 75, alpha).getRGB(), 3f);
                Fonts.INTER_SEMIBOLD.draw(labels[i], fx + 4f, fieldY + 4f, 5f,
                        new Color(150, 150, 160, alpha).getRGB());
                Fonts.INTER_SEMIBOLD.draw(values[i], fx + fieldW - 20f, fieldY + 4f, 5f,
                        new Color(220, 220, 225, alpha).getRGB());
                fx += fieldW + fieldGap;
            }
        }
    }

    public ModuleCategory getCategoryAtPosition(double mouseX, double mouseY, float bgX, float bgY) {
        if (mouseX < bgX + 7f || mouseX > bgX + BackgroundComponent.SIDEBAR_WIDTH - 5f) return null;

        float currentY = bgY + CATEGORIES_START_Y;
        
        // AIMBOT группа (пропускаем заголовок)
        currentY += 14.4f;
        for (int i = 0; i < AIMBOT_NAMES.length; i++) {
            if (mouseY >= currentY && mouseY <= currentY + ITEM_HEIGHT) {
                return AIMBOT_CATEGORIES[i];
            }
            currentY += ITEM_HEIGHT + 1.6f;
        }
        currentY += 6.4f;
        
        // COMMON группа (пропускаем заголовок)
        currentY += 14.4f;
        for (int i = 0; i < COMMON_NAMES.length; i++) {
            if (mouseY >= currentY && mouseY <= currentY + ITEM_HEIGHT) {
                return COMMON_CATEGORIES[i];
            }
            currentY += ITEM_HEIGHT + 1.6f;
        }
        currentY += 6.4f;
        
        // EXTENSIONS группа (пропускаем заголовок)
        currentY += 14.4f;
        for (int i = 0; i < EXTENSIONS_NAMES.length; i++) {
            if (mouseY >= currentY && mouseY <= currentY + ITEM_HEIGHT) {
                return EXTENSIONS_CATEGORIES[i];
            }
            currentY += ITEM_HEIGHT + 1.6f;
        }

        return null;
    }
}

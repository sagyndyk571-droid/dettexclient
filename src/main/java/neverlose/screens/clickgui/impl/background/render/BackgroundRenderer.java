package neverlose.screens.clickgui.impl.background.render;

import net.minecraft.client.gui.DrawContext;
import neverlose.screens.clickgui.ClickGuiStylePreference;
import neverlose.screens.clickgui.impl.background.BackgroundComponent;
import neverlose.util.render.Render2D;

import java.awt.*;

public class BackgroundRenderer {

    private static final float CORNER_RADIUS = 14;
    // фон левой панели: RGB (9,13,17)
    private static final Color BG_LEFT = new Color(5, 8, 12);
    private static final Color BG_LEFT_GLASS = new Color(8, 13, 18);
    // фон правой панели: RGB (10,15,18)
    private static final Color BG_RIGHT = new Color(7, 12, 15);
    private static final Color BG_RIGHT_GLASS = new Color(10, 16, 21);

    public void render(DrawContext context, float bgX, float bgY, float alphaMultiplier) {
boolean glass = ClickGuiStylePreference.isGlassStyle();
        Color left = glass ? BG_LEFT_GLASS : BG_LEFT;
        Color right = glass ? BG_RIGHT_GLASS : BG_RIGHT;
        int baseAlpha = (int) ((glass ? 240 : 250) * alphaMultiplier);
        float w = BackgroundComponent.BG_WIDTH;
        float h = BackgroundComponent.BG_HEIGHT;
        float sidebarW = BackgroundComponent.SIDEBAR_WIDTH;

        // Блюр для левой панели (умеренный, чтобы не "мылить" интерфейс)
        Render2D.blur(bgX, bgY, w, h, glass ? 7f : 4f, CORNER_RADIUS, new Color(4, 8, 12, (int) (44 * alphaMultiplier)).getRGB());

        // Левая панель (9,13,17) с прозрачностью
        int leftAlpha = (int) ((glass ? 236 : 246) * alphaMultiplier);
        Render2D.rect(bgX, bgY, sidebarW, h, new Color(left.getRed(), left.getGreen(), left.getBlue(), leftAlpha).getRGB(), CORNER_RADIUS, 0, 0, CORNER_RADIUS);

        // Правая панель (10,15,18)
        int rightTop = new Color(right.getRed(), right.getGreen(), right.getBlue(), baseAlpha).getRGB();
        int rightBottom = (glass ? new Color(7, 12, 16, baseAlpha) : new Color(5, 10, 12, baseAlpha)).getRGB();
        Render2D.gradientRect(bgX + sidebarW, bgY, w - sidebarW, h,
                new int[]{rightTop, rightTop, rightBottom, rightBottom}, 0, CORNER_RADIUS, CORNER_RADIUS, 0);
        Render2D.rect(bgX + sidebarW, bgY, w - sidebarW, 35f,
                (glass
                        ? new Color(9, 14, 20, (int) (180 * alphaMultiplier))
                        : new Color(8, 12, 18, (int) (210 * alphaMultiplier))).getRGB(), 0, CORNER_RADIUS, 0, 0);
        
        // Outline тонкий
        Render2D.outline(bgX, bgY, w, h, 0.6f,
                (glass
                        ? new Color(72, 100, 135, (int)(125 * alphaMultiplier))
                        : new Color(56, 76, 98, (int)(122 * alphaMultiplier))).getRGB(), CORNER_RADIUS);
    }

    public void renderCategoryPanel(float bgX, float bgY, float bgHeight, float alphaMultiplier) {
        float sidebarW = BackgroundComponent.SIDEBAR_WIDTH;
        boolean glass = ClickGuiStylePreference.isGlassStyle();
        int lineAlpha = (int) ((glass ? 92 : 120) * alphaMultiplier);
        // вертикальная полоска-разделитель между левой и правой панелью
        Render2D.rect(bgX + sidebarW, bgY, 0.7f, bgHeight,
                (glass ? new Color(50, 68, 86, lineAlpha) : new Color(32, 42, 52, lineAlpha)).getRGB(), 0);
    }
}

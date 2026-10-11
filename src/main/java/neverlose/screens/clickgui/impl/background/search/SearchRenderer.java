package neverlose.screens.clickgui.impl.background.search;

import net.minecraft.client.gui.DrawContext;
import neverlose.modules.module.ModuleStructure;
import neverlose.screens.clickgui.impl.background.BackgroundComponent;
import neverlose.util.render.Render2D;
import neverlose.util.render.shader.Scissor;
import neverlose.util.render.font.Fonts;

import java.awt.*;
import java.util.List;

public class SearchRenderer {

    private final SearchHandler searchHandler;
    private static final float PANEL_X_PAD = 4.8f;
    private static final float PANEL_Y_OFFSET = 43.5f;
    private static final float PANEL_BOTTOM_PAD = 6f;
    private static final float PANEL_SIDE_PAD = 4.8f;
    private static final float RESULTS_TOP = 35f;

    public SearchRenderer(SearchHandler searchHandler) {
        this.searchHandler = searchHandler;
    }

    public static float getPanelX(float bgX) {
        return bgX + PANEL_X_PAD;
    }

    public static float getPanelY(float bgY) {
        return bgY + PANEL_Y_OFFSET;
    }

    public static float getPanelW() {
        return BackgroundComponent.SIDEBAR_WIDTH - PANEL_SIDE_PAD * 2f;
    }

    public static float getPanelH(float bgHeight) {
        return bgHeight - PANEL_Y_OFFSET - PANEL_BOTTOM_PAD;
    }

    public static boolean isPanelHovered(double mouseX, double mouseY, float bgX, float bgY, float bgHeight) {
        float panelX = getPanelX(bgX);
        float panelY = getPanelY(bgY);
        float panelW = getPanelW();
        float panelH = getPanelH(bgHeight);
        return mouseX >= panelX && mouseX <= panelX + panelW && mouseY >= panelY && mouseY <= panelY + panelH;
    }

    public static float getVisibleResultsHeight(float bgHeight) {
        return Math.max(20f, getPanelH(bgHeight) - RESULTS_TOP - 6f);
    }

    public void render(DrawContext context, float bgX, float bgY, float bgWidth, float bgHeight,
                       float mouseX, float mouseY, int guiScale, float alphaMultiplier) {

        if (searchHandler.getSearchPanelAlpha() <= 0.01f) return;

        float panelW = getPanelW();
        float panelH = getPanelH(bgHeight);
        float panelX = getPanelX(bgX);
        float panelY = getPanelY(bgY);

        float resultAlpha = searchHandler.getSearchPanelAlpha() * alphaMultiplier;

        renderPanelBackground(panelX, panelY, panelW, panelH, resultAlpha);

        List<ModuleStructure> results = searchHandler.getSearchResults();
        if (results.isEmpty()) {
            renderEmptyState(panelX, panelY, panelW, panelH, resultAlpha);
            return;
        }

        Scissor.enable(panelX + 4, panelY + RESULTS_TOP, panelW - 8, panelH - RESULTS_TOP - 6, 2);
        renderResults(panelX, panelY, panelW, panelH, mouseX, mouseY, resultAlpha);
        Scissor.disable();

        renderScrollIndicators(panelX, panelY, panelW, panelH, resultAlpha);
    }

    private void renderPanelBackground(float panelX, float panelY, float panelW, float panelH, float resultAlpha) {
        int panelBgAlpha = (int) (246 * resultAlpha);
        int outlineAlpha = (int) (135 * resultAlpha);

        Render2D.gradientRect(panelX, panelY, panelW, panelH,
                new int[]{
                        new Color(10, 16, 20, panelBgAlpha).getRGB(),
                        new Color(10, 16, 20, panelBgAlpha).getRGB(),
                        new Color(7, 12, 15, panelBgAlpha).getRGB(),
                        new Color(7, 12, 15, panelBgAlpha).getRGB()
                }, 8f);
        Render2D.outline(panelX, panelY, panelW, panelH, 0.55f, new Color(45, 62, 75, outlineAlpha).getRGB(), 8f);

        Fonts.ICONS.draw("H", panelX + 8f, panelY + 6.5f, 5.6f,
                new Color(91, 132, 255, (int) (215 * resultAlpha)).getRGB());
        Fonts.INTER_SEMIBOLD.draw("SEARCH", panelX + 16.5f, panelY + 7.1f, 4.7f,
                new Color(114, 123, 137, (int) (205 * resultAlpha)).getRGB());

        String count = String.valueOf(searchHandler.getSearchResults().size());
        float countW = Math.max(12f, Fonts.INTER_MEDIUM.getWidth(count, 4.2f) + 8f);
        Render2D.rect(panelX + panelW - countW - 6f, panelY + 5.2f, countW, 10.4f,
                new Color(16, 24, 31, (int) (210 * resultAlpha)).getRGB(), 4f);
        Fonts.INTER_MEDIUM.drawCentered(count, panelX + panelW - countW / 2f - 6f, panelY + 8.1f, 4.2f,
                new Color(145, 154, 170, (int) (215 * resultAlpha)).getRGB());

        float inputY = panelY + 19f;
        Render2D.rect(panelX + 6f, inputY, panelW - 12f, 12.2f,
                new Color(13, 19, 25, (int) (205 * resultAlpha)).getRGB(), 4f);
        Render2D.outline(panelX + 6f, inputY, panelW - 12f, 12.2f, 0.4f,
                new Color(41, 53, 65, (int) (110 * resultAlpha)).getRGB(), 4f);
        String query = searchHandler.getSearchText().isEmpty() ? "Type module" : searchHandler.getSearchText();
        int queryColor = searchHandler.getSearchText().isEmpty()
                ? new Color(93, 103, 118, (int) (170 * resultAlpha)).getRGB()
                : new Color(190, 199, 216, (int) (230 * resultAlpha)).getRGB();
        Fonts.ICONS1.draw("H", panelX + 11f, inputY + 3.6f, 4.5f,
                new Color(91, 132, 255, (int) (150 * resultAlpha)).getRGB());
        Fonts.INTER_MEDIUM.draw(trimText(query, panelW - 31f, 4.4f), panelX + 18.5f, inputY + 4f, 4.4f, queryColor);

        Render2D.rect(panelX + 7f, panelY + RESULTS_TOP - 4f, panelW - 14f, 0.45f,
                new Color(34, 45, 55, (int) (125 * resultAlpha)).getRGB(), 0);
    }

    private void renderEmptyState(float panelX, float panelY, float panelW, float panelH, float resultAlpha) {
        String noResults = searchHandler.getSearchText().isEmpty() ? "Start typing..." : "No modules found";
        float textSize = 5.8f;
        float textWidth = Fonts.INTER_MEDIUM.getWidth(noResults, textSize);
        float textHeight = Fonts.INTER_MEDIUM.getHeight(textSize);
        float centerX = panelX + (panelW - textWidth) / 2f;
        float centerY = panelY + (panelH - textHeight) / 2f;
        Fonts.INTER_MEDIUM.draw(noResults, centerX, centerY, textSize, new Color(112, 120, 132, (int) (170 * resultAlpha)).getRGB());
    }

    private void renderResults(float panelX, float panelY, float panelW, float panelH,
                               float mouseX, float mouseY, float resultAlpha) {

        List<ModuleStructure> results = searchHandler.getSearchResults();
        float startY = panelY + RESULTS_TOP + searchHandler.getSearchScrollOffset();
        float resultHeight = searchHandler.getSearchResultHeight();

        int newHoveredIndex = -1;

        for (int i = 0; i < results.size(); i++) {
            ModuleStructure module = results.get(i);
            float itemY = startY + i * (resultHeight + 2);

            if (itemY + resultHeight < panelY + RESULTS_TOP || itemY > panelY + panelH - 5f) continue;

            float itemAnim = searchHandler.getSearchResultAnimations().getOrDefault(module, 0f);
            float itemAlpha = itemAnim * resultAlpha;

            if (itemAlpha <= 0.01f) continue;

            float itemOffsetX = (1f - itemAnim) * 20f;

            boolean hovered = mouseX >= panelX + 4 && mouseX <= panelX + panelW - 4 &&
                    mouseY >= itemY && mouseY <= itemY + resultHeight;

            if (hovered) {
                newHoveredIndex = i;
            }

            boolean selected = module == searchHandler.getSelectedSearchModule();

            renderResultItem(module, panelX, itemY, panelW, resultHeight,
                    itemOffsetX, itemAlpha, hovered, selected);
        }

        searchHandler.setHoveredSearchIndex(newHoveredIndex);
    }

    private void renderResultItem(ModuleStructure module, float panelX, float itemY, float panelW,
                                   float resultHeight, float itemOffsetX, float itemAlpha,
                                   boolean hovered, boolean selected) {

        Color bg;
        if (selected) {
            bg = new Color(22, 34, 46, (int) (190 * itemAlpha));
        } else if (hovered) {
            bg = new Color(16, 25, 32, (int) (185 * itemAlpha));
        } else {
            bg = new Color(10, 16, 20, (int) (170 * itemAlpha));
        }

        float itemX = panelX + 5 + itemOffsetX;
        float itemW = panelW - 10;

        Render2D.rect(itemX, itemY, itemW, resultHeight, bg.getRGB(), 6);

        if (selected) {
            Render2D.rect(itemX + 4f, itemY + 5f, 1.6f, resultHeight - 10f,
                    new Color(91, 132, 255, (int) (225 * itemAlpha)).getRGB(), 1f);
            Render2D.outline(itemX, itemY, itemW, resultHeight, 0.5f,
                    new Color(74, 102, 148, (int) (145 * itemAlpha)).getRGB(), 6);
        } else if (hovered) {
            Render2D.outline(itemX, itemY, itemW, resultHeight, 0.45f,
                    new Color(48, 64, 78, (int) (100 * itemAlpha)).getRGB(), 6);
        }

        float iconX = itemX + 5f;
        float iconY = itemY + 5.2f;
        Render2D.rect(iconX, iconY, 11f, 11f,
                new Color(17, 24, 32, (int) (220 * itemAlpha)).getRGB(), 4f);
        Fonts.GUI_ICONS.draw("A", iconX + 3f, iconY + 2.8f, 6f,
                new Color(91, 132, 255, (int) (210 * itemAlpha)).getRGB());

        Color textColor = module.isState()
                ? new Color(255, 255, 255, (int) (255 * itemAlpha))
                : new Color(198, 204, 216, (int) (220 * itemAlpha));

        Fonts.INTER_SEMIBOLD.draw(trimText(module.getName(), itemW - 31f, 5.1f),
                itemX + 20f, itemY + 4.2f, 5.1f, textColor.getRGB());

        String categoryName = module.getCategory().getReadableName();
        Color categoryColor = new Color(116, 126, 140, (int) (190 * itemAlpha));
        Fonts.INTER_MEDIUM.draw(trimText(categoryName, itemW - 31f, 3.9f),
                itemX + 20f, itemY + 13.1f, 3.9f, categoryColor.getRGB());

        Color stateColor = module.isState() ? new Color(91, 132, 255) : new Color(91, 104, 118);
        Render2D.rect(itemX + itemW - 11f, itemY + 8.2f, 5.2f, 5.2f,
                new Color(stateColor.getRed(), stateColor.getGreen(), stateColor.getBlue(), (int) (225 * itemAlpha)).getRGB(), 2.6f);
    }

    private void renderScrollIndicators(float panelX, float panelY, float panelW, float panelH, float resultAlpha) {
        List<ModuleStructure> results = searchHandler.getSearchResults();
        float resultHeight = searchHandler.getSearchResultHeight();
        float visibleHeight = Math.max(20f, panelH - RESULTS_TOP - 6f);
        float maxScroll = Math.max(0, results.size() * (resultHeight + 2) - visibleHeight);

        if (maxScroll > 0) {
            if (searchHandler.getSearchScrollOffset() < -0.5f) {
                for (int i = 0; i < 10; i++) {
                    float fadeAlpha = 60 * resultAlpha * (1f - i / 10f);
                    Render2D.rect(panelX + 3, panelY + 3 + i, panelW - 6, 1,
                            new Color(8, 12, 15, (int) fadeAlpha).getRGB(), 0);
                }
            }
            if (searchHandler.getSearchScrollOffset() > -maxScroll + 0.5f) {
                for (int i = 0; i < 10; i++) {
                    float fadeAlpha = 60 * resultAlpha * (i / 10f);
                    Render2D.rect(panelX + 3, panelY + panelH - 13 + i, panelW - 6, 1,
                            new Color(8, 12, 15, (int) fadeAlpha).getRGB(), 0);
                }
            }
        }
    }

    public ModuleStructure getModuleAtPosition(double mouseX, double mouseY, float bgX, float bgY,
                                               float bgWidth, float bgHeight, SearchHandler handler) {

        if (!handler.isSearchActive() || handler.getSearchResults().isEmpty()) return null;

        float panelW = getPanelW();
        float panelH = getPanelH(bgHeight);
        float panelX = getPanelX(bgX);
        float panelY = getPanelY(bgY);

        if (mouseX < panelX + 4 || mouseX > panelX + panelW - 4 ||
                mouseY < panelY || mouseY > panelY + panelH) return null;

        float startY = panelY + RESULTS_TOP + handler.getSearchScrollOffset();
        float resultHeight = handler.getSearchResultHeight();

        List<ModuleStructure> results = handler.getSearchResults();
        for (int i = 0; i < results.size(); i++) {
            float itemY = startY + i * (resultHeight + 2);

            if (mouseY >= itemY && mouseY <= itemY + resultHeight) {
                return results.get(i);
            }
        }

        return null;
    }

    private String trimText(String text, float maxWidth, float size) {
        if (text == null) return "";
        if (Fonts.INTER_MEDIUM.getWidth(text, size) <= maxWidth) return text;
        String value = text;
        while (value.length() > 1 && Fonts.INTER_MEDIUM.getWidth(value + "..", size) > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        return value + "..";
    }
}

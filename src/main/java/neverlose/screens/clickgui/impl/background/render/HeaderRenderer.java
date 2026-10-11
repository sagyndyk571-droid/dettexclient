package neverlose.screens.clickgui.impl.background.render;

import neverlose.modules.module.category.ModuleCategory;
import neverlose.screens.clickgui.ClickGuiStylePreference;
import neverlose.screens.clickgui.impl.background.BackgroundComponent;
import neverlose.screens.clickgui.impl.background.search.SearchHandler;
import neverlose.util.render.Render2D;
import neverlose.util.render.font.Fonts;
import neverlose.util.render.shader.Scissor;

import java.awt.*;

public class HeaderRenderer {

    private static final float SIDEBAR_W = BackgroundComponent.SIDEBAR_WIDTH;
    private static final float HEADER_Y = 8.5f;
    private static final float HEADER_H = 22f;
    private static final float SEARCH_BOX_W = 112f;
    private static final float SEARCH_BOX_H = 17f;
    private static final float CONTENT_PAD_X = 8f;
    private static final float CONTROL_GAP = 8f;
    private static final float TOOL_BUTTON_W = 20f;
    private static final float CATEGORY_X_OFFSET = 36f;
    private static final float CATEGORY_W = 70f;
    private static final float GLOBAL_X_OFFSET = 114f;
    private static final float GLOBAL_W = 62f;
    private static final float SAFE_BUTTON_W = 102f;
    private static final float SAFE_BUTTON_H = 18f;
    private static final float BUTTON_H = SEARCH_BOX_H;
    private static final ModuleCategory[] DROPDOWN_CATEGORIES = {
            ModuleCategory.COMBAT,
            ModuleCategory.RENDER,
            ModuleCategory.PLAYER,
            ModuleCategory.MOVEMENT,
            ModuleCategory.MISC,
            ModuleCategory.COSMETICS,
            ModuleCategory.CONFIG
    };
    private static final String[] GLOBAL_ITEMS = {"Global", "Configs", "Reset"};

    private boolean categoryDropdownOpen = false;
    private boolean globalDropdownOpen = false;

    public static final class HeaderClickResult {
        public static final String ACTION_RESET_GUI = "reset_gui";
        public static final String ACTION_SAFE_MODE = "safe_mode";

        public final boolean consumed;
        public final ModuleCategory category;
        public final String action;

        private HeaderClickResult(boolean consumed, ModuleCategory category) {
            this(consumed, category, null);
        }

        private HeaderClickResult(boolean consumed, ModuleCategory category, String action) {
            this.consumed = consumed;
            this.category = category;
            this.action = action;
        }
    }

    public void render(float bgX, float bgY, float bgWidth, ModuleCategory selectedCategory,
                       ModuleCategory previousCategory, ModuleCategory currentCategory,
                       float headerTransition, SearchHandler searchHandler, float alphaMultiplier) {
        renderTabsAndSearch(bgX, bgY, bgWidth, selectedCategory, searchHandler, alphaMultiplier);
    }

    private void renderTabsAndSearch(float bgX, float bgY, float bgWidth, ModuleCategory selectedCategory,
                                     SearchHandler searchHandler, float alphaMultiplier) {
        float contentLeft = bgX + SIDEBAR_W;
        float contentWidth = BackgroundComponent.CONTENT_WIDTH;
        float buttonY = getControlY(bgY);
        float buttonH = BUTTON_H;
        float tabX = contentLeft + CONTENT_PAD_X;
        int alpha = (int) (255 * alphaMultiplier);

        renderHeaderButton(tabX, buttonY, TOOL_BUTTON_W, buttonH, alphaMultiplier, ClickGuiStylePreference.isGlassStyle());
        int toolIcon = ClickGuiStylePreference.isGlassStyle()
                ? new Color(135, 160, 255, alpha).getRGB()
                : new Color(220, 225, 235, alpha).getRGB();
        Fonts.GUI_ICONS.draw("A", tabX + 5.5f, buttonY + 4.6f, 9f, toolIcon);
        tabX += TOOL_BUTTON_W + CONTROL_GAP;

        String categoryName = selectedCategory != null ? selectedCategory.getReadableName() : "Combat";
        float categoryW = CATEGORY_W;
        renderHeaderButton(tabX, buttonY, categoryW, buttonH, alphaMultiplier);
        Fonts.INTER_MEDIUM.draw(trimText(categoryName, categoryW - 18f, 6f), tabX +6f, buttonY + 5.45f, 6f,
                new Color(225, 228, 238, alpha).getRGB());
        Fonts.ICONS2.draw("A", tabX + categoryW - 12f, buttonY + 5.25f, 5.5f,
                new Color(170, 176, 190, alpha).getRGB());
        tabX += categoryW + CONTROL_GAP;

        renderHeaderButton(tabX, buttonY, GLOBAL_W, buttonH, alphaMultiplier);
        Fonts.INTER_MEDIUM.draw("Global", tabX + 9f, buttonY + 5.45f, 6f, new Color(205, 210, 224, alpha).getRGB());
        Fonts.ICONS2.draw("D", tabX + 49f, buttonY + 6.25f, 5.5f, new Color(170, 176, 190, alpha).getRGB());

        renderSafeModeButton(getSafeButtonX(bgX, bgWidth), getSafeButtonY(bgY), alphaMultiplier);

        float searchBoxX = getSearchBoxX(bgX);
        float searchBoxY = getControlY(bgY);
        if (!searchHandler.isSearchActive()) {
            renderSearchIconButton(searchBoxX + SEARCH_BOX_W - SEARCH_BOX_H, searchBoxY, SEARCH_BOX_H, alphaMultiplier);
        } else {
        int searchBg = new Color(14, 17, 25, (int) (205 * alphaMultiplier)).getRGB();
        int searchOutline = new Color(47, 56, 77, (int) (140 * alphaMultiplier)).getRGB();
        Render2D.rect(searchBoxX, searchBoxY, SEARCH_BOX_W, SEARCH_BOX_H, searchBg, 5f);
        Render2D.outline(searchBoxX, searchBoxY, SEARCH_BOX_W, SEARCH_BOX_H, 0.5f, searchOutline, 5f);

        float textX = searchBoxX + 10f;
        if (searchHandler.getSearchText().isEmpty()) {
            Fonts.INTER_MEDIUM.draw("Search modules", textX, searchBoxY + 5.35f, 5f,
                    new Color(106, 114, 132, (int) (155 * alphaMultiplier)).getRGB());
            renderSearchPlaceholder(searchBoxX, searchBoxY, SEARCH_BOX_H, textX, searchHandler, alphaMultiplier,
                    searchHandler.isSearchActive());
        } else {
            renderSearchText(searchBoxX, searchBoxY, SEARCH_BOX_W, SEARCH_BOX_H, textX, searchHandler, alphaMultiplier);
        }
        }

        float sepY = buttonY + BUTTON_H + 3f;
        Render2D.rect(contentLeft, sepY, contentWidth, 0.7f,
                new Color(34, 45, 55, (int) (130 * alphaMultiplier)).getRGB(), 0);

        if (categoryDropdownOpen) {
            renderCategoryDropdown(contentLeft + CATEGORY_X_OFFSET, buttonY + buttonH + 4f, categoryW, selectedCategory, alphaMultiplier);
        }
        if (globalDropdownOpen) {
            renderGlobalDropdown(contentLeft + GLOBAL_X_OFFSET, buttonY + buttonH + 4f, GLOBAL_W, alphaMultiplier);
        }
    }

    private void renderHeaderButton(float x, float y, float w, float h, float alphaMultiplier) {
        renderHeaderButton(x, y, w, h, alphaMultiplier, false);
    }

    private void renderHeaderButton(float x, float y, float w, float h, float alphaMultiplier, boolean active) {
        int bg = active
                ? new Color(18, 24, 34, (int) (205 * alphaMultiplier)).getRGB()
                : new Color(12, 16, 23, (int) (190 * alphaMultiplier)).getRGB();
        int border = active
                ? new Color(91, 132, 255, (int) (135 * alphaMultiplier)).getRGB()
                : new Color(42, 53, 68, (int) (105 * alphaMultiplier)).getRGB();
        Render2D.rect(x, y, w, h, bg, 5f);
        Render2D.outline(x, y, w, h, 0.5f, border, 5f);
    }

    private void renderSafeModeButton(float x, float y, float alphaMultiplier) {
        int alpha = (int) (255 * alphaMultiplier);
        int glow = new Color(82, 116, 255, (int) (34 * alphaMultiplier)).getRGB();
        int left = new Color(30, 42, 68, (int) (222 * alphaMultiplier)).getRGB();
        int right = new Color(15, 22, 36, (int) (222 * alphaMultiplier)).getRGB();
        int text = new Color(236, 241, 255, alpha).getRGB();
        int icon = new Color(128, 160, 255, alpha).getRGB();

        Render2D.rect(x - 2f, y - 2f, SAFE_BUTTON_W + 4f, SAFE_BUTTON_H + 4f, glow, 9f);
        Render2D.gradientRect(x, y, SAFE_BUTTON_W, SAFE_BUTTON_H, new int[]{left, right, right, left}, 8f);
        Fonts.GUI_ICONS.draw("A", x + 9f, y + 4.8f, 8f, icon);
        Fonts.INTER_MEDIUM.draw("Safe Mode", x + 24f, y + 5.8f, 5.8f, text);
    }

    private void renderSearchIconButton(float x, float y, float size, float alphaMultiplier) {
        int bg = new Color(12, 16, 23, (int) (175 * alphaMultiplier)).getRGB();
        int border = new Color(42, 53, 68, (int) (95 * alphaMultiplier)).getRGB();
        int icon = new Color(210, 216, 230, (int) (230 * alphaMultiplier)).getRGB();
        Render2D.rect(x, y, size, size, bg, 5f);
        Render2D.outline(x, y, size, size, 0.5f, border, 5f);
        Fonts.ICONS1.draw("H", x + 5.25f, y + 4.9f, 7.2f, icon);
    }

    private void renderCategoryDropdown(float x, float y, float w, ModuleCategory selectedCategory, float alphaMultiplier) {
        float rowH = 14f;
        float h = DROPDOWN_CATEGORIES.length * rowH + 6f;
        int bg = new Color(10, 14, 21, (int) (238 * alphaMultiplier)).getRGB();
        int outline = new Color(52, 65, 84, (int) (150 * alphaMultiplier)).getRGB();
        Render2D.rect(x, y, w, h, bg, 5f);
        Render2D.outline(x, y, w, h, 0.5f, outline, 5f);

        for (int i = 0; i < DROPDOWN_CATEGORIES.length; i++) {
            ModuleCategory category = DROPDOWN_CATEGORIES[i];
            float rowY = y + 3f + i * rowH;
            boolean active = category == selectedCategory;
            if (active) {
                Render2D.rect(x + 3f, rowY + 1f, w - 6f, rowH - 2f,
                        new Color(38, 49, 66, (int) (210 * alphaMultiplier)).getRGB(), 3f);
            }
            int color = active
                    ? new Color(232, 238, 255, (int) (255 * alphaMultiplier)).getRGB()
                    : new Color(158, 166, 182, (int) (220 * alphaMultiplier)).getRGB();
            Fonts.INTER_MEDIUM.draw(category.getReadableName(), x + 8f, rowY + 4.1f, 5.3f, color);
        }
    }

    private void renderGlobalDropdown(float x, float y, float w, float alphaMultiplier) {
        float rowH = 14f;
        float h = GLOBAL_ITEMS.length * rowH + 6f;
        int bg = new Color(10, 14, 21, (int) (238 * alphaMultiplier)).getRGB();
        int outline = new Color(52, 65, 84, (int) (150 * alphaMultiplier)).getRGB();
        Render2D.rect(x, y, w, h, bg, 5f);
        Render2D.outline(x, y, w, h, 0.5f, outline, 5f);

        for (int i = 0; i < GLOBAL_ITEMS.length; i++) {
            float rowY = y + 3f + i * rowH;
            if (i == 0) {
                Render2D.rect(x + 3f, rowY + 1f, w - 6f, rowH - 2f,
                        new Color(38, 49, 66, (int) (210 * alphaMultiplier)).getRGB(), 3f);
            }
            int color = i == 0
                    ? new Color(232, 238, 255, (int) (255 * alphaMultiplier)).getRGB()
                    : new Color(158, 166, 182, (int) (220 * alphaMultiplier)).getRGB();
            Fonts.INTER_MEDIUM.draw(trimText(GLOBAL_ITEMS[i], w - 12f, 5.3f), x + 8f, rowY + 4.1f, 5.3f, color);
        }
    }

    private void renderSearchText(float searchBoxX, float searchBoxY, float searchBoxW, float searchBoxH,
                                  float textAreaX, SearchHandler searchHandler, float alphaMultiplier) {
        Scissor.enable(textAreaX, searchBoxY, searchBoxW - (textAreaX - searchBoxX) - 6f, searchBoxH, 2);

        if (searchHandler.hasSearchSelection() && searchHandler.getSearchSelectionAnimation() > 0.01f) {
            renderSearchSelection(textAreaX, searchBoxY, searchBoxH, searchHandler, alphaMultiplier);
        }

        Fonts.INTER_MEDIUM.draw(searchHandler.getSearchText(), textAreaX, searchBoxY + 5.35f, 5,
                new Color(220, 225, 238, (int) (255 * alphaMultiplier)).getRGB());
        Scissor.disable();

        if (!searchHandler.hasSearchSelection()) {
            renderSearchCursor(textAreaX, searchBoxY, searchBoxH, searchHandler, alphaMultiplier);
        }
    }

    private void renderSearchSelection(float textAreaX, float searchBoxY, float searchBoxH,
                                       SearchHandler searchHandler, float alphaMultiplier) {
        int start = searchHandler.getSearchSelectionStart();
        int end = searchHandler.getSearchSelectionEnd();
        String beforeSelection = searchHandler.getSearchText().substring(0, start);
        String selection = searchHandler.getSearchText().substring(start, end);

        float selectionX = textAreaX + Fonts.INTER_MEDIUM.getWidth(beforeSelection, 5);
        float selectionWidth = Fonts.INTER_MEDIUM.getWidth(selection, 5);

        int selAlpha = (int) (100 * searchHandler.getSearchSelectionAnimation() * alphaMultiplier);
        Render2D.rect(selectionX, searchBoxY + 2, selectionWidth, searchBoxH - 4,
                new Color(100, 140, 180, selAlpha).getRGB(), 2f);
    }

    private void renderSearchCursor(float textAreaX, float searchBoxY, float searchBoxH,
                                    SearchHandler searchHandler, float alphaMultiplier) {
        if (!searchHandler.isSearchActive()) return;
        float cursorAlpha = (float) (Math.sin(searchHandler.getSearchCursorBlink() * Math.PI * 2) * 0.5 + 0.5);
        if (cursorAlpha <= 0.3f) return;
        String beforeCursor = searchHandler.getSearchText().substring(0, searchHandler.getSearchCursorPosition());
        float cursorX = textAreaX + Fonts.INTER_MEDIUM.getWidth(beforeCursor, 5);
        int cursorAlphaInt = (int) (255 * cursorAlpha * alphaMultiplier);
        Render2D.rect(cursorX, searchBoxY + searchBoxH - 5f, 4.5f, 1f,
                new Color(180, 180, 185, cursorAlphaInt).getRGB(), 0);
    }

    private void renderSearchPlaceholder(float searchBoxX, float searchBoxY, float searchBoxH,
                                         float textAreaX, SearchHandler searchHandler, float alphaMultiplier, boolean showCursor) {
        if (showCursor) {
            float cursorAlpha = (float) (Math.sin(searchHandler.getSearchCursorBlink() * Math.PI * 2) * 0.5 + 0.5);
            if (cursorAlpha > 0.3f) {
                int cursorAlphaInt = (int) (255 * cursorAlpha * alphaMultiplier);
                Render2D.rect(textAreaX + 2f, searchBoxY + searchBoxH - 5f, 3.5f, 0.7f,
                        new Color(180, 180, 185, cursorAlphaInt).getRGB(), 0);
            }
        }
    }

    public boolean isSearchBoxHovered(double mouseX, double mouseY, float bgX, float bgY, boolean searchActive) {
        float searchBoxX = getSearchBoxX(bgX);
        float searchBoxY = getControlY(bgY);
        float hitX = searchActive ? searchBoxX : searchBoxX + SEARCH_BOX_W - SEARCH_BOX_H;
        float hitW = searchActive ? SEARCH_BOX_W : SEARCH_BOX_H;
        return mouseX >= hitX && mouseX <= hitX + hitW &&
                mouseY >= searchBoxY && mouseY <= searchBoxY + SEARCH_BOX_H;
    }

    public HeaderClickResult handleHeaderClick(double mouseX, double mouseY, float bgX, float bgY, int button) {
        if (button != 0) {
            return new HeaderClickResult(false, null);
        }

        float contentLeft = bgX + SIDEBAR_W;
        float buttonY = getControlY(bgY);
        float toolX = contentLeft + CONTENT_PAD_X;
        float categoryX = contentLeft + CATEGORY_X_OFFSET;
        float globalX = contentLeft + GLOBAL_X_OFFSET;
        float safeX = getSafeButtonX(bgX, BackgroundComponent.BG_WIDTH);
        float safeY = getSafeButtonY(bgY);
        float dropdownY = buttonY + BUTTON_H + 4f;

        if (mouseX >= toolX && mouseX <= toolX + TOOL_BUTTON_W && mouseY >= buttonY && mouseY <= buttonY + BUTTON_H) {
            ClickGuiStylePreference.toggleGlassStyle();
            categoryDropdownOpen = false;
            globalDropdownOpen = false;
            return new HeaderClickResult(true, null);
        }

        if (categoryDropdownOpen) {
            float rowH = 14f;
            float dropdownH = DROPDOWN_CATEGORIES.length * rowH + 6f;
            if (mouseX >= categoryX && mouseX <= categoryX + CATEGORY_W && mouseY >= dropdownY && mouseY <= dropdownY + dropdownH) {
                int index = (int) ((mouseY - dropdownY - 3f) / rowH);
                if (index >= 0 && index < DROPDOWN_CATEGORIES.length) {
                    categoryDropdownOpen = false;
                    globalDropdownOpen = false;
                    return new HeaderClickResult(true, DROPDOWN_CATEGORIES[index]);
                }
            }
        }

        if (globalDropdownOpen) {
            float rowH = 14f;
            float dropdownH = GLOBAL_ITEMS.length * rowH + 6f;
            if (mouseX >= globalX && mouseX <= globalX + GLOBAL_W && mouseY >= dropdownY && mouseY <= dropdownY + dropdownH) {
                int index = (int) ((mouseY - dropdownY - 3f) / rowH);
                globalDropdownOpen = false;
                categoryDropdownOpen = false;
                if (index == 1) {
                    return new HeaderClickResult(true, ModuleCategory.CONFIG);
                }
                if (index == 2) {
                    return new HeaderClickResult(true, null, HeaderClickResult.ACTION_RESET_GUI);
                }
                return new HeaderClickResult(true, null);
            }
        }

        if (mouseX >= categoryX && mouseX <= categoryX + CATEGORY_W && mouseY >= buttonY && mouseY <= buttonY + BUTTON_H) {
            categoryDropdownOpen = !categoryDropdownOpen;
            globalDropdownOpen = false;
            return new HeaderClickResult(true, null);
        }

        if (mouseX >= globalX && mouseX <= globalX + GLOBAL_W && mouseY >= buttonY && mouseY <= buttonY + BUTTON_H) {
            globalDropdownOpen = !globalDropdownOpen;
            categoryDropdownOpen = false;
            return new HeaderClickResult(true, null);
        }

        if (mouseX >= safeX && mouseX <= safeX + SAFE_BUTTON_W && mouseY >= safeY && mouseY <= safeY + SAFE_BUTTON_H) {
            categoryDropdownOpen = false;
            globalDropdownOpen = false;
            return new HeaderClickResult(true, null, HeaderClickResult.ACTION_SAFE_MODE);
        }

        if (categoryDropdownOpen || globalDropdownOpen) {
            categoryDropdownOpen = false;
            globalDropdownOpen = false;
            return new HeaderClickResult(true, null);
        }

        return new HeaderClickResult(false, null);
    }

    private float getSearchBoxX(float bgX) {
        return bgX + SIDEBAR_W + BackgroundComponent.CONTENT_WIDTH - SEARCH_BOX_W - 8f;
    }

    private float getSafeButtonX(float bgX, float bgWidth) {
        return bgX + (bgWidth - SAFE_BUTTON_W) * 0.5f;
    }

    private float getSafeButtonY(float bgY) {
        return bgY - SAFE_BUTTON_H - 7f;
    }

    private float getControlY(float bgY) {
        return bgY + HEADER_Y;
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

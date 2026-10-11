package neverlose.screens.clickgui.impl.module;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import neverlose.IMinecraft;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.SettingComponentAdder;
import neverlose.screens.clickgui.impl.module.handler.ModuleAnimationHandler;
import neverlose.screens.clickgui.impl.module.handler.ModuleBindHandler;
import neverlose.screens.clickgui.impl.module.handler.ModuleFavoriteHandler;
import neverlose.screens.clickgui.impl.module.handler.ModuleScrollHandler;
import neverlose.screens.clickgui.impl.module.render.ModuleGridRenderer;
import neverlose.screens.clickgui.impl.module.render.ModuleListRenderer;
import neverlose.screens.clickgui.impl.module.render.SettingsPanelRenderer;
import neverlose.screens.clickgui.impl.module.render.WexsideTwoColumnLayout;
import neverlose.screens.clickgui.impl.module.render.WexsideTwoColumnRenderer;
import neverlose.util.render.Render2D;
import neverlose.util.render.font.Fonts;
import neverlose.util.render.shader.Scissor;

import neverlose.screens.clickgui.WexTrackMetrics;
import neverlose.screens.clickgui.impl.module.util.ModuleDisplayHelper;
import neverlose.util.interfaces.AbstractSettingComponent;

import java.awt.*;
import java.util.*;
import java.util.List;

@Getter
@Setter
public class ModuleComponent implements IMinecraft {

    private List<ModuleStructure> modules = new ArrayList<>();
    private List<ModuleStructure> displayModules = new ArrayList<>();
    private ModuleStructure selectedModule = null;
    private ModuleStructure bindingModule = null;
    private List<AbstractSettingComponent> settingComponents = new ArrayList<>();
    
    // Карта компонентов настроек для каждого модуля (для отображения всех настроек одновременно)
    private final Map<ModuleStructure, List<AbstractSettingComponent>> moduleSettingsMap = new HashMap<>();

    private final BindPopupWindow bindPopup = new BindPopupWindow();
    private final SettingsPopupWindow settingsPopup = new SettingsPopupWindow();

    private ModuleCategory currentCategory = null;

    public List<AbstractSettingComponent> getSettingComponents() {
        return settingComponents;
    }

    public ModuleStructure getBindingModule() {
        return bindingModule;
    }

    public ModuleStructure getSelectedModule() {
        return selectedModule;
    }

    public List<ModuleStructure> getModules() {
        return modules;
    }

    public List<ModuleStructure> getDisplayModules() {
        return displayModules;
    }

    public BindPopupWindow getBindPopup() {
        return bindPopup;
    }

    public SettingsPopupWindow getSettingsPopup() {
        return settingsPopup;
    }

    private final ModuleAnimationHandler animationHandler;
    private final ModuleScrollHandler scrollHandler;
    private final ModuleFavoriteHandler favoriteHandler;
    private final ModuleBindHandler bindHandler;
    private final ModuleListRenderer listRenderer;
    private final SettingsPanelRenderer settingsRenderer;
    private final ModuleGridRenderer gridRenderer;
    private final ModuleDisplayHelper displayHelper;


    private int savedGuiScale = 1;
    private float lastMouseX = 0, lastMouseY = 0;
    private float lastListX = 0, lastListY = 0, lastListWidth = 0, lastListHeight = 0;

    public ModuleComponent() {
        this.animationHandler = new ModuleAnimationHandler();
        this.scrollHandler = new ModuleScrollHandler();
        this.favoriteHandler = new ModuleFavoriteHandler();
        this.bindHandler = new ModuleBindHandler();
        this.displayHelper = new ModuleDisplayHelper();
        this.listRenderer = new ModuleListRenderer(animationHandler, bindHandler, displayHelper);
        this.settingsRenderer = new SettingsPanelRenderer(animationHandler);
        this.gridRenderer = new ModuleGridRenderer();

    }

    public void updateModules(List<ModuleStructure> newModules, ModuleCategory category) {
        if (category == currentCategory) return;

        animationHandler.prepareTransition(modules, displayModules);
        currentCategory = category;
        modules = newModules;
        rebuildDisplayList();

        scrollHandler.resetModuleScroll();
        scrollHandler.resetGridScroll();
        animationHandler.initModuleAnimations(displayModules);
        displayHelper.updateModulesWithSettings(displayModules);
        
        // Инициализируем настройки для всех модулей
        initializeAllModuleSettings();

        if (animationHandler.shouldScrollToModule() && displayModules.contains(animationHandler.getScrollTargetModule())) {
            scrollToModuleAndHighlight(animationHandler.getScrollTargetModule());
            animationHandler.clearScrollTarget();
        } else if (displayModules.isEmpty()) {
            selectedModule = null;
            settingComponents.clear();
        } else if (selectedModule != null && !displayModules.contains(selectedModule)) {
            selectedModule = null;
            settingComponents.clear();
        }
    }
    
    private void initializeAllModuleSettings() {
        moduleSettingsMap.clear();
        for (ModuleStructure module : displayModules) {
            List<AbstractSettingComponent> components = new ArrayList<>();
            new SettingComponentAdder().addSettingComponent(module.settings(), components);
            prepareInlineSettings(components);
            moduleSettingsMap.put(module, components);
            animationHandler.initSettingAnimations(components);
        }
    }

    private void rebuildDisplayList() {
        displayModules.clear();
        List<ModuleStructure> favorites = new ArrayList<>();
        List<ModuleStructure> nonFavorites = new ArrayList<>();

        for (ModuleStructure mod : modules) {
            if (mod.isFavorite()) favorites.add(mod);
            else nonFavorites.add(mod);
        }

        displayModules.addAll(favorites);
        displayModules.addAll(nonFavorites);
    }

    public void toggleFavorite(ModuleStructure module) {
        favoriteHandler.toggleFavorite(module, displayModules, animationHandler);
        rebuildDisplayList();
    }

    public void selectModuleFromSearch(ModuleStructure module) {
        animationHandler.setScrollTarget(module);
    }

    public void scrollToModuleAndHighlight(ModuleStructure module) {
        if (module == null || !displayModules.contains(module)) return;

        selectModule(module);
        int moduleIndex = displayModules.indexOf(module);
        if (moduleIndex >= 0 && scrollHandler.getLastModuleListHeight() > 0) {
            scrollHandler.scrollToModule(moduleIndex, displayModules.size());
        }
        animationHandler.startHighlight(module);
    }

    public void selectModule(ModuleStructure module) {
        if (module == selectedModule) return;

        selectedModule = module;
        scrollHandler.resetSettingScroll();
        settingComponents.clear();
        animationHandler.clearSettingAnimations();

        if (module == null) return;

        new SettingComponentAdder().addSettingComponent(module.settings(), settingComponents);
        animationHandler.initSettingAnimations(settingComponents);
    }

    /** Открывает отдельное перетаскиваемое окошко бинда (Bind + Visible) по клику на шестерёнку */
    public void openBindPopup(ModuleStructure module, float screenX, float screenY) {
        bindPopup.open(module, screenX, screenY);
    }

    public void closeBindPopup() {
        bindPopup.close();
    }

    public void openSettingsPopup(ModuleStructure module, float screenX, float screenY) {
        settingsPopup.open(module, screenX, screenY);
    }

    public void closeSettingsPopup() {
        settingsPopup.close();
    }
    
    /** Получить настройки конкретного модуля */
    public List<AbstractSettingComponent> getModuleSettings(ModuleStructure module) {
        return moduleSettingsMap.get(module);
    }
    
    /** Получить карту всех настроек модулей */
    public Map<ModuleStructure, List<AbstractSettingComponent>> getModuleSettingsMap() {
        return moduleSettingsMap;
    }

    private void prepareInlineSettings(List<AbstractSettingComponent> components) {
        if (components == null) return;
        for (AbstractSettingComponent component : components) {
            component.setWexsideStyle(true);
        }
    }

    private float calculateInlineSettingsHeight(List<AbstractSettingComponent> components) {
        prepareInlineSettings(components);
        animationHandler.syncSettingVisibilityForLayout(components);
        return settingsRenderer.calculateTotalHeight(components, animationHandler);
    }

    private float wexInlineClipHeight(float totalSettingsHeight) {
        if (totalSettingsHeight <= 0) return 10f;  // 12 * 0.8
        // Ограничиваем высоту клипа
        return Math.min(totalSettingsHeight, WexsideTwoColumnLayout.MAX_INLINE_SETTINGS_H);
    }

    /** Подкрутить общий скролл так, чтобы раскрытая карточка прижалась к верху области (меньше пустоты). */
    public void scrollWexListToOpenModule(float panelY, float contentH) {
        if (selectedModule == null) return;
        int idx = displayModules.indexOf(selectedModule);
        if (idx < 0) return;
        
        float contentY = panelY + WexsideTwoColumnRenderer.CONTENT_TOP;
        int n = displayModules.size();
        float[] cellY = new float[n];
        float[] cellH = new float[n];
        
        // Вычисляем позиции с реальными высотами
        float leftY = contentY;
        float rightY = contentY;
        for (int i = 0; i < n; i++) {
            ModuleStructure m = displayModules.get(i);
            
            float moduleSettingsHeight = 0f;
            if (m.settings() != null && !m.settings().isEmpty()) {
                List<AbstractSettingComponent> moduleSettings = moduleSettingsMap.get(m);
                if (moduleSettings != null) {
                    moduleSettingsHeight = calculateInlineSettingsHeight(moduleSettings);
                }
            }
            
            float h = WexsideTwoColumnLayout.cellHeight(m, selectedModule, moduleSettingsHeight);
            
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
        
        float padTop = 0f;
        float targetScroll = padTop - (cellY[idx] - contentY);
        float totalContentH = computeTotalWexContentHeight();
        scrollHandler.snapWexModuleScroll(targetScroll, contentH, totalContentH);
    }

    /** Две колонки: настройки внутри раскрытой карточки, сетка сдвигается вниз. */
    public void renderWexsideTwoColumns(DrawContext context, float panelX, float panelY, float contentH,
                                        float mouseX, float mouseY, int guiScale, float alphaMultiplier, float delta) {
        if (displayModules.isEmpty()) return;

        float contentY = panelY + WexsideTwoColumnRenderer.CONTENT_TOP;
        float scrollF = (float) scrollHandler.getModuleDisplayScroll();

        int n = displayModules.size();
        float[] cellY = new float[n];
        float[] cellH = new float[n];
        
        // Вычисляем высоту для каждого модуля отдельно
        float leftY = contentY + scrollF;
        float rightY = contentY + scrollF;
        for (int i = 0; i < n; i++) {
            ModuleStructure m = displayModules.get(i);
            
            // Получаем настройки для модуля
            float moduleSettingsHeight = 0f;
            if (m.settings() != null && !m.settings().isEmpty()) {
                List<AbstractSettingComponent> moduleSettings = moduleSettingsMap.get(m);
                if (moduleSettings == null) {
                    moduleSettings = new ArrayList<>();
                    new SettingComponentAdder().addSettingComponent(m.settings(), moduleSettings);
                    prepareInlineSettings(moduleSettings);
                    moduleSettingsMap.put(m, moduleSettings);
                    animationHandler.initSettingAnimations(moduleSettings);
                }
                moduleSettingsHeight = calculateInlineSettingsHeight(moduleSettings);
            }
            
            float h = WexsideTwoColumnLayout.cellHeight(m, selectedModule, moduleSettingsHeight);
            
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

        float contentX = panelX + WexsideTwoColumnRenderer.COL_LEFT - 1f;
        float clipW = WexsideTwoColumnRenderer.COL_RIGHT - WexsideTwoColumnRenderer.COL_LEFT + WexsideTwoColumnRenderer.COL_W + 2f;

        int a255 = (int) (255 * alphaMultiplier);
        // Neverlose стиль: цвет карточек (14, 22, 27)
        int cardTopBg = new Color(10, 16, 19, a255).getRGB();
        int cardBottomBg = new Color(8, 12, 15, a255).getRGB();
        int textPri = new Color(224, 226, 235, a255).getRGB();
        int textSec = new Color(218, 220, 230, a255).getRGB();
        int trackOff = new Color(WexTrackMetrics.TRACK_OFF_R, WexTrackMetrics.TRACK_OFF_G, WexTrackMetrics.TRACK_OFF_B, a255).getRGB();
        int trackOn = new Color(WexTrackMetrics.TRACK_ON_R, WexTrackMetrics.TRACK_ON_G, WexTrackMetrics.TRACK_ON_B, a255).getRGB();
        int knobOff = new Color(100, 110, 130, a255).getRGB();
        int knobOn = new Color(255, 255, 255, a255).getRGB();
        
        // Outline для карточек
        int outlineAlpha = (int) (112 * alphaMultiplier);
        int cardOutlineColor = new Color(38, 50, 60, outlineAlpha).getRGB();

        float clipPadY = 0f;
        float clipTop = contentY - clipPadY;
        float clipBottom = contentY + contentH + clipPadY;
        Scissor.enable(contentX, clipTop, clipW, contentH + clipPadY * 2f, guiScale);

        for (int i = 0; i < n; i++) {
            ModuleStructure m = displayModules.get(i);
            float x = panelX + (i % 2 == 0 ? WexsideTwoColumnRenderer.COL_LEFT : WexsideTwoColumnRenderer.COL_RIGHT);
            float cy = cellY[i];
            float ch = cellH[i];
            float w = WexsideTwoColumnRenderer.COL_W;
            boolean hasSettings = m.settings() != null && !m.settings().isEmpty();
            if (cy + ch < clipTop || cy > clipBottom) continue;

            // Название модуля БЕЗ ректа (просто текст сверху над карточкой)
            String sectionName = trimText(m.getName().toUpperCase(), w - 8f, 5.6f);
            Fonts.INTER_SEMIBOLD.draw(sectionName, x + 7f, cy,
                    5.6f, new Color(236, 240, 248, a255).getRGB());

            // Рект карточки начинается НИЖЕ названия
            float cardY = cy + 10f; // Отступ для названия (12 * 0.8)
            float cardH = ch - 10f;
            Render2D.gradientRect(x, cardY, w, cardH,
                    new int[]{cardTopBg, cardTopBg, cardBottomBg, cardBottomBg}, 9);
            
            // Outline для карточки - тонкий
            Render2D.outline(x, cardY, w, cardH, 0.55f, cardOutlineColor, 9);

            // Enabled ВСЕГДА показывается
            Fonts.INTER_SEMIBOLD.draw("Enabled", x + 7f, cardY + 6.2f, 6f, textSec);
            Fonts.INTER_SEMIBOLD.draw("...", x + w - 48f, cardY + 4f, 6f,
                    new Color(205, 208, 220, (int) (220 * alphaMultiplier)).getRGB());
            
            // Toggle справа как в настройке умных критов
            boolean on = m.isState();
            float toggleX = x + w - 29f;  // 32 * 0.8
            float toggleY = cardY + 4f;   // 5 * 0.8
            float toggleW = 20f;          // 24 * 0.8
            float toggleH = 11f;           // 11 * 0.8
            
            // Дорожка toggle
            float p = on ? 1f : 0f;
            int tr = (int) (WexTrackMetrics.TRACK_OFF_R + (WexTrackMetrics.TRACK_ON_R - WexTrackMetrics.TRACK_OFF_R) * p);
            int tg = (int) (WexTrackMetrics.TRACK_OFF_G + (WexTrackMetrics.TRACK_ON_G - WexTrackMetrics.TRACK_OFF_G) * p);
            int tb = (int) (WexTrackMetrics.TRACK_OFF_B + (WexTrackMetrics.TRACK_ON_B - WexTrackMetrics.TRACK_OFF_B) * p);
            int toggleAlpha = (int) (255 * alphaMultiplier);
            int trackColor = new Color(tr, tg, tb, toggleAlpha).getRGB();
            Render2D.rect(toggleX, toggleY, toggleW, toggleH, trackColor, toggleH * 0.5f);
            
            // Кнопка toggle
            float knobW = 9.0f;  // 7 * 0.8
            float knobH = 9.0f;  // 7 * 0.8
            float travel = Math.max(0f, toggleW - knobW - 3.2f);  // 4 * 0.8, 3 * 0.8
            float knobX = toggleX + 2.0f + travel * p;  // 2 * 0.8
            float knobY = toggleY + (toggleH - knobH) * 0.5f;
            int kr = (int) (118 + (248 - 118) * p);
            int kg = (int) (127 + (250 - 127) * p);
            int kb = (int) (139 + (255 - 139) * p);
            int knobColor = new Color(kr, kg, kb, toggleAlpha).getRGB();
            Render2D.rect(knobX, knobY, knobW , knobH, knobColor, knobH * 0.5f);
            if (hasSettings) {
                Render2D.rect(x + 7f, cardY + 17.5f, w - 14f, 0.35f,
                        new Color(31, 42, 48, (int) (145 * alphaMultiplier)).getRGB(), 0);
            }

            // Настройки показываются только если они есть и модуль имеет увеличенную высоту
            if (ch > WexsideTwoColumnRenderer.ROW_H && hasSettings) {
                float inlineY = cardY + 18f;  // 23 * 0.8
                
                // Получаем настройки для текущего модуля из кэша
                List<AbstractSettingComponent> moduleSettings = moduleSettingsMap.get(m);
                if (moduleSettings == null) {
                    moduleSettings = new ArrayList<>();
                    new SettingComponentAdder().addSettingComponent(m.settings(), moduleSettings);
                    prepareInlineSettings(moduleSettings);
                    moduleSettingsMap.put(m, moduleSettings);
                    animationHandler.initSettingAnimations(moduleSettings);
                }
                
                // Вычисляем высоту настроек для этого модуля
                float moduleSettingsHeight = calculateInlineSettingsHeight(moduleSettings);
                float inlineH = Math.min(wexInlineClipHeight(moduleSettingsHeight) + 3f,
                        Math.max(0f, cardY + cardH - inlineY - 3f));

                for (AbstractSettingComponent c : moduleSettings) {
                    c.setWexsideStyle(true);
                }

                if (!moduleSettings.isEmpty()) {
                    settingsRenderer.renderCardInline(context, moduleSettings, x + 6f, inlineY, w - 12f, inlineH,  // 8 * 0.75, 16 * 0.75
                            mouseX, mouseY, delta, guiScale, alphaMultiplier, scrollHandler, animationHandler);
                }
            }
        }

        Scissor.disable();
    }

    public WexsideTwoColumnLayout.HitResult pickWexHit(double mouseX, double mouseY, float panelX, float panelY, float contentH) {
        // Вычисляем высоту настроек для каждого модуля (настройки всегда открыты)
        float[] moduleSettingsHeights = new float[displayModules.size()];
        for (int i = 0; i < displayModules.size(); i++) {
            ModuleStructure m = displayModules.get(i);
            float moduleSettingsHeight = 0f;
            if (m.settings() != null && !m.settings().isEmpty()) {
                List<AbstractSettingComponent> moduleSettings = moduleSettingsMap.get(m);
                if (moduleSettings != null) {
                    moduleSettingsHeight = calculateInlineSettingsHeight(moduleSettings);
                }
            }
            moduleSettingsHeights[i] = moduleSettingsHeight;
        }
        
        float contentY = panelY + WexsideTwoColumnRenderer.CONTENT_TOP;
        return WexsideTwoColumnLayout.hitTest(mouseX, mouseY, contentY, contentH,
                (float) scrollHandler.getModuleDisplayScroll(), displayModules, selectedModule, moduleSettingsHeights, panelX);
    }

    public void updateWexScrollFades(float contentH) {
        float totalContentH = computeTotalWexContentHeight();
        scrollHandler.updateWexFades(contentH, totalContentH);
    }

    public void updateWexScrollFadesWithSettings(float contentH) {
        float totalContentH = computeTotalWexContentHeight();
        float totalSettingsH = calculateTotalSettingHeight();
        float inlineClip = wexInlineClipHeight(totalSettingsH);
        scrollHandler.updateWexFadesWithSettings(contentH, totalContentH, totalSettingsH, inlineClip);
    }

    public void handleWexTwoColumnScroll(double vertical, float contentH) {
        if (animationHandler.isCategoryTransitioning()) return;
        float totalContentH = computeTotalWexContentHeight();
        scrollHandler.handleWexTwoColumnScroll(vertical, contentH, totalContentH);
    }
    
    private float computeTotalWexContentHeight() {
        if (displayModules.isEmpty()) return 0;
        
        int n = displayModules.size();
        float leftY = 0f;
        float rightY = 0f;
        
        for (int i = 0; i < n; i++) {
            ModuleStructure m = displayModules.get(i);
            
            // Получаем настройки для модуля
            float moduleSettingsHeight = 0f;
            if (m.settings() != null && !m.settings().isEmpty()) {
                List<AbstractSettingComponent> moduleSettings = moduleSettingsMap.get(m);
                if (moduleSettings != null) {
                    moduleSettingsHeight = calculateInlineSettingsHeight(moduleSettings);
                }
            }
            
            float h = WexsideTwoColumnLayout.cellHeight(m, selectedModule, moduleSettingsHeight);
            
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

    public void handleWexInlineSettingScroll(double vertical) {
        float totalSettingsH = calculateTotalSettingHeight();
        float inlineClip = wexInlineClipHeight(totalSettingsH);
        scrollHandler.handleWexInlineSettingScroll(vertical, inlineClip, totalSettingsH);
    }

    public void renderModuleList(DrawContext context, float x, float y, float width, float height,
                                 float mouseX, float mouseY, int guiScale, float alphaMultiplier) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        lastListX = x;
        lastListY = y;
        lastListWidth = width;
        lastListHeight = height;

        animationHandler.updateAll(displayModules, selectedModule, mouseX, mouseY, x, y, width, height,
                (float) scrollHandler.getModuleDisplayScroll());
        listRenderer.render(context, displayModules, selectedModule, bindingModule, x, y, width, height,
                mouseX, mouseY, guiScale, alphaMultiplier, animationHandler, scrollHandler);
    }

    public void renderSettingsPanel(DrawContext context, float x, float y, float width, float height,
                                    float mouseX, float mouseY, float delta, int guiScale, float alphaMultiplier) {
        renderSettingsPanel(context, x, y, width, height, mouseX, mouseY, delta, guiScale, alphaMultiplier, false);
    }

    public void renderSettingsPanel(DrawContext context, float x, float y, float width, float height,
                                    float mouseX, float mouseY, float delta, int guiScale, float alphaMultiplier,
                                    boolean wexsideStyle) {
        savedGuiScale = guiScale;
        for (AbstractSettingComponent c : settingComponents) {
            c.setWexsideStyle(wexsideStyle);
        }
        settingsRenderer.render(context, selectedModule, settingComponents, x, y, width, height,
                mouseX, mouseY, delta, guiScale, alphaMultiplier, scrollHandler, animationHandler, wexsideStyle);
    }

    public void updateScroll(float delta, float scrollSpeed) {
        animationHandler.tickCategoryTransition();
        scrollHandler.update(delta);
    }

    public void updateScrollFades(float delta, float scrollSpeed, float moduleListHeight, float settingsPanelHeight) {
        scrollHandler.updateFades(displayModules.size(), calculateTotalSettingHeight(), moduleListHeight, settingsPanelHeight);
    }

    public float calculateTotalSettingHeight() {
        animationHandler.syncSettingVisibilityForLayout(settingComponents);
        return settingsRenderer.calculateTotalHeight(settingComponents, animationHandler);
    }

    public ModuleStructure getModuleAtPosition(double mouseX, double mouseY, float listX, float listY,
                                               float listWidth, float listHeight) {
        return listRenderer.getModuleAtPosition(displayModules, mouseX, mouseY, listX, listY, listWidth, listHeight,
                scrollHandler.getModuleDisplayScroll(), animationHandler.isCategoryTransitioning());
    }

    public boolean isStarClicked(double mouseX, double mouseY, float listX, float listY,
                                 float listWidth, float listHeight) {
        return listRenderer.isStarClicked(displayModules, mouseX, mouseY, listX, listY, listWidth, listHeight,
                scrollHandler.getModuleDisplayScroll(), displayHelper, animationHandler.isCategoryTransitioning());
    }

    public ModuleStructure getModuleForStarClick(double mouseX, double mouseY, float listX, float listY,
                                                 float listWidth, float listHeight) {
        return listRenderer.getModuleForStarClick(displayModules, mouseX, mouseY, listX, listY, listWidth, listHeight,
                scrollHandler.getModuleDisplayScroll(), displayHelper, animationHandler.isCategoryTransitioning());
    }

    public void handleModuleScroll(double vertical, float listHeight) {
        if (animationHandler.isCategoryTransitioning()) return;
        scrollHandler.handleModuleScroll(vertical, listHeight, displayModules.size());
    }

    public void handleSettingScroll(double vertical, float panelHeight) {
        scrollHandler.handleSettingScroll(vertical, panelHeight, calculateTotalSettingHeight());
    }

    public void handleGridScroll(double vertical, float gridHeight) {
        scrollHandler.handleGridScroll(vertical, gridHeight, displayModules.size());
    }

    public void renderModuleGrid(DrawContext context, float x, float y, float width, float height,
                                 float mouseX, float mouseY, float delta, int guiScale, float alphaMultiplier) {
        double gridScroll = scrollHandler.getGridDisplayScroll();
        gridRenderer.render(context, displayModules, x, y, width, height, mouseX, mouseY, delta, gridScroll, guiScale, alphaMultiplier);
    }

    public ModuleStructure getModuleAtGridPosition(double mouseX, double mouseY, float gridX, float gridY, float gridW, float gridH) {
        return gridRenderer.getModuleAtPosition(displayModules, mouseX, mouseY, gridX, gridY, gridW, gridH, scrollHandler.getGridDisplayScroll());
    }

    public boolean isGridToggleClicked(double mouseX, double mouseY, ModuleStructure module, float gridX, float gridY, float gridW) {
        // должно совпадать с раскладкой в ModuleGridRenderer.render
        final float cardPad = 10f;
        final float cardGap = 9f;
        final float cardHeight = 52f;
        int cols = 2;
        float cardWidth = (gridW - cardPad * 2 - cardGap) / cols;
        float rowHeight = cardHeight + cardGap;
        float contentTop = gridY + cardPad - 12f;
        int i = displayModules.indexOf(module);
        if (i < 0) return false;
        int col = i % cols;
        int row = i / cols;
        float startY = contentTop + (float) scrollHandler.getGridDisplayScroll();
        float cardX = gridX + cardPad + col * (cardWidth + cardGap);
        float cardY = startY + row * rowHeight;
        return gridRenderer.isToggleClicked(mouseX, mouseY, cardX, cardY, cardWidth);
    }

    /** Возвращает модуль, если клик был по иконке шестерёнки (настройки) на карточке. */
    public ModuleStructure getModuleForGearClick(double mouseX, double mouseY, float gridX, float gridY, float gridW, float gridH) {
        return gridRenderer.getModuleForGearClick(displayModules, mouseX, mouseY, gridX, gridY, gridW, gridH, scrollHandler.getGridDisplayScroll());
    }

    public void tick() {
        settingComponents.forEach(AbstractSettingComponent::tick);
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

    public boolean isTransitioning() {
        return animationHandler.isCategoryTransitioning();
    }
}

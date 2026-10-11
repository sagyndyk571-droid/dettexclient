package neverlose.screens.clickgui.impl.module.handler;

import lombok.Getter;

@Getter
public class ModuleScrollHandler {

    private double moduleTargetScroll = 0, moduleDisplayScroll = 0;
    private double settingTargetScroll = 0, settingDisplayScroll = 0;
    private double gridTargetScroll = 0, gridDisplayScroll = 0;
    private float moduleScrollTopFade = 0f, moduleScrollBottomFade = 0f;
    private float settingScrollTopFade = 0f, settingScrollBottomFade = 0f;

    private float lastSettingsPanelHeight = 0f;
    private float lastModuleListHeight = 0f;
    private long lastScrollUpdateTime = System.currentTimeMillis();

    private static final float SCROLL_SPEED = 12f;
    private static final float FADE_SPEED = 8f;
    private static final float CORNER_INSET = 3f;
    private static final float MODULE_ITEM_HEIGHT = 22f;

    public void resetModuleScroll() {
        moduleTargetScroll = moduleDisplayScroll = 0;
    }

    public void resetSettingScroll() {
        settingTargetScroll = settingDisplayScroll = 0;
    }

    public void resetGridScroll() {
        gridTargetScroll = gridDisplayScroll = 0;
    }

    public void update(float delta) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastScrollUpdateTime) / 1000f, 0.1f);
        lastScrollUpdateTime = currentTime;

        moduleDisplayScroll = smoothScroll(moduleDisplayScroll, moduleTargetScroll, deltaTime);
        settingDisplayScroll = smoothScroll(settingDisplayScroll, settingTargetScroll, deltaTime);
        gridDisplayScroll = smoothScroll(gridDisplayScroll, gridTargetScroll, deltaTime);
    }

    private double smoothScroll(double current, double target, float deltaTime) {
        double diff = target - current;
        if (Math.abs(diff) < 0.5) return target;
        return current + diff * SCROLL_SPEED * deltaTime;
    }

    public void updateFades(int moduleCount, float totalSettingHeight, float moduleListHeight, float settingsPanelHeight) {
        lastSettingsPanelHeight = settingsPanelHeight;
        lastModuleListHeight = moduleListHeight;

        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastScrollUpdateTime) / 1000f, 0.1f);

        float maxModuleScroll = Math.max(0, moduleCount * 24f - moduleListHeight + 10);
        float maxSettingScroll = Math.max(0, totalSettingHeight - settingsPanelHeight + 45);

        moduleScrollTopFade = updateFade(moduleScrollTopFade, moduleDisplayScroll < -0.5f, deltaTime);
        moduleScrollBottomFade = updateFade(moduleScrollBottomFade, moduleDisplayScroll > -maxModuleScroll + 0.5f && maxModuleScroll > 0, deltaTime);
        settingScrollTopFade = updateFade(settingScrollTopFade, settingDisplayScroll < -0.5f, deltaTime);
        settingScrollBottomFade = updateFade(settingScrollBottomFade, settingDisplayScroll > -maxSettingScroll + 0.5f && maxSettingScroll > 0, deltaTime);
    }

    private float updateFade(float current, boolean condition, float deltaTime) {
        float target = condition ? 1f : 0f;
        float diff = target - current;
        if (Math.abs(diff) < 0.01f) return target;
        return current + diff * FADE_SPEED * deltaTime;
    }

    public void handleModuleScroll(double vertical, float listHeight, int moduleCount) {
        float effectiveHeight = listHeight - CORNER_INSET * 2 - 2;
        float maxScroll = Math.max(0, moduleCount * 24f - effectiveHeight + 10);
        moduleTargetScroll = Math.max(-maxScroll, Math.min(0, moduleTargetScroll + vertical * 25));
    }

    public void handleSettingScroll(double vertical, float panelHeight, float totalSettingHeight) {
        float effectiveHeight = panelHeight - 31 - CORNER_INSET - 3;
        float maxScroll = Math.max(0, totalSettingHeight - effectiveHeight + 10);
        settingTargetScroll = Math.max(-maxScroll, Math.min(0, settingTargetScroll + vertical * 25));
    }

    public void scrollToModule(int moduleIndex, int totalModules) {
        float moduleY = moduleIndex * (MODULE_ITEM_HEIGHT + 2);
        float visibleHeight = lastModuleListHeight - CORNER_INSET * 2 - 4;
        float centerOffset = (visibleHeight - MODULE_ITEM_HEIGHT) / 2f;
        float targetScroll = -(moduleY - centerOffset);

        float maxScroll = Math.max(0, totalModules * (MODULE_ITEM_HEIGHT + 2) - visibleHeight);
        targetScroll = Math.max(-maxScroll, Math.min(0, targetScroll));

        moduleTargetScroll = targetScroll;
    }

    public void handleGridScroll(double vertical, float gridHeight, int moduleCount) {
        float rowHeight = 60f; // CARD_HEIGHT(54) + CARD_GAP(6)
        int cols = 2;
        int rows = (int) Math.ceil((double) moduleCount / cols);
        float totalHeight = rows * rowHeight;
        float maxScroll = Math.max(0, totalHeight - gridHeight + 10);
        gridTargetScroll = Math.max(-maxScroll, Math.min(0, gridTargetScroll + vertical * 20));
    }

    /** Скролл списка модулей Wex: высота контента с учётом раскрытой карточки. */
    public void handleWexTwoColumnScroll(double vertical, float listHeight, float totalContentHeight) {
        float effectiveHeight = listHeight - 4f;
        float maxScroll = Math.max(0, totalContentHeight - effectiveHeight);
        moduleTargetScroll = Math.max(-maxScroll, Math.min(0, moduleTargetScroll + vertical * 25));
    }

    /** Скролл настроек внутри раскрытой карточки. */
    public void handleWexInlineSettingScroll(double vertical, float visibleClipHeight, float totalSettingHeight) {
        lastSettingsPanelHeight = visibleClipHeight;
        float maxScroll = Math.max(0, totalSettingHeight - visibleClipHeight + 4f);
        settingTargetScroll = Math.max(-maxScroll, Math.min(0, settingTargetScroll + vertical * 25));
    }

    /** Мгновенно подвинуть список модулей (без сглаживания), чтобы раскрытая карточка была у верха без лишней пустоты. */
    public void snapWexModuleScroll(float targetScroll, float listHeight, float totalContentHeight) {
        float effectiveHeight = listHeight - 4f;
        float maxScroll = Math.max(0, totalContentHeight - effectiveHeight);
        float clamped = Math.max(-maxScroll, Math.min(0, targetScroll));
        moduleTargetScroll = moduleDisplayScroll = clamped;
    }

    public void updateWexFades(float listHeight, float totalContentHeight) {
        float maxModuleScroll = Math.max(0, totalContentHeight - listHeight + 4);
        lastModuleListHeight = listHeight;

        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastScrollUpdateTime) / 1000f, 0.1f);

        moduleScrollTopFade = updateFade(moduleScrollTopFade, moduleDisplayScroll < -0.5f, deltaTime);
        moduleScrollBottomFade = updateFade(moduleScrollBottomFade,
                moduleDisplayScroll > -maxModuleScroll + 0.5f && maxModuleScroll > 0, deltaTime);
    }

    /** Wex: фейды скролла списка и скролла настроек внутри карточки. */
    public void updateWexFadesWithSettings(float moduleListHeight, float totalContentHeight,
                                         float totalSettingHeight, float inlineSettingsClipHeight) {
        lastSettingsPanelHeight = inlineSettingsClipHeight;
        lastModuleListHeight = moduleListHeight;

        float maxModuleScroll = Math.max(0, totalContentHeight - moduleListHeight + 4);
        float maxSettingScroll = Math.max(0, totalSettingHeight - inlineSettingsClipHeight + 4);

        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastScrollUpdateTime) / 1000f, 0.1f);

        moduleScrollTopFade = updateFade(moduleScrollTopFade, moduleDisplayScroll < -0.5f, deltaTime);
        moduleScrollBottomFade = updateFade(moduleScrollBottomFade,
                moduleDisplayScroll > -maxModuleScroll + 0.5f && maxModuleScroll > 0, deltaTime);
        settingScrollTopFade = updateFade(settingScrollTopFade, settingDisplayScroll < -0.5f, deltaTime);
        settingScrollBottomFade = updateFade(settingScrollBottomFade,
                settingDisplayScroll > -maxSettingScroll + 0.5f && maxSettingScroll > 0, deltaTime);
    }

    public void correctSettingScrollPosition(float totalSettingHeight) {
        if (lastSettingsPanelHeight <= 0) return;

        float maxScroll = Math.max(0, totalSettingHeight - lastSettingsPanelHeight + 45);
        if (settingTargetScroll < -maxScroll) {
            settingTargetScroll = -maxScroll;
        }
        if (settingDisplayScroll < -maxScroll) {
            settingDisplayScroll = -maxScroll;
        }
    }
}
package neverlose.screens.clickgui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import neverlose.IMinecraft;
import neverlose.debob;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.ModuleStructure;
import neverlose.screens.clickgui.impl.DragHandler;
import neverlose.screens.clickgui.impl.autobuy.autobuyui.AutoBuyRenderer;
import neverlose.screens.clickgui.impl.background.BackgroundComponent;
import neverlose.screens.clickgui.impl.background.render.HeaderRenderer;
import neverlose.screens.clickgui.impl.configs.ConfigsRenderer;
import neverlose.screens.clickgui.impl.cosmetics.CosmeticsRenderer;
import neverlose.screens.clickgui.impl.module.ModuleComponent;
import neverlose.screens.clickgui.impl.module.render.WexsideTwoColumnLayout;
import neverlose.screens.clickgui.impl.module.render.WexsideTwoColumnRenderer;
import neverlose.screens.clickgui.impl.settingsrender.BindComponent;
import neverlose.screens.clickgui.impl.settingsrender.TextComponent;
import neverlose.util.animations.Direction;
import neverlose.util.animations.GuiAnimation;
import neverlose.util.interfaces.AbstractSettingComponent;
import neverlose.util.render.font.Fonts;
import neverlose.util.math.FrameRateCounter;
import neverlose.util.render.Render2D;
import neverlose.util.render.shader.Scissor;
import neverlose.util.render.gif.GifRender;
import neverlose.util.security.ClientUnhook;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ClickGui extends Screen implements IMinecraft, ClickGuiOverlay {
    public static ClickGui INSTANCE = new ClickGui();
    private static final int FIXED_GUI_SCALE = 2;
    private static final int WEX_MENU_SWITCH_KEY = GLFW.GLFW_KEY_TAB;

    private final BackgroundComponent background = new BackgroundComponent();
    private final ModuleComponent moduleComponent = new ModuleComponent();
    private final AutoBuyRenderer autoBuyRenderer = new AutoBuyRenderer();
    private final ConfigsRenderer configsRenderer = new ConfigsRenderer();
    private final CosmeticsRenderer cosmeticsRenderer = new CosmeticsRenderer();
    private final DragHandler dragHandler = new DragHandler();
    private ModuleCategory selectedCategory = ModuleCategory.COMBAT;

    private final GuiAnimation openAnimation = new GuiAnimation();
    private boolean closing = false;
    private boolean waitingForSlide = false;
    private boolean slideTriggered = false;

    private float hintAlphaAnimation = 0f;
    private long lastHintUpdateTime = System.currentTimeMillis();
    private static final float HINT_ANIM_SPEED = 6f;
    private static final float OFFSET_THRESHOLD = 5f;

    private int lastMouseX;
    private int lastMouseY;
    private float lastDelta;

    public ClickGui() {
        super(Text.of("MenuScreen"));
    }

    /** Открыть меню с выбранной категорией. */
    public void openFromWexMenu(ModuleCategory category) {
        selectedCategory = category != null ? category : ModuleCategory.COMBAT;
        ClickGuiStylePreference.setLastCategory(selectedCategory);
        closing = false;
        waitingForSlide = false;
        slideTriggered = false;
        openAnimation.setMs(250).setValue(1.0).setDirection(Direction.FORWARDS).reset();
        moduleComponent.selectModule(null);
        moduleComponent.closeSettingsPopup();
        moduleComponent.closeBindPopup();
        moduleComponent.setBindingModule(null);
        updateModules();
        mc.setScreen(this);
    }

    public boolean isClosing() {
        return closing;
    }

    @Override
    protected void init() {
        super.init();
        closing = false;
        waitingForSlide = false;
        slideTriggered = false;
        openAnimation.setMs(250).setValue(1.0).setDirection(Direction.FORWARDS).reset();
        hintAlphaAnimation = 0f;
        lastHintUpdateTime = System.currentTimeMillis();

        long handle = mc.getWindow().getHandle();
        double centerX = mc.getWindow().getWidth() / 2.0;
        double centerY = mc.getWindow().getHeight() / 2.0;
        GLFW.glfwSetCursorPos(handle, centerX, centerY);

        selectedCategory = ClickGuiStylePreference.getLastCategory();
        background.setSearchActive(false);
        autoBuyRenderer.resetForClose();
        updateModules();
    }

    private void updateModules() {
        List<ModuleStructure> modules = new ArrayList<>();
        try {
            var repo = debob.getInstance().getManager().getModuleRepository();
            if (repo != null) {
                for (ModuleStructure m : repo.modules()) {
                    if (m.getCategory() == selectedCategory) modules.add(m);
                }
            }
        } catch (Exception ignored) {}
        moduleComponent.updateModules(modules, selectedCategory);
    }

    /**
     * Закрывает текущий экран и открывает ClickGUI с выбранной категорией.
     */
    public static void openModernToCategory(ModuleCategory category) {
        ClickGuiStylePreference.setLastCategory(category != null ? category : ModuleCategory.RENDER);
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(null);
        ClickGui.INSTANCE.openGui();
    }

    public void openGui() {
        if (mc.currentScreen == null) {
            closing = false;
            waitingForSlide = false;
            slideTriggered = false;
            openAnimation.setMs(250).setValue(1.0).setDirection(Direction.FORWARDS).reset();
            selectedCategory = ClickGuiStylePreference.getLastCategory();
            updateModules();
            mc.setScreen(this);
        }
    }

    @Override
    public void tick() {
        GifRender.tick();
        moduleComponent.tick();
        super.tick();
    }

    private float[] calculateBackground(float scale) {
        int vw = mc.getWindow().getWidth() / FIXED_GUI_SCALE;
        int vh = mc.getWindow().getHeight() / FIXED_GUI_SCALE;
        float bgX = (vw - BackgroundComponent.BG_WIDTH) / 2f + dragHandler.getOffsetX();
        float bgY = (vh - BackgroundComponent.BG_HEIGHT) / 2f + dragHandler.getOffsetY();
        return new float[]{bgX, bgY, vw, vh};
    }

    private boolean isAnyBindListening() {
        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                return true;
            }
        }
        for (List<AbstractSettingComponent> components : moduleComponent.getModuleSettingsMap().values()) {
            for (AbstractSettingComponent c : components) {
                if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                    return true;
                }
            }
        }
        if (moduleComponent.getBindPopup().isOpen()) {
            for (AbstractSettingComponent c : moduleComponent.getBindPopup().getComponents()) {
                if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean handleListeningBindKey(int key, int scancode, int modifiers) {
        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                return bindComponent.keyPressed(key, scancode, modifiers);
            }
        }
        for (List<AbstractSettingComponent> components : moduleComponent.getModuleSettingsMap().values()) {
            for (AbstractSettingComponent c : components) {
                if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                    return bindComponent.keyPressed(key, scancode, modifiers);
                }
            }
        }
        if (moduleComponent.getBindPopup().isOpen()) {
            for (AbstractSettingComponent c : moduleComponent.getBindPopup().getComponents()) {
                if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                    return bindComponent.keyPressed(key, scancode, modifiers);
                }
            }
        }
        if (moduleComponent.getSettingsPopup().isOpen()) {
            for (AbstractSettingComponent c : moduleComponent.getSettingsPopup().getComponents()) {
                if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                    return bindComponent.keyPressed(key, scancode, modifiers);
                }
            }
        }
        return false;
    }

    private boolean handleListeningBindScroll(double vertical) {
        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                bindComponent.handleScrollBind(vertical);
                return true;
            }
        }
        for (List<AbstractSettingComponent> components : moduleComponent.getModuleSettingsMap().values()) {
            for (AbstractSettingComponent c : components) {
                if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                    bindComponent.handleScrollBind(vertical);
                    return true;
                }
            }
        }
        if (moduleComponent.getBindPopup().isOpen()) {
            for (AbstractSettingComponent c : moduleComponent.getBindPopup().getComponents()) {
                if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                    bindComponent.handleScrollBind(vertical);
                    return true;
                }
            }
        }
        if (moduleComponent.getSettingsPopup().isOpen()) {
            for (AbstractSettingComponent c : moduleComponent.getSettingsPopup().getComponents()) {
                if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                    bindComponent.handleScrollBind(vertical);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean handleListeningMiddleMouseBind() {
        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                bindComponent.handleMiddleMouseBind();
                return true;
            }
        }
        for (List<AbstractSettingComponent> components : moduleComponent.getModuleSettingsMap().values()) {
            for (AbstractSettingComponent c : components) {
                if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                    bindComponent.handleMiddleMouseBind();
                    return true;
                }
            }
        }
        if (moduleComponent.getBindPopup().isOpen()) {
            for (AbstractSettingComponent c : moduleComponent.getBindPopup().getComponents()) {
                if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                    bindComponent.handleMiddleMouseBind();
                    return true;
                }
            }
        }
        if (moduleComponent.getSettingsPopup().isOpen()) {
            for (AbstractSettingComponent c : moduleComponent.getSettingsPopup().getComponents()) {
                if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                    bindComponent.handleMiddleMouseBind();
                    return true;
                }
            }
        }
        return false;
    }

    private void updateHintAnimation() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastHintUpdateTime) / 1000f, 0.1f);
        lastHintUpdateTime = currentTime;

        float offsetX = Math.abs(dragHandler.getOffsetX());
        float offsetY = Math.abs(dragHandler.getOffsetY());
        boolean shouldShow = (offsetX > OFFSET_THRESHOLD || offsetY > OFFSET_THRESHOLD);

        float target = shouldShow ? 1f : 0f;
        float diff = target - hintAlphaAnimation;

        if (Math.abs(diff) < 0.001f) {
            hintAlphaAnimation = target;
        } else {
            hintAlphaAnimation += diff * HINT_ANIM_SPEED * deltaTime;
            hintAlphaAnimation = Math.max(0f, Math.min(1f, hintAlphaAnimation));
        }
    }

    private boolean isModuleCategory(ModuleCategory category) {
        return category != ModuleCategory.AUTOBUY
                && category != ModuleCategory.CONFIG
                && category != ModuleCategory.COSMETICS;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        lastDelta = delta;

        FrameRateCounter.INSTANCE.recordFrame();

        if (waitingForSlide && selectedCategory == ModuleCategory.AUTOBUY) {
            if (!slideTriggered) {
                autoBuyRenderer.triggerSlideOut();
                slideTriggered = true;
            }

            if (autoBuyRenderer.isSlideOutComplete()) {
                waitingForSlide = false;
                slideTriggered = false;
                startActualClose();
            }
        }

        if (closing && !waitingForSlide && openAnimation.isFinished(Direction.BACKWARDS)) {
            closing = false;
            TextComponent.typing = false;
            moduleComponent.setBindingModule(null);
            dragHandler.stopDrag();
            autoBuyRenderer.resetForClose();
            mc.currentScreen = null;
        }
    }

    public void renderOverlay(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.getWindow() == null) return;

        // Оверлей из миксина: берём реальный dt кадра; lastDelta из Screen.render иногда 0 — тогда тумблеры не анимировались
        float delta = tickCounter.getDynamicDeltaTicks();
        if (delta <= 0f || Float.isNaN(delta)) {
            delta = Math.max(lastDelta, 1f / 60f);
        }
        int mouseX = lastMouseX;
        int mouseY = lastMouseY;

        float scrollSpeed = Math.min(1f, 60f / Math.max(FrameRateCounter.INSTANCE.getFps(), 1));
        float animValue = openAnimation.getOutput().floatValue();

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        context.createNewRootLayer();

        int dimAlpha = (int) (125 * animValue);
        if (dimAlpha > 0) {
            Render2D.rect(0, 0, 5000, 5000, new Color(0, 0, 0, dimAlpha).getRGB(), 0);
        }

        int guiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        float scale = (float) FIXED_GUI_SCALE / guiScale;

        float mx = mouseX / scale, my = mouseY / scale;

        if (!closing || waitingForSlide) {
            dragHandler.update(mx, my);
        }

        updateHintAnimation();

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);

        float[] bg = calculateBackground(scale);
        float bgX = bg[0];
        float bgY = bg[1];
        int vw = (int) bg[2];
        int vh = (int) bg[3];

        float yOffset;
        if (closing && !waitingForSlide) {
            yOffset = (1f - animValue) * 30f;
        } else {
            yOffset = (1f - animValue) * -15f;
        }
        bgY += yOffset;

        float alphaMultiplier = animValue;

        context.getMatrices().pushMatrix();

        background.render(context, bgX, bgY, selectedCategory, delta, alphaMultiplier);
        background.renderCategoryPanel(bgX, bgY, alphaMultiplier);

        float contentX = bgX + BackgroundComponent.SIDEBAR_WIDTH;
        float contentY = bgY + 30f;  // 38 * 0.8
        float contentW = BackgroundComponent.CONTENT_WIDTH;
        float contentH = BackgroundComponent.BG_HEIGHT - 37f;  // 46 * 0.8

        float normalAlpha = background.getNormalPanelAlpha();
        float searchAlpha = background.getSearchPanelAlpha();

        if (normalAlpha > 0.01f) {
            configsRenderer.render(context, bgX, bgY, mx, my, delta, FIXED_GUI_SCALE, alphaMultiplier * normalAlpha, selectedCategory);
            cosmeticsRenderer.render(context, bgX, bgY, mx, my, delta, FIXED_GUI_SCALE, alphaMultiplier * normalAlpha, selectedCategory);

            boolean isAutoBuySliding = autoBuyRenderer.isSliding();
            boolean shouldRenderModules = isModuleCategory(selectedCategory);
            boolean slidingToModuleCategory = isAutoBuySliding && isModuleCategory(selectedCategory);

            boolean hasSettingsOpen = moduleComponent.getSelectedModule() != null && isModuleCategory(selectedCategory);

            if (shouldRenderModules || slidingToModuleCategory) {
                moduleComponent.updateScroll(delta, scrollSpeed);
                if (hasSettingsOpen) {
                    moduleComponent.updateWexScrollFadesWithSettings(contentH);
                } else {
                    moduleComponent.updateWexScrollFades(contentH);
                }
                // Двухколоночный рендер модулей как в WexsideClickGuiScreen
                Scissor.reset();
                moduleComponent.renderWexsideTwoColumns(context, contentX, contentY, contentH, mx, my, FIXED_GUI_SCALE, alphaMultiplier * normalAlpha, delta);
            }

            autoBuyRenderer.render(context, bgX, bgY, mx, my, delta, FIXED_GUI_SCALE, alphaMultiplier * normalAlpha, selectedCategory);
        }

        Scissor.reset();
        background.renderCategoryNames(context, bgX, bgY, selectedCategory, alphaMultiplier * normalAlpha);
        background.renderHeader(bgX, bgY, selectedCategory, alphaMultiplier);

        if (searchAlpha > 0.01f) {
            background.renderSearchResults(context, bgX, bgY, mx, my, FIXED_GUI_SCALE, alphaMultiplier);
        }

        if (moduleComponent.getBindPopup().isOpen()) {
            moduleComponent.getBindPopup().render(context, (float) mx, (float) my, delta, FIXED_GUI_SCALE, alphaMultiplier);
        }
        if (moduleComponent.getSettingsPopup().isOpen()) {
            moduleComponent.getSettingsPopup().render(context, (float) mx, (float) my, delta, FIXED_GUI_SCALE, alphaMultiplier);
        }

        background.renderClientColorUI(context, bgX, bgY, mx, my, alphaMultiplier);

        Scissor.reset();

        context.getMatrices().popMatrix();

        float finalHintAlpha = hintAlphaAnimation * alphaMultiplier;
        if (finalHintAlpha > 0.01f) {
            int hintAlpha = (int) (255 * finalHintAlpha);
            float centerX = vw / 2f;
            float centerY = vh / 2f;
            float textY = centerY + BackgroundComponent.BG_HEIGHT / 2f + 10f;
//            Fonts.TEST.drawCentered("Press CTRL + ALT to reset position", centerX, textY + 65, 6, new Color(150, 150, 150, hintAlpha).getRGB());
        }

        context.getMatrices().popMatrix();
    }

    private void renderSettingsPanel(DrawContext context, float contentX, float contentY, float contentW, float contentH, float alpha) {
        ModuleStructure selectedModule = moduleComponent.getSelectedModule();
        if (selectedModule == null) return;

        float settingsPanelX = contentX + contentW * 0.52f + 6f;
        float settingsPanelW = contentW - contentW * 0.52f - 6f;

        context.getMatrices().pushMatrix();

        // Фон для панели настроек
        Render2D.rect(settingsPanelX, contentY, settingsPanelW, contentH, new Color(22, 22, 22, (int)(245 * alpha)).getRGB(), 6f);
        Render2D.outline(settingsPanelX, contentY, settingsPanelW, contentH, 1f, new Color(60, 60, 60, (int)(255 * alpha)).getRGB(), 6f);

        // Заголовок с названием модуля
        Fonts.NEVERLOSE.draw(selectedModule.getName(), settingsPanelX + 8, contentY + 8, 8, new Color(255, 255, 255, (int)(255 * alpha)).getRGB());

        // Тут будет рендер самих настроек, который уже реализован внутри renderWexsideTwoColumns
        context.getMatrices().popMatrix();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (closing) return false;

        int guiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        float scale = (float) FIXED_GUI_SCALE / guiScale;
        double mx = click.x() / scale, my = click.y() / scale;

        if (moduleComponent.getBindPopup().isOpen()) {
            if (moduleComponent.getBindPopup().mouseClicked(mx, my, click.button())) return true;
        }
        if (moduleComponent.getSettingsPopup().isOpen()) {
            if (moduleComponent.getSettingsPopup().mouseClicked(mx, my, click.button())) return true;
        }

        float[] bg = calculateBackground(scale);
        float bgX = bg[0], bgY = bg[1];

        if (background.isSearchBoxHovered(mx, my, bgX, bgY) && click.button() == 0) {
            background.setSearchActive(true);
            return true;
        }

        var headerClick = background.handleHeaderClick(mx, my, bgX, bgY, click.button());
        if (headerClick.consumed) {
            if (headerClick.category != null) {
                selectedCategory = headerClick.category;
                ClickGuiStylePreference.setLastCategory(selectedCategory);
                updateModules();
            }
            if (HeaderRenderer.HeaderClickResult.ACTION_RESET_GUI.equals(headerClick.action)) {
                dragHandler.reset();
            }
            if (HeaderRenderer.HeaderClickResult.ACTION_SAFE_MODE.equals(headerClick.action)) {
                ClientUnhook.enterHiddenMode();
            }
            return true;
        }

        if (background.isSearchActive()) {
            if (click.button() == 0) {
                ModuleStructure searchModule = background.getSearchModuleAtPosition(mx, my, bgX, bgY);
                if (searchModule != null) {
                    searchModule.switchState();
                    return true;
                }

                if (background.isSearchPanelHovered(mx, my, bgX, bgY)) {
                    return true;
                }

                if (!background.isSearchBoxHovered(mx, my, bgX, bgY)) {
                    background.setSearchActive(false);
                }
            } else if (click.button() == 1) {
                ModuleStructure searchModule = background.getSearchModuleAtPosition(mx, my, bgX, bgY);
                if (searchModule != null) {
                    background.setSearchActive(false);
                    selectedCategory = searchModule.getCategory();
                    ClickGuiStylePreference.setLastCategory(selectedCategory);
                    moduleComponent.selectModuleFromSearch(searchModule);
                    updateModules();
                    return true;
                }
            }
            return true;
        }

        ModuleCategory sidebarCategory = background.getCategoryAtPosition(mx, my, bgX, bgY);
        if (sidebarCategory != null) {
            selectedCategory = sidebarCategory;
            ClickGuiStylePreference.setLastCategory(selectedCategory);
            updateModules();
            return true;
        }

        if (selectedCategory == ModuleCategory.AUTOBUY) {
            if (autoBuyRenderer.mouseClicked(mx, my, click.button(), bgX, bgY, selectedCategory)) {
                return true;
            }
        }

        if (selectedCategory == ModuleCategory.CONFIG) {
            if (configsRenderer.mouseClicked(mx, my, click.button(), bgX, bgY, selectedCategory)) {
                return true;
            }
        }

        if (selectedCategory == ModuleCategory.COSMETICS) {
            if (cosmeticsRenderer.mouseClicked(mx, my, click.button(), bgX, bgY, selectedCategory)) {
                return true;
            }
        }

        float contentX = bgX + BackgroundComponent.SIDEBAR_WIDTH;
        float contentY = bgY + 30f;  // 38 * 0.8
        float contentW = BackgroundComponent.CONTENT_WIDTH;
        float contentH = BackgroundComponent.BG_HEIGHT - 37f;  // 46 * 0.8

        final float DRAG_HEADER_H = 22f;  // 28 * 0.8
        boolean inDragZone = mx >= bgX && mx <= bgX + BackgroundComponent.BG_WIDTH && my >= bgY && my <= bgY + DRAG_HEADER_H;
        if (click.button() == 0 && inDragZone) {
            if (dragHandler.startDragWithButton(mx, my, bgX, bgY, BackgroundComponent.BG_WIDTH, (int) DRAG_HEADER_H, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT)) {
                return true;
            }
        }

        if (click.button() == 2) {
        if (isAnyBindListening()) {
            if (handleListeningMiddleMouseBind()) return true;
        }

            if (moduleComponent.getBindingModule() != null) {
                return true;
            }

            if (dragHandler.startDragWithButton(mx, my, bgX, bgY, BackgroundComponent.BG_WIDTH, BackgroundComponent.BG_HEIGHT, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE)) {
                return true;
            }
        }

        // клик по квадратику цвета клиента / палитрам
        if (background.handleClientColorClick(mx, my, bgX, bgY, click.button())) {
            return true;
        }

        if (isModuleCategory(selectedCategory)) {
            // Двухколоночный layout - обработка кликов
            WexsideTwoColumnLayout.HitResult hit = moduleComponent.pickWexHit(mx, my, contentX, contentY, contentH);
            
            if (hit != null && hit.part == WexsideTwoColumnLayout.HitPart.SETTINGS) {
                // Получаем настройки для конкретного модуля, по которому кликнули
                List<AbstractSettingComponent> moduleSettings = moduleComponent.getModuleSettings(hit.module);
                if (moduleSettings != null) {
                    // Передаем абсолютные координаты - компоненты сами знают свою позицию
                    for (int i = 0; i < moduleSettings.size(); i++) {
                        AbstractSettingComponent c = moduleSettings.get(i);
                        if (c.mouseClicked(mx, my, click.button())) {
                            return true;
                        }
                    }
                }
                return true;
            }

            if (hit != null && hit.part == WexsideTwoColumnLayout.HitPart.HEADER) {
                ModuleStructure mod = hit.module;
                if (click.button() == 0) {
                    float dotsX = hit.cardX + hit.cardW - 52f;
                    float dotsY = hit.cardY + 3f;
                    float dotsW = 18f;
                    float dotsH = 13f;
                    if (mx >= dotsX && mx <= dotsX + dotsW &&
                            my >= dotsY && my <= dotsY + dotsH) {
                        moduleComponent.openBindPopup(mod, dotsX + dotsW * 0.5f, dotsY + dotsH * 0.5f);
                        return true;
                    }

                    // Проверяем клик по toggle - используем hit result для получения координат
                    float actualToggleW = 21f;
                    float actualToggleH = 10f;
                    float actualToggleX = hit.cardX + hit.cardW - 29f;
                    float actualToggleY = hit.cardY + 4f;
                    if (mx >= actualToggleX - 2f && mx <= actualToggleX + actualToggleW + 2f &&
                            my >= actualToggleY - 2f && my <= actualToggleY + actualToggleH + 2f) {
                        mod.switchState();
                        return true;
                    }

                    int idx = moduleComponent.getDisplayModules().indexOf(mod);
                    if (idx >= 0) {
                        // Вычисляем позицию карточки из hit result
                        float x = contentX + (idx % 2 == 0 ? WexsideTwoColumnRenderer.COL_LEFT : WexsideTwoColumnRenderer.COL_RIGHT);
                        float w = WexsideTwoColumnRenderer.COL_W;
                        
                        // Координаты toggle (относительно hit.module)
                        float toggleX = x + w - 32f;
                        float toggleW = 24f;
                        float toggleH = 11f;
                        
                        // Проверяем клик по toggle (Y координаты берем из mouseY напрямую)
                        if (mx >= toggleX && mx <= toggleX + toggleW && my >= hit.cardY + 2f && my <= hit.cardY + toggleH + 8f) {
                            // Клик в X-диапазоне toggle - переключаем состояние
                            mod.switchState();
                            return true;
                        }
                    }
                    // Клик по карточке (не по toggle) - открываем настройки
                    if (mod != moduleComponent.getSelectedModule()) {
                        moduleComponent.closeSettingsPopup();
                        // Автоскролл убран, так как настройки всегда открыты
                    }
                    return true;
                }
                if (click.button() == 1) {
                    if (mod == moduleComponent.getSelectedModule()) {
                        moduleComponent.closeSettingsPopup();
                    } else {
                        moduleComponent.closeSettingsPopup();
                        // Автоскролл убран, так как настройки всегда открыты
                    }
                    return true;
                }
                if (click.button() == 2) {
                    moduleComponent.openBindPopup(mod, (float) mx, (float) my);
                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (closing) return false;

        if (selectedCategory == ModuleCategory.AUTOBUY) {
            autoBuyRenderer.mouseReleased(click.x(), click.y(), click.button());
        }

        if (moduleComponent.getBindPopup().isOpen()) {
            int guiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
            float scale = (float) FIXED_GUI_SCALE / guiScale;
            double mx = click.x() / scale, my = click.y() / scale;
            moduleComponent.getBindPopup().mouseReleased(mx, my, click.button());
        }
        if (moduleComponent.getSettingsPopup().isOpen()) {
            int guiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
            float scale = (float) FIXED_GUI_SCALE / guiScale;
            double mx = click.x() / scale, my = click.y() / scale;
            moduleComponent.getSettingsPopup().mouseReleased(mx, my, click.button());
        }

        // Обрабатываем mouseReleased для настроек в двухколоночном layout
        if (isModuleCategory(selectedCategory)) {
            int guiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
            float scale = (float) FIXED_GUI_SCALE / guiScale;
            double mx = click.x() / scale, my = click.y() / scale;
            
            float[] bg = calculateBackground(scale);
            float bgX = bg[0], bgY = bg[1];
            float contentX = bgX + BackgroundComponent.SIDEBAR_WIDTH;
            float contentY = bgY + 30f;
            float contentH = BackgroundComponent.BG_HEIGHT - 37f;
            
            WexsideTwoColumnLayout.HitResult hit = moduleComponent.pickWexHit(mx, my, contentX, contentY, contentH);
            if (hit != null && hit.part == WexsideTwoColumnLayout.HitPart.SETTINGS) {
                List<AbstractSettingComponent> moduleSettings = moduleComponent.getModuleSettings(hit.module);
                if (moduleSettings != null) {
                    // Передаем абсолютные координаты - компоненты сами знают свою позицию
                    for (AbstractSettingComponent c : moduleSettings) {
                        if (c.mouseReleased(mx, my, click.button())) {
                            return true;
                        }
                    }
                }
            }
        }

        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (closing) return false;

        if (isAnyBindListening()) {
            if (handleListeningBindScroll(vertical)) return true;
        }

        if (moduleComponent.getBindingModule() != null) {
            return true;
        }

        int guiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        float scale = (float) FIXED_GUI_SCALE / guiScale;
        // Те же координаты, что в render/renderOverlay (миксин может передавать другой масштаб).
        double mx = lastMouseX / scale, my = lastMouseY / scale;

        float[] bg = calculateBackground(scale);
        float bgX = bg[0], bgY = bg[1];

        if (moduleComponent.getSettingsPopup().isOpen()) {
            moduleComponent.getSettingsPopup().mouseScrolled(vertical);
            return true;
        }

        if (background.isSearchActive()) {
            if (background.isSearchPanelHovered(mx, my, bgX, bgY)) {
                background.handleSearchScroll(vertical, background.getSearchVisibleHeight());
                return true;
            }
        }

        if (selectedCategory == ModuleCategory.AUTOBUY) {
            if (autoBuyRenderer.mouseScrolled(mx, my, vertical, bgX, bgY, selectedCategory)) {
                return true;
            }
        }

        if (selectedCategory == ModuleCategory.CONFIG) {
            if (configsRenderer.mouseScrolled(mx, my, vertical, bgX, bgY, selectedCategory)) {
                return true;
            }
        }

        if (selectedCategory == ModuleCategory.COSMETICS) {
            if (cosmeticsRenderer.mouseScrolled(mx, my, vertical, bgX, bgY, selectedCategory)) {
                return true;
            }
        }

        float contentX = bgX + BackgroundComponent.SIDEBAR_WIDTH;
        float contentY = bgY + 30f;
        float contentW = BackgroundComponent.CONTENT_WIDTH;
        float contentH = BackgroundComponent.BG_HEIGHT - 37f;

        if (isModuleCategory(selectedCategory)) {
            // Двухколоночный layout - обработка скролла
            // Скроллим список модулей, если курсор в области контента
            if (mx >= contentX && mx <= contentX + contentW && my >= contentY && my <= contentY + contentH) {
                moduleComponent.handleWexTwoColumnScroll(vertical, contentH);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (!closing && isAnyBindListening()) {
            if (handleListeningBindKey(input.key(), input.scancode(), input.modifiers())) {
                return true;
            }
        }

        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (autoBuyRenderer.isEditing()) {
                return true;
            }
            if (configsRenderer.isEditing()) {
                return true;
            }
            if (moduleComponent.getBindPopup().isOpen()) {
                moduleComponent.closeBindPopup();
                return true;
            }
            if (moduleComponent.getSettingsPopup().isOpen()) {
                moduleComponent.closeSettingsPopup();
                return true;
            }
            if (moduleComponent.getSelectedModule() != null) {
                moduleComponent.selectModule(null);
                return true;
            }
            if (background.isSearchActive()) {
                background.setSearchActive(false);
                return true;
            }
            close();
            return true;
        }

        if (closing) return false;

        if (selectedCategory == ModuleCategory.AUTOBUY) {
            if (autoBuyRenderer.keyPressed(input.key(), input.scancode(), input.modifiers())) {
                return true;
            }
        }

        if (selectedCategory == ModuleCategory.CONFIG) {
            if (configsRenderer.keyPressed(input.key(), input.scancode(), input.modifiers())) {
                return true;
            }
        }

        if (background.isSearchActive()) {
            if (background.handleSearchKey(input.key())) {
                return true;
            }
        }

        if (dragHandler.isResetNeeded(input.key(), input.modifiers())) {
            dragHandler.reset();
            return true;
        }

        if (moduleComponent.getBindPopup().isOpen()) {
            if (moduleComponent.getBindPopup().keyPressed(input.key(), input.scancode(), input.modifiers())) return true;
        }
        if (moduleComponent.getSettingsPopup().isOpen()) {
            if (moduleComponent.getSettingsPopup().keyPressed(input.key(), input.scancode(), input.modifiers())) return true;
        }

        ModuleStructure binding = moduleComponent.getBindingModule();
        if (binding != null) {
            binding.setKey(input.key() == GLFW.GLFW_KEY_DELETE ? GLFW.GLFW_KEY_UNKNOWN : input.key());
            moduleComponent.setBindingModule(null);
            return true;
        }

        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c.getSetting().isVisible() && c.keyPressed(input.key(), input.scancode(), input.modifiers())) return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (closing) return false;

        if (selectedCategory == ModuleCategory.AUTOBUY) {
            if (autoBuyRenderer.charTyped((char) input.codepoint(), input.modifiers())) {
                return true;
            }
        }

        if (selectedCategory == ModuleCategory.CONFIG) {
            if (configsRenderer.charTyped((char) input.codepoint(), input.modifiers())) {
                return true;
            }
        }

        if (background.isSearchActive()) {
            if (background.handleSearchChar((char) input.codepoint())) {
                return true;
            }
        }

        if (moduleComponent.getBindPopup().isOpen()) {
            if (moduleComponent.getBindPopup().charTyped((char) input.codepoint(), input.modifiers())) return true;
        }
        if (moduleComponent.getSettingsPopup().isOpen()) {
            if (moduleComponent.getSettingsPopup().charTyped((char) input.codepoint(), input.modifiers())) return true;
        }

        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c.getSetting().isVisible() && c.charTyped((char) input.codepoint(), input.modifiers())) return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void startActualClose() {
        openAnimation.setDirection(Direction.BACKWARDS);
        openAnimation.reset();

        long handle = mc.getWindow().getHandle();
        double centerX = mc.getWindow().getWidth() / 2.0;
        double centerY = mc.getWindow().getHeight() / 2.0;

        GLFW.glfwSetInputMode(handle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        GLFW.glfwSetCursorPos(handle, centerX, centerY);

        TextComponent.typing = false;
        moduleComponent.setBindingModule(null);
        moduleComponent.closeBindPopup();
        background.setSearchActive(false);
        dragHandler.stopDrag();
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;

            if (selectedCategory == ModuleCategory.AUTOBUY) {
                waitingForSlide = true;
                slideTriggered = false;
            } else {
                waitingForSlide = false;
                startActualClose();
            }
        }
    }
}

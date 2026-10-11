package neverlose.screens.clickgui.impl.configs;

import net.minecraft.client.gui.DrawContext;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.screens.clickgui.impl.background.BackgroundComponent;
import neverlose.screens.clickgui.impl.configs.handler.ConfigAnimationHandler;
import neverlose.screens.clickgui.impl.configs.handler.ConfigDataHandler;
import neverlose.screens.clickgui.impl.configs.render.ConfigCreateBoxRenderer;
import neverlose.screens.clickgui.impl.configs.render.ConfigHeaderRenderer;
import neverlose.screens.clickgui.impl.configs.render.ConfigListRenderer;
import neverlose.screens.clickgui.impl.configs.render.ConfigNotificationRenderer;
import neverlose.util.render.Render2D;

import java.awt.*;

public class ConfigsRenderer {

    public static final float PANEL_X_OFFSET = BackgroundComponent.SIDEBAR_WIDTH + 8f;
    public static final float PANEL_Y_OFFSET = 38f;
    public static final float PANEL_WIDTH = BackgroundComponent.CONTENT_WIDTH - 16f;
    public static final float PANEL_HEIGHT = BackgroundComponent.BG_HEIGHT - 48f;
    public static final float CORNER_RADIUS = 8f;

    private final ConfigAnimationHandler animationHandler;
    private final ConfigDataHandler dataHandler;
    private final ConfigHeaderRenderer headerRenderer;
    private final ConfigListRenderer listRenderer;
    private final ConfigCreateBoxRenderer createBoxRenderer;
    private final ConfigNotificationRenderer notificationRenderer;

    private boolean isActive = false;
    private boolean wasActive = false;

    public ConfigsRenderer() {
        this.animationHandler = new ConfigAnimationHandler();
        this.dataHandler = new ConfigDataHandler(animationHandler);
        this.notificationRenderer = new ConfigNotificationRenderer();
        this.headerRenderer = new ConfigHeaderRenderer(dataHandler);
        this.listRenderer = new ConfigListRenderer(animationHandler, dataHandler, notificationRenderer);
        this.createBoxRenderer = new ConfigCreateBoxRenderer(dataHandler, notificationRenderer);
    }

    public void render(DrawContext context, float bgX, float bgY, float mouseX, float mouseY,
                       float delta, int guiScale, float alphaMultiplier, ModuleCategory category) {

        boolean shouldBeActive = category == ModuleCategory.CONFIG;

        if (shouldBeActive && !wasActive) {
            isActive = true;
            animationHandler.reset();
            dataHandler.refreshConfigs();
            animationHandler.initItemAnimations(dataHandler.getConfigs());
        } else if (!shouldBeActive && wasActive) {
            isActive = false;
        }

        wasActive = shouldBeActive;

        animationHandler.update(isActive, dataHandler.getConfigs(), dataHandler.isCreating());

        if (animationHandler.isFullyHidden() && !isActive) {
            return;
        }

        if (isActive) {
            dataHandler.refreshConfigs();
            animationHandler.initItemAnimations(dataHandler.getConfigs());
        }

        float panelX = bgX + PANEL_X_OFFSET;
        float panelY = bgY + PANEL_Y_OFFSET;

        float slideOffset = (1f - animationHandler.getPanelSlide()) * 20f;
        float finalAlpha = alphaMultiplier * animationHandler.getPanelAlpha();

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(slideOffset, 0);

        renderPanel(panelX, panelY, finalAlpha);
        headerRenderer.render(panelX, panelY, mouseX - slideOffset, mouseY, finalAlpha);
        listRenderer.render(context, panelX, panelY, mouseX - slideOffset, mouseY, guiScale, finalAlpha);
        createBoxRenderer.render(panelX, panelY, finalAlpha);
        notificationRenderer.render(panelX, panelY, finalAlpha);

        context.getMatrices().popMatrix();
    }

    private void renderPanel(float x, float y, float alpha) {
        int panelAlpha = (int) (244 * alpha);
        int outlineAlpha = (int) (135 * alpha);

        Render2D.gradientRect(x, y, PANEL_WIDTH, PANEL_HEIGHT,
                new int[]{
                        new Color(11, 18, 23, panelAlpha).getRGB(),
                        new Color(10, 16, 21, panelAlpha).getRGB(),
                        new Color(7, 12, 15, panelAlpha).getRGB(),
                        new Color(7, 12, 15, panelAlpha).getRGB()
                }, CORNER_RADIUS);
        Render2D.outline(x, y, PANEL_WIDTH, PANEL_HEIGHT, 0.55f, new Color(45, 62, 75, outlineAlpha).getRGB(), CORNER_RADIUS);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, float bgX, float bgY, ModuleCategory category) {
        if (category != ModuleCategory.CONFIG) return false;
        if (animationHandler.getPanelAlpha() < 0.5f) return false;

        float panelX = bgX + PANEL_X_OFFSET;
        float panelY = bgY + PANEL_Y_OFFSET;

        float slideOffset = (1f - animationHandler.getPanelSlide()) * 20f;
        mouseX -= slideOffset;

        if (headerRenderer.mouseClicked(mouseX, mouseY, button, panelX, panelY)) {
            return true;
        }

        if (createBoxRenderer.mouseClicked(mouseX, mouseY, button, panelX, panelY)) {
            return true;
        }

        if (listRenderer.mouseClicked(mouseX, mouseY, button, panelX, panelY)) {
            return true;
        }

        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double vertical, float bgX, float bgY, ModuleCategory category) {
        if (category != ModuleCategory.CONFIG) return false;
        if (animationHandler.getPanelAlpha() < 0.5f) return false;

        float panelX = bgX + PANEL_X_OFFSET;
        float panelY = bgY + PANEL_Y_OFFSET;

        return listRenderer.mouseScrolled(mouseX, mouseY, vertical, panelX, panelY);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return createBoxRenderer.keyPressed(keyCode);
    }

    public boolean charTyped(char chr, int modifiers) {
        return createBoxRenderer.charTyped(chr);
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    public boolean isEditing() {
        return dataHandler.isCreating();
    }
}

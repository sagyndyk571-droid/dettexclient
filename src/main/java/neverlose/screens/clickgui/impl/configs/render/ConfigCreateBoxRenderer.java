package neverlose.screens.clickgui.impl.configs.render;

import neverlose.screens.clickgui.impl.configs.ConfigsRenderer;
import neverlose.screens.clickgui.impl.configs.handler.ConfigDataHandler;
import neverlose.util.render.Render2D;
import neverlose.util.render.font.Fonts;

import java.awt.*;

public class ConfigCreateBoxRenderer {

    private final ConfigDataHandler dataHandler;
    private final ConfigNotificationRenderer notificationRenderer;

    private float createBoxAnimation = 0f;
    private float cursorBlink = 0f;
    private long lastUpdateTime = System.currentTimeMillis();

    public ConfigCreateBoxRenderer(ConfigDataHandler dataHandler, 
                                    ConfigNotificationRenderer notificationRenderer) {
        this.dataHandler = dataHandler;
        this.notificationRenderer = notificationRenderer;
    }

    public void render(float x, float y, float alpha) {
        updateAnimations();

        if (createBoxAnimation < 0.01f) return;

        float boxY = y + ConfigsRenderer.PANEL_HEIGHT - 46;
        float boxAlpha = createBoxAnimation * alpha;

        renderBackground(x, boxY, boxAlpha);
        renderInput(x, boxY, boxAlpha);
        renderSaveButton(x, boxY, boxAlpha);
    }

    private void updateAnimations() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastUpdateTime) / 1000f, 0.1f);
        lastUpdateTime = currentTime;

        float targetCreate = dataHandler.isCreating() ? 1f : 0f;
        createBoxAnimation += (targetCreate - createBoxAnimation) * 14f * deltaTime;

        cursorBlink += deltaTime * 2f;
        if (cursorBlink > 1f) cursorBlink -= 1f;
    }

    private void renderBackground(float x, float boxY, float alpha) {
        Render2D.rect(x + 8, boxY, ConfigsRenderer.PANEL_WIDTH - 16, 38,
                new Color(9, 15, 20, (int) (242 * alpha)).getRGB(), 7);
        Render2D.outline(x + 8, boxY, ConfigsRenderer.PANEL_WIDTH - 16, 38, 0.5f,
                new Color(52, 67, 88, (int) (135 * alpha)).getRGB(), 7);
    }

    private void renderInput(float x, float boxY, float alpha) {
        float inputX = x + 15;
        float inputY = boxY + 9;
        float inputW = ConfigsRenderer.PANEL_WIDTH - 113;
        float inputH = 20;

        Render2D.rect(inputX, inputY, inputW, inputH, 
                new Color(14, 21, 29, (int) (232 * alpha)).getRGB(), 5);
        Render2D.outline(inputX, inputY, inputW, inputH, 0.5f, 
                new Color(55, 68, 90, (int) (135 * alpha)).getRGB(), 5);

        String configName = dataHandler.getNewConfigName();
        if (configName.isEmpty()) {
            Fonts.INTER_MEDIUM.draw("Enter config name", inputX + 7, inputY + 5.6f, 4.8f,
                    new Color(112, 120, 132, (int) (170 * alpha)).getRGB());
        } else {
            Fonts.INTER_MEDIUM.draw(configName, inputX + 7, inputY + 5.6f, 4.8f,
                    new Color(220, 225, 238, (int) (255 * alpha)).getRGB());
        }

        if (dataHandler.isCreating()) {
            renderCursor(inputX, inputY, inputH, configName, alpha);
        }
    }

    private void renderCursor(float inputX, float inputY, float inputH, String text, float alpha) {
        float cursorAlpha = (float) (Math.sin(cursorBlink * Math.PI * 2) * 0.5 + 0.5);
        if (cursorAlpha > 0.3f) {
            float cursorX = inputX + 7 + Fonts.INTER_MEDIUM.getWidth(text, 4.8f);
            Render2D.rect(cursorX, inputY + 4, 0.6f, inputH - 8,
                    new Color(220, 225, 238, (int) (255 * cursorAlpha * alpha)).getRGB(), 0);
        }
    }

    private void renderSaveButton(float x, float boxY, float alpha) {
        float saveX = x + ConfigsRenderer.PANEL_WIDTH - 82;
        float saveY = boxY + 8;
        float saveW = 67;
        float saveH = 22;

        Render2D.rect(saveX, saveY, saveW, saveH, 
                new Color(83, 126, 245, (int) (222 * alpha)).getRGB(), 5);
        Render2D.outline(saveX, saveY, saveW, saveH, 0.5f, 
                new Color(128, 160, 255, (int) (130 * alpha)).getRGB(), 5);

        float textWidth = Fonts.INTER_SEMIBOLD.getWidth("Save", 4.9f);
        Fonts.INTER_SEMIBOLD.draw("Save", saveX + (saveW - textWidth) / 2, saveY + 7.65f, 4.9f,
                new Color(248, 250, 255, (int) (255 * alpha)).getRGB());
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, float panelX, float panelY) {
        if (!dataHandler.isCreating() || createBoxAnimation < 0.5f) return false;

        float saveX = panelX + ConfigsRenderer.PANEL_WIDTH - 82;
        float saveY = panelY + ConfigsRenderer.PANEL_HEIGHT - 38;

        if (mouseX >= saveX && mouseX <= saveX + 67 &&
                mouseY >= saveY && mouseY <= saveY + 22 && button == 0) {
            saveConfig();
            return true;
        }

        return false;
    }

    public boolean keyPressed(int keyCode) {
        if (!dataHandler.isCreating()) return false;

        if (keyCode == 259) {
            dataHandler.removeLastChar();
            return true;
        }

        if (keyCode == 257) {
            saveConfig();
            return true;
        }

        return false;
    }

    public boolean charTyped(char chr) {
        if (!dataHandler.isCreating()) return false;

        dataHandler.appendChar(chr);
        return true;
    }

    private void saveConfig() {
        String name = dataHandler.getNewConfigName();

        if (name.isEmpty()) {
            notificationRenderer.show("Enter a config name", 
                    ConfigNotificationRenderer.NotificationType.ERROR);
            return;
        }

        if (name.equalsIgnoreCase("autoconfig")) {
            notificationRenderer.show("This name is reserved", 
                    ConfigNotificationRenderer.NotificationType.ERROR);
            return;
        }

        if (dataHandler.saveConfig(name)) {
            notificationRenderer.show("Config saved: " + name, 
                    ConfigNotificationRenderer.NotificationType.SUCCESS);
            dataHandler.clearNewConfigName();
            dataHandler.setCreating(false);
        } else {
            notificationRenderer.show("Config already exists", 
                    ConfigNotificationRenderer.NotificationType.ERROR);
        }
    }
}

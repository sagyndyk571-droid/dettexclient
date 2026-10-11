package neverlose.screens.clickgui.impl.configs.render;

import net.minecraft.client.gui.DrawContext;
import neverlose.screens.clickgui.impl.configs.ConfigsRenderer;
import neverlose.screens.clickgui.impl.configs.handler.ConfigAnimationHandler;
import neverlose.screens.clickgui.impl.configs.handler.ConfigDataHandler;
import neverlose.util.render.Render2D;
import neverlose.util.render.font.Fonts;
import neverlose.util.render.shader.Scissor;

import java.awt.Color;
import java.util.Map;

public class ConfigListRenderer {

    private static final float CONFIG_ITEM_HEIGHT = 34f;
    private static final float CONFIG_ITEM_SPACING = 5f;
    private static final float HOVER_SPEED = 0.15f;

    private final ConfigAnimationHandler animationHandler;
    private final ConfigDataHandler dataHandler;
    private final ConfigNotificationRenderer notificationRenderer;

    public ConfigListRenderer(ConfigAnimationHandler animationHandler, ConfigDataHandler dataHandler,
                              ConfigNotificationRenderer notificationRenderer) {
        this.animationHandler = animationHandler;
        this.dataHandler = dataHandler;
        this.notificationRenderer = notificationRenderer;
    }

    public void render(DrawContext context, float x, float y, float mouseX, float mouseY,
                       int guiScale, float alpha) {
        float listX = x + 8f;
        float listY = y + 42f;
        float listW = ConfigsRenderer.PANEL_WIDTH - 16f;
        float listH = ConfigsRenderer.PANEL_HEIGHT - 52f;

        if (dataHandler.isCreating()) {
            listH -= 46f * animationHandler.getCreateBoxAnimation();
        }

        dataHandler.updateScroll(0.016f);
        dataHandler.updateScrollFades(listH);

        Scissor.enable(listX, listY, listW, listH, guiScale);

        float itemY = listY + (float) dataHandler.getScrollOffset();
        for (String config : dataHandler.getConfigs()) {
            float itemAlpha = animationHandler.getItemAppearAnimation(config);

            if (itemAlpha < 0.01f) {
                itemY += CONFIG_ITEM_HEIGHT + CONFIG_ITEM_SPACING;
                continue;
            }

            if (itemY + CONFIG_ITEM_HEIGHT >= listY && itemY <= listY + listH) {
                float itemSlide = (1f - itemAlpha) * 15f;
                renderConfigItem(config, listX + itemSlide, itemY, listW, mouseX, mouseY, alpha * itemAlpha);
            }
            itemY += CONFIG_ITEM_HEIGHT + CONFIG_ITEM_SPACING;
        }

        if (dataHandler.getConfigs().isEmpty()) {
            renderEmptyMessage(x, y, alpha);
        }

        Scissor.disable();
        renderScrollFade(listX, listY, listW, listH,
                dataHandler.getScrollTopFade(), dataHandler.getScrollBottomFade(), alpha);
    }

    private void renderConfigItem(String config, float x, float y, float width,
                                  float mouseX, float mouseY, float alpha) {
        boolean isHovered = mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + CONFIG_ITEM_HEIGHT;
        boolean isSelected = config.equals(dataHandler.getSelectedConfig());

        float hoverAnim = animationHandler.getHoverAnimation(config);
        hoverAnim += ((isHovered ? 1f : 0f) - hoverAnim) * HOVER_SPEED;
        animationHandler.setHoverAnimation(config, hoverAnim);

        renderItemBackground(x, y, width, isSelected, hoverAnim, alpha);
        renderItemName(config, x, y, width, alpha);
        renderActionButtons(config, x, y, width, mouseX, mouseY, alpha);
    }

    private void renderItemBackground(float x, float y, float width, boolean isSelected,
                                      float hoverAnim, float alpha) {
        int bgAlpha = (int) ((180 + 34 * hoverAnim + (isSelected ? 25 : 0)) * alpha);
        int r = (int) (10 + 7 * hoverAnim + (isSelected ? 8 : 0));
        int g = (int) (16 + 9 * hoverAnim + (isSelected ? 11 : 0));
        int b = (int) (21 + 12 * hoverAnim + (isSelected ? 17 : 0));
        Render2D.rect(x, y, width, CONFIG_ITEM_HEIGHT, new Color(r, g, b, bgAlpha).getRGB(), 7f);

        if (isSelected) {
            Render2D.rect(x + 5f, y + 8f, 1.7f, CONFIG_ITEM_HEIGHT - 16f,
                    new Color(83, 126, 245, (int) (210 * alpha)).getRGB(), 1f);
        }

        int outlineAlpha = (int) ((54 + 54 * hoverAnim + (isSelected ? 42 : 0)) * alpha);
        Render2D.outline(x, y, width, CONFIG_ITEM_HEIGHT, 0.5f,
                new Color(50, 64, 82, outlineAlpha).getRGB(), 7f);
    }

    private void renderItemName(String config, float x, float y, float width, float alpha) {
        Render2D.rect(x + 11f, y + 8f, 18f, 18f,
                new Color(16, 24, 32, (int) (225 * alpha)).getRGB(), 6f);
        Render2D.outline(x + 11f, y + 8f, 18f, 18f, 0.4f,
                new Color(55, 72, 95, (int) (85 * alpha)).getRGB(), 6f);
        Fonts.GUI_ICONS.draw("B", x + 15f, y + 12.2f, 10f,
                new Color(91, 132, 255, (int) (215 * alpha)).getRGB());

        Fonts.INTER_SEMIBOLD.draw(trimText(config, width - 154f, 5.7f),
                x + 38f, y + 8.2f, 5.7f,
                new Color(220, 225, 238, (int) (255 * alpha)).getRGB());
        Fonts.INTER_MEDIUM.draw("Local profile", x + 38f, y + 18.6f, 4.2f,
                new Color(112, 120, 132, (int) (180 * alpha)).getRGB());
    }

    private void renderActionButtons(String config, float x, float y, float width,
                                     float mouseX, float mouseY, float alpha) {
        float buttonW = 34f;
        float buttonH = 17f;
        float buttonY = y + (CONFIG_ITEM_HEIGHT - buttonH) / 2f;
        float deleteButtonX = x + width - buttonW - 9f;
        float refreshButtonX = deleteButtonX - buttonW - 5f;
        float loadButtonX = refreshButtonX - buttonW - 5f;

        renderActionButton(loadButtonX, buttonY, buttonW, buttonH, "Load",
                mouseX, mouseY, animationHandler.getLoadHoverAnimations(), config,
                new Color(80, 180, 80), alpha);
        renderActionButton(refreshButtonX, buttonY, buttonW, buttonH, "Save",
                mouseX, mouseY, animationHandler.getRefreshHoverAnimations(), config,
                new Color(80, 140, 200), alpha);
        renderActionButton(deleteButtonX, buttonY, buttonW, buttonH, "Del",
                mouseX, mouseY, animationHandler.getDeleteHoverAnimations(), config,
                new Color(180, 80, 80), alpha);
    }

    private void renderActionButton(float x, float y, float width, float height, String label,
                                    float mouseX, float mouseY,
                                    Map<String, Float> animations, String config,
                                    Color hoverColor, float alpha) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        float anim = animations.getOrDefault(config, 0f);
        anim += ((hovered ? 1f : 0f) - anim) * HOVER_SPEED;
        animations.put(config, anim);

        int bgAlpha = (int) ((185 + 36 * anim) * alpha);
        int r = (int) (17 + (hoverColor.getRed() - 17) * anim);
        int g = (int) (24 + (hoverColor.getGreen() - 24) * anim);
        int b = (int) (33 + (hoverColor.getBlue() - 33) * anim);

        Render2D.rect(x, y, width, height, new Color(r, g, b, bgAlpha).getRGB(), 5f);
        Render2D.outline(x, y, width, height, 0.45f,
                new Color(58, 72, 94, (int) ((74 + 50 * anim) * alpha)).getRGB(), 5f);

        int textAlpha = (int) ((170 + 85 * anim) * alpha);
        float textW = Fonts.INTER_MEDIUM.getWidth(label, 4.25f);
        Fonts.INTER_MEDIUM.draw(label, x + (width - textW) / 2f, y + 5.4f, 4.25f,
                new Color(218, 224, 238, textAlpha).getRGB());
    }

    private void renderEmptyMessage(float x, float y, float alpha) {
        String text = "No configs found";
        float textWidth = Fonts.INTER_MEDIUM.getWidth(text, 5.8f);
        Fonts.INTER_MEDIUM.draw(text, x + (ConfigsRenderer.PANEL_WIDTH - textWidth) / 2f,
                y + ConfigsRenderer.PANEL_HEIGHT / 2f, 5.8f,
                new Color(112, 120, 132, (int) (170 * alpha)).getRGB());
    }

    private void renderScrollFade(float x, float y, float w, float h, float topFade, float bottomFade, float alpha) {
        int size = 15;
        if (topFade > 0.01f) {
            for (int i = 0; i < size; i++) {
                float fadeAlpha = 80 * topFade * (1f - i / (float) size) * alpha;
                Render2D.rect(x, y + i, w, 1f, new Color(10, 14, 19, (int) fadeAlpha).getRGB(), 0);
            }
        }
        if (bottomFade > 0.01f) {
            for (int i = 0; i < size; i++) {
                float fadeAlpha = 80 * bottomFade * (i / (float) size) * alpha;
                Render2D.rect(x, y + h - size + i, w, 1f, new Color(10, 14, 19, (int) fadeAlpha).getRGB(), 0);
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, float panelX, float panelY) {
        float listX = panelX + 8f;
        float listY = panelY + 42f;
        float listW = ConfigsRenderer.PANEL_WIDTH - 16f;
        float listH = ConfigsRenderer.PANEL_HEIGHT - 52f;

        if (dataHandler.isCreating()) {
            listH -= 46f * animationHandler.getCreateBoxAnimation();
        }

        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            float itemY = listY + (float) dataHandler.getScrollOffset();

            for (String config : dataHandler.getConfigs()) {
                float itemAlpha = animationHandler.getItemAppearAnimation(config);
                if (itemAlpha < 0.5f) {
                    itemY += CONFIG_ITEM_HEIGHT + CONFIG_ITEM_SPACING;
                    continue;
                }

                if (mouseY >= itemY && mouseY <= itemY + CONFIG_ITEM_HEIGHT) {
                    return handleItemClick(config, mouseX, mouseY, button, listX, listW, itemY);
                }
                itemY += CONFIG_ITEM_HEIGHT + CONFIG_ITEM_SPACING;
            }
        }

        return false;
    }

    private boolean handleItemClick(String config, double mouseX, double mouseY, int button,
                                    float listX, float listW, float itemY) {
        float buttonW = 34f;
        float buttonH = 17f;
        float buttonY = itemY + (CONFIG_ITEM_HEIGHT - buttonH) / 2f;
        float deleteButtonX = listX + listW - buttonW - 9f;
        float refreshButtonX = deleteButtonX - buttonW - 5f;
        float loadButtonX = refreshButtonX - buttonW - 5f;

        if (mouseX >= loadButtonX && mouseX <= loadButtonX + buttonW
                && mouseY >= buttonY && mouseY <= buttonY + buttonH && button == 0) {
            if (dataHandler.loadConfig(config)) {
                notificationRenderer.show("Config loaded: " + config,
                        ConfigNotificationRenderer.NotificationType.SUCCESS);
            } else {
                notificationRenderer.show("Config not found",
                        ConfigNotificationRenderer.NotificationType.ERROR);
            }
            return true;
        }

        if (mouseX >= refreshButtonX && mouseX <= refreshButtonX + buttonW
                && mouseY >= buttonY && mouseY <= buttonY + buttonH && button == 0) {
            if (dataHandler.refreshConfig(config)) {
                notificationRenderer.show("Config saved: " + config,
                        ConfigNotificationRenderer.NotificationType.INFO);
            } else {
                notificationRenderer.show("Error saving config",
                        ConfigNotificationRenderer.NotificationType.ERROR);
            }
            return true;
        }

        if (mouseX >= deleteButtonX && mouseX <= deleteButtonX + buttonW
                && mouseY >= buttonY && mouseY <= buttonY + buttonH && button == 0) {
            if (dataHandler.deleteConfig(config)) {
                notificationRenderer.show("Config deleted: " + config,
                        ConfigNotificationRenderer.NotificationType.SUCCESS);
            } else {
                notificationRenderer.show("Error deleting config",
                        ConfigNotificationRenderer.NotificationType.ERROR);
            }
            return true;
        }

        if (button == 0) {
            String current = dataHandler.getSelectedConfig();
            dataHandler.setSelectedConfig(config.equals(current) ? null : config);
            return true;
        }

        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double vertical,
                                 float panelX, float panelY) {
        if (mouseX >= panelX && mouseX <= panelX + ConfigsRenderer.PANEL_WIDTH
                && mouseY >= panelY && mouseY <= panelY + ConfigsRenderer.PANEL_HEIGHT) {

            float visibleHeight = ConfigsRenderer.PANEL_HEIGHT - 52f;
            if (dataHandler.isCreating()) {
                visibleHeight -= 46f;
            }

            dataHandler.handleScroll(vertical, visibleHeight);
            return true;
        }

        return false;
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
}

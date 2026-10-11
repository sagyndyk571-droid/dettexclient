package neverlose.screens.clickgui.impl.configs.render;

import neverlose.screens.clickgui.impl.configs.ConfigsRenderer;
import neverlose.screens.clickgui.impl.configs.handler.ConfigDataHandler;
import neverlose.util.render.Render2D;
import neverlose.util.render.font.Fonts;

import java.awt.*;

public class ConfigHeaderRenderer {

    private final ConfigDataHandler dataHandler;

    public ConfigHeaderRenderer(ConfigDataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    public void render(float x, float y, float mouseX, float mouseY, float alpha) {
        Fonts.INTER_SEMIBOLD.draw("CONFIGS", x + 12, y + 7.4f, 5.8f,
                new Color(222, 228, 240, (int) (245 * alpha)).getRGB());
        Fonts.INTER_MEDIUM.draw(dataHandler.getConfigs().size() + " saved profiles", x + 12, y + 18.4f, 4.4f,
                new Color(117, 126, 140, (int) (185 * alpha)).getRGB());

        renderCreateButton(x, y, mouseX, mouseY, alpha);
        renderSeparator(x, y, alpha);
    }

    private void renderCreateButton(float x, float y, float mouseX, float mouseY, float alpha) {
        float buttonX = x + ConfigsRenderer.PANEL_WIDTH - 80;
        float buttonY = y + 7;
        float buttonW = 68;
        float buttonH = 18;

        boolean hovered = mouseX >= buttonX && mouseX <= buttonX + buttonW &&
                mouseY >= buttonY && mouseY <= buttonY + buttonH;

        int bgAlpha = (int) ((hovered ? 235 : 215) * alpha);
        int outlineAlpha = (int) ((hovered ? 145 : 95) * alpha);

        Render2D.rect(buttonX, buttonY, buttonW, buttonH, new Color(14, 21, 29, bgAlpha).getRGB(), 5);
        Render2D.outline(buttonX, buttonY, buttonW, buttonH, 0.5f, new Color(62, 78, 104, outlineAlpha).getRGB(), 5);

        String text = dataHandler.isCreating() ? "Cancel" : "Create";
        float textWidth = Fonts.INTER_MEDIUM.getWidth(text, 5);
        int iconColor = new Color(91, 132, 255, (int) (235 * alpha)).getRGB();
        Fonts.INTER_MEDIUM.draw(dataHandler.isCreating() ? "-" : "+", buttonX + 9f, buttonY + 5.35f, 5.6f, iconColor);
        Fonts.INTER_MEDIUM.draw(text, buttonX + 22f + (buttonW - 24f - textWidth) / 2f, buttonY + 6.3f, 5,
                new Color(215, 222, 238, (int) (255 * alpha)).getRGB());
    }

    private void renderSeparator(float x, float y, float alpha) {
        Render2D.rect(x + 10, y + 33, ConfigsRenderer.PANEL_WIDTH - 20, 0.5f,
                new Color(34, 45, 55, (int) (115 * alpha)).getRGB(), 0);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, float panelX, float panelY) {
        float buttonX = panelX + ConfigsRenderer.PANEL_WIDTH - 80;
        float buttonY = panelY + 7;

        if (mouseX >= buttonX && mouseX <= buttonX + 68 &&
                mouseY >= buttonY && mouseY <= buttonY + 18 && button == 0) {
            dataHandler.toggleCreating();
            return true;
        }

        return false;
    }
}

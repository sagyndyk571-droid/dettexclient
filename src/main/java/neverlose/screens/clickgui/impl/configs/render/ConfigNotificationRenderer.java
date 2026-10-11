package neverlose.screens.clickgui.impl.configs.render;

import lombok.Getter;
import neverlose.screens.clickgui.impl.configs.ConfigsRenderer;
import neverlose.util.render.Render2D;
import neverlose.util.render.font.Fonts;

import java.awt.*;

public class ConfigNotificationRenderer {

    private String notification = null;
    private NotificationType notificationType = NotificationType.SUCCESS;
    private float notificationAlpha = 0f;
    private long notificationTime = 0;
    private long lastUpdateTime = System.currentTimeMillis();

    @Getter
    public enum NotificationType {
        SUCCESS(new Color(60, 120, 60), new Color(180, 255, 180)),
        ERROR(new Color(120, 60, 60), new Color(255, 180, 180)),
        INFO(new Color(60, 100, 140), new Color(180, 220, 255));

        private final Color bgColor;
        private final Color textColor;

        NotificationType(Color bgColor, Color textColor) {
            this.bgColor = bgColor;
            this.textColor = textColor;
        }
    }

    public void render(float x, float y, float alpha) {
        updateAnimation();

        if (notification == null || notificationAlpha < 0.01f) return;

        float notifY = y + ConfigsRenderer.PANEL_HEIGHT - 25;
        float notifAlpha = notificationAlpha * alpha;

        Color bgColor = notificationType.getBgColor();
        Color textColor = notificationType.getTextColor();

        float textWidth = Fonts.INTER_MEDIUM.getWidth(notification, 5);
        float notifW = textWidth + 20;
        float notifX = x + (ConfigsRenderer.PANEL_WIDTH - notifW) / 2;

        Render2D.rect(notifX, notifY, notifW, 18,
                new Color(12, 17, 24, (int) (235 * notifAlpha)).getRGB(), 4);
        Render2D.outline(notifX, notifY, notifW, 18, 0.5f,
                new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(),
                        (int) (115 * notifAlpha)).getRGB(), 4);
        Render2D.rect(notifX + 5, notifY + 5, 2, 8,
                new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(),
                        (int) (220 * notifAlpha)).getRGB(), 1);

        Fonts.INTER_MEDIUM.draw(notification, notifX + 11, notifY + 6, 5,
                new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), 
                        (int) (255 * notifAlpha)).getRGB());
    }

    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastUpdateTime) / 1000f, 0.1f);
        lastUpdateTime = currentTime;

        if (notification != null) {
            if (System.currentTimeMillis() - notificationTime < 2000) {
                notificationAlpha += (1f - notificationAlpha) * 8f * deltaTime;
            } else {
                notificationAlpha += (0f - notificationAlpha) * 4f * deltaTime;
                if (notificationAlpha < 0.01f) {
                    notification = null;
                }
            }
        }
    }

    public void show(String message, NotificationType type) {
        if (message != null && message.indexOf('\u0420') >= 0) {
            message = type == NotificationType.INFO ? "Config refreshed" : "Config updated";
        }
        notification = message;
        notificationType = type;
        notificationTime = System.currentTimeMillis();
        notificationAlpha = 0f;
    }
}

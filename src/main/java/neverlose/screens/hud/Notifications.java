package neverlose.screens.hud;

import net.minecraft.client.gui.DrawContext;
import neverlose.client.draggables.AbstractHudElement;
import neverlose.modules.impl.render.Hud;
import neverlose.util.animations.Animation;
import neverlose.util.animations.Direction;
import neverlose.util.animations.OutBack;
import neverlose.util.render.Render2D;
import neverlose.util.render.font.Fonts;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Notifications extends AbstractHudElement {

    private static final int FORCED_GUI_SCALE = 2;

    private static Notifications instance;

    public static Notifications getInstance() {
        return instance;
    }

    List<Notification> getNotifications() {
        return list;
    }

    private final List<Notification> list = new ArrayList<>();
    private static final float NOTIFICATION_HEIGHT = 16f;
    private static final float NOTIFICATION_GAP = 3f;

    public Notifications() {
        super("Notifications", 0, 0, 110, 16, false);
        instance = this;
    }

    private int getCurrentGuiScale() {
        int scale = mc.options.getGuiScale().getValue();
        if (scale == 0) {
            scale = mc.getWindow().calculateScaleFactor(0, mc.forcesUnicodeFont());
        }
        return scale;
    }

    private float getScaleFactor() {
        return (float) getCurrentGuiScale() / (float) FORCED_GUI_SCALE;
    }

    private float getVirtualWidth() {
        return mc.getWindow().getFramebufferWidth() / (float) FORCED_GUI_SCALE;
    }

    private float getVirtualHeight() {
        return mc.getWindow().getFramebufferHeight() / (float) FORCED_GUI_SCALE;
    }

    @Override
    public boolean visible() {
        return !list.isEmpty();
    }

    @Override
    public void tick() {
        list.forEach(notif -> {
            if (System.currentTimeMillis() > notif.removeTime ||
                    (notif.text.contains("Notification") && !isChat(mc.currentScreen))) {
                notif.anim.setDirection(Direction.BACKWARDS);
            }
        });
        list.removeIf(notif -> notif.anim.isFinished(Direction.BACKWARDS));

        if (isChat(mc.currentScreen)) {
            boolean hasHiNotification = list.stream()
                    .anyMatch(n -> n.text.contains("Notification"));
            if (!hasHiNotification) {
                addNotification("Notification", 99999999);
            }
        }

        updatePosition();
    }

    private void updatePosition() {
        if (mc.getWindow() == null) return;

        float virtualWidth = getVirtualWidth();
        float virtualHeight = getVirtualHeight();

        float crosshairX = virtualWidth / 2f;
        float crosshairY = virtualHeight / 2f;

        this.setX((int) (crosshairX - 60));
        this.setY((int) (crosshairY + 100));
    }

    public void addNotification(String text, long duration) {
        Animation anim = new OutBack().setMs(700).setValue(1);
        anim.setDirection(Direction.FORWARDS);

        int targetIndex = list.size();
        float targetY = targetIndex * (NOTIFICATION_HEIGHT + NOTIFICATION_GAP);

        Notification notification = new Notification(text, anim, System.currentTimeMillis(), System.currentTimeMillis() + duration);
        notification.currentY = targetY;
        notification.targetY = targetY;
        notification.velocityY = 0;

        list.add(notification);
        if (list.size() > 12) list.removeFirst();
        list.sort(Comparator.comparingDouble(notif -> -notif.removeTime));

        updateTargetPositions();
    }

    private void updateTargetPositions() {
        float offsetY = 0;
        for (int i = 0; i < list.size(); i++) {
            Notification notif = list.get(i);
            float anim = notif.anim.getOutput().floatValue();
            notif.targetY = offsetY;
            offsetY += (NOTIFICATION_HEIGHT + NOTIFICATION_GAP) * anim;
        }
    }

    private int clampAlpha(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private int clampAlpha(float value) {
        return Math.max(0, Math.min(255, (int) value));
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        alpha = clampAlpha(alpha);
        if (alpha <= 0) return;

        float alphaFactor = alpha / 255.0f;
        updatePosition();
        updateTargetPositions();

        float springStiffness = 180f;
        float damping = 12f;
        float deltaTime = 0.016f;

        for (Notification notification : list) {
            float diff = notification.targetY - notification.currentY;
            float springForce = diff * springStiffness;
            float dampingForce = notification.velocityY * damping;
            float acceleration = springForce - dampingForce;

            notification.velocityY += acceleration * deltaTime;
            notification.currentY += notification.velocityY * deltaTime;

            if (Math.abs(diff) < 0.01f && Math.abs(notification.velocityY) < 0.01f) {
                notification.currentY = notification.targetY;
                notification.velocityY = 0;
            }
        }

        float offsetX = 5;
        float maxWidth = 0;
        float totalHeight = 0;

        for (Notification notification : list) {
            float anim = notification.anim.getOutput().floatValue();
            if (anim <= 0.01f) continue;

            anim = Math.max(0f, Math.min(1f, anim));

            float textWidth = Fonts.FONT67.getWidth(notification.text, 6);
            float width = textWidth + offsetX * 2 + 20;
            maxWidth = Math.max(maxWidth, width);

            float startY = this.getY() + notification.currentY;
            float startX = this.getX() + (120 - width) / 2;

            int bgAlpha = clampAlpha((useTransparentBackground() ? 145 : 240) * anim * alphaFactor);
            int textAlpha = clampAlpha(255 * anim * alphaFactor);
            int iconAlpha = clampAlpha(255 * anim * alphaFactor);

            if (bgAlpha > 0) {
                int panelAlpha = HudTheme.backgroundAlpha(bgAlpha);
                int Color67 = new Color(20, 15, 23, panelAlpha).getRGB();
                int Color677 = new Color(29, 23, 33, panelAlpha).getRGB();

                Render2D.gradientRect(startX, startY, width, NOTIFICATION_HEIGHT,
                        new int[]{
                                Color67,
                                Color67,
                                Color67,
                                Color677
                        },
                        8);

                float iconBoxX = startX + width - offsetX - 12.3f;
                float iconBoxY = startY + NOTIFICATION_HEIGHT / 2 - 6.5f;

                Fonts.FONT67.draw(notification.text, startX + offsetX, startY + NOTIFICATION_HEIGHT / 2 - 3, 6,
                        HudTheme.text(textAlpha));

                Fonts.ICONS2.draw("J", iconBoxX + 2.5f, iconBoxY + 3.5f, 8,
                        HudTheme.accent(iconAlpha));
            }

            totalHeight = Math.max(totalHeight, notification.currentY + NOTIFICATION_HEIGHT);
        }

        if (maxWidth > 0) {
            setWidth((int) Math.ceil(maxWidth));
        }
        setHeight((int) Math.ceil(Math.max(NOTIFICATION_HEIGHT, totalHeight)));
    }

    private boolean useTransparentBackground() {
        Hud hud = Hud.getInstance();
        return hud != null && hud.notifyAlphaBg != null && hud.notifyAlphaBg.isValue();
    }

    public static class Notification {
        String text;
        Animation anim;
        long startTime;
        long removeTime;

        float currentY;
        float targetY;
        float velocityY;

        Notification(String text, Animation anim, long startTime, long removeTime) {
            this.text = text;
            this.anim = anim;
            this.startTime = startTime;
            this.removeTime = removeTime;
            this.currentY = 0;
            this.targetY = 0;
            this.velocityY = 0;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > removeTime;
        }
    }
}

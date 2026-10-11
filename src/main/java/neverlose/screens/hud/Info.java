package neverlose.screens.hud;

import net.minecraft.client.gui.DrawContext;
import neverlose.client.draggables.AbstractHudElement;
import neverlose.modules.impl.render.Hud;
import neverlose.util.animations.Direction;
import neverlose.util.render.Render2D;
import neverlose.util.render.shader.Scissor;
import neverlose.util.render.font.Fonts;

import java.awt.*;
import java.util.Locale;

public class Info extends AbstractHudElement {

    private double lastX = 0;
    private double lastZ = 0;
    private double currentBps = 0;
    private double displayBps = 0;
    private long lastSampleTime = 0;

    private float animatedWidth = 80;
    private float animatedHeight = 23;
    private long lastRenderTime = System.currentTimeMillis();

    private static final double BPS_SMOOTHING = 0.42;
    private static final double DISPLAY_BPS_SPEED = 10.0;
    private static final float ANIMATION_SPEED = 8.0f;
    private static final float FONT_SIZE = 6.0f;
    private static final float HEIGHT = 15.5f;

    public Info() {
        super("Info", 10, 28, 80, 16, true);
        startAnimation();
    }

    @Override
    public boolean visible() {
        return !scaleAnimation.isFinished(Direction.BACKWARDS);
    }

    @Override
    public void tick() {
        startAnimation();
        updateBps();
    }

    private void updateBps() {
        if (mc.player == null) {
            resetBps();
            return;
        }

        long now = System.nanoTime();
        double x = mc.player.getX();
        double z = mc.player.getZ();

        if (lastSampleTime == 0) {
            lastX = x;
            lastZ = z;
            lastSampleTime = now;
            return;
        }

        double elapsed = (now - lastSampleTime) / 1_000_000_000.0;
        double dx = x - lastX;
        double dz = z - lastZ;
        double distance = Math.sqrt(dx * dx + dz * dz);

        lastX = x;
        lastZ = z;
        lastSampleTime = now;

        if (elapsed <= 0.0) {
            return;
        }

        if (elapsed > 0.25 || distance > 16.0) {
            currentBps = 0.0;
            return;
        }

        double instantBps = distance / elapsed;
        if (instantBps < 0.03) {
            currentBps *= 0.45;
            if (currentBps < 0.03) {
                currentBps = 0.0;
            }
            return;
        }

        currentBps += (instantBps - currentBps) * BPS_SMOOTHING;
    }

    private void resetBps() {
        lastX = 0;
        lastZ = 0;
        currentBps = 0;
        displayBps = 0;
        lastSampleTime = 0;
    }

    private float lerp(float current, float target, float deltaTime) {
        float factor = (float) (1.0 - Math.pow(0.001, deltaTime * ANIMATION_SPEED));
        return current + (target - current) * factor;
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) return;
        if (mc.player == null) return;

        float alphaFactor = alpha / 255.0f;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastRenderTime) / 1000.0f;
        lastRenderTime = currentTime;
        deltaTime = Math.min(deltaTime, 0.1f);

        boolean showBps = Hud.getInstance() != null && Hud.getInstance().showBps.isValue();

        double displayFactor = 1.0 - Math.pow(0.001, deltaTime * DISPLAY_BPS_SPEED);
        displayBps += (currentBps - displayBps) * displayFactor;
        if (Math.abs(displayBps - currentBps) < 0.01) {
            displayBps = currentBps;
        }

        float x = getX();
        float y = getY();

        float padding = 8;
        float iconSize = 10f;
        float sectionGap = 8f;

        int playerX = (int) mc.player.getX();
        int playerY = (int) mc.player.getY();
        int playerZ = (int) mc.player.getZ();

        String coordsText = playerX + " " + playerY + " " + playerZ;
        float coordsWidth = Fonts.FONT67.getWidth(coordsText, FONT_SIZE);

        String bpsText = String.format(Locale.US, "%.2f b/s", Math.max(0.0, displayBps));
        float bpsWidth = Fonts.FONT67.getWidth(bpsText, FONT_SIZE);

        float targetWidth = padding * 2f + iconSize + sectionGap + coordsWidth + padding;
        if (showBps) {
            targetWidth += sectionGap + iconSize + sectionGap + bpsWidth;
        }

        float targetHeight = HEIGHT;

        animatedWidth = lerp(animatedWidth, targetWidth, deltaTime);
        animatedHeight = lerp(animatedHeight, targetHeight, deltaTime);

        if (Math.abs(animatedWidth - targetWidth) < 0.3f) {
            animatedWidth = targetWidth;
        }
        if (Math.abs(animatedHeight - targetHeight) < 0.3f) {
            animatedHeight = targetHeight;
        }

        setWidth((int) Math.ceil(animatedWidth));
        setHeight((int) Math.ceil(animatedHeight));

        int bgAlpha = (int) (255 * alphaFactor);
        int panelTop = HudTheme.panelTop(bgAlpha);
        int panel = HudTheme.panel(bgAlpha);
        int panelBottom = HudTheme.panelBottom(bgAlpha);
        int accent = HudTheme.accent(bgAlpha);

        Render2D.gradientRect(x, y, animatedWidth, animatedHeight,
                new int[]{
                        panelTop,
                        panelTop,
                        panel,
                        panelBottom
                },
                8);

        Scissor.enable(x, y, animatedWidth, animatedHeight, 2);

        float textY = y + HEIGHT / 2f - Fonts.FONT67.getHeight(FONT_SIZE) / 2f + 0.7f;
        float iconY = y + HEIGHT / 2f - iconSize / 2f;
        float currentX = x + padding;

        Fonts.iconsnew.draw("M", currentX, iconY - 2f, iconSize + 2f, accent);
        currentX += iconSize + sectionGap;
        Fonts.FONT67.draw(coordsText, currentX, textY, FONT_SIZE, HudTheme.text(bgAlpha));
        currentX += coordsWidth + sectionGap;

        if (showBps) {
            Fonts.iconsnew.draw("D", currentX, iconY - 2f, iconSize + 2f, accent);
            currentX += iconSize + sectionGap;
            Fonts.FONT67.draw(bpsText, currentX, textY, FONT_SIZE, HudTheme.text(bgAlpha));
        }

        Scissor.disable();
    }
}

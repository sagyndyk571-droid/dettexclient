package neverlose.client.draggables;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import neverlose.modules.impl.render.Hud;
import neverlose.util.ColorUtil;
import neverlose.util.animations.Animation;
import neverlose.util.animations.Decelerate;
import neverlose.util.animations.Direction;
import neverlose.util.render.HudRender;
import neverlose.util.render.Render2D;

public abstract class AbstractHudElement implements HudElement {

    protected int x, y, width, height;
    protected String name;
    protected boolean enabled = true;
    protected boolean draggable = true;
    protected float hudScale = 1.0f;

    protected final MinecraftClient mc = MinecraftClient.getInstance();
    protected final Animation scaleAnimation = new Decelerate().setMs(300).setValue(1);
    protected float lastTickDelta = 0f;

    public AbstractHudElement(String name, int x, int y, int width, int height, boolean draggable) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.draggable = draggable;
    }

    @Override
    public void render(DrawContext context, float tickDelta) {
        if (!visible()) return;

        this.lastTickDelta = tickDelta;
        scaleAnimation.update();

        Hud hud = Hud.getInstance();
        int alpha = (int) (scaleAnimation.getOutput().floatValue() * 255);
        if (alpha <= 0) return;

        HudRender.push(x, y, hudScale);
        try {
            drawHudBlur(hud, alpha);
            drawDraggable(context, alpha);
        } finally {
            HudRender.pop();
        }
    }

    protected void drawHudBlur(Hud hud, int alpha) {
        if (hud == null || hud.hudBlur == null || hud.hudBlurRadius == null || !hud.hudBlur.isValue()) {
            return;
        }

        float blurX = snapToFixedPixel(getHudBlurX());
        float blurY = snapToFixedPixel(getHudBlurY());
        float blurWidth = snapToFixedPixel(getHudBlurWidth());
        float blurHeight = snapToFixedPixel(getHudBlurHeight());

        if (blurWidth <= 1 || blurHeight <= 1) {
            return;
        }

        float blurRadius = Math.max(0.1f, hud.hudBlurRadius.getValue());
        int tint = ColorUtil.rgba(8, 10, 14, (int) (34 * (alpha / 255.0f)));
        Render2D.blur(
                blurX,
                blurY,
                blurWidth,
                blurHeight,
                blurRadius,
                getHudBlurRoundingRadius(),
                tint
        );
    }

    protected float getHudBlurX() {
        return x;
    }

    protected float getHudBlurY() {
        return y;
    }

    protected float getHudBlurWidth() {
        return width;
    }

    protected float getHudBlurHeight() {
        return height;
    }

    protected float getHudBlurRoundingRadius() {
        return getRoundingRadius();
    }

    protected float snapToFixedPixel(float value) {
        float scale = (float) MinecraftClient.getInstance().getWindow().getScaleFactor();
        if (scale <= 0.0f) {
            scale = Render2D.getFixedGuiScale();
        }
        return Math.round(value * scale) / scale;
    }

    public abstract void drawDraggable(DrawContext context, int alpha);

    @Override
    public void tick() {
    }

    @Override
    public boolean visible() {
        return true;
    }

    public void startAnimation() {
        scaleAnimation.setDirection(Direction.FORWARDS);
    }

    public void stopAnimation() {
        scaleAnimation.setDirection(Direction.BACKWARDS);
    }

    protected boolean isChat(Screen screen) {
        return screen instanceof ChatScreen;
    }

    public boolean isDraggable() {
        return draggable;
    }

    public float getLastTickDelta() {
        return lastTickDelta;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public float getHudScale() {
        return hudScale;
    }

    @Override
    public void setHudScale(float scale) {
        this.hudScale = Math.max(0.25f, Math.min(4.0f, scale));
    }
}

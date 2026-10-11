package neverlose.screens.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import neverlose.client.draggables.AbstractHudElement;
import neverlose.util.color.ColorAssist;
import neverlose.util.render.Render2D;
import neverlose.util.render.font.Fonts;

import java.awt.Color;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Watermark extends AbstractHudElement {

    public Watermark() {
        super("Watermark", 10, 10, 200, 20, true);
        startAnimation();
    }

    @Override
    public boolean visible() {
        return true;
    }

    private int getPing() {
        if (mc.getNetworkHandler() != null && mc.player != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) {
                return entry.getLatency();
            }
        }
        return 0;
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0 || mc.player == null) return;

        float x = getX();
        float y = getY();
        float h = 18f;
        float radius = 9f;

        String username = mc.getSession().getUsername();
        int ping = getPing();
        String pingText = ping + " ms";
        String fpsText = mc.getCurrentFps() + " FPS";
        String coordsText = (int)mc.player.getX() + " " + (int)mc.player.getY() + " " + (int)mc.player.getZ();
        String timeText = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        float fontSize = 6.5f;
        float iconSize = 12f;
        float textHeight = Fonts.BOLD.getHeight(fontSize);

        float logoFontSize = 7.5f;
        float logoTextWidth = Fonts.BOLD.getWidth("Dettex", logoFontSize);
        float faceSize = 12f;
        float iconSizeToDraw = 10f;

        float cx = x + 8f;
        // 1. Client brand text
        cx += logoTextWidth + 6f;
        // 2. Face
        cx += faceSize + 4f;
        // 3. Name
        cx += Fonts.BOLD.getWidth(username, fontSize) + 8f;
        // 4. Ping
        cx += iconSize + 3f + Fonts.BOLD.getWidth(pingText, fontSize) + 8f;
        // 5. FPS
        cx += iconSize + 3f + Fonts.BOLD.getWidth(fpsText, fontSize) + 8f;
        // 6. Coords
        cx += iconSize + 3f + Fonts.BOLD.getWidth(coordsText, fontSize) + 8f;
        // 7. Time
        cx += iconSize + 3f + Fonts.BOLD.getWidth(timeText, fontSize) + 8f;

        float w = cx - x;
        setWidth((int)w);
        setHeight((int)h);

        // Background
        int bgColor = new Color(20, 20, 25, 120).getRGB();
        int steps = 8;
        for (int i = 1; i <= steps; i++) {
            float alphaFactor = (float) Math.pow(1f - (i / (float)steps), 1.5);
            int shadowAlpha = (int)(70 * alphaFactor);
            float offset = i * 0.75f;
            Render2D.outline(x - offset, y - offset, w + offset*2, h + offset*2, 0.75f, new Color(0, 0, 0, shadowAlpha).getRGB(), radius + offset);
        }
        Render2D.blur(x, y, w, h, 10f, radius, bgColor);

        int accent = ColorAssist.colorForRectsCustom$();
        int white = -1;
        float textY = y + h / 2f - textHeight / 2f - 0.5f;
        float iconYToDraw = y + h / 2f - iconSizeToDraw / 2f;

        float curX = x + 8f;

        // 1. Text first (значения рисуем до любых текстур, чтобы текстуры не ломали MSDF-текст)
        float logoTextHeight = Fonts.BOLD.getHeight(logoFontSize);
        float logoTextY = y + h / 2f - logoTextHeight / 2f - 0.5f;
        Fonts.BOLD.draw("Dettex", curX, logoTextY, logoFontSize, accent);

        float tX = curX + logoTextWidth + 6f + faceSize + 4f;
        Fonts.BOLD.draw(username, tX, textY, fontSize, white);
        tX += Fonts.BOLD.getWidth(username, fontSize) + 8f;

        tX += iconSizeToDraw + 3f;
        Fonts.BOLD.draw(pingText, tX, textY, fontSize, white);
        tX += Fonts.BOLD.getWidth(pingText, fontSize) + 8f;

        tX += iconSizeToDraw + 3f;
        Fonts.BOLD.draw(fpsText, tX, textY, fontSize, white);
        tX += Fonts.BOLD.getWidth(fpsText, fontSize) + 8f;

        tX += iconSizeToDraw + 3f;
        Fonts.BOLD.draw(coordsText, tX, textY, fontSize, white);
        tX += Fonts.BOLD.getWidth(coordsText, fontSize) + 8f;

        tX += iconSizeToDraw + 3f;
        Fonts.BOLD.draw(timeText, tX, textY, fontSize, white);

        // 2. Face
        curX += logoTextWidth + 6f;
        drawFace(curX, y + h / 2f - faceSize / 2f, faceSize);
        curX += faceSize + 4f;

        // 3. Icons
        curX += Fonts.BOLD.getWidth(username, fontSize) + 8f;

        Render2D.texture(Identifier.of("icons", "ping.png"), curX, iconYToDraw, iconSizeToDraw, iconSizeToDraw, accent);
        curX += iconSizeToDraw + 3f + Fonts.BOLD.getWidth(pingText, fontSize) + 8f;

        Render2D.texture(Identifier.of("icons", "fps.png"), curX, iconYToDraw, iconSizeToDraw, iconSizeToDraw, accent);
        curX += iconSizeToDraw + 3f + Fonts.BOLD.getWidth(fpsText, fontSize) + 8f;

        Render2D.texture(Identifier.of("icons", "coordinates.png"), curX, iconYToDraw, iconSizeToDraw, iconSizeToDraw, accent);
        curX += iconSizeToDraw + 3f + Fonts.BOLD.getWidth(coordsText, fontSize) + 8f;

        Render2D.texture(Identifier.of("icons", "time.png"), curX, iconYToDraw, iconSizeToDraw, iconSizeToDraw, accent);
    }

    private void drawFace(float faceX, float faceY, float faceSize) {
        EntityRenderer<? super LivingEntity, ?> baseRenderer = mc.getEntityRenderDispatcher().getRenderer(mc.player);
        if (!(baseRenderer instanceof LivingEntityRenderer<?, ?, ?>)) {
            return;
        }
        @SuppressWarnings("unchecked")
        LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?> renderer =
                (LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>) baseRenderer;
        LivingEntityRenderState state = renderer.getAndUpdateRenderState(mc.player, 1.0f);
        Identifier textureLocation = renderer.getTexture(state);

        float u0 = 8f / 64f;
        float v0 = 8f / 64f;
        float u1 = 16f / 64f;
        float v1 = 16f / 64f;
        float radius = faceSize / 2f;

        Render2D.texture(textureLocation, faceX, faceY, faceSize, faceSize,
                u0, v0, u1, v1, -1, 0, radius);

        float hatScale = 1.1f;
        float hatSize = faceSize * hatScale;
        float hatOffset = (hatSize - faceSize) / 2f;
        float hatU0 = 40f / 64f;
        float hatV0 = 8f / 64f;
        float hatU1 = 48f / 64f;
        float hatV1 = 16f / 64f;

        Render2D.texture(textureLocation, faceX - hatOffset, faceY - hatOffset, hatSize, hatSize,
                hatU0, hatV0, hatU1, hatV1, -1, 0f, radius);
    }
}

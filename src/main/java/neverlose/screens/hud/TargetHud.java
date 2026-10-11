package neverlose.screens.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import neverlose.client.draggables.AbstractHudElement;
import neverlose.modules.impl.combat.Aura;
import neverlose.util.network.Network;
import neverlose.util.render.Render2D;
import neverlose.util.render.item.ItemRender;
import neverlose.util.render.font.Fonts;
import neverlose.util.timer.StopWatch;
import neverlose.util.color.ColorAssist;

import java.awt.*;

public class TargetHud extends AbstractHudElement {

    private final StopWatch stopWatch = new StopWatch();
    private LivingEntity lastTarget;

    private float healthAnimation = 0;
    private float displayedHealth = 0;
    private long lastUpdateTime = System.currentTimeMillis();

    public TargetHud() {
        super("TargetHud", 10, 80, 140, 42, true);
    }

    @Override
    public boolean visible() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity auraTarget = Aura.target;
        if (auraTarget != null) {
            lastTarget = auraTarget;
            startAnimation();
            stopWatch.reset();
        } else if (isChat(mc.currentScreen)) {
            lastTarget = mc.player;
            startAnimation();
            stopWatch.reset();
        } else if (stopWatch.finished(10)) {
            stopAnimation();
        }
    }

    private float lerp(float current, float target, float deltaTime, float speed) {
        float factor = (float) (1.0 - Math.pow(0.001, deltaTime * speed));
        return current + (target - current) * factor;
    }

    private float getHealth(LivingEntity entity) {
        if (entity.isInvisible() && !Network.isSpookyTime() && !Network.isCopyTime()) {
            return entity.getMaxHealth();
        }
        return entity.getHealth();
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) return;
        if (lastTarget == null) return;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;
        deltaTime = Math.min(deltaTime, 0.1f);

        float x = getX();
        float y = getY();

        float scale = 1f;

        setWidth(140);
        setHeight(42);

        float scaleAlpha = scaleAnimation.getOutput().floatValue();

        drawBackground(x, y, scaleAlpha, scale);
        drawFace(x, y, scaleAlpha, scale);
        drawContent(context, x, y, scaleAlpha, deltaTime, scale);
    }

    private void drawBackground(float x, float y, float alpha, float scale) {
        int alphaInt = (int)(150 * alpha);
        float w = getWidth();
        float h = getHeight();
        float radius = 10f * scale;

        // "тень не снаружи" -> glowOutline bled inside the transparent background.
        // We will use multiple solid outlines expanding outwards so it never bleeds inside!
        int steps = 6;
        for (int i = 1; i <= steps; i++) {
            float alphaFactor = (float) Math.pow(1f - (i / (float)steps), 1.5);
            int shadowAlpha = (int)(60 * alphaFactor * alpha);
            float offset = i * 0.75f * scale;
            Render2D.outline(x - offset, y - offset, w + offset*2, h + offset*2, 0.75f * scale, new Color(0, 0, 0, shadowAlpha).getRGB(), radius + offset);
        }

        Render2D.blur(x, y, w, h, 15f * scale, radius, new Color(15, 15, 15, alphaInt).getRGB());

        // outline removed
    }

    private void drawFace(float x, float y, float alpha, float scale) {
        EntityRenderer<? super LivingEntity, ?> baseRenderer = mc.getEntityRenderDispatcher().getRenderer(lastTarget);
        if (!(baseRenderer instanceof LivingEntityRenderer<?, ?, ?>)) {
            return;
        }

        @SuppressWarnings("unchecked")
        LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?> renderer =
                (LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>) baseRenderer;

        LivingEntityRenderState state = renderer.getAndUpdateRenderState(lastTarget, lastTickDelta);
        Identifier textureLocation = renderer.getTexture(state);

        float faceSize = 28 * scale;
        float faceX = x + 7 * scale;
        float faceY = y + 7 * scale;

        float hurtPercent = lastTarget.hurtTime > 0 ? lastTarget.hurtTime / 10.0f : 0.0f;
        int r = 255;
        int g = (int) (255 * (1.0f - hurtPercent));
        int b = (int) (255 * (1.0f - hurtPercent));
        int color = new Color(r, g, b, (int) (255 * alpha)).getRGB();

        float u0 = 8f / 64f;
        float v0 = 8f / 64f;
        float u1 = 16f / 64f;
        float v1 = 16f / 64f;

        float radius = 6f * scale;

        // Shadow around face outwards
        for (int i = 1; i <= 6; i++) {
            float alphaFactor = (float) Math.pow(1f - (i / 6f), 1.5);
            float offset = i * 0.75f * scale;
            Render2D.outline(faceX - offset, faceY - offset, faceSize + offset*2, faceSize + offset*2, 0.75f * scale, new Color(0, 0, 0, (int)(70 * alphaFactor * alpha)).getRGB(), radius + offset);
        }

        Render2D.texture(textureLocation, faceX, faceY, faceSize, faceSize,
                u0, v0, u1, v1, color, 0, radius);

        float hatScale = 1.1f;
        float hatSize = faceSize * hatScale;
        float hatOffset = (hatSize - faceSize) / 2f;

        float hatU0 = 40f / 64f;
        float hatV0 = 8f / 64f;
        float hatU1 = 48f / 64f;
        float hatV1 = 16f / 64f;

        Render2D.texture(textureLocation, faceX - hatOffset, faceY - hatOffset, hatSize, hatSize,
                hatU0, hatV0, hatU1, hatV1, color, 0f, radius);
    }

    private void drawContent(DrawContext context, float x, float y, float alpha, float deltaTime, float scale) {
        float faceSize = 28 * scale;
        float faceX = x + 7 * scale;
        float contentX = faceX + faceSize + 8 * scale;

        float hp = getHealth(lastTarget);
        float maxHp = lastTarget.getMaxHealth();

        boolean isInvisible = lastTarget.isInvisible() && !Network.isSpookyTime() && !Network.isCopyTime();

        float targetDisplayHealth = isInvisible ? maxHp : hp;
        displayedHealth = lerp(displayedHealth, targetDisplayHealth, deltaTime, 5f);

        String name = lastTarget.getName().getString();

        float maxNameWidth = getWidth() - (contentX - x) - 8 * scale;
        float defaultFontSize = 7.5f * scale;
        float nameWidth = Fonts.BOLD.getWidth(name, defaultFontSize);
        float fontSize = defaultFontSize;
        if (nameWidth > maxNameWidth) {
            fontSize = defaultFontSize * (maxNameWidth / nameWidth);
        }

        float textHeightOffset = (Fonts.BOLD.getHeight(defaultFontSize) - Fonts.BOLD.getHeight(fontSize)) / 2f;
        float nameY = y + 6f * scale + textHeightOffset;

        Fonts.BOLD.draw(name, contentX, nameY, fontSize,
                new Color(255, 255, 255, (int) (255 * alpha)).getRGB());

        float targetHealth = isInvisible ? 1.0f : hp / maxHp;
        healthAnimation = lerp(healthAnimation, targetHealth, deltaTime, 6f);

        // "хилт бар сделай совсем чуть чуть тоньше, и сделай ее сильнее скругленной"
        // Thinner healthbar: 4px instead of 5px.
        float barY = y + 26 * scale;
        float barWidth = 70 * scale;
        float barHeight = 6f * scale;
        float barRadius = barHeight / 2f; // Fully rounded

        // Dark background for health bar
        Render2D.rect(contentX, barY, barWidth, barHeight,
                new Color(25, 20, 25, (int) (180 * alpha)).getRGB(), barRadius);

        float healthPercent = Math.max(0, Math.min(1, healthAnimation));

        if (healthPercent > 0.01f) {
            int themeColor = ColorAssist.colorForRectsCustom$();
            int tr = (themeColor >> 16) & 0xFF;
            int tg = (themeColor >> 8) & 0xFF;
            int tb = themeColor & 0xFF;

            int fillR = Math.min(255, tr + 40);
            int fillG = Math.min(255, tg + 40);
            int fillB = Math.min(255, tb + 40);

            int color1 = new Color(fillR, fillG, fillB, (int) (255 * alpha)).getRGB();
            int color2 = new Color(Math.max(0, fillR-40), Math.max(0, fillG-40), Math.max(0, fillB-40), (int)(255 * alpha)).getRGB();

            Render2D.gradientRect(contentX, barY, barWidth * healthPercent, barHeight,
                new int[] {color1, color2, color2, color1}, barRadius);
        }

        int displayHpInt = (int) Math.ceil(displayedHealth);
        String hpStr = isInvisible ? "??" : String.valueOf(displayHpInt);

        int tColor = ColorAssist.colorForRectsCustom$();
        int tr = (tColor >> 16) & 0xFF;
        int tg = (tColor >> 8) & 0xFF;
        int tb = tColor & 0xFF;
        int textColor = new Color(Math.min(255, tr + 50), Math.min(255, tg + 50), Math.min(255, tb + 50), (int)(255 * alpha)).getRGB();

        // "хп в числах сделай ровнее"
        // Vertical alignment: font height ~6.5f. We subtract ~3.25f from center.
        float textHeight = Fonts.BOLD.getHeight(6.5f * scale);
        float textY = barY + barHeight / 2f - textHeight / 2f;
        Fonts.BOLD.draw(hpStr, contentX + barWidth + 6 * scale, textY, 6.5f * scale, textColor);
        float heartSize = 7.5f * scale;
        float hpWidth = Fonts.BOLD.getWidth(hpStr, 6.5f * scale);

        Render2D.texture(net.minecraft.util.Identifier.of("icons", "heart.png"), contentX + barWidth + 6 * scale + hpWidth + 2 * scale, barY + barHeight / 2f - heartSize / 2f + 0.5f * scale, heartSize, heartSize, tColor);

        // Draw armor and held items between name and health bar
        float s = getHudScale();
        float baseItemY = barY - 9f * scale;
        float itemScale = 0.55f * scale;
        float itemSpacing = 16 * itemScale + 4 * scale;
        float itemX = contentX;

        java.util.List<net.minecraft.item.ItemStack> items = new java.util.ArrayList<>();
        java.util.List<net.minecraft.item.ItemStack> armorList = new java.util.ArrayList<>();
        for (net.minecraft.entity.EquipmentSlot slot : new net.minecraft.entity.EquipmentSlot[]{net.minecraft.entity.EquipmentSlot.HEAD, net.minecraft.entity.EquipmentSlot.CHEST, net.minecraft.entity.EquipmentSlot.LEGS, net.minecraft.entity.EquipmentSlot.FEET}) {
            armorList.add(lastTarget.getEquippedStack(slot));
        }

        items.addAll(armorList);
        items.add(lastTarget.getMainHandStack());
        items.add(lastTarget.getOffHandStack());

        for (net.minecraft.item.ItemStack stack : items) {
            if (stack != null && !stack.isEmpty()) {
                float scaledItemX = x + (itemX - x) * s;
                float scaledItemY = y + (baseItemY - y) * s;
                ItemRender.drawItemWithContext(context, stack, scaledItemX, scaledItemY, itemScale * s, alpha);
                itemX += itemSpacing;
            }
        }
    }
}

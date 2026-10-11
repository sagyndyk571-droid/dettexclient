package neverlose.screens.hud;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import neverlose.client.draggables.AbstractHudElement;
import neverlose.util.animations.Direction;
import neverlose.util.render.Render2D;
import neverlose.util.render.shader.Scissor;
import neverlose.util.render.font.Fonts;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Potions extends AbstractHudElement {

    private List<StatusEffectInstance> effectsList = new java.util.ArrayList<>();

    private float animatedWidth = 80;
    private float animatedHeight = 23;
    private long lastUpdateTime = System.currentTimeMillis();

    private static final float ANIMATION_SPEED = 8.0f;
    private static final float ICON_SIZE = 9f;
    private static final int BLINK_THRESHOLD_TICKS = 100;

    public Potions() {
        super("Potions", 300, 100, 80, 23, true);
        stopAnimation();
    }

    @Override
    public boolean visible() {
        return !scaleAnimation.isFinished(Direction.BACKWARDS);
    }

    @Override
    public void tick() {
        if (mc.player == null) {
            effectsList = new java.util.ArrayList<>();
            stopAnimation();
            return;
        }

        Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();
        effectsList = new java.util.ArrayList<>(effects.stream()
                .filter(StatusEffectInstance::shouldShowIcon)
                .toList());

        boolean hasActiveEffects = !effectsList.isEmpty();
        boolean inChat = isChat(mc.currentScreen);

        if (hasActiveEffects || inChat) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }

    private float lerp(float current, float target, float deltaTime) {
        float factor = (float) (1.0 - Math.pow(0.001, deltaTime * ANIMATION_SPEED));
        return current + (target - current) * factor;
    }

    private String formatDuration(int ticks) {
        if (ticks == -1) {
            return "∞∞:∞∞";
        }
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String getEffectName(StatusEffectInstance effect) {
        return effect.getEffectType().value().getName().getString();
    }

    private String getLevelText(int amplifier) {
        if (amplifier <= 0) {
            return "";
        }
        return "LVL " + (amplifier + 1);
    }

    private Identifier getEffectTexture(RegistryEntry<StatusEffect> effect) {
        return effect.getKey()
                .map(RegistryKey::getValue)
                .map(id -> id.withPrefixedPath("mob_effect/"))
                .orElse(Identifier.ofVanilla("mob_effect/speed"));
    }

    private int getBlinkAlpha(int duration, int baseAlpha) {
        if (duration == -1 || duration > BLINK_THRESHOLD_TICKS) {
            return baseAlpha;
        }

        long currentTime = System.currentTimeMillis();
        double blinkSpeed = 0.008;
        double blinkWave = Math.sin(currentTime * blinkSpeed);
        float blinkFactor = (float) ((blinkWave + 1.0) / 2.0);

        int minAlpha = Math.max(50, baseAlpha - 150);
        return (int) (minAlpha + (baseAlpha - minAlpha) * (1.0f - blinkFactor));
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) return;

        float alphaFactor = alpha / 255.0f;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;
        deltaTime = Math.min(deltaTime, 0.1f);

        float x = getX();
        float y = getY();

        // Calculate dimensions
        float headerHeight = 15;
        float itemHeight = 11;
        float itemSpacing = 1.5f;
        float padding = 8;

        float targetWidth = 80;
        float targetHeight = headerHeight + 5;

        // Show example effect when in chat and no effects
        boolean showExample = effectsList.isEmpty() && isChat(mc.currentScreen);
        
        if (showExample) {
            String exampleName = "Example Effect";
            float nameWidth = Fonts.FONT67.getWidth(exampleName, 6);
            float timerWidth = Fonts.FONT67.getWidth("05:00", 6);
            targetWidth = Math.max(nameWidth + timerWidth + padding * 2 + 15, targetWidth);
            targetHeight += itemHeight + itemSpacing;
        } else {
            for (StatusEffectInstance effect : effectsList) {
                String name = getEffectName(effect);
                int amplifier = effect.getAmplifier();
                String levelText = getLevelText(amplifier);
                String timer = formatDuration(effect.getDuration());
                
                String displayName = name;
                if (amplifier > 0) {
                    displayName = name + " " + levelText;
                }
                
                float nameWidth = Fonts.FONT67.getWidth(displayName, 6);
                float timerWidth = Fonts.FONT67.getWidth(timer, 6);
                
                targetWidth = Math.max(nameWidth + timerWidth + padding * 2 + 15, targetWidth);
                targetHeight += itemHeight + itemSpacing;
            }
        }

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

        int bgAlpha = (int) (240 * alphaFactor);
        int panelAlpha = HudTheme.backgroundAlpha(bgAlpha);
        int Color67 = new Color(20, 15, 23, panelAlpha).getRGB();
        int Color677 = new Color(29, 23, 33, panelAlpha).getRGB();

        // Background with gradient like keybinds
        Render2D.gradientRect(x, y, animatedWidth, animatedHeight,
                new int[]{
                        Color67,
                        Color67,
                        Color67,
                        Color677
                },
                8);

        // Title
        Fonts.FONT67.draw("Potions", x + padding, y + headerHeight / 2 - 3 - 0.5f + 2, 6,
                HudTheme.text(bgAlpha));

        // Icon box
//        Render2D.rect(x + animatedWidth - padding - 12.3f, y + headerHeight / 2 - 4 + 2 - 2, 13, 13, new Color(0, 0, 0, 255).getRGB(), 3);
//        Render2D.outline(x + animatedWidth - padding - 12.3f, y + headerHeight / 2 - 4 + 2 - 2, 13, 13, 1, new Color(255, 255, 255, 30).getRGB(), 3);
        Fonts.iconsnew.draw("I", x + animatedWidth - padding - 10, y + headerHeight / 2 - 4 + 2 - 3, 12,
                HudTheme.accent(bgAlpha));

        Scissor.enable(x, y, animatedWidth, animatedHeight, 2);

        float currentY = y + headerHeight + 5;

        if (showExample) {
            String name = "Example Effect";
            String timer = "05:00";

            // Effect name
            Fonts.FONT67.draw(name, x + padding, currentY + itemHeight / 2 - 3 - 0.5f, 6,
                    HudTheme.text(bgAlpha));

            // Timer on the right
            float timerWidth = Fonts.FONT67.getWidth(timer, 6);
            Fonts.FONT67.draw(timer, x + animatedWidth - padding - timerWidth - 3, currentY + itemHeight / 2 - 3 - 0.5f, 6,
                    HudTheme.accent(bgAlpha));
        } else {
            for (StatusEffectInstance effect : effectsList) {
            String name = getEffectName(effect);
            int amplifier = effect.getAmplifier();
            String levelText = getLevelText(amplifier);
            String timer = formatDuration(effect.getDuration());
            int duration = effect.getDuration();

            int blinkAlpha = getBlinkAlpha(duration, bgAlpha);

            // Calculate rect width based on effect name and level
            float nameWidth = Fonts.BOLD.getWidth(name, 6);
            float levelWidth = amplifier > 0 ? Fonts.TEST.getWidth(levelText, 5) + 3 : 0;
            float rectWidth = nameWidth + levelWidth + ICON_SIZE + 15;

            // Effect name with level
            String displayName = name;
            if (amplifier > 0) {
                displayName = name + " " + levelText;
            }
            
            Fonts.FONT67.draw(displayName, x + padding, currentY + itemHeight / 2 - 3 - 0.5f, 6,
                    HudTheme.text(blinkAlpha));

            // Timer on the right
            float timerWidth = Fonts.FONT67.getWidth(timer, 6);
            Fonts.FONT67.draw(timer, x + animatedWidth - padding - timerWidth - 3, currentY + itemHeight / 2 - 3 - 0.5f, 6,
                    HudTheme.accent(blinkAlpha));

            currentY += itemHeight + itemSpacing;
            }
        }

        Scissor.disable();
    }
}

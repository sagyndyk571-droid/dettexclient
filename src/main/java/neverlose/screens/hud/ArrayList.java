package neverlose.screens.hud;

import net.minecraft.client.gui.DrawContext;
import neverlose.debob;
import neverlose.client.draggables.AbstractHudElement;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.setting.Setting;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.util.animations.Animation;
import neverlose.util.animations.Decelerate;
import neverlose.util.animations.Direction;
import neverlose.util.render.Render2D;
import neverlose.util.render.font.Fonts;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

public class ArrayList extends AbstractHudElement {
    private final Map<ModuleStructure, ModuleAnimation> moduleAnimations = new LinkedHashMap<>();
    private java.util.List<ModuleStructure> sortedModules = new java.util.ArrayList<>();
    private float animatedWidth = 112;
    private float animatedHeight = 95;
    private long lastUpdateTime = System.currentTimeMillis();

    private static final float ANIMATION_SPEED = 8.0f;
    private static final int MAX_VISIBLE_MODULES = 30;
    private static final float ITEM_HEIGHT = 8.0f;
    private static final float ITEM_SPACING = 0.0f;
    private static final float PADDING_X = 5.0f;
    private static final float FONT_SIZE = 6.0f;
    private static final float LINE_WIDTH = 1.4f;
    private static final float LINE_GAP = 3.0f;

    public ArrayList() {
        super("ArrayList", 10, 10, 112, 95, true);
        stopAnimation();
    }

    @Override
    public boolean visible() {
        return !scaleAnimation.isFinished(Direction.BACKWARDS);
    }

    @Override
    public void tick() {
        if (debob.getInstance() == null ||
                debob.getInstance().getManager() == null ||
                debob.getInstance().getManager().getModuleProvider() == null) {
            return;
        }

        java.util.List<ModuleStructure> activeModules = debob.getInstance().getManager()
                .getModuleProvider()
                .getModuleStructures()
                .stream()
                .filter(ModuleStructure::isState)
                .sorted((m1, m2) -> Float.compare(getModuleWidth(m2), getModuleWidth(m1)))
                .limit(MAX_VISIBLE_MODULES)
                .toList();

        sortedModules = new java.util.ArrayList<>(activeModules);

        for (ModuleStructure module : activeModules) {
            if (!moduleAnimations.containsKey(module)) {
                ModuleAnimation anim = new ModuleAnimation();
                anim.animation.setDirection(Direction.FORWARDS);
                moduleAnimations.put(module, anim);
            }
        }

        moduleAnimations.entrySet().removeIf(entry -> {
            if (!activeModules.contains(entry.getKey())) {
                entry.getValue().animation.setDirection(Direction.BACKWARDS);
                return entry.getValue().animation.isFinished(Direction.BACKWARDS);
            }
            return false;
        });

        if (!sortedModules.isEmpty() || isChat(mc.currentScreen)) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }

    private float lerp(float current, float target, float deltaTime) {
        float factor = (float) (1.0 - Math.pow(0.001, deltaTime * ANIMATION_SPEED));
        return current + (target - current) * factor;
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) {
            return;
        }

        float alphaFactor = alpha / 255.0f;
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastUpdateTime) / 1000.0f, 0.1f);
        lastUpdateTime = currentTime;

        float x = getX();
        float y = getY();
        float maxWidth = 66.0f;
        float totalHeight = 0.0f;
        boolean showExample = sortedModules.isEmpty() && isChat(mc.currentScreen);
        java.util.List<ModuleRenderData> renderData = new java.util.ArrayList<>();

        if (showExample) {
            String[][] examples = {
                    {"Elytra Booster", "ReallyWorld"},
                    {"AutoSwap", "Items"},
                    {"GuiMove", "ReallyWorld"}
            };

            for (String[] example : examples) {
                float itemWidth = getTextWidth(example[0], example[1]) + PADDING_X * 2.0f;
                maxWidth = Math.max(maxWidth, itemWidth);
                renderData.add(new ModuleRenderData(example[0], example[1], itemWidth, 1.0f));
                totalHeight += ITEM_HEIGHT + ITEM_SPACING;
            }
        } else {
            for (ModuleStructure module : sortedModules) {
                ModuleAnimation anim = moduleAnimations.get(module);
                if (anim == null) {
                    continue;
                }

                float animProgress = anim.animation.getOutput().floatValue();
                if (animProgress <= 0.01f) {
                    continue;
                }

                String suffix = getSuffix(module);
                float itemWidth = getTextWidth(module.getName(), suffix) + PADDING_X * 2.0f;
                maxWidth = Math.max(maxWidth, itemWidth);
                renderData.add(new ModuleRenderData(module.getName(), suffix, itemWidth, animProgress));
                totalHeight += (ITEM_HEIGHT + ITEM_SPACING) * animProgress;
            }
        }

        if (totalHeight > 0.0f) {
            totalHeight -= ITEM_SPACING;
        }

        animatedWidth = lerp(animatedWidth, maxWidth, deltaTime);
        animatedHeight = lerp(animatedHeight, totalHeight, deltaTime);

        if (Math.abs(animatedWidth - maxWidth) < 0.3f) {
            animatedWidth = maxWidth;
        }
        if (Math.abs(animatedHeight - totalHeight) < 0.3f) {
            animatedHeight = totalHeight;
        }

        setWidth((int) Math.ceil(animatedWidth + LINE_WIDTH + LINE_GAP));
        setHeight((int) Math.ceil(animatedHeight));

        if (animatedHeight > 1.0f) {
            int lineAlpha = (int) (220 * alphaFactor);
            Render2D.gradientRect(x, y - 0.5f, LINE_WIDTH, animatedHeight + 1.0f,
                    new int[]{
                            shimmer(HudTheme.accent(lineAlpha), 0.0f),
                            shimmer(HudTheme.accent(lineAlpha), 10.0f),
                            shimmer(HudTheme.accent(lineAlpha), animatedHeight + 28.0f),
                            shimmer(HudTheme.accent(lineAlpha), animatedHeight + 18.0f)
                    }, 0.4f);
        }

        float currentY = y;
        for (ModuleRenderData data : renderData) {
            float itemHeight = ITEM_HEIGHT * data.animProgress;
            int itemAlpha = (int) (210 * alphaFactor * data.animProgress);
            float rowX = x + LINE_WIDTH + LINE_GAP;

            int rowAlpha = HudTheme.backgroundAlpha(itemAlpha);
            Render2D.gradientRect(rowX, currentY, data.width, itemHeight + 0.35f,
                    new int[]{
                            new Color(20, 15, 23, rowAlpha).getRGB(),
                            new Color(20, 15, 23, rowAlpha).getRGB(),
                            new Color(29, 23, 33, rowAlpha).getRGB(),
                            new Color(29, 23, 33, rowAlpha).getRGB()
                    }, 0.5f);

            float textY = currentY + itemHeight / 2.0f - 2.85f;
            int nameColor = shimmer(HudTheme.accent(itemAlpha), currentY * 2.0f);
            Fonts.INTER_SEMIBOLD.draw(data.name, rowX + PADDING_X, textY, FONT_SIZE, nameColor);

            if (!data.suffix.isEmpty()) {
                float suffixX = rowX + PADDING_X + Fonts.INTER_SEMIBOLD.getWidth(data.name, FONT_SIZE) + 3.0f;
                Fonts.INTER_SEMIBOLD.draw(data.suffix, suffixX, textY, FONT_SIZE, HudTheme.text(itemAlpha));
            }

            currentY += ITEM_HEIGHT * data.animProgress;
        }
    }

    private int shimmer(int baseColor, float offset) {
        float wave = (float) (Math.sin(System.currentTimeMillis() * 0.0045 + offset * 0.055) * 0.5 + 0.5);
        int target = new Color(235, 240, 255, (baseColor >>> 24) & 0xFF).getRGB();
        return Render2D.interpolateColor(baseColor, target, wave * 0.38f);
    }

    private float getModuleWidth(ModuleStructure module) {
        return getTextWidth(module.getName(), getSuffix(module)) + PADDING_X * 2.0f;
    }

    private float getTextWidth(String name, String suffix) {
        float width = Fonts.INTER_SEMIBOLD.getWidth(name, FONT_SIZE);
        if (suffix != null && !suffix.isEmpty()) {
            width += 3.0f + Fonts.INTER_SEMIBOLD.getWidth(suffix, FONT_SIZE);
        }
        return width;
    }

    private String getSuffix(ModuleStructure module) {
        for (Setting setting : module.getSettings()) {
            if (setting instanceof SelectSetting select && setting.isVisible()) {
                String selected = select.getSelected();
                if (selected != null && !selected.isBlank() && !selected.equalsIgnoreCase("None")) {
                    return selected;
                }
            }
        }
        return "";
    }

    private static class ModuleRenderData {
        String name;
        String suffix;
        float width;
        float animProgress;

        ModuleRenderData(String name, String suffix, float width, float animProgress) {
            this.name = name;
            this.suffix = suffix == null ? "" : suffix;
            this.width = width;
            this.animProgress = animProgress;
        }
    }

    private static class ModuleAnimation {
        Animation animation = new Decelerate().setValue(1.0).setMs(180);
    }
}

package neverlose.screens.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import neverlose.client.draggables.AbstractHudElement;
import neverlose.util.animations.Direction;
import neverlose.util.render.Render2D;
import neverlose.util.render.font.Fonts;
import neverlose.util.render.item.ItemRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Inventory extends AbstractHudElement {

    private static final int SLOT_SIZE = 12;
    private static final int SLOTS_PER_ROW = 9;
    private static final int INVENTORY_ROWS = 3;
    private static final float ITEM_SCALE = 0.5f;
    private static final float FONT_SIZE = 6f;

    private int filledSlots = 0;

    public Inventory() {
        super("Inventory", 20, 60, 120, 60, true);
        stopAnimation();
    }

    @Override
    public boolean visible() {
        return !scaleAnimation.isFinished(Direction.BACKWARDS);
    }

    @Override
    public void tick() {
        if (mc.player == null) {
            filledSlots = 0;
            stopAnimation();
            return;
        }

        filledSlots = 0;
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                filledSlots++;
            }
        }

        boolean hasItems = filledSlots > 0;
        boolean inChat = isChat(mc.currentScreen);

        if (hasItems || inChat) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) return;
        if (mc.player == null) return;

        float alphaFactor = alpha / 255.0f;

        float x = getX();
        float y = getY();

        float headerHeight = 16;
        float padding = 4;
        float slotGap = 1;

        float slotsWidth = SLOTS_PER_ROW * SLOT_SIZE + (SLOTS_PER_ROW - 1) * slotGap;
        float slotsHeight = INVENTORY_ROWS * SLOT_SIZE + (INVENTORY_ROWS - 1) * slotGap;

        float contentWidth = slotsWidth + padding * 2;
        float contentHeight = slotsHeight + padding * 2 + headerHeight;

        setWidth((int) contentWidth);
        setHeight((int) (contentHeight + 4));

        float contentY = y + headerHeight;

        int bgAlpha = (int) (240 * alphaFactor);

        Render2D.gradientRect(x, y, contentWidth, contentHeight,
                new int[]{
                        HudTheme.panelTop(bgAlpha),
                        HudTheme.panelTop(bgAlpha),
                        HudTheme.panelBottom(bgAlpha),
                        HudTheme.panelBottom(bgAlpha)
                },
                8);

        String headerText = "Inventory";
        Fonts.FONT67.draw(headerText, x + padding, y + headerHeight / 2 - 3.5f + 2, 6,
                HudTheme.text(bgAlpha));
        Fonts.iconsnew.draw("F", x + contentWidth - padding - 10, y + headerHeight / 2 - 5, 12,
                HudTheme.accent(bgAlpha));

        float slotsStartX = x + padding;
        float slotsStartY = contentY + padding;

        List<CountLabel> countLabels = new ArrayList<>();

        for (int row = 0; row < INVENTORY_ROWS; row++) {
            for (int col = 0; col < SLOTS_PER_ROW; col++) {
                int slotIndex = 9 + row * SLOTS_PER_ROW + col;

                float slotX = slotsStartX + col * (SLOT_SIZE + slotGap);
                float slotY = slotsStartY + row * (SLOT_SIZE + slotGap);

                ItemStack stack = mc.player.getInventory().getStack(slotIndex);

                Render2D.rect(slotX, slotY, SLOT_SIZE, SLOT_SIZE, HudTheme.itemSoft((int) (185 * alphaFactor)), 3);

                if (!stack.isEmpty()) {
                    float itemSize = 16 * ITEM_SCALE;
                    float itemX = slotX + (SLOT_SIZE - itemSize) / 2;
                    float itemY = slotY + (SLOT_SIZE - itemSize) / 2;

                    if (ItemRender.needsContextRender(stack)) {
                        ItemRender.drawItemWithContext(context, stack, itemX, itemY, ITEM_SCALE, alphaFactor);
                    } else {
                        ItemRender.drawItem(stack, itemX, itemY, ITEM_SCALE, alphaFactor);
                    }

                    int count = stack.getCount();
                    if (count > 1) {
                        countLabels.add(new CountLabel(slotX, slotY, count));
                    }
                }
            }
        }

        int textColor = HudTheme.text(bgAlpha);

        for (CountLabel label : countLabels) {
            String countText = String.valueOf(label.count);
            int textWidth = (int) Fonts.NEVERLOSE.getWidth(countText, FONT_SIZE);
            int textX = (int) (label.slotX + SLOT_SIZE - textWidth);
            int textY = (int) (label.slotY + SLOT_SIZE - Fonts.NEVERLOSE.getHeight(FONT_SIZE) + 1);

            Fonts.NEVERLOSE.draw(countText, textX, textY, FONT_SIZE, textColor);
        }
    }

    @Override
    protected void drawHudBlur(neverlose.modules.impl.render.Hud hud, int alpha) {
        if (hud == null || hud.hudBlur == null || hud.hudBlurRadius == null || !hud.hudBlur.isValue()) {
            return;
        }

        float headerHeight = 16;
        float padding = 4;
        float slotGap = 1;
        float slotsWidth = SLOTS_PER_ROW * SLOT_SIZE + (SLOTS_PER_ROW - 1) * slotGap;
        float slotsHeight = INVENTORY_ROWS * SLOT_SIZE + (INVENTORY_ROWS - 1) * slotGap;
        float contentWidth = slotsWidth + padding * 2;
        float contentHeight = slotsHeight + padding * 2 + headerHeight;
        float blurRadius = Math.max(0.1f, hud.hudBlurRadius.getValue());
        int tint = neverlose.util.ColorUtil.rgba(8, 10, 14, (int) (34 * (alpha / 255.0f)));

        Render2D.blur(
                snapToFixedPixel(getX()),
                snapToFixedPixel(getY()),
                snapToFixedPixel(contentWidth),
                snapToFixedPixel(contentHeight),
                blurRadius,
                8,
                tint
        );
    }

    private record CountLabel(float slotX, float slotY, int count) {}
}

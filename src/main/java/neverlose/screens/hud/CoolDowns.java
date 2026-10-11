package neverlose.screens.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import neverlose.client.draggables.AbstractHudElement;
import neverlose.util.animations.Direction;
import neverlose.util.render.Render2D;
import neverlose.util.render.shader.Scissor;
import neverlose.util.render.font.Fonts;
import neverlose.util.render.item.ItemRender;

import java.awt.*;
import java.util.*;
import java.util.List;

public class CoolDowns extends AbstractHudElement {

    private List<Item> activeCooldowns = new java.util.ArrayList<>();
    private final Map<Item, Long> estimatedRemainingMs = new HashMap<>();

    private float animatedWidth = 80;
    private float animatedHeight = 23;
    private long lastUpdateTime = System.currentTimeMillis();

    private static final float ANIMATION_SPEED = 8.0f;
    private static final float HEADER_RECT_WIDTH = 70; // фиксированная ширина заголовка

    public CoolDowns() {
        super("CoolDowns", 10, 40, 80, 23, true);
        stopAnimation();
    }

    @Override
    public boolean visible() {
        return !scaleAnimation.isFinished(Direction.BACKWARDS);
    }

    @Override
    public void tick() {
        if (mc.player == null) {
            activeCooldowns.clear();
            estimatedRemainingMs.clear();
            stopAnimation();
            return;
        }

        // Собираем все предметы с активным кулдауном
        Set<Item> itemsWithCooldown = new HashSet<>();
        var cooldownManager = mc.player.getItemCooldownManager();

        // Проверяем весь инвентарь
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && cooldownManager.isCoolingDown(stack)) {
                itemsWithCooldown.add(stack.getItem());
            }
        }
        // Проверяем основную руку и оффхенд (они уже могли попасть, но дубликаты не страшны)
        ItemStack mainHand = mc.player.getMainHandStack();
        if (!mainHand.isEmpty() && cooldownManager.isCoolingDown(mainHand)) {
            itemsWithCooldown.add(mainHand.getItem());
        }
        ItemStack offHand = mc.player.getOffHandStack();
        if (!offHand.isEmpty() && cooldownManager.isCoolingDown(offHand)) {
            itemsWithCooldown.add(offHand.getItem());
        }

        // Обновляем список активных кулдаунов и оцениваем оставшееся время
        activeCooldowns = new java.util.ArrayList<>(itemsWithCooldown);
        for (Item item : activeCooldowns) {
            float progress = cooldownManager.getCooldownProgress(item.getDefaultStack(), 0.0f);
            // Грубая оценка: если прогресс уменьшился с прошлого тика, вычисляем общее время
            // Для простоты используем фиксированное обновление раз в секунду
            long now = System.currentTimeMillis();
            Long prevRemaining = estimatedRemainingMs.get(item);
            if (prevRemaining == null || progress <= 0.01f) {
                // Недостаточно данных, пока не оцениваем
                estimatedRemainingMs.put(item, 0L);
            } else {
                // Если уже есть оценка, просто обновляем оставшееся время (прогресс * общее_время)
                // Для упрощения используем прогресс напрямую: оставшиеся секунды = ceil(прогресс * 30) например,
                // но лучше динамически вычислять. Здесь для демонстрации используем простой подход:
                // Считаем, что полный кулдаун не более 30 секунд, или можно получить из менеджера, но стандартный API не даёт.
                // Оставим пока -1
                estimatedRemainingMs.put(item, -1L);
            }
        }

        // Удаляем оценки для предметов, у которых кулдаун кончился
        estimatedRemainingMs.keySet().removeIf(item -> !activeCooldowns.contains(item));

        boolean hasActive = !activeCooldowns.isEmpty();
        boolean inChat = isChat(mc.currentScreen);

        if (hasActive || inChat) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }

    private String formatDuration(int seconds) {
        if (seconds < 0) return "...";
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }

    private int getRemainingSeconds(Item item) {
        if (mc.player == null) return -1;
        var cooldownManager = mc.player.getItemCooldownManager();
        float progress = cooldownManager.getCooldownProgress(item.getDefaultStack(), 0.0f);
        if (progress <= 0) return 0;
        // Приблизительная оценка: считаем, что полный кулдаун стандартный (1 секунда? на самом деле разный)
        // В реальном клиенте нельзя узнать точное оставшееся время, поэтому показываем "..."
        // Для демонстрации сделаем так: если прогресс > 0 и мы не знаем точное время, показываем секунды от 30
        // Лучше использовать прогресс * 30, но это не точно. Можно просто показывать "..." или "~N".
        // Возвращаем -1, что вызовет отображение "..."
        return -1;
    }

    private float lerp(float current, float target, float deltaTime) {
        float factor = (float) (1.0 - Math.pow(0.001, deltaTime * ANIMATION_SPEED));
        return current + (target - current) * factor;
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

        boolean hasActive = !activeCooldowns.isEmpty();
        boolean showExample = !hasActive && isChat(mc.currentScreen);

        float headerHeight = 15;
        float itemHeight = 11;
        float itemSpacing = 1.5f;
        float padding = 8;

        float targetWidth = 80;
        float targetHeight = headerHeight + 5;

        // Ширина для таймера (примерно "MM:SS")
        float timerWidth = Fonts.BOLD.getWidth("00:00", 6);
        float timerBoxWidth = timerWidth + 8; // добавляем отступы

        if (showExample) {
            String exampleName = "Example Item";
            float nameWidth = Fonts.BOLD.getWidth(exampleName, 6);
            targetWidth = Math.max(nameWidth + timerBoxWidth + 25, targetWidth);
            targetHeight += itemHeight + itemSpacing;
        } else {
            for (Item item : activeCooldowns) {
                String name = item.getDefaultStack().getName().getString();
                float nameWidth = Fonts.BOLD.getWidth(name, 6);
                targetWidth = Math.max(nameWidth + timerBoxWidth + 25, targetWidth);
                targetHeight += itemHeight + itemSpacing;
            }
        }

        animatedWidth = lerp(animatedWidth, targetWidth, deltaTime);
        animatedHeight = lerp(animatedHeight, targetHeight, deltaTime);

        if (Math.abs(animatedWidth - targetWidth) < 0.3f) animatedWidth = targetWidth;
        if (Math.abs(animatedHeight - targetHeight) < 0.3f) animatedHeight = targetHeight;

        setWidth((int) Math.ceil(animatedWidth));
        setHeight((int) Math.ceil(animatedHeight));

        int bgAlpha = (int) (240 * alphaFactor);

        // Header с фиксированной шириной (как в Potions)
        Render2D.gradientRect(x, y, animatedWidth, animatedHeight,
                new int[]{
                        HudTheme.panelTop(bgAlpha),
                        HudTheme.panelTop(bgAlpha),
                        HudTheme.panelBottom(bgAlpha),
                        HudTheme.panelBottom(bgAlpha)
                },
                8);

        // Иконка (часы)
        Fonts.iconsnew.draw("m", x + animatedWidth - padding - 10, y + headerHeight / 2 - 5, 12,
                HudTheme.accent(bgAlpha));
        // Текст заголовка
        Fonts.FONT67.draw("Cooldowns", x + padding, y + headerHeight / 2 - 3.5f + 2, 6,
                HudTheme.text(bgAlpha));

        Scissor.enable(x, y, animatedWidth, animatedHeight, 2);

        float startY = y + headerHeight + 5;
        float currentY = startY;

        if (showExample) {
            String name = "Example Item";
            String timer = "0:00";

            float nameWidth = Fonts.BOLD.getWidth(name, 6);
            float rectWidth = nameWidth + 15 + 4; // место под иконку предмета

            // Фон строки
            // Иконка примера (камень)
            ItemStack exampleStack = net.minecraft.item.Items.STONE.getDefaultStack();
            float itemX = x + padding;
            float itemY = currentY + (itemHeight - 9) / 2;
            Render2D.rect(itemX - 2, currentY - 1, itemHeight + 2, itemHeight + 2,
                    HudTheme.itemSoft((int) (150 * alphaFactor)), 3);
            ItemRender.drawItem(exampleStack, itemX, itemY, 0.5f, alphaFactor);

            // Название
            float nameX = itemX + 11;
            Fonts.BOLD.draw(name, nameX, currentY + itemHeight / 2 - 3, 6,
                    HudTheme.text(bgAlpha));

            // Бокс таймера
            float timerBoxX = x + getWidth() - timerBoxWidth - padding + 4;
            Render2D.rect(timerBoxX, currentY, timerBoxWidth, itemHeight,
                    HudTheme.itemSoft((int) (135 * alphaFactor)), 3);

            float timerTextX = timerBoxX + (timerBoxWidth - Fonts.BOLD.getWidth(timer, 6)) / 2;
            Fonts.BOLD.draw(timer, timerTextX, currentY + itemHeight / 2 - 3, 6,
                    HudTheme.muted(bgAlpha));
        } else {
            for (Item item : activeCooldowns) {
                String name = item.getDefaultStack().getName().getString();
                int seconds = getRemainingSeconds(item);
                String timer = seconds >= 0 ? formatDuration(seconds) : "...";

                float nameWidth = Fonts.BOLD.getWidth(name, 6);
                float rectWidth = nameWidth + 15 + 4;

                // Фон строки
                // Иконка предмета
                ItemStack stack = item.getDefaultStack();
                float itemX = x + padding;
                float itemY = currentY + (itemHeight - 9) / 2;
                Render2D.rect(itemX - 2, currentY - 1, itemHeight + 2, itemHeight + 2,
                        HudTheme.itemSoft((int) (150 * alphaFactor)), 3);
                if (ItemRender.needsContextRender(stack)) {
                    ItemRender.drawItemWithContext(context, stack, itemX, itemY, 0.5f, alphaFactor);
                } else {
                    ItemRender.drawItem(stack, itemX, itemY, 0.5f, alphaFactor);
                }

                // Название
                float nameX = itemX + 11;
                Fonts.BOLD.draw(name, nameX, currentY + itemHeight / 2 - 3, 6,
                        HudTheme.text(bgAlpha));

                // Бокс таймера
                float timerBoxX = x + getWidth() - timerBoxWidth - padding + 4;
                Render2D.rect(timerBoxX, currentY, timerBoxWidth, itemHeight,
                        HudTheme.itemSoft((int) (135 * alphaFactor)), 3);

                float timerTextX = timerBoxX + (timerBoxWidth - Fonts.BOLD.getWidth(timer, 6)) / 2;
                Fonts.BOLD.draw(timer, timerTextX, currentY + itemHeight / 2 - 3, 6,
                        HudTheme.muted(bgAlpha));

                currentY += itemHeight + itemSpacing;
            }
        }

        Scissor.disable();
    }
}

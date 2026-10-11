package neverlose.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4d;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.DrawEvent;
import neverlose.events.impl.TickEvent;
import neverlose.events.impl.WorldLoadEvent;
import neverlose.events.impl.WorldRenderEvent;
import neverlose.modules.impl.combat.AntiBot;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.ColorSetting;
import neverlose.modules.module.setting.implement.MultiSelectSetting;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.ColorUtil;
import neverlose.util.Instance;
import neverlose.util.math.Projection;
import neverlose.util.modules.esp.RwPrefix;
import neverlose.util.network.Network;
import neverlose.util.render.Render2D;
import neverlose.util.render.font.Fonts;
import neverlose.util.render.item.ItemRender;
import neverlose.util.render.Render3D;
import neverlose.util.repository.friend.FriendUtils;
import neverlose.util.string.PlayerInteractionHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Esp extends ModuleStructure {

    public static Esp getInstance() {
        return Instance.get(Esp.class);
    }

    Identifier TEXTURE = Identifier.of("neverlose", "textures/features/esp/container.png");
    List<PlayerEntity> players = new ArrayList<>();

    public MultiSelectSetting entityType = new MultiSelectSetting("Тип сущности", "Сущности, которые будут отображаться")
            .value("Player", "Item").selected("Player", "Item");

    MultiSelectSetting playerSetting = new MultiSelectSetting("Настройки игрока", "Настройки для игроков")
            .value("Box", "Health", "Armor", "NameTags", "Hand Items").selected("Box", "Health", "Armor", "NameTags", "Hand Items")
            .visible(() -> entityType.isSelected("Player"));

    public SelectSetting boxType = new SelectSetting("Тип", "Тип")
            .value("Corner", "3D Box").selected("Corner")
            .visible(() -> playerSetting.isSelected("Box"));

    public ColorSetting boxColor = new ColorSetting("Цвет бокса", "Цвет для отображения бокса")
            .value(ColorSetting.DEFAULT_COLOR)
            .visible(() -> playerSetting.isSelected("Box"));

    public ColorSetting friendColor = new ColorSetting("Цвет друга", "Цвет для отображения друзей")
            .value(ColorSetting.DEFAULT_COLOR)
            .visible(() -> playerSetting.isSelected("Box"));

    public BooleanSetting flatBoxOutline = new BooleanSetting("Контур", "Контур для плоских боксов")
            .visible(() -> playerSetting.isSelected("Box") && boxType.isSelected("Corner"));

    public SliderSettings boxAlpha = new SliderSettings("Прозрачность", "Прозрачность бокса")
            .setValue(1.0F).range(0.1F, 1.0F).visible(() -> boxType.isSelected("3D Box"));

    private static final int GRAY_COLOR = 0xFF888888;
    private static final int WHITE_COLOR = 0xFFFFFFFF;

    public Esp() {
        super("Esp", "Esp", ModuleCategory.RENDER);
        settings(entityType, playerSetting, boxType, boxColor, friendColor, flatBoxOutline, boxAlpha);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        players.clear();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        players.clear();
        if (mc.world != null) {
            players.addAll(mc.world.getPlayers());
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (!entityType.isSelected("Player")) return;
        float tickDelta = e.getPartialTicks();

        for (PlayerEntity player : players) {
            if (player == null || (player == mc.player && mc.options.getPerspective().isFirstPerson())) continue;

            double interpX = MathHelper.lerp(tickDelta, player.lastX, player.getX());
            double interpY = MathHelper.lerp(tickDelta, player.lastY, player.getY());
            double interpZ = MathHelper.lerp(tickDelta, player.lastZ, player.getZ());

            boolean friend = FriendUtils.isFriend(player);
            int baseColor = friend ? getFriendColor() : getClientColor();
            int alpha = (int) (boxAlpha.getValue() * 255);
            int fillColor = (baseColor & 0x00FFFFFF) | (alpha << 24);
            int outlineColor = baseColor | 0xFF000000;

            if (boxType.isSelected("3D Box") && playerSetting.isSelected("Box")) {
                Box interpBox = player.getDimensions(player.getPose()).getBoxAt(interpX, interpY, interpZ);
                Render3D.drawBox(interpBox, fillColor, 2, true, true, false);
                Render3D.drawBox(interpBox, outlineColor, 2, true, false, false);
            }
        }
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        DrawContext context = e.getDrawContext();
        float tickDelta = e.getPartialTicks();
        float size = 5.5f;

        if (entityType.isSelected("Player")) {
            for (PlayerEntity player : players) {
                if (player == null || (player == mc.player && mc.options.getPerspective().isFirstPerson())) continue;

                Vector4d vec4d = Projection.getVector4D(player, tickDelta);
                if (Projection.cantSee(vec4d)) continue;

                boolean friend = FriendUtils.isFriend(player);

                if (playerSetting.isSelected("Box") && !boxType.isSelected("3D Box")) {
                    drawBox(friend, vec4d, player);
                }
                if (playerSetting.isSelected("Health")) {
                    drawHealthBar(player, vec4d);
                }
                if (playerSetting.isSelected("Armor")) {
                    drawArmor(context, player, vec4d);
                }
                if (playerSetting.isSelected("Hand Items")) {
                    drawHands(context, player, vec4d, size);
                }
                if (playerSetting.isSelected("NameTags")) {
                    drawPlayerName(context, player, friend, Projection.centerX(vec4d), vec4d.y - 2, size);
                }
            }
        }

        if (entityType.isSelected("Item")) {
            for (Entity entity : PlayerInteractionHelper.streamEntities().toList()) {
                if (entity instanceof ItemEntity item) {
                    Vector4d vec4d = Projection.getVector4D(entity, tickDelta);
                    if (Projection.cantSee(vec4d)) continue;

                    ItemStack stack = item.getStack();
                    ContainerComponent compoundTag = stack.get(DataComponentTypes.CONTAINER);
                    List<ItemStack> list = compoundTag != null ? compoundTag.stream().toList() : List.of();

                    if (!list.isEmpty()) {
                        drawShulkerBox(context, stack, list, vec4d);
                    } else {
                        drawColoredText(context, stack.getName().getString(), Projection.centerX(vec4d), vec4d.y, size, getItemColor(stack));
                    }
                }
            }
        }
    }

    private void drawPlayerName(DrawContext context, PlayerEntity player, boolean friend, double centerX, double startY, float size) {
        StringBuilder extraInfo = new StringBuilder();
        if (friend) extraInfo.append("[F]");
        if (AntiBot.getInstance() != null && AntiBot.getInstance().isState() && AntiBot.getInstance().isBot(player)) {
            extraInfo.append("[BOT]");
        }

        Text displayText = player.getDisplayName();
        String displayName = displayText.getString();
        RwPrefix.ParsedName parsed = RwPrefix.parseDisplayName(displayName);
        String actualName = player.getName().getString();
        String cleanDisplayName = RwPrefix.stripFormatting(displayName);
        int actualNameIndex = cleanDisplayName.indexOf(actualName);

        String sphere = "";
        ItemStack offHand = player.getStackInHand(Hand.OFF_HAND);
        if (offHand.getItem().equals(Items.PLAYER_HEAD) || offHand.getItem().equals(Items.TOTEM_OF_UNDYING)) {
            sphere = getSphere(offHand);
        }

        String prefix;
        String suffix;
        if (actualNameIndex >= 0) {
            prefix = RwPrefix.replaceIcons(cleanDisplayName.substring(0, actualNameIndex));
            suffix = RwPrefix.replaceIcons(cleanDisplayName.substring(actualNameIndex + actualName.length()));
        } else {
            prefix = parsed.prefix;
            suffix = parsed.clan;
        }

        String prefixPart = prefix.isEmpty() ? "" : prefix + " ";
        String namePart = actualName;
        String clanPart = suffix.isEmpty() ? "" : " " + suffix;

        float extraWidth = extraInfo.isEmpty() ? 0 : Fonts.BOLD.getWidth(extraInfo.toString(), size);
        float prefixWidth = prefixPart.isEmpty() ? 0 : Fonts.BOLD.getWidth(prefixPart, size);
        float nameWidth = Fonts.BOLD.getWidth(namePart, size);
        float clanWidth = clanPart.isEmpty() ? 0 : Fonts.BOLD.getWidth(clanPart, size);
        float sphereWidth = sphere.isEmpty() ? 0 : Fonts.BOLD.getWidth(sphere, size);

        float totalWidth = extraWidth + prefixWidth + nameWidth + clanWidth + sphereWidth;
        float height = Fonts.BOLD.getHeight(size);
        float posX = (float) centerX - totalWidth / 2;
        float posY = (float) startY - height;

        Render2D.rect(posX - 4, posY - 1.25f, totalWidth + 8, height + 2, 0x80000000, 2f);

        float drawX = posX;

        if (!extraInfo.isEmpty()) {
            Fonts.BOLD.draw(extraInfo.toString(), drawX, posY, size, friend ? getFriendColor() : 0xFFFF5555);
            drawX += extraWidth;
        }

        if (!prefixPart.isEmpty()) {
            Fonts.BOLD.draw(prefixPart, drawX, posY, size, getPrefixColor(displayText, prefixPart, actualName));
            drawX += prefixWidth;
        }

        Fonts.BOLD.draw(namePart, drawX, posY, size, friend ? getFriendColor() : WHITE_COLOR);
        drawX += nameWidth;

        if (!clanPart.isEmpty()) {
            Fonts.BOLD.draw(clanPart, drawX, posY, size, GRAY_COLOR);
            drawX += clanWidth;
        }

        if (!sphere.isEmpty()) {
            int sphereColor = getItemColor(offHand);
            Fonts.BOLD.draw(sphere, drawX, posY, size, sphereColor);
        }
    }

    /**
     * Извлекает цвет из строки с цветовыми кодами (параграфами)
     */
    private int getPrefixColor(Text displayText, String prefixText, String playerName) {
        int rankColor = getKnownRankColor(prefixText);
        if (rankColor != -1) {
            return rankColor;
        }

        int nameStart = Math.max(0, displayText.getString().indexOf(playerName));
        int styledColor = getStyledColorBefore(displayText, nameStart);
        if (styledColor != -1 && !isNeutralColor(styledColor)) {
            return styledColor;
        }

        int formattingColor = getFormattingColorBefore(displayText.getString(), nameStart);
        if (formattingColor != -1 && !isNeutralColor(formattingColor)) {
            return formattingColor;
        }

        return 0xFFFFAA00;
    }

    private int getStyledColorBefore(Text text, int endIndex) {
        final int[] cursor = {0};
        final int[] color = {-1};
        final int[] score = {-1};

        text.visit((Style style, String string) -> {
            int start = cursor[0];
            int end = start + string.length();
            int visibleEnd = Math.max(0, Math.min(string.length(), endIndex - start));
            boolean hasVisiblePrefixText = visibleEnd > 0 && !string.substring(0, visibleEnd).trim().isEmpty();
            if (start < endIndex && hasVisiblePrefixText && style.getColor() != null) {
                int rgb = style.getColor().getRgb();
                int currentScore = getColorfulness(rgb);
                if (currentScore > score[0]) {
                    color[0] = rgb | 0xFF000000;
                    score[0] = currentScore;
                }
            }
            cursor[0] = end;
            return Optional.empty();
        }, Style.EMPTY);

        return color[0];
    }

    private int getColorfulness(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        return (max - min) * 3 + max;
    }

    private boolean isNeutralColor(int color) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        return max - min < 18;
    }

    private int getFormattingColorBefore(String text, int endIndex) {
        int result = -1;
        int limit = Math.min(Math.max(endIndex, 0), text.length());

        for (int i = 0; i < limit - 1; i++) {
            if (text.charAt(i) != '\u00A7') {
                continue;
            }

            if (text.charAt(i + 1) == '#' && i + 7 < limit) {
                try {
                    result = Integer.parseInt(text.substring(i + 2, i + 8), 16) | 0xFF000000;
                    i += 7;
                    continue;
                } catch (NumberFormatException ignored) {
                }
            }

            if ((text.charAt(i + 1) == 'x' || text.charAt(i + 1) == 'X') && i + 13 < limit) {
                StringBuilder hex = new StringBuilder(6);
                for (int j = 0; j < 6; j++) {
                    int index = i + 3 + j * 2;
                    if (index < limit) {
                        hex.append(text.charAt(index));
                    }
                }
                try {
                    result = Integer.parseInt(hex.toString(), 16) | 0xFF000000;
                    i += 13;
                    continue;
                } catch (NumberFormatException ignored) {
                }
            }

            Formatting formatting = Formatting.byCode(Character.toLowerCase(text.charAt(i + 1)));
            if (formatting != null && formatting.getColorValue() != null) {
                result = formatting.getColorValue() | 0xFF000000;
            }
        }

        return result;
    }

    private int getKnownRankColor(String prefixText) {
        String prefix = RwPrefix.stripFormatting(prefixText).toUpperCase();
        if (containsRank(prefix, "IMPERATOR", "ИМПЕРАТОР")) return 0xFFFF4D5A;
        if (containsRank(prefix, "TITAN", "ТИТАН")) return 0xFFFFD84D;
        if (containsRank(prefix, "PLAYER", "ИГРОК")) return 0xFF9AA1AA;
        if (containsRank(prefix, "DRAGON", "ДРАГОН")) return 0xFFB060FF;
        if (containsRank(prefix, "MODER", "МОДЕР")) return 0xFF55D8FF;
        if (containsRank(prefix, "ADMIN", "АДМИН")) return 0xFFFF4D5A;
        if (containsRank(prefix, "OVERLORD", "ОВЕРЛОРД")) return 0xFF55D8FF;
        if (containsRank(prefix, "MAGISTER", "МАГИСТР")) return 0xFFFFA640;
        if (containsRank(prefix, "HERO", "ГЕРОЙ")) return 0xFF4D7DFF;
        if (containsRank(prefix, "AVENGER", "АВЕНДЖЕР")) return 0xFF55D8FF;
        return -1;
    }

    private boolean containsRank(String prefix, String... names) {
        for (String name : names) {
            if (prefix.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private void drawHealthBar(PlayerEntity player, Vector4d vec) {
        float x = (float) vec.x - 5.0f;
        float y = (float) vec.y;
        float height = (float) (vec.w - vec.y);
        if (height < 5.0f) {
            return;
        }

        float maxHealth = Math.max(1.0f, player.getMaxHealth());
        float health = MathHelper.clamp(player.getHealth() + player.getAbsorptionAmount(), 0.0f, maxHealth);
        float percent = MathHelper.clamp(health / maxHealth, 0.0f, 1.0f);

        float width = 2.2f;
        Render2D.rect(x, y, width, height, 0x9604090E, 1.6f);
        Render2D.outline(x, y, width, height, 0.35f, 0xA0202933, 1.6f);

        float fillHeight = Math.max(1.0f, height * percent);
        float fillY = y + height - fillHeight;
        int bottomColor = getHealthColor(Math.max(0.0f, percent - 0.25f));
        int topColor = getHealthColor(Math.min(1.0f, percent + 0.28f));
        int[] colors = {topColor, topColor, bottomColor, bottomColor};
        Render2D.gradientRect(x + 0.7f, fillY + 0.7f, width - 1.4f, Math.max(0.9f, fillHeight - 1.4f), colors, 1.0f);
    }

    private int getHealthColor(float percent) {
        int low = 0xFFFF4B5F;
        int mid = 0xFFFFD166;
        int high = 0xFF55F08A;
        return percent < 0.5f
                ? ColorUtil.interpolateColor(low, mid, percent / 0.5f)
                : ColorUtil.interpolateColor(mid, high, (percent - 0.5f) / 0.5f);
    }

    private void drawHands(DrawContext context, PlayerEntity player, Vector4d vec, float size) {
        double posY = vec.w;
        ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);
        ItemStack offHand = player.getStackInHand(Hand.OFF_HAND);

        ItemStack[] hands = {mainHand, offHand};

        for (ItemStack stack : hands) {
            if (stack.isEmpty()) continue;

            int itemColor = getItemColor(stack);
            String text = stack.getName().getString();
            posY += Fonts.BOLD.getHeight(size) + 2;
            drawColoredText(context, text, Projection.centerX(vec), posY, size, itemColor);
        }
    }

    private int getItemColor(ItemStack stack) {
        if (stack.isEmpty()) return WHITE_COLOR;
        Text name = stack.getName();

        if (name.getStyle().getColor() != null) {
            return name.getStyle().getColor().getRgb() | 0xFF000000;
        }

        for (Text sibling : name.getSiblings()) {
            if (sibling.getStyle().getColor() != null) {
                return sibling.getStyle().getColor().getRgb() | 0xFF000000;
            }
        }

        String raw = name.getString();
        if (raw.contains("§")) {
            for (int i = 0; i < raw.length() - 1; i++) {
                if (raw.charAt(i) == '§') {
                    Formatting formatting = Formatting.byCode(raw.toLowerCase().charAt(i + 1));
                    if (formatting != null && formatting.getColorValue() != null) {
                        return formatting.getColorValue() | 0xFF000000;
                    }
                }
            }
        }

        Integer rarityColor = stack.getRarity().getFormatting().getColorValue();
        if (rarityColor != null) {
            return rarityColor | 0xFF000000;
        }

        return WHITE_COLOR;
    }

    private void drawColoredText(DrawContext context, String text, double startX, double startY, float size, int color) {
        String cleanText = RwPrefix.stripFormatting(text);
        float width = Fonts.BOLD.getWidth(cleanText, size);
        float height = Fonts.BOLD.getHeight(size);
        float posX = (float) (startX - width / 2);
        float posY = (float) startY - height;

        Render2D.rect(posX - 4, posY - 1, width + 8, height + 2, 0x80000000, 2f);
        Fonts.BOLD.draw(cleanText, posX, posY, size, color);
    }

    private void drawBox(boolean friend, Vector4d vec, PlayerEntity player) {
        int client = friend ? getFriendColor() : getClientColor();
        float posX = (float) vec.x; float posY = (float) vec.y;
        float endPosX = (float) vec.z; float endPosY = (float) vec.w;
        float size = (endPosX - posX) / 4;

        if (boxType.isSelected("Corner")) {
            int top = ColorUtil.interpolateColor(client | 0xFF000000, 0xFFFFFFFF, 0.28f);
            int bottom = ColorUtil.interpolateColor(client | 0xFF000000, 0xFF05070C, 0.12f);
            int soft = ColorUtil.multAlpha(client | 0xFF000000, 0.18f);
            drawCornerSegment(posX - 1.2f, posY - 1.2f, size + 0.9f, 1.8f, soft, soft);
            drawCornerSegment(posX - 1.2f, posY - 0.4f, 1.8f, size + 1.0f, soft, soft);
            drawCornerSegment(endPosX - size + 0.3f, posY - 1.2f, size + 0.9f, 1.8f, soft, soft);
            drawCornerSegment(endPosX + 0.2f, posY - 0.4f, 1.8f, size + 1.0f, soft, soft);
            drawCornerSegment(posX - 0.7F, posY - 0.7F, size, 0.8F, top, client);
            drawCornerSegment(posX - 0.7F, posY, 0.8F, size + 0.7F, top, client);
            drawCornerSegment(posX - 0.7F, endPosY - size - 0.7F, 0.8F, size, client, bottom);
            drawCornerSegment(posX - 0.7F, endPosY - 0.7F, size, 0.8F, client, bottom);
            drawCornerSegment(endPosX - size + 1, posY - 0.7F, size, 0.8F, top, client);
            drawCornerSegment(endPosX + 0.7F, posY, 0.8F, size + 0.7F, top, client);
            drawCornerSegment(endPosX + 0.7F, endPosY - size - 0.7F, 0.8F, size, client, bottom);
            drawCornerSegment(endPosX - size + 1, endPosY - 0.7F, size, 0.8F, client, bottom);
        }
    }

    private void drawCornerSegment(float x, float y, float width, float height, int topColor, int bottomColor) {
        Render2D.gradientRect(x, y, width, height,
                new int[]{topColor, topColor, bottomColor, bottomColor}, 0.45f);
    }

    private void drawArmor(DrawContext context, PlayerEntity player, Vector4d vec) {
        List<ItemStack> items = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmorSlot()) {
                ItemStack stack = player.getEquippedStack(slot);
                if (!stack.isEmpty()) items.add(stack);
            }
        }
        if (items.isEmpty()) {
            return;
        }

        float scale = 0.5f;
        float iconSize = 16f * scale;
        float slotSize = 10.5f;
        float slotGap = 1.8f;
        float totalW = items.size() * slotSize + (items.size() - 1) * slotGap;
        float posX = (float) (Projection.centerX(vec) - totalW / 2f);
        float posY = (float) (vec.y - 21.5f);
        float offset = 0f;

        for (ItemStack stack : items) {
            float slotX = posX + offset;
            Render2D.rect(slotX, posY, slotSize, slotSize, new java.awt.Color(7, 11, 16, 178).getRGB(), 3f);
            Render2D.outline(slotX, posY, slotSize, slotSize, 0.45f, new java.awt.Color(45, 58, 70, 150).getRGB(), 3f);
            Render2D.rect(slotX + 1f, posY + 1f, slotSize - 2f, slotSize - 2f, new java.awt.Color(17, 24, 33, 115).getRGB(), 2.4f);
            ItemRender.drawItemWithContext(context, stack,
                    slotX + (slotSize - iconSize) / 2f,
                    posY + (slotSize - iconSize) / 2f,
                    scale, 1.0F);
            offset += slotSize + slotGap;
        }
    }

    private void drawShulkerBox(DrawContext context, ItemStack itemStack, List<ItemStack> stacks, Vector4d vec) {
        int width = 176; int height = 67;
        int color = ((BlockItem) itemStack.getItem()).getBlock().getDefaultMapColor().color | 0xFF000000;
        float scale = 0.5F;
        float drawX = (float) Projection.centerX(vec) - (width * scale) / 2;
        float drawY = (float) vec.y - (height * scale) - 2;
        Render2D.texture(TEXTURE, drawX, drawY, width * scale, height * scale, color);
        int col = 0; int row = 0;
        for (ItemStack stack : stacks) {
            ItemRender.drawItemWithContext(context, stack, drawX + (7 + col * 18) * scale, drawY + (6 + row * 18) * scale, scale, 1F);
            if (++col >= 9) { col = 0; row++; }
        }
    }

    private String getSphere(ItemStack stack) {
        var component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (Network.isFunTime() && component != null) {
            NbtCompound compound = component.copyNbt();
            String donItem = compound.getString("don-item").orElse("");

            if (!donItem.isEmpty()) {
                return " [" + donItem.replace("sphere-", "").toUpperCase() + "]";
            }
        }
        return "";
    }

    private int getFriendColor() { return friendColor.getColorNoAlpha(); }
    private int getClientColor() { return boxColor.getColorNoAlpha(); }
}

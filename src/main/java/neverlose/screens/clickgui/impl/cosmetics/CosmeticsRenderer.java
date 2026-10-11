package neverlose.screens.clickgui.impl.cosmetics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import neverlose.cosmetics.CosmeticManager;
import neverlose.cosmetics.CosmeticModel;
import neverlose.cosmetics.CosmeticRepository;
import neverlose.cosmetics.CosmeticType;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.screens.clickgui.impl.background.BackgroundComponent;
import neverlose.screens.clickgui.impl.background.render.CategoryRenderer;
import neverlose.util.render.Render2D;
import neverlose.util.render.font.Fonts;
import neverlose.util.render.shader.Scissor;

import java.awt.Color;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CosmeticsRenderer {
    private static final float PANEL_X_OFFSET = BackgroundComponent.SIDEBAR_WIDTH + 8f;
    private static final float PANEL_Y_OFFSET = 36f;
    private static final float PANEL_WIDTH = BackgroundComponent.CONTENT_WIDTH - 16f;
    private static final float PANEL_HEIGHT = BackgroundComponent.BG_HEIGHT - 46f;
    private static final float HEADER_HEIGHT = 50f;
    private static final float CARD_GAP = 6f;
    private static final float CARD_HEIGHT = 54f;
    private static final int COLUMNS = 2;
    private static final float TAB_GAP = 4f;

    private final MinecraftClient client = MinecraftClient.getInstance();
    private CosmeticType selectedType = CosmeticType.MODEL;
    private float scroll;
    private float animatedScroll;
    private boolean wasActive;
    private final Map<Identifier, TextureSize> textureSizes = new HashMap<>();

    public void render(DrawContext context, float bgX, float bgY, float mouseX, float mouseY,
                       float delta, int guiScale, float alpha, ModuleCategory category) {
        boolean active = category == ModuleCategory.COSMETICS;
        if (active && !wasActive) {
            CosmeticManager.getInstance().ensureInitialized();
            scroll = 0f;
            animatedScroll = 0f;
        }
        wasActive = active;
        if (!active) return;

        CosmeticManager manager = CosmeticManager.getInstance();
        float panelX = bgX + PANEL_X_OFFSET;
        float panelY = bgY + PANEL_Y_OFFSET;
        int fullAlpha = (int) (255 * alpha);
        Color accent = CategoryRenderer.getClientColor();

        Render2D.gradientRect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, new int[]{
                new Color(11, 18, 23, (int) (244 * alpha)).getRGB(),
                new Color(9, 15, 20, (int) (244 * alpha)).getRGB(),
                new Color(7, 12, 15, (int) (244 * alpha)).getRGB(),
                new Color(7, 12, 15, (int) (244 * alpha)).getRGB()
        }, 8f);
        Render2D.outline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0.55f,
                new Color(45, 62, 75, (int) (135 * alpha)).getRGB(), 8f);

        Fonts.INTER_SEMIBOLD.draw("Cosmetics", panelX + 12f, panelY + 7.3f, 6.4f,
                new Color(235, 239, 247, fullAlpha).getRGB());
        int count = CosmeticRepository.getInstance().all().size();
        Fonts.INTER_MEDIUM.draw(count + " installed items", panelX + 12f, panelY + 18.2f, 4.35f,
                new Color(119, 128, 143, (int) (220 * alpha)).getRGB());

        renderActionButton(panelX + PANEL_WIDTH - 121f, panelY + 7f, 43f, "Folder", mouseX, mouseY, alpha);
        renderActionButton(panelX + PANEL_WIDTH - 73f, panelY + 7f, 43f, "Reload", mouseX, mouseY, alpha);
        renderToggle(panelX + PANEL_WIDTH - 25f, panelY + 9f, manager.isEnabled(), accent, alpha);

        float tabsY = panelY + 29.5f;
        float tabX = panelX + 10f;
        float tabW = (PANEL_WIDTH - 20f - TAB_GAP * 2f) / 3f;
        for (CosmeticType type : CosmeticType.values()) {
            boolean selected = type == selectedType;
            boolean hovered = inside(mouseX, mouseY, tabX, tabsY, tabW, 15f);
            Render2D.rect(tabX, tabsY, tabW, 15f,
                    selected
                            ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (45 * alpha)).getRGB()
                            : new Color(13, 20, 26, (int) ((hovered ? 218 : 156) * alpha)).getRGB(), 4f);
            Render2D.outline(tabX, tabsY, tabW, 15f, 0.45f,
                    selected
                            ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (145 * alpha)).getRGB()
                            : new Color(42, 56, 69, (int) (85 * alpha)).getRGB(), 4f);
            Fonts.INTER_MEDIUM.drawCentered(type.getDisplayName(), tabX + tabW / 2f, tabsY + 5f, 4.7f,
                    selected ? new Color(230, 235, 245, fullAlpha).getRGB()
                            : new Color(126, 135, 150, (int) (220 * alpha)).getRGB());
            tabX += tabW + TAB_GAP;
        }

        Render2D.rect(panelX + 10f, panelY + HEADER_HEIGHT, PANEL_WIDTH - 20f, 0.5f,
                new Color(42, 55, 67, (int) (125 * alpha)).getRGB(), 0f);

        animatedScroll += (scroll - animatedScroll) * Math.min(1f, Math.max(0.12f, delta * 0.35f));
        renderCards(context, panelX, panelY, mouseX, mouseY, guiScale, alpha, manager, accent);
    }

    private void renderCards(DrawContext context, float panelX, float panelY, float mouseX, float mouseY,
                             int guiScale, float alpha, CosmeticManager manager, Color accent) {
        List<CosmeticModel> models = CosmeticRepository.getInstance().byType(selectedType);
        float areaX = panelX + 10f;
        float areaY = panelY + HEADER_HEIGHT + 6f;
        float areaW = PANEL_WIDTH - 20f;
        float areaH = PANEL_HEIGHT - HEADER_HEIGHT - 12f;
        float cardW = (areaW - CARD_GAP * (COLUMNS - 1)) / COLUMNS;

        Scissor.enable(areaX, areaY, areaW, areaH, guiScale);
        if (models.isEmpty()) {
            Fonts.INTER_MEDIUM.drawCentered("No cosmetics found", areaX + areaW / 2f, areaY + areaH / 2f - 4f,
                    5.4f, new Color(126, 135, 150, (int) (220 * alpha)).getRGB());
        }

        CosmeticModel equipped = manager.getActive(selectedType);
        for (int i = 0; i < models.size(); i++) {
            int column = i % COLUMNS;
            int row = i / COLUMNS;
            float x = areaX + column * (cardW + CARD_GAP);
            float y = areaY + row * (CARD_HEIGHT + CARD_GAP) - animatedScroll;
            if (y + CARD_HEIGHT < areaY || y > areaY + areaH) continue;

            CosmeticModel model = models.get(i);
            boolean selected = equipped != null && equipped.getId().equals(model.getId());
            boolean hovered = inside(mouseX, mouseY, x, y, cardW, CARD_HEIGHT)
                    && inside(mouseX, mouseY, areaX, areaY, areaW, areaH);
            boolean faulted = model.isRenderFaulted();
            int bg = selected
                    ? new Color(25, 34, 45, (int) (245 * alpha)).getRGB()
                    : hovered
                    ? new Color(18, 27, 35, (int) (242 * alpha)).getRGB()
                    : new Color(13, 21, 28, (int) (232 * alpha)).getRGB();
            if (faulted) {
                bg = new Color(27, 18, 21, (int) (236 * alpha)).getRGB();
            }
            Render2D.rect(x, y, cardW, CARD_HEIGHT, bg, 6f);
            Render2D.outline(x, y, cardW, CARD_HEIGHT, 0.5f,
                    selected
                            ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (175 * alpha)).getRGB()
                            : faulted
                            ? new Color(198, 78, 92, (int) (115 * alpha)).getRGB()
                            : new Color(42, 56, 69, (int) ((hovered ? 145 : 92) * alpha)).getRGB(), 6f);
            if (selected) {
                Render2D.rect(x + 1.4f, y + 6f, 1.6f, CARD_HEIGHT - 12f,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (235 * alpha)).getRGB(), 1f);
            }

            Color modelAccent = parseColor(model.getAccentColor(), accent);
            float previewSize = 42f;
            Render2D.rect(x + 6f, y + 6f, previewSize, previewSize,
                    new Color(modelAccent.getRed(), modelAccent.getGreen(), modelAccent.getBlue(), (int) (28 * alpha)).getRGB(), 4f);
            renderThumbnail(context, model, x + 6f, y + 6f, previewSize, previewSize, alpha, modelAccent);

            float textX = x + 56f;
            float textW = cardW - 64f;
            Fonts.INTER_SEMIBOLD.draw(trim(model.getName(), textW, 5.2f), textX, y + 8.5f, 5.2f,
                    new Color(232, 236, 244, (int) (255 * alpha)).getRGB());
            String source = model.getSource() == null ? "Built-in" : model.getSource();
            Fonts.INTER_MEDIUM.draw(trim(source, textW, 4f), textX, y + 19.8f, 4f,
                    new Color(110, 120, 135, (int) (225 * alpha)).getRGB());

            if (faulted) {
                String reason = model.getRenderFailure() == null ? "Render disabled" : model.getRenderFailure();
                Fonts.INTER_MEDIUM.draw(trim(reason, textW, 3.8f), textX, y + 35.5f, 3.8f,
                        new Color(240, 112, 126, (int) (235 * alpha)).getRGB());
            } else if (selected) {
                float badgeW = Math.min(49f, textW);
                Render2D.rect(textX, y + 34.5f, badgeW, 11.5f,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (48 * alpha)).getRGB(), 4f);
                Fonts.INTER_MEDIUM.drawCentered("Equipped", textX + badgeW / 2f, y + 38.2f, 4f,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (250 * alpha)).getRGB());
            } else {
                Fonts.INTER_MEDIUM.draw("Click to equip", textX, y + 37f, 3.8f,
                        new Color(91, 101, 115, (int) (190 * alpha)).getRGB());
            }
        }
        Scissor.disable();
    }

    private void renderThumbnail(DrawContext context, CosmeticModel model, float x, float y, float w, float h,
                                 float alpha, Color fallback) {
        if (model.getExternalDirectory() == null) {
            Identifier id = Identifier.of("neverlose", "cosmetic/" + model.getFolderName() + "/avatar.png");
            if (client.getResourceManager().getResource(id).isPresent()) {
                int color = ((int) (255 * alpha) << 24) | 0x00FFFFFF;
                TextureSize size = textureSizes.computeIfAbsent(id, this::readTextureSize);
                float aspect = size.width() / (float) size.height();
                float drawW = aspect >= 1f ? w : h * aspect;
                float drawH = aspect >= 1f ? w / aspect : h;
                float drawX = x + (w - drawW) / 2f;
                float drawY = y + (h - drawH) / 2f;
                context.drawTexture(RenderPipelines.GUI_TEXTURED, id, Math.round(drawX), Math.round(drawY), 0f, 0f,
                        Math.round(drawW), Math.round(drawH), size.width(), size.height(), size.width(), size.height(), color);
                return;
            }
        }
        String initial = model.getName().isBlank() ? "?" : model.getName().substring(0, 1).toUpperCase();
        Fonts.INTER_SEMIBOLD.drawCentered(initial, x + w / 2f, y + 11f, 12f,
                new Color(fallback.getRed(), fallback.getGreen(), fallback.getBlue(), (int) (230 * alpha)).getRGB());
    }

    private void renderActionButton(float x, float y, float w, String text, float mouseX, float mouseY, float alpha) {
        boolean hovered = inside(mouseX, mouseY, x, y, w, 13f);
        Render2D.rect(x, y, w, 13f,
                new Color(14, 22, 29, (int) ((hovered ? 242 : 210) * alpha)).getRGB(), 4f);
        Render2D.outline(x, y, w, 13f, 0.5f,
                new Color(45, 60, 74, (int) ((hovered ? 155 : 105) * alpha)).getRGB(), 4f);
        Fonts.INTER_MEDIUM.drawCentered(text, x + w / 2f, y + 4f, 4.1f,
                new Color(174, 182, 195, (int) (240 * alpha)).getRGB());
    }

    private void renderToggle(float x, float y, boolean enabled, Color accent, float alpha) {
        float w = 19f;
        float h = 9f;
        Render2D.rect(x, y, w, h, enabled
                ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (240 * alpha)).getRGB()
                : new Color(31, 42, 52, (int) (230 * alpha)).getRGB(), h / 2f);
        float knob = 7f;
        Render2D.rect(x + (enabled ? w - knob - 1f : 1f), y + 1f, knob, knob,
                new Color(enabled ? 245 : 142, enabled ? 248 : 151, enabled ? 252 : 163, (int) (255 * alpha)).getRGB(), knob / 2f);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, float bgX, float bgY,
                                ModuleCategory category) {
        if (category != ModuleCategory.COSMETICS || button != 0) return false;
        float panelX = bgX + PANEL_X_OFFSET;
        float panelY = bgY + PANEL_Y_OFFSET;
        if (!inside(mouseX, mouseY, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT)) return false;
        CosmeticManager manager = CosmeticManager.getInstance();

        if (inside(mouseX, mouseY, panelX + PANEL_WIDTH - 25f, panelY + 7f, 22f, 14f)) {
            manager.setEnabled(!manager.isEnabled());
            return true;
        }
        if (inside(mouseX, mouseY, panelX + PANEL_WIDTH - 121f, panelY + 7f, 43f, 13f)) {
            CosmeticRepository.openCosmeticsFolder();
            return true;
        }
        if (inside(mouseX, mouseY, panelX + PANEL_WIDTH - 73f, panelY + 7f, 43f, 13f)) {
            manager.init();
            return true;
        }

        float tabX = panelX + 10f;
        float tabsY = panelY + 29f;
        float tabW = (PANEL_WIDTH - 20f - TAB_GAP * 2f) / 3f;
        for (CosmeticType type : CosmeticType.values()) {
            if (inside(mouseX, mouseY, tabX, tabsY, tabW, 15f)) {
                selectedType = type;
                scroll = animatedScroll = 0f;
                return true;
            }
            tabX += tabW + TAB_GAP;
        }

        float areaX = panelX + 10f;
        float areaY = panelY + HEADER_HEIGHT + 6f;
        float areaW = PANEL_WIDTH - 20f;
        float areaH = PANEL_HEIGHT - HEADER_HEIGHT - 12f;
        if (!inside(mouseX, mouseY, areaX, areaY, areaW, areaH)) return false;

        List<CosmeticModel> models = CosmeticRepository.getInstance().byType(selectedType);
        float cardW = (areaW - CARD_GAP * (COLUMNS - 1)) / COLUMNS;
        for (int i = 0; i < models.size(); i++) {
            float x = areaX + (i % COLUMNS) * (cardW + CARD_GAP);
            float y = areaY + (i / COLUMNS) * (CARD_HEIGHT + CARD_GAP) - animatedScroll;
            if (!inside(mouseX, mouseY, x, y, cardW, CARD_HEIGHT)) continue;
            CosmeticModel model = models.get(i);
            CosmeticModel equipped = manager.getActive(selectedType);
            if (equipped != null && equipped.getId().equals(model.getId())) {
                manager.clear(selectedType);
            } else {
                manager.setEnabled(true);
                manager.setActive(selectedType, model);
            }
            return true;
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double vertical, float bgX, float bgY,
                                 ModuleCategory category) {
        if (category != ModuleCategory.COSMETICS) return false;
        float panelX = bgX + PANEL_X_OFFSET;
        float panelY = bgY + PANEL_Y_OFFSET;
        float areaY = panelY + HEADER_HEIGHT + 6f;
        float areaH = PANEL_HEIGHT - HEADER_HEIGHT - 12f;
        if (!inside(mouseX, mouseY, panelX + 10f, areaY, PANEL_WIDTH - 20f, areaH)) return false;

        int rows = (int) Math.ceil(CosmeticRepository.getInstance().byType(selectedType).size() / (double) COLUMNS);
        float contentHeight = rows * (CARD_HEIGHT + CARD_GAP) - CARD_GAP;
        float maxScroll = Math.max(0f, contentHeight - areaH);
        scroll = Math.max(0f, Math.min(maxScroll, scroll - (float) vertical * 22f));
        return true;
    }

    private static boolean inside(double mouseX, double mouseY, float x, float y, float w, float h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private static Color parseColor(String value, Color fallback) {
        try {
            return Color.decode(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String trim(String text, float maxWidth, float size) {
        if (text == null) return "";
        if (Fonts.INTER_SEMIBOLD.getWidth(text, size) <= maxWidth) return text;
        String value = text;
        while (value.length() > 1 && Fonts.INTER_SEMIBOLD.getWidth(value + "..", size) > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        return value + "..";
    }

    private TextureSize readTextureSize(Identifier id) {
        try (InputStream stream = client.getResourceManager().open(id);
             NativeImage image = NativeImage.read(stream)) {
            return new TextureSize(Math.max(1, image.getWidth()), Math.max(1, image.getHeight()));
        } catch (Exception ignored) {
            return new TextureSize(1, 1);
        }
    }

    private record TextureSize(int width, int height) {
    }
}

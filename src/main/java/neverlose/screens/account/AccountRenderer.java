package neverlose.screens.account;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import neverlose.screens.menu.MainMenuScreen;
import neverlose.util.render.Render2D;
import neverlose.util.render.font.Fonts;
import neverlose.util.render.shader.Scissor;

import java.util.List;

public class AccountRenderer {

    private static final float BLUR_RADIUS = 15f;
    private static final float OUTLINE_THICKNESS = 1f;


    public String handleKeyPress(int keyCode, String currentText, boolean focused) {
        if (!focused) return currentText;

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!currentText.isEmpty()) {
                return currentText.substring(0, currentText.length() - 1);
            }
        }
        return currentText;
    }

    public Object[] handleMouseClick(float mx, float my, float x, float y, float width, float height,
                                     List<AccountEntry> accounts, float scrollOffset) {

        float listX = x + 5;
        float listY = y + 28;
        float listW = width - 10;
        float listH = height - 31;
        float cardW = (listW - 5) / 2f;
        float cardH = 40;
        float gap = 5;

        for (int i = 0; i < accounts.size(); i++) {
            float cX = listX + (i % 2) * (cardW + gap);
            float cY = listY + (i / 2) * (cardH + gap) - scrollOffset;

            if (cY + cardH < listY || cY > listY + listH) continue;

            float bSize = 14;
            float bX = cX + cardW - bSize - 5;
            float bY = cY + cardH - bSize - 5;

            if (isMouseOver(mx, my, bX, bY, bSize, bSize)) {
                return new Object[]{"REMOVE", accounts.get(i)};
            }

            if (isMouseOver(mx, my, cX, cY, cardW, cardH)) {
                return new Object[]{"SELECT", accounts.get(i)};
            }
        }
        return null;
    }


    private void drawStyledPanel(float x, float y, float w, float h, float alpha, String title, boolean hasAccent) {
        int titleAlpha = (int) (alpha * 255);
        Render2D.rect(x - 1f, y - 1f, w + 2f, h + 2f, withAlpha(0x000000, (int)(alpha * 50)), 6);
        Render2D.rect(x, y, w, h, withAlpha(0x0f1114, (int)(alpha * 230)), 6);
        Render2D.outline(x, y, w, h, OUTLINE_THICKNESS, withAlpha(0x252a36, (int)(alpha * 150)), 6);

        if (hasAccent) {
            Render2D.rect(x + 2f, y + 5f, 1.5f, 12f, withAlpha(0x008cff, titleAlpha), 1);
        }

        if (title != null) {
            Fonts.BOLD.draw(title, x + (hasAccent ? 8 : 6), y + 7, 7.5f, withAlpha(0xFFFFFF, titleAlpha));
            Render2D.rect(x + 5, y + 21, w - 10, 0.5f, withAlpha(0x252a36, (int)(alpha * 100)), 0);
        }
    }

    public void renderLeftPanelTop(float x, float y, float width, float height, float contentAlpha,
                                   String nicknameText, boolean nicknameFieldFocused,
                                   float scaledMouseX, float scaledMouseY, long currentTime) {
        drawStyledPanel(x, y, width, height, contentAlpha, "Account Panel", true);

        float fieldX = x + 5, fieldY = y + 38, fieldH = 14, addSize = 14;
        float fieldW = width - 10 - addSize - 4;

        renderNicknameField(fieldX, fieldY, fieldW, fieldH, contentAlpha, nicknameText, nicknameFieldFocused, currentTime);

        boolean addH = isMouseOver(scaledMouseX, scaledMouseY, fieldX + fieldW + 4, fieldY, addSize, addSize);
        renderAddButton(fieldX + fieldW + 4, fieldY, addSize, contentAlpha, addH);

        float btnW = width - 10, btnH = 16;
        float rndY = fieldY + fieldH + 7;
        boolean rndH = isMouseOver(scaledMouseX, scaledMouseY, x + 5, rndY, btnW, btnH);
        renderModernButton(x + 5, rndY, btnW, btnH, "Random", "B", contentAlpha, rndH, false);

        float clrY = rndY + btnH + 5;
        boolean clrH = isMouseOver(scaledMouseX, scaledMouseY, x + 5, clrY, btnW, btnH);
        renderModernButton(x + 5, clrY, btnW, btnH, "Delete", "O", contentAlpha, clrH, true);
    }

    public void renderLeftPanelBottom(float x, float y, float width, float height, float contentAlpha,
                                      String activeAccountName, String activeAccountDate, Identifier activeAccountSkin) {
        drawStyledPanel(x, y, width, height, contentAlpha, "Active Session", true);

        if (activeAccountName != null && !activeAccountName.isEmpty()) {
            drawPlayerFace(SkinManager.getSkin(activeAccountName), x + 8, y + 28, 22, withAlpha(0xFFFFFF, (int)(contentAlpha * 255)));
            Fonts.INTER_MEDIUM.draw(activeAccountName, x + 36, y + 30, 6f, -1);
            Fonts.INTER_MEDIUM.draw(activeAccountDate, x + 36, y + 40, 4.5f, withAlpha(0x808890, 255));
        }
    }

    public void renderRightPanel(float x, float y, float width, float height, float contentAlpha,
                                 List<AccountEntry> accounts, float scrollOffset,
                                 float scaledMouseX, float scaledMouseY, float scale, int guiScale) {
        drawStyledPanel(x, y, width, height, contentAlpha, "Accounts List", true);

        float listX = x + 5;
        float listY = y + 28;
        float listW = width - 10;
        float listH = height - 31;
        float cardW = (listW - 5) / 2f;
        float cardH = 40;
        float gap = 5;

        Scissor.enable(listX * scale, listY * scale, listW * scale, listH * scale, (int)(guiScale / scale));

        for (int i = 0; i < accounts.size(); i++) {
            float cX = listX + (i % 2) * (cardW + gap);
            float cY = listY + (i / 2) * (cardH + gap) - scrollOffset;

            if (cY + cardH < listY - 10 || cY > listY + listH + 10) continue;
            renderAccountCard(cX, cY, cardW, cardH, accounts.get(i), contentAlpha, scaledMouseX, scaledMouseY, listY, listH);
        }

        Scissor.disable();
    }

    private void renderAccountCard(float x, float y, float width, float height, AccountEntry account,
                                   float alpha, float mx, float my, float ly, float lh) {
        boolean hovered = isMouseOver(mx, my, x, y, width, height) && my >= ly && my <= ly + lh;
        int titleAlpha = (int) (alpha * 255);

        Render2D.rect(x, y, width, height, withAlpha(0x12151c, hovered ? 180 : 120), 4);
        Render2D.outline(x, y, width, height, 0.5f, hovered ? 0xFF008CFF : 0xFF252A36, 4);

        drawPlayerFace(SkinManager.getSkin(account.getName()), x + 6, y + 6, 28, withAlpha(0xFFFFFF, titleAlpha));
        Fonts.INTER_MEDIUM.draw(account.getName(), x + 38, y + 10, 7f, withAlpha(0xFFFFFF, titleAlpha));
        Fonts.INTER_MEDIUM.draw(account.getDate(), x + 38, y + 20, 5.5f, withAlpha(0x707888, titleAlpha));

        float bSize = 14;
        float bX = x + width - bSize - 5;
        float bY = y + height - bSize - 5;
        boolean bH = isMouseOver(mx, my, bX, bY, bSize, bSize) && my >= ly && my <= ly + lh;

        Render2D.rect(bX, bY, bSize, bSize, withAlpha(0x0a0c0f, titleAlpha), 3);
        Render2D.outline(bX, bY, bSize, bSize, 0.5f, bH ? 0xFFFF4646 : 0xFF252A36, 3);
        Fonts.GUI_ICONS.drawCentered("O", bX + bSize/2f, bY + 2f, 10f, bH ? 0xFFFF4646 : 0xFF808890);
    }

    private void renderModernButton(float x, float y, float w, float h, String text, String icon, float alpha, boolean hovered, boolean isRed) {
        float hProgress = hovered ? 1f : 0f;
        float dY = y - (hProgress * 2f);
        int accent = isRed ? MainMenuScreen.interpolateColor(0xFF503232, 0xFFFF4646, hProgress) : MainMenuScreen.interpolateColor(0xFF1E2832, 0xFF008CFF, hProgress);

        if (hovered) Render2D.rect(x - 1.5f, dY - 1.5f, w + 3f, h + 3f, withAlpha(isRed ? 0xFF0000 : 0x008CFF, 40), 5);
        Render2D.rect(x, dY, w, h, withAlpha(0x0f1114, 255), 5);
        Render2D.outline(x, dY, w, h, 0.5f, withAlpha(accent, 255), 5);

        int txtClr = hovered ? -1 : 0xFFB4B4B4;
        Fonts.INTER_MEDIUM.draw(text, x + 6, dY + h/2f - 2.5f, 6f, txtClr);
        if (icon != null) {
            if (isRed) Fonts.GUI_ICONS.draw(icon, x + w - 12, dY + 3, 9f, txtClr);
            else Fonts.ICONS.draw(icon, x + w - 12, dY + 3.5f, 8f, txtClr);
        }
    }

    private void renderNicknameField(float x, float y, float width, float height, float contentAlpha,
                                     String nicknameText, boolean focused, long currentTime) {
        Render2D.rect(x, y, width, height, withAlpha(0x07080a, 255), 3);
        Render2D.outline(x, y, width, height, 0.5f, focused ? 0xFF008CFF : 0xFF252A36, 3);
        String text = (nicknameText == null || nicknameText.isEmpty()) && !focused ? "Enter nick..." : nicknameText;
        Fonts.INTER_MEDIUM.draw(text, x + 4, y + height/2f - 2.5f, 5.5f, (nicknameText == null || nicknameText.isEmpty()) ? 0xFF606878 : -1);
    }

    private void renderAddButton(float x, float y, float size, float contentAlpha, boolean hovered) {
        float dY = y - (hovered ? 1.5f : 0);
        Render2D.rect(x, dY, size, size, withAlpha(0x0f1114, 255), 3);
        Render2D.outline(x, dY, size, size, 0.5f, hovered ? 0xFF008CFF : 0xFF252A36, 3);
        Render2D.rect(x + size/2f - 2, dY + size/2f - 0.5f, 4, 1, -1, 0);
        Render2D.rect(x + size/2f - 0.5f, dY + size/2f - 2, 1, 4, -1, 0);
    }

    public void drawPlayerFace(Identifier skin, float x, float y, float size, int color) {
        Render2D.texture(skin, x, y, size, size, 8f/64f, 8f/64f, 16f/64f, 16f/64f, color, 0, 3f);
        Render2D.texture(skin, x - (size*0.06f), y - (size*0.06f), size*1.12f, size*1.12f, 40f/64f, 8f/64f, 48f/64f, 16f/64f, color, 0f, 3f);
    }

    public boolean isMouseOver(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (MathHelper.clamp(alpha, 0, 255) << 24);
    }
}
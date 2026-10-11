package neverlose.screens.menu;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import neverlose.screens.account.AccountEntry;
import neverlose.screens.account.AccountRenderer;
import neverlose.util.config.impl.account.AccountConfig;
import neverlose.util.render.Render2D;
import neverlose.util.render.font.Fonts;
import neverlose.util.session.SessionChanger;
import neverlose.util.sounds.SoundManager;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

public class MainMenuScreen extends Screen {

    private static final Identifier BACKGROUND_TEXTURE = Identifier.of("neverlose", "textures/menu/backmenu.png");
    private static final float FIXED_GUI_SCALE = 2.0f;
    private static final int PARTICLE_COUNT = 50;
    private static final float PARTICLE_FADE_THRESHOLD = 0.8f;

    private static class Particle {
        float x, y, vx, vy, size, alpha, lifetime, maxLifetime;
        boolean isDead = false;

        Particle(float x, float y) {
            this.x = x; this.y = y;
            Random rand = new Random();
            this.vx = (rand.nextFloat() - 0.5f) * 0.2f;
            this.vy = (rand.nextFloat() - 0.5f) * 0.2f;
            this.size = 0.5f + rand.nextFloat() * 1f;
            this.alpha = 0.2f + rand.nextFloat() * 0.3f;
            this.maxLifetime = 10f + rand.nextFloat() * 10f;
            this.lifetime = 0f;
        }

        void update(float delta, int width, int height) {
            lifetime += delta / 60f;
            if (lifetime > maxLifetime * PARTICLE_FADE_THRESHOLD) {
                float fadeProgress = (lifetime - maxLifetime * PARTICLE_FADE_THRESHOLD) / (maxLifetime * (1f - PARTICLE_FADE_THRESHOLD));
                alpha = Math.max(0, alpha * (1f - fadeProgress));
            }
            if (lifetime >= maxLifetime) { isDead = true; return; }
            x += vx * delta; y += vy * delta;
            if (x < -10 || x > width + 10 || y < -10 || y > height + 10) isDead = true;
        }
    }

    private final List<Particle> particles = new java.util.ArrayList<>();
    private boolean particlesInitialized = false;
    private int lastWindowWidth = 0, lastWindowHeight = 0;

    private enum View { MAIN_MENU, ALT_SCREEN }
    private View currentView = View.MAIN_MENU;

    private long screenStartTime = 0L;
    private boolean initialized = false;
    private long lastRenderTime = 0L;

    private int hoveredButton = -1;
    private float[] buttonHoverProgress = new float[6];

    private final AccountRenderer accountRenderer;
    private final AccountConfig accountConfig;
    private String nicknameText = "";
    private boolean nicknameFieldFocused = false;
    private float scrollOffset = 0f, targetScrollOffset = 0f;

    public MainMenuScreen() {
        super(Text.literal("Main Menu"));
        for (int i = 0; i < 6; i++) buttonHoverProgress[i] = 0f;
        this.accountRenderer = new AccountRenderer();
        this.accountConfig = AccountConfig.getInstance();
        this.accountConfig.load();
    }

    @Override protected void init() { initialized = false; particlesInitialized = false; }

    private int getFixedScaledWidth() { return (int) Math.ceil((double) client.getWindow().getFramebufferWidth() / FIXED_GUI_SCALE); }
    private int getFixedScaledHeight() { return (int) Math.ceil((double) client.getWindow().getFramebufferHeight() / FIXED_GUI_SCALE); }
    private float toFixedCoord(double coord) { return (float) (coord * client.getWindow().getScaleFactor() / FIXED_GUI_SCALE); }

    private void initParticles(int width, int height) {
        if (particlesInitialized) return;
        Random rand = new Random();
        for (int i = 0; i < PARTICLE_COUNT; i++) particles.add(new Particle(rand.nextFloat() * width, rand.nextFloat() * height));
        particlesInitialized = true;
    }

    private void drawBackground() {
        int w = getFixedScaledWidth(), h = getFixedScaledHeight();
        Render2D.rect(0, 0, w, h, new Color(12, 15, 18).getRGB());
        initParticles(w, h);
        for (Particle p : particles) {
            if (!p.isDead && p.alpha > 0.01f) {
                Render2D.rect(p.x, p.y, p.size, p.size, new Color(255, 255, 255, (int)(p.alpha * 255)).getRGB());
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long currentTime = Util.getMeasuringTimeMs();
        if (!initialized) { screenStartTime = currentTime; lastRenderTime = currentTime; initialized = true; }
        float deltaTime = Math.min(delta, 0.05f);
        lastRenderTime = currentTime;
        int fw = getFixedScaledWidth(), fh = getFixedScaledHeight();

        if (lastWindowWidth != fw || lastWindowHeight != fh) {
            particles.clear(); particlesInitialized = false;
            lastWindowWidth = fw; lastWindowHeight = fh;
        }

        for (Particle p : particles) p.update(deltaTime * 60f, fw, fh);
        particles.removeIf(p -> p.isDead);
        Random rand = new Random();
        while (particles.size() < PARTICLE_COUNT) particles.add(new Particle(rand.nextFloat() * fw, rand.nextFloat() * fh));

        float smX = toFixedCoord(mouseX), smY = toFixedCoord(mouseY);
        updateButtonAnimations(deltaTime, smX, smY, fw, fh);

        Render2D.beginOverlay();
        drawBackground();
        if (currentView == View.MAIN_MENU) renderMainMenu(fw, fh, smX, smY);
        else renderAltScreen(fw, fh, smX, smY, currentTime);
        Render2D.endOverlay();
    }

    private void renderMainMenu(int screenWidth, int screenHeight, float mouseX, float mouseY) {
        float centerX = screenWidth / 2f, centerY = screenHeight / 2f;
        float logoY = centerY - 95, titleY = logoY + 40;

        Fonts.BOLD.drawCentered("DettexClient", centerX, titleY, 10f, new Color(0, 160, 255, 45).getRGB());
        Fonts.BOLD.drawCentered("DettexClient", centerX, titleY, 10f, Color.WHITE.getRGB());
        Fonts.INTER_MEDIUM.drawCentered("Future is now. Experience the difference.", centerX, titleY + 18, 6.5f, new Color(100, 105, 110).getRGB());

        float btnW = 120, btnH = 18, spc = 6, startY = titleY + 45;

        drawModernButton(centerX - btnW / 2f, startY, btnW, btnH, "Singleplayer", 0, mouseX, mouseY, false);
        drawModernButton(centerX - btnW / 2f, startY + (btnH + spc), btnW, btnH, "Multiplayer", 1, mouseX, mouseY, false);
        drawModernButton(centerX - btnW / 2f, startY + (btnH + spc) * 2, btnW, btnH, "Alt Manager", 2, mouseX, mouseY, true);

        float rowSpc = 5f;
        float halfW = (btnW - rowSpc) / 2f;
        float rowY = startY + (btnH + spc) * 3;

        drawModernButton(centerX - btnW / 2f, rowY, halfW, btnH, "Settings", 3, mouseX, mouseY, false);
        drawModernButton(centerX - btnW / 2f + halfW + rowSpc, rowY, halfW, btnH, "Exit", 5, mouseX, mouseY, false);
    }

    private void drawModernButton(float x, float y, float w, float h, String txt, int idx, float mX, float mY, boolean acc) {
        float hover = buttonHoverProgress[idx];
        float dY = y - (hover * 2f);
        int accClr = new Color(0, 140, 255, (int)(100 + hover * 155)).getRGB();

        if (hover > 0.01f) Render2D.rect(x - 2f, dY - 2f, w + 4f, h + 4f, new Color(0, 120, 255, (int)(hover * 50)).getRGB(), 6);
        Render2D.rect(x - 0.5f, dY - 0.5f, w + 1f, h + 1f, interpolateColor(new Color(255, 255, 255, 15).getRGB(), accClr, hover), 5);
        Render2D.rect(x, dY, w, h, new Color(15, 17, 20, 180).getRGB(), 5);

        float fS = 7.5f;
        int tC = interpolateColor(new Color(180, 180, 180).getRGB(), Color.WHITE.getRGB(), hover);
        Fonts.INTER_MEDIUM.drawCentered(txt, x + (w / 2f) + (hover * 2f), dY + (h - Fonts.INTER_MEDIUM.getHeight(fS)) / 2f, fS, tC);
        if (acc || hover > 0.1f) Render2D.rect(x + 3f, dY + 4f, 1.5f, h - 8f, interpolateColor(new Color(0,0,0,0).getRGB(), accClr, acc ? 1f : hover), 1);
    }

    private void updateButtonAnimations(float dT, float mX, float mY, int sw, int sh) {
        float cX = sw / 2f, cY = sh / 2f, lS = 1f - (float) Math.pow(0.001f, dT);
        if (currentView == View.MAIN_MENU) {
            float sY = (cY - 95) + 40 + 45, bW = 120, bH = 18, spc = 6;
            for (int i = 0; i < 3; i++) {
                boolean h = isMouseOver(mX, mY, cX - bW / 2f, sY + i * (bH + spc), bW, bH);
                buttonHoverProgress[i] = MathHelper.lerp(lS, buttonHoverProgress[i], h ? 1f : 0f);
            }
            float rY = sY + (bH + spc) * 3, rSpc = 5f, hW = (bW - rSpc) / 2f;
            buttonHoverProgress[3] = MathHelper.lerp(lS, buttonHoverProgress[3], isMouseOver(mX, mY, cX - bW / 2f, rY, hW, bH) ? 1f : 0f);
            buttonHoverProgress[5] = MathHelper.lerp(lS, buttonHoverProgress[5], isMouseOver(mX, mY, cX - bW / 2f + hW + rSpc, rY, hW, bH) ? 1f : 0f);
            buttonHoverProgress[4] = 0f;
        } else for (int i = 0; i < 6; i++) buttonHoverProgress[i] = MathHelper.lerp(lS, buttonHoverProgress[i], 0f);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        float smX = toFixedCoord(click.x()), smY = toFixedCoord(click.y());

        if (currentView == View.MAIN_MENU && click.button() == 0) {
            float cX = getFixedScaledWidth() / 2f, cY = getFixedScaledHeight() / 2f;
            float sY = (cY - 95) + 40 + 45, bW = 120, bH = 18, spc = 6;
            for (int i = 0; i < 3; i++)
                if (isMouseOver(smX, smY, cX - bW / 2f, sY + i * (bH + spc), bW, bH)) { handleMainMenuButtonClick(i); return true; }
            float rY = sY + (bH + spc) * 3, rSpc = 5f, hW = (bW - rSpc) / 2f;
            if (isMouseOver(smX, smY, cX - bW / 2f, rY, hW, bH)) { handleMainMenuButtonClick(3); return true; }
            if (isMouseOver(smX, smY, cX - bW / 2f + hW + rSpc, rY, hW, bH)) { handleMainMenuButtonClick(5); return true; }
        } else if (currentView == View.ALT_SCREEN) {
            return handleAltScreenClick(smX, smY, click);
        }
        return super.mouseClicked(click, doubled);
    }

    private void handleMainMenuButtonClick(int idx) {
        switch (idx) {
            case 0 -> this.client.setScreen(new SelectWorldScreen(this));
            case 1 -> this.client.setScreen(this.client.options.skipMultiplayerWarning ? new MultiplayerScreen(this) : new MultiplayerWarningScreen(this));
            case 2 -> {
                playAltClick(SoundManager.CLICKGUI_OPEN, 0.55f, 1.05f);
                currentView = View.ALT_SCREEN;
            }
            case 3 -> this.client.setScreen(new OptionsScreen(this, this.client.options));
            case 5 -> this.client.scheduleStop();
        }
    }

    private void renderAltScreen(int sw, int sh, float mX, float mY, long time) {
        float tw = 405, th = 163, cx = sw / 2f, cy = sh / 2f;
        float sx = cx - tw / 2f, sy = cy - th / 2f;

        accountRenderer.renderLeftPanelTop(sx, sy, 100, 100, 1f, nicknameText, nicknameFieldFocused, mX, mY, time);
        accountRenderer.renderLeftPanelBottom(sx, sy + 105, 100, 58, 1f, accountConfig.getActiveAccountName(), accountConfig.getActiveAccountDate(), accountConfig.getActiveAccountSkin());
        accountRenderer.renderRightPanel(sx + 105, sy, 300, 165, 1f, accountConfig.getSortedAccounts(), scrollOffset, mX, mY, 1f, (int) FIXED_GUI_SCALE);
    }

    private boolean handleAltScreenClick(float mX, float mY, Click click) {
        float sw = getFixedScaledWidth(), sh = getFixedScaledHeight();
        float sx = (sw / 2f) - 202.5f, sy = (sh / 2f) - 81.5f;
        if (isMouseOver(mX, mY, sx + 5, sy + 38, 100 - 27, 14) && click.button() == 0) {
            playAltClick(SoundManager.CLICKGUI_SELECT, 0.45f, 1.15f);
            nicknameFieldFocused = true;
            return true;
        }

        if (click.button() == 0) {
            nicknameFieldFocused = false;
        }

        if (isMouseOver(mX, mY, sx + 78, sy + 38, 14, 14) && click.button() == 0 && !nicknameText.isEmpty()) {
            playAltClick(SoundManager.CLICKGUI_FUNCTION_ON, 0.5f, 1.05f);
            addAccount(nicknameText);
            nicknameText = "";
            return true;
        }

        if (isMouseOver(mX, mY, sx + 5, sy + 58, 90, 16) && click.button() == 0) {
            playAltClick(SoundManager.CLICKGUI_CLACK, 0.48f, 1.08f);
            addAccount(generateRandomNickname());
            return true;
        }

        if (isMouseOver(mX, mY, sx + 5, sy + 79, 90, 16) && click.button() == 0) {
            playAltClick(SoundManager.CLICKGUI_FUNCTION_OFF, 0.5f, 0.95f);
            accountConfig.clearAllAccounts();
            scrollOffset = 0;
            return true;
        }

        Object[] result = accountRenderer.handleMouseClick(
                mX, mY,
                sx + 105, sy, 300, 165,
                accountConfig.getSortedAccounts(),
                scrollOffset
        );

        if (result != null && click.button() == 0) {
            String action = (String) result[0];
            AccountEntry account = (AccountEntry) result[1];

            if ("REMOVE".equals(action)) {
                playAltClick(SoundManager.CLICKGUI_FUNCTION_OFF, 0.5f, 0.95f);
                accountConfig.removeAccount(account);
                accountConfig.save();
                return true;
            }
            if ("SELECT".equals(action)) {
                playAltClick(SoundManager.CLICKGUI_SELECT, 0.5f, 1.1f);
                setActiveAccount(account);
                accountConfig.save();
                return true;
            }
        }

        return false;
    }


    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();


        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (currentView == View.ALT_SCREEN) {
                playAltClick(SoundManager.CLICKGUI_CLOSE, 0.5f, 1.0f);
                currentView = View.MAIN_MENU;
                return true;
            }
        }


        if (nicknameFieldFocused && key == GLFW.GLFW_KEY_BACKSPACE) {
            nicknameText = accountRenderer.handleKeyPress(key, nicknameText, nicknameFieldFocused);
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        // Добавление символов в поле ника
        if (nicknameFieldFocused && nicknameText.length() < 16) {
            nicknameText += (char) input.codepoint();
        }
        return true;
    }

    public static int interpolateColor(int c1, int c2, float f) {
        int a = (int) MathHelper.lerp(f, (c1 >> 24) & 0xFF, (c2 >> 24) & 0xFF);
        int r = (int) MathHelper.lerp(f, (c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF);
        int g = (int) MathHelper.lerp(f, (c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF);
        int b = (int) MathHelper.lerp(f, c1 & 0xFF, c2 & 0xFF);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private boolean isMouseOver(float mX, float mY, float x, float y, float w, float h) {
        return mX >= x && mX <= x + w && mY >= y && mY <= y + h;
    }

    private void setActiveAccount(AccountEntry acc) {
        accountConfig.setActiveAccount(acc.getName(), acc.getDate(), acc.getSkin());
        SessionChanger.changeUsername(acc.getName());
    }

    private void addAccount(String nick) {
        AccountEntry e = new AccountEntry(nick, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), null);
        accountConfig.addAccount(e);
        setActiveAccount(e);
    }

    private void playAltClick(SoundEvent sound, float volume, float pitch) {
        try {
            SoundManager.playSoundDirect(sound, volume, pitch);
        } catch (Exception ignored) {
        }
    }

    private String generateRandomNickname() {
        Random r = new Random(); StringBuilder sb = new StringBuilder();
        char[] v = {'a','e','i','o','u'}, c = {'b','c','d','f','g','h','j','k','l','m','n','p','r','s','t','v','w','x','y','z'};
        for (int i=0; i<8; i++) sb.append(i%2==0 ? c[r.nextInt(c.length)] : v[r.nextInt(v.length)]);
        return sb.substring(0,1).toUpperCase() + sb.substring(1) + (r.nextInt(90)+10);
    }

    private void drawButton(float x, float y, float w, float h, String txt, int idx, float mX, float mY, Color bC, boolean p) {
        float hP = buttonHoverProgress[idx]; int br = (int)(hP*(p?15:10));
        Color bgColor = new Color(MathHelper.clamp(bC.getRed()+br,0,255), MathHelper.clamp(bC.getGreen()+br,0,255), MathHelper.clamp(bC.getBlue()+br,0,255));
        Render2D.rect(x, y, w, h, bgColor.getRGB(), 4);
        Fonts.INTER_MEDIUM.drawCentered(txt, x + w/2f, y + (h - Fonts.INTER_MEDIUM.getHeight(7f))/2f, 7f, p ? -1 : 0xFFA0A0A0);
    }

    private void drawSmallButton(float x, float y, float w, float h, String txt, int idx, float mX, float mY) {
        float hP = buttonHoverProgress[idx];
        Fonts.INTER_MEDIUM.drawCentered(txt, x + w/2f, y + (h - Fonts.INTER_MEDIUM.getHeight(6.5f))/2f, 6.5f, new Color(180,180,180,(int)(130+hP*40)).getRGB());
    }

    @Override public boolean mouseScrolled(double x, double y, double h, double v) {
        scrollOffset = MathHelper.clamp(scrollOffset - (float)v * 15, 0, 1000);
        return true;
    }

    @Override public void renderBackground(DrawContext c, int x, int y, float d) { drawBackground(); }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }
}

package neverlose.screens.hud;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import neverlose.client.draggables.AbstractHudElement;
import neverlose.util.animations.Direction;
import neverlose.util.render.Render2D;
import neverlose.util.render.font.Font;
import neverlose.util.render.font.Fonts;
import neverlose.util.render.shader.Scissor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class MusicInfo extends AbstractHudElement {
    private static final Identifier ARTWORK = Identifier.of("neverlose", "dynamic/music_artwork");
    private static final int WIDTH = 112;
    private static final int HEIGHT = 40;
    private static final float PADDING = 4.0f;
    private static final float COVER_SIZE = 32.0f;
    private static final long POLL_INTERVAL_MS = 500L;
    private static final long MEDIA_TIMEOUT_MS = 5000L;

    private final ExecutorService mediaExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Neverlose-MediaInfo");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean polling = new AtomicBoolean();

    private volatile MediaInfo mediaInfo;
    private volatile IMediaSession session;
    private volatile String sessionOwner = "";
    private volatile long lastPollTime;
    private volatile long lastMediaTime;
    private volatile boolean mediaAvailable = true;
    private volatile boolean artworkReady;
    private volatile int artworkHash;
    private float animatedProgress;

    public MusicInfo() {
        super("Music Info", 10, 250, WIDTH, HEIGHT, true);
        stopAnimation();
    }

    @Override
    public boolean visible() {
        return isChat(mc.currentScreen)
                || hasRecentMedia()
                || !scaleAnimation.isFinished(Direction.BACKWARDS);
    }

    @Override
    public void tick() {
        if (mc.player == null) {
            stopAnimation();
            return;
        }

        long now = System.currentTimeMillis();
        if (mediaAvailable && now - lastPollTime >= POLL_INTERVAL_MS && polling.compareAndSet(false, true)) {
            lastPollTime = now;
            mediaExecutor.execute(this::pollMedia);
        }

        if (hasRecentMedia() || isChat(mc.currentScreen)) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }

    private void pollMedia() {
        try {
            List<IMediaSession> sessions = MediaPlayerInfo.Instance.getMediaSessions();
            IMediaSession current = selectSession(sessions);
            if (current == null) {
                return;
            }

            MediaInfo info = current.getMedia();
            if (info == null || (info.getTitle().isBlank() && info.getArtist().isBlank())) {
                return;
            }

            mediaInfo = info;
            session = current;
            sessionOwner = current.getOwner() == null ? "" : current.getOwner();
            lastMediaTime = System.currentTimeMillis();
            updateArtwork(info.getArtworkPng());
        } catch (Throwable ignored) {
            mediaAvailable = false;
        } finally {
            polling.set(false);
        }
    }

    private IMediaSession selectSession(List<IMediaSession> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return null;
        }

        return sessions.stream()
                .filter(candidate -> candidate != null && candidate.getMedia() != null)
                .max(Comparator.comparing(candidate -> {
                    try {
                        return candidate.getMedia().getPlaying();
                    } catch (Throwable ignored) {
                        return false;
                    }
                }))
                .orElse(null);
    }

    private void updateArtwork(byte[] artworkBytes) {
        if (artworkBytes == null || artworkBytes.length == 0) {
            artworkReady = false;
            artworkHash = 0;
            return;
        }

        int hash = Arrays.hashCode(artworkBytes);
        if (hash == artworkHash && artworkReady) {
            return;
        }

        byte[] imageData = Arrays.copyOf(artworkBytes, artworkBytes.length);
        mc.execute(() -> {
            try {
                NativeImage image = NativeImage.read(imageData);
                NativeImageBackedTexture texture = new NativeImageBackedTexture(
                        () -> "DettexClient music artwork",
                        image
                );
                mc.getTextureManager().destroyTexture(ARTWORK);
                mc.getTextureManager().registerTexture(ARTWORK, texture);
                artworkHash = hash;
                artworkReady = true;
            } catch (Throwable ignored) {
                artworkReady = false;
            }
        });
    }

    private boolean hasRecentMedia() {
        return mediaInfo != null && System.currentTimeMillis() - lastMediaTime <= MEDIA_TIMEOUT_MS;
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) {
            return;
        }

        setWidth(WIDTH);
        setHeight(HEIGHT);

        float x = getX();
        float y = getY();
        float alphaFactor = alpha / 255.0f;
        int panelAlpha = Math.min(245, alpha);
        MediaInfo info = mediaInfo;
        boolean preview = info == null || !hasRecentMedia();

        Render2D.gradientRect(x, y, WIDTH, HEIGHT,
                new int[]{
                        HudTheme.panelTop(panelAlpha),
                        HudTheme.panel(panelAlpha),
                        HudTheme.panelBottom(panelAlpha),
                        HudTheme.item(panelAlpha)
                }, 6);

        float coverX = x + PADDING;
        float coverY = y + PADDING;
        drawArtwork(coverX, coverY, alpha, preview);

        float contentX = coverX + COVER_SIZE + 5.0f;
        float contentWidth = WIDTH - (contentX - x) - PADDING;
        String title = preview ? "No media playing" : safeText(info.getTitle(), "Unknown track");
        String artist = preview ? "Open your music player" : safeText(info.getArtist(), "Unknown artist");

        drawScrollingText(Fonts.INTER_SEMIBOLD, title, contentX, y + 3.5f,
                5.8f, HudTheme.text(alpha), contentWidth);
        drawScrollingText(Fonts.INTER_MEDIUM, artist, contentX, y + 10.5f,
                4.8f, HudTheme.muted(alpha), contentWidth);

        float targetProgress = preview || info.getDuration() <= 0
                ? 0.0f
                : clamp((float) info.getPosition() / (float) info.getDuration());
        animatedProgress += (targetProgress - animatedProgress) * 0.18f;

        float progressY = y + 17.5f;
        Render2D.rect(contentX, progressY, contentWidth, 1.5f,
                HudTheme.track((int) (220 * alphaFactor)), 0.75f);
        if (animatedProgress > 0.001f) {
            Render2D.rect(contentX, progressY, contentWidth * animatedProgress, 1.5f,
                    HudTheme.accent(alpha), 0.75f);
        }

        String elapsed = preview ? "0:00" : formatTime(info.getPosition());
        String duration = preview ? "0:00" : formatTime(info.getDuration());
        String time = elapsed + " / " + duration;
        Fonts.INTER_MEDIUM.drawCentered(time, contentX + contentWidth / 2.0f, y + 20.0f,
                4.7f, HudTheme.text(alpha));

        drawControls(context, contentX, contentWidth, y + 29.0f, alpha, preview || !mediaAvailable,
                !preview && info.getPlaying());
    }

    private void drawArtwork(float x, float y, int alpha, boolean preview) {
        Render2D.rect(x, y, COVER_SIZE, COVER_SIZE, HudTheme.item(alpha), 4.0f);
        if (!preview && artworkReady) {
            Render2D.texture(ARTWORK, x, y, COVER_SIZE, COVER_SIZE,
                    0, 0, 1, 1, (alpha << 24) | 0xFFFFFF, 0.0f, 4.0f);
            return;
        }

        Render2D.gradientRect(x, y, COVER_SIZE, COVER_SIZE,
                new int[]{
                        HudTheme.itemSoft(alpha),
                        HudTheme.itemSoft(alpha),
                        HudTheme.track(alpha),
                        HudTheme.item(alpha)
                }, 4.0f);
        Fonts.ICONS2.drawCentered("M", x + COVER_SIZE / 2.0f, y + 10.0f,
                9.0f, HudTheme.accent(alpha));
    }

    private void drawControls(DrawContext context, float contentX, float contentWidth, float y, int alpha,
                              boolean disabled, boolean playing) {
        float buttonSize = 8.0f;
        float gap = 3.0f;
        float rowWidth = buttonSize * 3.0f + gap * 2.0f;
        float startX = contentX + (contentWidth - rowWidth) / 2.0f;
        int iconColor = disabled ? HudTheme.muted((int) (135 * (alpha / 255.0f))) : HudTheme.accent(alpha);

        drawControlButton(startX, y, buttonSize, "K", iconColor, alpha);
        drawControlButton(startX + buttonSize + gap, y, buttonSize,
                playing ? "O" : "I", iconColor, alpha);
        drawControlButton(startX + (buttonSize + gap) * 2.0f, y, buttonSize, "L", iconColor, alpha);
    }

    private void drawControlButton(float x, float y, float size, String icon, int iconColor, int alpha) {
        Render2D.rect(x, y, size, size, HudTheme.itemSoft((int) (220 * (alpha / 255.0f))), 2.5f);
        float iconSize = 6.2f;
        float iconY = y + (size - Fonts.NLEZ.getHeight(iconSize)) / 2.0f + 0.25f;
        Fonts.NLEZ.drawCentered(icon, x + size / 2.0f, iconY, iconSize, iconColor);
    }

    private void drawScrollingText(Font font, String text, float x, float y, float size,
                                   int color, float maxWidth) {
        float width = font.getWidth(text, size);
        float scroll = 0.0f;
        if (width > maxWidth) {
            float overflow = width - maxWidth;
            long cycle = 7000L;
            long time = System.currentTimeMillis() % cycle;
            if (time > 1200L && time < 4200L) {
                scroll = overflow * ((time - 1200L) / 3000.0f);
            } else if (time >= 4200L && time < 5400L) {
                scroll = overflow;
            } else if (time >= 5400L) {
                scroll = overflow * (1.0f - (time - 5400L) / 1600.0f);
            }
        }

        Scissor.enable(x, y - 1.0f, maxWidth, size + 3.0f);
        font.draw(text, x - scroll, y, size, color);
        Scissor.disable();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || mediaInfo == null || !hasRecentMedia() || !mediaAvailable) {
            return false;
        }

        float contentX = getX() + PADDING + COVER_SIZE + 5.0f;
        float contentWidth = WIDTH - (contentX - getX()) - PADDING;
        float buttonSize = 8.0f;
        float gap = 3.0f;
        float startX = contentX + (contentWidth - buttonSize * 3.0f - gap * 2.0f) / 2.0f;
        float buttonY = getY() + 29.0f;

        if (inside(mouseX, mouseY, startX, buttonY, buttonSize)) {
            control(IMediaSession::previous);
            return true;
        }
        if (inside(mouseX, mouseY, startX + buttonSize + gap, buttonY, buttonSize)) {
            control(IMediaSession::playPause);
            return true;
        }
        if (inside(mouseX, mouseY, startX + (buttonSize + gap) * 2.0f, buttonY, buttonSize)) {
            control(IMediaSession::next);
            return true;
        }
        return false;
    }

    private boolean inside(double mouseX, double mouseY, float x, float y, float size) {
        return mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;
    }

    private void control(Consumer<IMediaSession> action) {
        mediaExecutor.execute(() -> {
            try {
                List<IMediaSession> sessions = MediaPlayerInfo.Instance.getMediaSessions();
                IMediaSession target = sessions.stream()
                        .filter(candidate -> candidate != null && candidate.getMedia() != null)
                        .filter(candidate -> sessionOwner.isEmpty() || sessionOwner.equals(candidate.getOwner()))
                        .filter(candidate -> mediaInfo == null
                                || mediaInfo.getTitle().equals(candidate.getMedia().getTitle()))
                        .findFirst()
                        .orElse(session);
                if (target != null) {
                    action.accept(target);
                    lastPollTime = 0L;
                }
            } catch (Throwable ignored) {
                mediaAvailable = false;
            }
        });
    }

    private String safeText(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text;
    }

    private String formatTime(long seconds) {
        long totalSeconds = Math.max(0L, seconds);
        return totalSeconds / 60L + ":" + String.format("%02d", totalSeconds % 60L);
    }

    private float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}

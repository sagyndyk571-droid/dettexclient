package neverlose.screens.hud;

import neverlose.modules.impl.render.Hud;

final class HudTheme {
    private static final Palette BLUE = new Palette(
            rgb(14, 20, 26),
            rgb(20, 27, 34),
            rgb(11, 16, 21),
            rgb(14, 20, 26),
            rgb(14, 20, 26),
            rgb(64, 86, 106),
            rgb(70, 130, 220),
            rgb(226, 234, 242),
            rgb(132, 145, 158),
            rgb(38, 48, 58)
    );

    private static final Palette CYAN = new Palette(
            rgb(11, 21, 25),
            rgb(16, 30, 34),
            rgb(8, 16, 20),
            rgb(11, 21, 25),
            rgb(12, 24, 29),
            rgb(48, 92, 104),
            rgb(65, 195, 230),
            rgb(226, 244, 247),
            rgb(126, 158, 166),
            rgb(31, 54, 61)
    );

    private static final Palette PURPLE = new Palette(
            rgb(17, 16, 25),
            rgb(24, 22, 34),
            rgb(12, 11, 20),
            rgb(17, 16, 25),
            rgb(20, 18, 31),
            rgb(82, 72, 118),
            rgb(145, 105, 245),
            rgb(236, 231, 248),
            rgb(150, 140, 166),
            rgb(45, 39, 62)
    );

    private static final Palette EMERALD = new Palette(
            rgb(12, 22, 19),
            rgb(17, 31, 27),
            rgb(8, 16, 14),
            rgb(12, 22, 19),
            rgb(14, 27, 23),
            rgb(48, 96, 80),
            rgb(65, 190, 135),
            rgb(225, 242, 235),
            rgb(126, 158, 146),
            rgb(31, 56, 48)
    );

    private static final Palette ROSE = new Palette(
            rgb(25, 15, 20),
            rgb(35, 21, 28),
            rgb(18, 10, 14),
            rgb(25, 15, 20),
            rgb(31, 18, 24),
            rgb(112, 62, 82),
            rgb(235, 90, 135),
            rgb(246, 229, 236),
            rgb(168, 135, 148),
            rgb(62, 36, 46)
    );

    private static final Palette AMBER = new Palette(
            rgb(24, 19, 13),
            rgb(34, 27, 18),
            rgb(17, 13, 9),
            rgb(24, 19, 13),
            rgb(30, 24, 16),
            rgb(108, 84, 48),
            rgb(235, 170, 70),
            rgb(245, 238, 226),
            rgb(166, 148, 123),
            rgb(61, 48, 31)
    );

    private static final Palette MONO = new Palette(
            rgb(17, 19, 22),
            rgb(24, 26, 30),
            rgb(12, 14, 17),
            rgb(17, 19, 22),
            rgb(20, 23, 27),
            rgb(78, 84, 92),
            rgb(205, 215, 225),
            rgb(232, 236, 240),
            rgb(145, 152, 160),
            rgb(42, 46, 52)
    );

    private HudTheme() {
    }

    static int panel(int alpha) {
        return color(active().panel, backgroundAlpha(alpha));
    }

    static int panelTop(int alpha) {
        return color(active().panelTop, backgroundAlpha(alpha));
    }

    static int panelBottom(int alpha) {
        return color(active().panelBottom, backgroundAlpha(alpha));
    }

    static int item(int alpha) {
        return color(active().item, backgroundAlpha(alpha));
    }

    static int itemSoft(int alpha) {
        return color(active().itemSoft, backgroundAlpha(alpha));
    }

    static int outline(int alpha) {
        return color(active().outline, backgroundAlpha(alpha));
    }

    public static int accent(int alpha) {
        return color(active().accent, alpha);
    }

    static int accentSoft(int alpha) {
        return color(active().accent, alpha);
    }

    static int text(int alpha) {
        return color(active().text, alpha);
    }

    static int muted(int alpha) {
        return color(active().muted, alpha);
    }

    static int track(int alpha) {
        return color(active().track, backgroundAlpha(alpha));
    }

    static int backgroundAlpha(int alpha) {
        try {
            Hud hud = Hud.getInstance();
            float opacity = hud != null && hud.hudOpacity != null
                    ? Math.max(0.0f, Math.min(1.0f, hud.hudOpacity.getValue() / 100.0f))
                    : 1.0f;
            float blurGlass = hud != null && hud.hudBlur != null && hud.hudBlur.isValue() ? 0.68f : 1.0f;
            return clamp(Math.round(alpha * opacity * blurGlass));
        } catch (Throwable ignored) {
            return clamp(alpha);
        }
    }

    private static Palette active() {
        try {
            Hud hud = Hud.getInstance();
            if (hud == null || hud.hudTheme == null) {
                return BLUE;
            }

            return switch (hud.hudTheme.getSelected()) {
                case "Cyan" -> CYAN;
                case "Purple" -> PURPLE;
                case "Emerald" -> EMERALD;
                case "Rose" -> ROSE;
                case "Amber" -> AMBER;
                case "Mono" -> MONO;
                case "Custom" -> custom(hud);
                default -> BLUE;
            };
        } catch (Throwable ignored) {
            return BLUE;
        }
    }

    private static Palette custom(Hud hud) {
        int accent = hud.hudAccent == null
                ? BLUE.accent
                : hud.hudAccent.getColorNoAlpha() & 0xFFFFFF;
        return new Palette(
                BLUE.panel,
                BLUE.panelTop,
                BLUE.panelBottom,
                BLUE.item,
                BLUE.itemSoft,
                mix(rgb(52, 62, 72), accent, 0.42f),
                accent,
                BLUE.text,
                mix(BLUE.muted, accent, 0.16f),
                mix(BLUE.track, accent, 0.10f)
        );
    }

    private static int color(int rgb, int alpha) {
        return (clamp(alpha) << 24) | (rgb & 0xFFFFFF);
    }

    private static int rgb(int r, int g, int b) {
        return (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int mix(int from, int to, float amount) {
        amount = Math.max(0f, Math.min(1f, amount));
        int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * amount);
        int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * amount);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * amount);
        return rgb(r, g, b);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private record Palette(
            int panel,
            int panelTop,
            int panelBottom,
            int item,
            int itemSoft,
            int outline,
            int accent,
            int text,
            int muted,
            int track
    ) {
    }
}

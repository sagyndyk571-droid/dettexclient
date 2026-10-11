package neverlose.screens.hud;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Identifier;
import neverlose.client.draggables.AbstractHudElement;
import neverlose.util.ColorUtil;
import neverlose.util.animations.Direction;
import neverlose.util.render.Render2D;
import neverlose.util.render.shader.Scissor;
import neverlose.util.render.font.Fonts;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

public class Staff extends AbstractHudElement {

    private static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");
    private static final Pattern DIGIT_ONLY_PATTERN = Pattern.compile("^\\d{1,4}$");
    private static final Identifier STEVE_SKIN = Identifier.of("neverlose", "textures/entity/player/wide/steve.png");

    private static class StaffInfo {
        String name;
        GameProfile profile;
        Identifier skin;

        StaffInfo(String name, GameProfile profile, Identifier skin) {
            this.name = name;
            this.profile = profile;
            this.skin = skin;
        }
    }

    private Map<String, StaffInfo> staffMap = new LinkedHashMap<>();
    private Set<String> activeStaffIds = new HashSet<>();
    private Identifier cachedRandomSkin = null;
    private boolean skinCached = false;

    private float animatedWidth = 80;
    private float animatedHeight = 23;
    private long lastUpdateTime = System.currentTimeMillis();

    private static final float ANIMATION_SPEED = 8.0f;
    private static final float FACE_SIZE = 8f;

    public Staff() {
        super("Staff", 300, 150, 80, 23, true);
        stopAnimation();
    }

    @Override
    public boolean visible() {
        return !scaleAnimation.isFinished(Direction.BACKWARDS);
    }

    private Identifier getSkinFromEntry(PlayerListEntry entry) {
        try {
            SkinTextures skinTextures = entry.getSkinTextures();
            if (skinTextures != null && skinTextures.body() != null) {
                Identifier texturePath = skinTextures.body().texturePath();
                if (texturePath != null) {
                    return texturePath;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Identifier findRandomOnlineSkin() {
        if (mc.player == null || mc.player.networkHandler == null) {
            return STEVE_SKIN;
        }

        List<PlayerListEntry> players = new java.util.ArrayList<>(mc.player.networkHandler.getPlayerList());
        Collections.shuffle(players);

        for (PlayerListEntry entry : players) {
            Identifier skin = getSkinFromEntry(entry);
            if (skin != null) {
                return skin;
            }
        }

        return STEVE_SKIN;
    }

    @Override
    public void tick() {
        if (mc.player == null || mc.world == null) {
            staffMap.clear();
            activeStaffIds.clear();
            cachedRandomSkin = null;
            skinCached = false;
            stopAnimation();
            return;
        }

        String myName = mc.player.getName().getString();
        activeStaffIds.clear();

        Scoreboard scoreboard = mc.world.getScoreboard();
        List<Team> teams = new java.util.ArrayList<>(scoreboard.getTeams());
        teams.sort(Comparator.comparing(Team::getName));

        Collection<PlayerListEntry> online = mc.player.networkHandler.getPlayerList();
        Set<String> onlineNames = new HashSet<>();
        for (PlayerListEntry entry : online) {
            if (entry.getProfile() != null && entry.getProfile().name() != null) {
                onlineNames.add(entry.getProfile().name());
            }
        }

        for (Team team : teams) {
            Collection<String> members = team.getPlayerList();
            if (members.size() != 1) continue;
            String name = members.iterator().next();
            if (!NAME_PATTERN.matcher(name).matches()) continue;
            if (name.equals(myName)) continue;
            if (DIGIT_ONLY_PATTERN.matcher(name).matches()) continue;

            boolean isOnline = onlineNames.contains(name);

            if (!isOnline) {
                activeStaffIds.add(name);

                if (!staffMap.containsKey(name)) {
                    GameProfile fakeProfile = new GameProfile(UUID.randomUUID(), name);
                    Identifier randomSkin = findRandomOnlineSkin();
                    staffMap.put(name, new StaffInfo(name, fakeProfile, randomSkin));
                }
            }
        }

        boolean hasActiveStaff = !activeStaffIds.isEmpty();
        boolean inChat = isChat(mc.currentScreen);

        if (hasActiveStaff || inChat) {
            startAnimation();
        } else {
            stopAnimation();
        }

        // Add example staff when in chat
        if (inChat && activeStaffIds.isEmpty()) {
            String exampleName = "ExampleStaff";
            activeStaffIds.add(exampleName);
            if (!staffMap.containsKey(exampleName)) {
                GameProfile fakeProfile = new GameProfile(UUID.randomUUID(), exampleName);
                Identifier randomSkin = findRandomOnlineSkin();
                staffMap.put(exampleName, new StaffInfo(exampleName, fakeProfile, randomSkin));
            }
        }
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

        // Calculate dimensions
        float headerHeight = 15;
        float itemHeight = 11;
        float itemSpacing = 1.5f;
        float padding = 8;

        float targetWidth = 80;
        float targetHeight = headerHeight + 5;

        for (String id : activeStaffIds) {
            StaffInfo info = staffMap.get(id);
            if (info == null) continue;

            float nameWidth = Fonts.FONT67.getWidth(info.name, 6);
            targetWidth = Math.max(nameWidth + padding * 2 + 15, targetWidth);
            targetHeight += itemHeight + itemSpacing;
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
        Fonts.FONT67.draw("Staff", x + padding, y + headerHeight / 2 - 3 - 0.5f + 2, 6,
                HudTheme.text(bgAlpha));

        // Icon box
//        Render2D.rect(x + animatedWidth - padding - 12.3f, y + headerHeight / 2 - 4 + 2 - 2, 13, 13, new Color(0, 0, 0, 255).getRGB(), 3);
//        Render2D.outline(x + animatedWidth - padding - 12.3f, y + headerHeight / 2 - 4 + 2 - 2, 13, 13, 1, new Color(255, 255, 255, 30).getRGB(), 3);
        Fonts.iconsnew.draw("N", x + animatedWidth - padding - 10, y + headerHeight / 2 - 4 + 2 - 3, 12,
                HudTheme.accent(bgAlpha));

        Scissor.enable(x, y, animatedWidth, animatedHeight, 2);

        float currentY = y + headerHeight + 3;

        for (String id : activeStaffIds) {
            StaffInfo info = staffMap.get(id);
            if (info == null) continue;

            // Staff name
            Fonts.FONT67.draw(info.name, x + padding, currentY + itemHeight / 2 - 3 - 0.5f, 6,
                    HudTheme.text(bgAlpha));

            // Status indicator on the right
            float statusX = x + animatedWidth - padding - 8;
            float statusY = currentY + itemHeight / 2 - 2.5f;
            Render2D.rect(statusX, statusY, 5, 5,
                    HudTheme.accent(bgAlpha), 2.5f);

            currentY += itemHeight + itemSpacing;
        }

        Scissor.disable();
    }

    private void drawFace(Identifier skin, float faceX, float faceY, int alpha) {
        int color = new Color(255, 255, 255, alpha).getRGB();

        Render2D.texture(skin, faceX, faceY, FACE_SIZE, FACE_SIZE,
                8f / 64f, 8f / 64f, 16f / 64f, 16f / 64f, color, 0, 2f);

        float hatScale = 1.15f;
        float hatSize = FACE_SIZE * hatScale;
        float hatOffset = (hatSize - FACE_SIZE) / 2f;

        Render2D.texture(skin, faceX - hatOffset, faceY - hatOffset, hatSize, hatSize,
                40f / 64f, 8f / 64f, 48f / 64f, 16f / 64f, color, 0, 2f);

        Render2D.blur(faceX, faceY, 1, 1, 0f, 0, ColorUtil.rgba(0, 0, 0, 0));
    }
}

package neverlose.screens.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import neverlose.debob;
import neverlose.modules.impl.combat.Aura;
import neverlose.modules.impl.render.Hud;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.setting.Setting;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.util.render.Render2D;
import neverlose.util.render.font.Font;
import neverlose.util.render.font.Fonts;
import neverlose.util.render.item.ItemRender;

import java.awt.Color;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class Hud2Renderer {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final float PAD = 6f;

    private static final Map<ModuleStructure, ModuleAnim> MODULE_ANIM = new LinkedHashMap<>();

    private static long lastTickMs = System.currentTimeMillis();
    private static long lastRenderMs = System.currentTimeMillis();

    private static float targetAnim = 0f;
    private static LivingEntity lastTarget;
    private static float displayedHealth = 0f;
    private static float healthRatio = 0f;

    private static double lastX = 0, lastZ = 0, currentBps = 0, displayBps = 0;
    private static long lastSampleTime = 0;

    private Hud2Renderer() {
    }

    private static final class ModuleAnim {
        float prog;
        float target;
    }

    // ---------------------------------------------------------------- lifecycle

    public static void tick() {
        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastTickMs) / 1000f, 0.2f);
        lastTickMs = now;

        updateModules(dt);
        updateBps();

        boolean hasTarget = Aura.target != null || mc.currentScreen instanceof ChatScreen;
        targetAnim += ((hasTarget ? 1f : 0f) - targetAnim) * Math.min(1f, dt * 12f);
        if (targetAnim < 0.001f && !hasTarget) targetAnim = 0f;
    }

    private static void updateModules(float dt) {
        if (debob.getInstance() == null || debob.getInstance().getManager() == null
                || debob.getInstance().getManager().getModuleProvider() == null) {
            return;
        }

        List<ModuleStructure> modules = debob.getInstance().getManager().getModuleProvider().getModuleStructures();
        Set<ModuleStructure> active = new HashSet<>();
        for (ModuleStructure module : modules) {
            if (module.isState()) active.add(module);
        }

        for (ModuleStructure module : modules) {
            ModuleAnim anim = MODULE_ANIM.computeIfAbsent(module, k -> new ModuleAnim());
            anim.target = active.contains(module) ? 1f : 0f;
        }

        Iterator<Map.Entry<ModuleStructure, ModuleAnim>> it = MODULE_ANIM.entrySet().iterator();
        while (it.hasNext()) {
            ModuleAnim anim = it.next().getValue();
            anim.prog += (anim.target - anim.prog) * Math.min(1f, dt * 14f);
            if (anim.target == 0f && anim.prog < 0.02f) {
                it.remove();
            }
        }
    }

    private static void updateBps() {
        if (mc.player == null) {
            resetBps();
            return;
        }

        long now = System.nanoTime();
        double x = mc.player.getX();
        double z = mc.player.getZ();

        if (lastSampleTime == 0) {
            lastX = x;
            lastZ = z;
            lastSampleTime = now;
            return;
        }

        double elapsed = (now - lastSampleTime) / 1_000_000_000.0;
        double dx = x - lastX;
        double dz = z - lastZ;
        double distance = Math.sqrt(dx * dx + dz * dz);

        lastX = x;
        lastZ = z;
        lastSampleTime = now;

        if (elapsed <= 0.0) return;
        if (elapsed > 0.25 || distance > 16.0) {
            currentBps = 0.0;
            return;
        }

        double instantBps = distance / elapsed;
        if (instantBps < 0.03) {
            currentBps *= 0.45;
            if (currentBps < 0.03) currentBps = 0.0;
            return;
        }

        currentBps += (instantBps - currentBps) * 0.42;
    }

    private static void resetBps() {
        lastX = 0;
        lastZ = 0;
        currentBps = 0;
        displayBps = 0;
        lastSampleTime = 0;
    }

    // ---------------------------------------------------------------- render

    public static void render(DrawContext context, float tickDelta, int mouseX, int mouseY) {
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastRenderMs) / 1000f, 0.1f);
        lastRenderMs = now;

        displayBps += (currentBps - displayBps) * (1.0 - Math.pow(0.001, dt * 10f));
        if (Math.abs(displayBps - currentBps) < 0.01) displayBps = currentBps;

        float sw = Render2D.getFixedScaledWidth();
        float sh = Render2D.getFixedScaledHeight();

        drawWatermark(sw);
        drawArrayList(sw);
        drawPotions(sh);
        drawInfo(sh);
        drawArmor(context, sw, sh);
        drawTargetHud(context, sw, sh, tickDelta);
        drawNotifications(sw, sh);
    }

    // ---------------------------------------------------------------- watermark

    private static void drawWatermark(float sw) {
        float x = PAD, y = PAD, h = 20f, radius = 6f, pad = 4f;
        float badge = 12f;

        String fpsVal = String.valueOf(mc.getCurrentFps());
        String pingVal = String.valueOf(getPing());
        String time = LocalTime.now().format(TIME_FMT);

        Font title = Fonts.INTER_SEMIBOLD;
        Font stat = Fonts.INTER;

        float titleSize = 8f;
        float statSize = 6.8f;
        float labelSize = 6.2f;

        float titleW = title.getWidth("Dettex", titleSize);
        float fpsW = stat.getWidth(fpsVal, statSize);
        float fpsLblW = stat.getWidth(" fps", labelSize);
        float pingW = stat.getWidth(pingVal, statSize);
        float pingLblW = stat.getWidth(" ms", labelSize);
        float timeW = stat.getWidth(time, statSize);

        float w = pad * 2f + badge + 6f + titleW + 9f
                + fpsW + fpsLblW + 9f + pingW + pingLblW + 9f + timeW;

        panel(x, y, w, h, radius);

        float cy = y + h / 2f;
        float curX = x + pad;

        // badge
        float badgeY = cy - badge / 2f;
        Render2D.gradientRect(curX, badgeY, badge, badge,
                new int[]{accent(255), accent(255), accentDark(255), accentDark(255)}, 4f);
        drawCentered(Fonts.INTER_SEMIBOLD, "D", curX + badge / 2f, cy + 0.4f, 7.5f, 0xFFFFFFFF);
        curX += badge + 6f;

        // title
        title.draw("Dettex", curX, cy - title.getHeight(titleSize) / 2f, titleSize, HudTheme.text(255));
        curX += titleW + 9f;

        // divider
        Render2D.rect(curX - 4.5f, cy - 5f, 0.7f, 10f, HudTheme.muted(70));
        curX += 0f;

        // fps
        stat.draw(fpsVal, curX, cy - stat.getHeight(statSize) / 2f, statSize, HudTheme.accent(255));
        curX += fpsW;
        stat.draw(" fps", curX, cy - stat.getHeight(labelSize) / 2f, labelSize, HudTheme.muted(220));
        curX += fpsLblW + 9f;

        // ping
        stat.draw(pingVal, curX, cy - stat.getHeight(statSize) / 2f, statSize, HudTheme.accent(255));
        curX += pingW;
        stat.draw(" ms", curX, cy - stat.getHeight(labelSize) / 2f, labelSize, HudTheme.muted(220));
        curX += pingLblW + 9f;

        // time
        stat.draw(time, curX, cy - stat.getHeight(statSize) / 2f, statSize, HudTheme.text(235));
    }

    private static int getPing() {
        if (mc.getNetworkHandler() != null && mc.player != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) return entry.getLatency();
        }
        return 0;
    }

    // ---------------------------------------------------------------- arraylist

    private static void drawArrayList(float sw) {
        Font nameFont = Fonts.INTER_SEMIBOLD;
        Font suffixFont = Fonts.INTER;
        float nameSize = 7f;
        float suffixSize = 6.2f;
        float rowH = 9f;
        float xEnd = sw - PAD;
        float y = PAD;

        List<ModuleStructure> visible = new java.util.ArrayList<>();
        for (Map.Entry<ModuleStructure, ModuleAnim> entry : MODULE_ANIM.entrySet()) {
            if (entry.getValue().prog > 0.02f) visible.add(entry.getKey());
        }
        visible.sort((a, b) -> Float.compare(rowWidth(b, nameFont, suffixFont, nameSize, suffixSize),
                rowWidth(a, nameFont, suffixFont, nameSize, suffixSize)));

        // header
        int count = visible.size();
        String header = "MODULES";
        float hs = 6f;
        float hw = nameFont.getWidth(header, hs);
        float cw = suffixFont.getWidth(String.valueOf(count), hs);
        nameFont.draw(header, xEnd - hw - 5f - cw, y, hs, HudTheme.muted(210));
        suffixFont.draw(String.valueOf(count), xEnd - cw, y, hs, HudTheme.accent(255));
        Render2D.rect(xEnd - 0.7f, y + 1f, 0.7f, 6f, HudTheme.accent(200));

        float curY = y + 12f;
        for (ModuleStructure module : visible) {
            ModuleAnim anim = MODULE_ANIM.get(module);
            if (anim == null) continue;
            float prog = Math.max(0f, Math.min(1f, anim.prog));
            if (prog <= 0.02f) continue;

            String name = module.getName();
            String suffix = getSuffix(module);

            float nameW = nameFont.getWidth(name, nameSize);
            float suffixW = suffix.isEmpty() ? 0f : suffixFont.getWidth(suffix, suffixSize);
            float barW = 1.6f;
            float rowW = nameW + (suffix.isEmpty() ? 0f : 4f + suffixW) + 5f + barW;

            float slide = (1f - prog) * 7f;
            float xStart = xEnd - rowW + slide;
            float rowY = curY;

            Render2D.gradientRect(xStart - 4f, rowY - 1f, rowW + 7f, rowH,
                    new int[]{
                            rgba(12, 14, 18, (int) (110 * prog)),
                            rgba(12, 14, 18, (int) (110 * prog)),
                            rgba(8, 9, 12, (int) (70 * prog)),
                            rgba(8, 9, 12, (int) (70 * prog))
                    }, 3f);

            float ty = rowY + rowH / 2f - nameFont.getHeight(nameSize) / 2f;
            nameFont.draw(name, xStart, ty, nameSize, withAlpha(HudTheme.text(255), prog));
            if (!suffix.isEmpty()) {
                suffixFont.draw(suffix, xStart + nameW + 4f, rowY + rowH / 2f - suffixFont.getHeight(suffixSize) / 2f,
                        suffixSize, withAlpha(HudTheme.muted(220), prog));
            }

            Render2D.gradientRect(xEnd - barW, rowY + 1f, barW, rowH - 2f,
                    new int[]{
                            withAlpha(accent(255), prog),
                            withAlpha(accent(255), prog),
                            withAlpha(accentDark(255), prog),
                            withAlpha(accentDark(255), prog)
                    }, 0.8f);

            curY += rowH * prog;
        }
    }

    private static float rowWidth(ModuleStructure module, Font nameFont, Font suffixFont, float nameSize, float suffixSize) {
        String suffix = getSuffix(module);
        float w = nameFont.getWidth(module.getName(), nameSize);
        if (!suffix.isEmpty()) w += 4f + suffixFont.getWidth(suffix, suffixSize);
        return w;
    }

    private static String getSuffix(ModuleStructure module) {
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

    // ---------------------------------------------------------------- info

    private static void drawInfo(float sh) {
        float h = 15.5f;
        float y = sh - PAD - h;
        float x = PAD, radius = 6f, pad = 6f, icon = 10f, gap = 7f;

        String coords = (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ();
        Font f = Fonts.INTER_SEMIBOLD;
        float size = 6.5f;

        float coordsW = f.getWidth(coords, size);
        boolean showBps = Hud.getInstance() != null && Hud.getInstance().showBps.isValue();
        String bpsText = String.format(Locale.US, "%.2f b/s", Math.max(0.0, displayBps));
        float bpsW = f.getWidth(bpsText, size);

        float w = pad * 2f + icon + gap + coordsW;
        if (showBps) w += gap + icon + gap + bpsW;

        panel(x, y, w, h, radius);

        float cy = y + h / 2f;
        float curX = x + pad;

        Fonts.iconsnew.draw("M", curX, cy - (icon + 2f) / 2f - 1f, icon + 2f, HudTheme.accent(255));
        curX += icon + gap;
        f.draw(coords, curX, cy - f.getHeight(size) / 2f, size, HudTheme.text(255));
        curX += coordsW + gap;

        if (showBps) {
            Fonts.iconsnew.draw("D", curX, cy - (icon + 2f) / 2f - 1f, icon + 2f, HudTheme.accent(255));
            curX += icon + gap;
            f.draw(bpsText, curX, cy - f.getHeight(size) / 2f, size, HudTheme.text(255));
        }
    }

    // ---------------------------------------------------------------- potions

    private static void drawPotions(float sh) {
        List<StatusEffectInstance> effects = new java.util.ArrayList<>();
        if (mc.player != null) {
            for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
                if (effect.shouldShowIcon()) effects.add(effect);
            }
        }

        boolean chat = mc.currentScreen instanceof ChatScreen;
        if (effects.isEmpty() && !chat) return;

        Font f = Fonts.INTER_SEMIBOLD;
        float size = 6.5f;
        float pad = 6f, icon = 9f, rowH = 11f, headerH = 13f;

        float maxRow = 60f;
        List<String[]> rows = new java.util.ArrayList<>();
        for (StatusEffectInstance effect : effects) {
            String name = effect.getEffectType().value().getName().getString();
            int amp = effect.getAmplifier();
            if (amp > 0) name = name + " " + (amp + 1);
            String timer = formatDuration(effect.getDuration());
            maxRow = Math.max(maxRow, icon + 6f + f.getWidth(name, size) + 8f + f.getWidth(timer, size));
            rows.add(new String[]{name, timer});
        }
        if (rows.isEmpty() && chat) {
            rows.add(new String[]{"Example Effect", "05:00"});
        }

        float w = pad * 2f + maxRow;
        float h = headerH + 3f + rows.size() * rowH + 2f;
        float infoH = 15.5f;
        float y = sh - PAD - infoH - 6f - h;
        float x = PAD;

        panel(x, y, w, h, 6f);

        f.draw("Potions", x + pad, y + headerH / 2f - f.getHeight(size) / 2f, size, HudTheme.text(255));
        Fonts.iconsnew.draw("I", x + w - pad - 9f, y + headerH / 2f - 6f, 11f, HudTheme.accent(255));

        float curY = y + headerH + 2f;
        int i = 0;
        for (StatusEffectInstance effect : effects) {
            String[] row = rows.get(i++);
            int duration = effect.getDuration();
            int blink = getBlinkAlpha(duration, 255);

            Identifier tex = getEffectTexture(effect.getEffectType());
            Render2D.texture(tex, x + pad, curY + rowH / 2f - icon / 2f, icon, icon, withAlpha(0xFFFFFFFF, blink / 255f));

            float tx = x + pad + icon + 6f;
            f.draw(row[0], tx, curY + rowH / 2f - f.getHeight(size) / 2f, size, withAlpha(HudTheme.text(255), blink / 255f));

            float timerW = f.getWidth(row[1], size);
            f.draw(row[1], x + w - pad - timerW, curY + rowH / 2f - f.getHeight(size) / 2f, size,
                    withAlpha(HudTheme.accent(255), blink / 255f));
            curY += rowH;
        }
        // example row in chat
        if (effects.isEmpty() && !rows.isEmpty()) {
            String[] row = rows.get(0);
            f.draw(row[0], x + pad + icon + 6f, curY + rowH / 2f - f.getHeight(size) / 2f, size, HudTheme.text(200));
            float timerW = f.getWidth(row[1], size);
            f.draw(row[1], x + w - pad - timerW, curY + rowH / 2f - f.getHeight(size) / 2f, size, HudTheme.accent(200));
        }
    }

    private static String formatDuration(int ticks) {
        if (ticks == -1) return "\u221e";
        int total = ticks / 20;
        return String.format("%02d:%02d", total / 60, total % 60);
    }

    private static int getBlinkAlpha(int duration, int baseAlpha) {
        if (duration == -1 || duration > 100) return baseAlpha;
        double wave = Math.sin(System.currentTimeMillis() * 0.008);
        float factor = (float) ((wave + 1.0) / 2.0);
        int min = Math.max(60, baseAlpha - 150);
        return (int) (min + (baseAlpha - min) * (1.0f - factor));
    }

    private static Identifier getEffectTexture(RegistryEntry<StatusEffect> effect) {
        return effect.getKey()
                .map(RegistryKey::getValue)
                .map(id -> id.withPrefixedPath("mob_effect/"))
                .orElse(Identifier.ofVanilla("mob_effect/speed"));
    }

    // ---------------------------------------------------------------- armor

    private static void drawArmor(DrawContext context, float sw, float sh) {
        List<ItemStack> items = new java.util.ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = mc.player.getEquippedStack(slot);
            if (!stack.isEmpty()) items.add(stack);
        }
        ItemStack main = mc.player.getMainHandStack();
        ItemStack off = mc.player.getOffHandStack();
        if (!main.isEmpty()) items.add(main);
        if (!off.isEmpty()) items.add(off);
        if (items.isEmpty()) return;

        float scale = 1f;
        float item = 16f * scale;
        float spacing = item + 3f;
        float total = items.size() * spacing - 3f;
        float startX = sw / 2f - total / 2f;
        float y = sh - 48f;

        for (ItemStack stack : items) {
            ItemRender.drawItemWithContext(context, stack, startX, y, scale, 1f);

            if (stack.isDamageable() && stack.getMaxDamage() > 0) {
                float pct = 1f - (stack.getDamage() / (float) stack.getMaxDamage());
                pct = Math.max(0f, Math.min(1f, pct));
                int barY = (int) (y + item + 1f);
                Render2D.rect(startX, barY, item, 2.5f, rgba(0, 0, 0, 160), 1.2f);
                int color = pct > 0.6f ? rgba(80, 220, 120, 255) : pct > 0.3f ? rgba(235, 200, 80, 255) : rgba(235, 90, 90, 255);
                if (pct > 0.01f) {
                    Render2D.rect(startX, barY, item * pct, 2.5f, color, 1.2f);
                }
            }
            startX += spacing;
        }
    }

    // ---------------------------------------------------------------- target hud

    private static void drawTargetHud(DrawContext context, float sw, float sh, float tickDelta) {
        if (targetAnim <= 0.02f) return;

        LivingEntity target = Aura.target;
        if (target == null && mc.currentScreen instanceof ChatScreen) target = mc.player;
        if (target == null) return;

        if (target != lastTarget) {
            lastTarget = target;
            displayedHealth = target.getHealth();
            healthRatio = target.getHealth() / Math.max(1f, target.getMaxHealth());
        }

        float w = 152f, h = 44f, radius = 8f;
        float x = sw / 2f - w / 2f;
        float y = sh - 96f + (1f - targetAnim) * 6f;
        float alpha = targetAnim;

        // card
        if (mc.getWindow() != null && Hud.getInstance() != null && Hud.getInstance().hudBlur != null
                && Hud.getInstance().hudBlur.isValue()) {
            Render2D.blur(x, y, w, h, Hud.getInstance().hudBlurRadius.getValue(), radius, new Color(8, 10, 14, 40).getRGB());
        }
        Render2D.gradientRect(x, y, w, h,
                new int[]{withAlpha(HudTheme.panelTop(240), alpha), withAlpha(HudTheme.panelTop(240), alpha),
                        withAlpha(HudTheme.panel(240), alpha), withAlpha(HudTheme.panelBottom(240), alpha)}, radius);
        Render2D.outline(x, y, w, h, 0.6f, withAlpha(HudTheme.outline(70), alpha), radius);

        // face
        float face = 30f;
        float faceX = x + 7f, faceY = y + 7f;
        drawFace(target, faceX, faceY, face, tickDelta, alpha);

        float contentX = faceX + face + 8f;
        float rightX = x + w - 8f;

        String name = target.getName().getString();
        Font f = Fonts.INTER_SEMIBOLD;
        float nameSize = 8f;
        float maxNameW = (rightX - contentX) - 26f;
        float nameW = f.getWidth(name, nameSize);
        if (nameW > maxNameW && maxNameW > 0f) nameSize *= maxNameW / nameW;
        f.draw(name, contentX, y + 8f, nameSize, withAlpha(HudTheme.text(255), alpha));

        // distance
        float dist = mc.player != null ? mc.player.distanceTo(target) : 0f;
        String distText = String.format(Locale.US, "%.1fm", dist);
        f.draw(distText, rightX - f.getWidth(distText, 6.5f), y + 9f, 6.5f, withAlpha(HudTheme.accent(255), alpha));

        // health smoothing
        float hp = target.getHealth();
        float maxHp = Math.max(1f, target.getMaxHealth());
        healthRatio += ((hp / maxHp) - healthRatio) * 0.2f;
        displayedHealth += (hp - displayedHealth) * 0.2f;

        float barX = contentX;
        float barY = y + 27f;
        float barW = rightX - contentX;
        float barH = 7f;
        Render2D.rect(barX, barY, barW, barH, withAlpha(rgba(20, 16, 22, 255), 0.85f * alpha), barH / 2f);

        float healthPercent = Math.max(0f, Math.min(1f, healthRatio));
        if (healthPercent > 0.01f) {
            Render2D.gradientRect(barX, barY, barW * healthPercent, barH,
                    new int[]{withAlpha(accent(255), alpha), withAlpha(accent(255), alpha),
                            withAlpha(accentDark(255), alpha), withAlpha(accentDark(255), alpha)}, barH / 2f);
        }

        String hpText = String.valueOf((int) Math.ceil(displayedHealth));
        drawCentered(f, hpText, barX + barW / 2f, barY + barH / 2f, 6.5f, withAlpha(0xFFFFFFFF, alpha));
    }

    private static void drawFace(LivingEntity entity, float x, float y, float size, float tickDelta, float alpha) {
        EntityRenderer<? super LivingEntity, ?> base = mc.getEntityRenderDispatcher().getRenderer(entity);
        if (!(base instanceof LivingEntityRenderer<?, ?, ?>)) return;

        @SuppressWarnings("unchecked")
        LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?> renderer =
                (LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>) base;
        LivingEntityRenderState state = renderer.getAndUpdateRenderState(entity, tickDelta);
        Identifier texture = renderer.getTexture(state);

        int color = withAlpha(0xFFFFFFFF, alpha);
        float radius = 7f;
        Render2D.texture(texture, x, y, size, size, 8f / 64f, 8f / 64f, 16f / 64f, 16f / 64f, color, 0f, radius);

        float hatScale = 1.1f;
        float hatSize = size * hatScale;
        float off = (hatSize - size) / 2f;
        Render2D.texture(texture, x - off, y - off, hatSize, hatSize,
                40f / 64f, 8f / 64f, 48f / 64f, 16f / 64f, color, 0f, radius);
    }

    // ---------------------------------------------------------------- notifications

    private static void drawNotifications(float sw, float sh) {
        Notifications notifications = Notifications.getInstance();
        if (notifications == null) return;

        List<Notifications.Notification> list = notifications.getNotifications();
        if (list == null || list.isEmpty()) return;

        Font f = Fonts.INTER_SEMIBOLD;
        float size = 6.5f;
        float rowH = 15f, gap = 3f;
        float xEnd = sw - PAD;
        float baseY = sh - PAD;

        int index = 0;
        for (Notifications.Notification notification : list) {
            if (notification.text.contains("Notification")) continue;
            float prog = Math.max(0f, Math.min(1f, notification.anim.getOutput().floatValue()));
            if (prog <= 0.02f) continue;

            float textW = f.getWidth(notification.text, size);
            float w = 10f + textW + 16f;
            float x = xEnd - w + (1f - prog) * 16f;
            float y = baseY - (index + 1) * (rowH + gap) + gap;

            Render2D.gradientRect(x, y, w, rowH,
                    new int[]{withAlpha(HudTheme.panelTop(240), prog), withAlpha(HudTheme.panelTop(240), prog),
                            withAlpha(HudTheme.panel(240), prog), withAlpha(HudTheme.panelBottom(240), prog)}, 5f);

            Render2D.gradientRect(x + 0.8f, y + 2.5f, 1.6f, rowH - 5f,
                    new int[]{withAlpha(accent(255), prog), withAlpha(accent(255), prog),
                            withAlpha(accentDark(255), prog), withAlpha(accentDark(255), prog)}, 0.8f);

            f.draw(notification.text, x + 7f, y + rowH / 2f - f.getHeight(size) / 2f, size, withAlpha(HudTheme.text(255), prog));
            Fonts.iconsnew.draw("J", x + w - 12f, y + rowH / 2f - 4.5f, 9f, withAlpha(accent(255), prog));

            index++;
        }
    }

    // ---------------------------------------------------------------- helpers

    private static void panel(float x, float y, float w, float h, float radius) {
        Hud hud = Hud.getInstance();
        if (hud != null && hud.hudBlur != null && hud.hudBlur.isValue() && hud.hudBlurRadius != null) {
            Render2D.blur(x, y, w, h, hud.hudBlurRadius.getValue(), radius, new Color(8, 10, 14, 36).getRGB());
        }
        Render2D.gradientRect(x, y, w, h,
                new int[]{HudTheme.panelTop(238), HudTheme.panelTop(238), HudTheme.panel(238), HudTheme.panelBottom(238)}, radius);
        Render2D.outline(x, y, w, h, 0.6f, HudTheme.outline(58), radius);
    }

    private static void drawCentered(Font font, String text, float cx, float cy, float size, int color) {
        float w = font.getWidth(text, size);
        float h = font.getHeight(size);
        font.draw(text, cx - w / 2f, cy - h / 2f, size, color);
    }

    private static int accent(int alpha) {
        return HudTheme.accent(alpha);
    }

    private static int accentDark(int alpha) {
        int acc = HudTheme.accent(255);
        int r = (int) (((acc >> 16) & 0xFF) * 0.68f);
        int g = (int) (((acc >> 8) & 0xFF) * 0.68f);
        int b = (int) ((acc & 0xFF) * 0.68f);
        return rgba(r, g, b, alpha);
    }

    private static int withAlpha(int argb, float factor) {
        int a = (int) (((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, factor)));
        return (clamp(a) << 24) | (argb & 0xFFFFFF);
    }

    private static int rgba(int r, int g, int b, int a) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : Math.min(v, 255);
    }
}

package neverlose.modules.impl.render;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.PacketEvent;
import neverlose.events.impl.TickEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.screens.hud.Notifications;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class NotificationRuntime extends ModuleStructure {
    private static final long DEFAULT_COOLDOWN_MS = 4000L;
    private static final long DURABILITY_COOLDOWN_MS = 15000L;
    private static final RegistryEntry<StatusEffect>[] IMPORTANT_EFFECTS = new RegistryEntry[]{
            StatusEffects.STRENGTH,
            StatusEffects.SPEED,
            StatusEffects.RESISTANCE,
            StatusEffects.FIRE_RESISTANCE,
            StatusEffects.REGENERATION,
            StatusEffects.HASTE,
            StatusEffects.ABSORPTION
    };

    private static NotificationRuntime instance;

    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<String, Long> durabilityCooldowns = new HashMap<>();
    private final Set<RegistryEntry<StatusEffect>> activeImportantEffects = new HashSet<>();
    private long lastTickCheck;
    private boolean effectsInitialized;

    public NotificationRuntime() {
        super("NotificationRuntime", ModuleCategory.RENDER);
        instance = this;
    }

    public static void notifyShieldBreak(String targetName) {
        NotificationRuntime runtime = instance;
        if (runtime != null) {
            runtime.show(Hud.getInstance() != null ? Hud.getInstance().notifyShield : null,
                    "Shield break: " + targetName, "shield:" + targetName, DEFAULT_COOLDOWN_MS);
        }
    }

    @EventHandler
    private void onPacket(PacketEvent event) {
        if (event.getType() != PacketEvent.Type.RECEIVE || !(event.getPacket() instanceof GameMessageS2CPacket packet)) {
            return;
        }

        String message = packet.content().getString();
        if (message == null || message.isBlank()) {
            return;
        }

        String lower = message.toLowerCase(Locale.ROOT);
        Hud hud = Hud.getInstance();
        if (hud == null) {
            return;
        }
        if (isWarpAd(lower)) {
            show(hud.notifyWarps, "Warp ad detected", "warp:" + compact(message), 7000L);
        }
        if (isSpecRequest(lower)) {
            show(hud.notifySpec, "Spectator request", "spec:" + compact(message), 7000L);
        }
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            effectsInitialized = false;
            activeImportantEffects.clear();
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastTickCheck < 1000L) {
            return;
        }
        lastTickCheck = now;

        checkDurability();
        checkImportantEffects();
    }

    private void checkDurability() {
        Hud hud = Hud.getInstance();
        if (!isEnabled(hud, hud != null ? hud.notifyLowStrength : null)) {
            return;
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = mc.player.getEquippedStack(slot);
            checkStackDurability(slot.getName(), stack);
        }
        for (int slot = 0; slot < 9; slot++) {
            checkStackDurability("hotbar" + slot, mc.player.getInventory().getStack(slot));
        }
    }

    private void checkStackDurability(String slotKey, ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getMaxDamage() <= 0) {
            return;
        }

        int max = stack.getMaxDamage();
        int left = max - stack.getDamage();
        float ratio = left / (float) max;
        if (ratio > 0.12f && left > 20) {
            return;
        }

        long now = System.currentTimeMillis();
        String key = slotKey + ":" + stack.getItem().toString();
        long next = durabilityCooldowns.getOrDefault(key, 0L);
        if (now < next) {
            return;
        }

        durabilityCooldowns.put(key, now + DURABILITY_COOLDOWN_MS);
        notification("Low durability: " + stack.getName().getString() + " (" + left + ")", 3500L);
    }

    private void checkImportantEffects() {
        Hud hud = Hud.getInstance();
        if (!isEnabled(hud, hud != null ? hud.notifyEffects : null)) {
            effectsInitialized = false;
            activeImportantEffects.clear();
            return;
        }

        Set<RegistryEntry<StatusEffect>> current = new HashSet<>();
        for (RegistryEntry<StatusEffect> effect : IMPORTANT_EFFECTS) {
            StatusEffectInstance instance = mc.player.getStatusEffect(effect);
            if (instance != null && instance.getDuration() > 0) {
                current.add(effect);
            }
        }

        if (effectsInitialized) {
            for (RegistryEntry<StatusEffect> effect : activeImportantEffects) {
                if (!current.contains(effect)) {
                    String name = effect.value().getName().getString();
                    show(hud.notifyEffects, "Effect ended: " + name, "effect:" + name, 5000L);
                }
            }
        }

        effectsInitialized = true;
        activeImportantEffects.clear();
        activeImportantEffects.addAll(current);
    }

    private boolean isWarpAd(String message) {
        return message.contains("/warp")
                || message.contains(" warp")
                || message.contains("warp ")
                || message.contains("\u0432\u0430\u0440\u043f")
                || message.contains("\u0432\u0430\u0440\u043f\u044b");
    }

    private boolean isSpecRequest(String message) {
        return message.contains("spectat")
                || message.contains("spec ")
                || message.contains("\u043d\u0430\u0431\u043b\u044e\u0434")
                || message.contains("\u0441\u043b\u0435\u0434\u0438\u0442\u044c")
                || message.contains("\u043f\u043e\u0441\u043c\u043e\u0442\u0440\u0438");
    }

    private boolean isEnabled(Hud hud, BooleanSetting setting) {
        return hud != null
                && hud.isState()
                && hud.interfaceSettings.isSelected("Notifications")
                && setting != null
                && setting.isValue()
                && Notifications.getInstance() != null;
    }

    private void show(BooleanSetting setting, String text, String key, long cooldownMs) {
        Hud hud = Hud.getInstance();
        if (!isEnabled(hud, setting)) {
            return;
        }

        long now = System.currentTimeMillis();
        long next = cooldowns.getOrDefault(key, 0L);
        if (now < next) {
            return;
        }

        cooldowns.put(key, now + cooldownMs);
        notification(text, 3200L);
    }

    private void notification(String text, long duration) {
        Notifications notifications = Notifications.getInstance();
        if (notifications != null) {
            notifications.addNotification(text, duration);
        }
    }

    private String compact(String text) {
        String cleaned = text.replaceAll("\\s+", " ").trim();
        return cleaned.length() > 40 ? cleaned.substring(0, 40) : cleaned;
    }
}

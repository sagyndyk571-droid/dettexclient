package neverlose.modules.impl.misc;

import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.PacketEvent;
import neverlose.events.impl.TickEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.modules.module.setting.implement.TextSetting;
import neverlose.util.repository.friend.FriendUtils;

import java.util.Locale;
import java.util.Random;

public class AutoMassage extends ModuleStructure {
    private static final String RU_KILL_PHRASE = "\u0412\u044b \u0443\u0431\u0438\u043b\u0438";
    private static final String RU_KILL_PHRASE_LOWER = "\u0432\u044b \u0443\u0431\u0438\u043b\u0438";
    private static final String[] RANDOM_TEMPLATES = {
            "! лох гетни neverlose.tech {player}",
            "! выебал твою мертвую мамашу by neverlose.tech {player}",
            "! pounch client пососал от лучшего папочки дебоба {player}",
            "! опять упал в колени и скушал мою сперму {player}"
    };
    private static final String[] TAUNT_TEMPLATES = {
            "! -ezz {player} фри гетай neverlose.tech и не сливайся ",
            "! -ezz {player} мой элитро таргет тебя заебашил нищий гетай neverlose.tech",
            "! -ezz {player} убил тебя c neverlose.tech гетни тоже",
            "! -ezz {player} лох гетни neverlose.tech",
            "! -ezz {player} фришный кил нищий хуесос гетни neverlose.tech",
            "! -ezz {player} гетай neverlose.tech",
            "! -ezz {player} твоя мама даже купила neverlose.tech а ты нет"
    };

    private final TextSetting template = new TextSetting("Message", "Use {player} for victim name")
            .setText("! -безмамный выблядок {player} гетай neverlose.tech")
            .setMax(128);

    private final BooleanSetting randomMessage = new BooleanSetting("Random", "Use random message template")
            .setValue(false);

    private final SelectSetting style = new SelectSetting("Style", "Random message style")
            .value("Taunt", "Normal")
            .selected("Taunt")
            .visible(randomMessage::isValue);

    private final BooleanSetting ignoreFriends = new BooleanSetting("Ignore friends", "Do not send message after killing friends")
            .setValue(true);

    private final SliderSettings delay = new SliderSettings("Delay", "Delay before sending message")
            .range(0, 3000)
            .setValue(250);

    private final Random random = new Random();
    private String pendingMessage;
    private String lastVictim = "";
    private long sendAtMillis;
    private long lastTriggerMillis;

    public AutoMassage() {
        super("AutoMassage", "Sends a chat message after kill", ModuleCategory.MISC);
        settings(template, randomMessage, style, ignoreFriends, delay);
    }

    @Override
    public void deactivate() {
        clearPending();
    }

    @EventHandler
    private void onPacket(PacketEvent event) {
        if (mc.player == null || mc.world == null || event.getType() != PacketEvent.Type.RECEIVE) {
            return;
        }

        if (!(event.getPacket() instanceof GameMessageS2CPacket packet)) {
            return;
        }

        String victim = extractVictim(packet.content().getString());
        if (victim == null || victim.equalsIgnoreCase(mc.player.getGameProfile().name())) {
            return;
        }

        if (ignoreFriends.isValue() && FriendUtils.isFriend(victim)) {
            return;
        }

        long now = System.currentTimeMillis();
        if (victim.equalsIgnoreCase(lastVictim) && now - lastTriggerMillis < 1500L) {
            return;
        }

        lastVictim = victim;
        lastTriggerMillis = now;
        pendingMessage = buildMessage(victim);
        sendAtMillis = now + (long) delay.getValue();
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (pendingMessage == null || mc.player == null || mc.player.networkHandler == null) {
            return;
        }

        if (System.currentTimeMillis() < sendAtMillis) {
            return;
        }

        sendMessage(pendingMessage);
        clearPending();
    }

    private String extractVictim(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return null;
        }

        String message = rawMessage.replaceAll("\u00a7.", "").trim();
        String lower = message.toLowerCase(Locale.ROOT);
        int index = lower.indexOf(RU_KILL_PHRASE_LOWER);
        int phraseLength = RU_KILL_PHRASE.length();

        if (index == -1) {
            index = lower.indexOf("тупой фрик умер, гетай neverlose.tech");
            if (index == -1) {
                index = lower.indexOf("тупой фрик умер, гетай neverlose.tech");
            }
            phraseLength = "тупой фрик умер, гетай neverlose.tech".length();
        }

        if (index == -1) {
            return null;
        }

        String tail = message.substring(index + phraseLength)
                .replaceAll("^[!.,:;\\s]+", "")
                .trim();
        if (tail.isEmpty()) {
            return null;
        }

        String name = tail.split("\\s+")[0].replaceAll("[!.,:;]+$", "").trim();
        return name.isEmpty() ? null : name;
    }

    private String buildMessage(String victim) {
        String source = template.getText();
        if (randomMessage.isValue()) {
            String[] templates = style.isSelected("Taunt") ? TAUNT_TEMPLATES : RANDOM_TEMPLATES;
            source = templates[random.nextInt(templates.length)];
        }
        if (source == null || source.isBlank()) {
            source = "! gg {player}";
        }
        return source.replace("{player}", victim).trim();
    }

    private void sendMessage(String message) {
        if (message.startsWith("/")) {
            mc.player.networkHandler.sendChatCommand(message.substring(1));
        } else {
            mc.player.networkHandler.sendChatMessage(message);
        }
    }

    private void clearPending() {
        pendingMessage = null;
        sendAtMillis = 0L;
    }
}

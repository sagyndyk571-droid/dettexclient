package neverlose.util.security;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.lwjgl.glfw.GLFW;
import neverlose.debob;
import neverlose.modules.module.ModuleRepository;
import neverlose.modules.module.ModuleStructure;
import neverlose.screens.clickgui.ClickGui;
import neverlose.screens.menu.MainMenuScreen;

import java.security.SecureRandom;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class ClientUnhook {
    private static final String FIXED_RECOVERY_KEY = "debob";
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> previouslyEnabled = new LinkedHashSet<>();

    private static boolean hidden;
    private static String recoveryKey = "";
    private static String typedBuffer = "";
    private static long lastTypedAt;

    private ClientUnhook() {
    }

    public static boolean isHidden() {
        return hidden;
    }

    public static String prepareRecoveryKey() {
        recoveryKey = FIXED_RECOVERY_KEY;
        typedBuffer = "";
        return recoveryKey;
    }

    public static String getRecoveryKey() {
        if (recoveryKey == null || recoveryKey.isEmpty()) {
            return prepareRecoveryKey();
        }
        return recoveryKey;
    }

    public static void enterHiddenMode() {
        if (hidden) return;
        if (recoveryKey == null || recoveryKey.isEmpty()) {
            prepareRecoveryKey();
        }

        previouslyEnabled.clear();
        ModuleRepository repository = getRepository();
        if (repository != null) {
            for (ModuleStructure module : repository.allModules()) {
                if (module.isState()) {
                    previouslyEnabled.add(module.getName());
                }
            }

            ModuleStructure.runWithoutToggleFeedback(() -> {
                for (ModuleStructure module : repository.allModules()) {
                    if (module.isState()) {
                        module.setState(false);
                    }
                }
            });
        }

        hidden = true;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof ClickGui) {
            client.setScreen(null);
        } else if (client.currentScreen instanceof MainMenuScreen) {
            client.setScreen(new TitleScreen());
        }
    }

    public static void restore() {
        if (!hidden) return;
        hidden = false;
        typedBuffer = "";

        ModuleRepository repository = getRepository();
        if (repository != null && !previouslyEnabled.isEmpty()) {
            ModuleStructure.runWithoutToggleFeedback(() -> {
                for (ModuleStructure module : repository.allModules()) {
                    if (previouslyEnabled.contains(module.getName())) {
                        module.setState(true);
                    }
                }
            });
        }

        previouslyEnabled.clear();
    }

    public static boolean handleKeyPress(int key) {
        if (!hidden) return false;

        String token = keyToToken(key);
        if (token.isEmpty()) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - lastTypedAt > 2500L) {
            typedBuffer = "";
        }
        lastTypedAt = now;

        typedBuffer = (typedBuffer + token).toUpperCase(Locale.ROOT);
        if (typedBuffer.length() > 24) {
            typedBuffer = typedBuffer.substring(typedBuffer.length() - 24);
        }

        String keyValue = getRecoveryKey().toUpperCase(Locale.ROOT);
        if (typedBuffer.endsWith(keyValue) || typedBuffer.endsWith(FIXED_RECOVERY_KEY.toUpperCase(Locale.ROOT))) {
            restore();
            return true;
        }
        return false;
    }

    private static String keyToToken(int key) {
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) {
            return String.valueOf((char) ('A' + key - GLFW.GLFW_KEY_A));
        }
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) {
            return String.valueOf((char) ('0' + key - GLFW.GLFW_KEY_0));
        }
        if (key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_9) {
            return String.valueOf((char) ('0' + key - GLFW.GLFW_KEY_KP_0));
        }
        return "";
    }

    private static String generateKey() {
        StringBuilder builder = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }

    private static ModuleRepository getRepository() {
        debob instance = debob.getInstance();
        if (instance == null || instance.getManager() == null) {
            return null;
        }
        return instance.getManager().getModuleRepository();
    }
}

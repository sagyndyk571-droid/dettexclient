package neverlose.screens.clickgui.impl.configs.handler;

import lombok.Getter;
import lombok.Setter;
import neverlose.util.config.ConfigSystem;
import neverlose.util.config.impl.ConfigPath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ConfigDataHandler {

    private final List<String> configs = new ArrayList<>();
    private final ConfigAnimationHandler animationHandler;

    private String selectedConfig = null;
    private boolean isCreating = false;
    private String newConfigName = "";

    private double scrollOffset = 0;
    private double targetScrollOffset = 0;
    private float scrollTopFade = 0f;
    private float scrollBottomFade = 0f;

    public ConfigDataHandler(ConfigAnimationHandler animationHandler) {
        this.animationHandler = animationHandler;
    }

    public void refreshConfigs() {
        List<String> oldConfigs = new ArrayList<>(configs);
        configs.clear();
        try {
            Path configDir = ConfigPath.getConfigDirectory();
            if (Files.exists(configDir)) {
                Files.list(configDir)
                        .filter(path -> path.toString().endsWith(".json"))
                        .forEach(path -> {
                            String name = path.getFileName().toString();
                            String configName = name.substring(0, name.length() - 5);
                            if (!configName.equalsIgnoreCase("autoconfig")) {
                                configs.add(configName);
                            }
                        });
            }
        } catch (IOException ignored) {}

        for (String config : configs) {
            if (!oldConfigs.contains(config)) {
                animationHandler.getItemAppearAnimations().put(config, 0f);
            }
        }
    }

    public void updateScroll(float deltaTime) {
        scrollOffset += (targetScrollOffset - scrollOffset) * 12f * deltaTime;
    }

    public void updateScrollFades(float visibleHeight) {
        float contentHeight = configs.size() * 39f;

        if (contentHeight <= visibleHeight) {
            scrollTopFade = 0f;
            scrollBottomFade = 0f;
            return;
        }

        float maxScroll = contentHeight - visibleHeight;
        scrollTopFade = (float) Math.min(1f, -scrollOffset / 20f);
        scrollBottomFade = (float) Math.min(1f, (maxScroll + scrollOffset) / 20f);
    }

    public void handleScroll(double vertical, float visibleHeight) {
        float contentHeight = configs.size() * 39f;
        float maxScroll = Math.max(0, contentHeight - visibleHeight);
        targetScrollOffset += vertical * 25;
        targetScrollOffset = Math.max(-maxScroll, Math.min(0, targetScrollOffset));
    }

    public boolean saveConfig(String name) {
        if (name.equalsIgnoreCase("autoconfig")) {
            return false;
        }

        try {
            String normalizedName = ConfigPath.normalizeConfigName(name);
            if (Files.exists(ConfigPath.getConfigFile(normalizedName))) {
                return false;
            }

            boolean saved = ConfigSystem.getInstance().save(normalizedName);
            refreshConfigs();
            return saved;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean loadConfig(String name) {
        try {
            return ConfigSystem.getInstance().load(name);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean refreshConfig(String name) {
        try {
            if (!ConfigSystem.getInstance().exists(name)) {
                return false;
            }

            return ConfigSystem.getInstance().save(name);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteConfig(String name) {
        try {
            Path configDir = ConfigPath.getConfigDirectory();
            Path configFile = configDir.resolve(name + ".json");

            if (Files.exists(configFile)) {
                Files.delete(configFile);
                if (name.equals(selectedConfig)) {
                    selectedConfig = null;
                }
                refreshConfigs();
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void toggleCreating() {
        isCreating = !isCreating;
        if (!isCreating) {
            newConfigName = "";
        }
    }

    public void appendChar(char chr) {
        if (newConfigName.length() < 32 && (Character.isLetterOrDigit(chr) || chr == '_' || chr == '-')) {
            newConfigName += chr;
        }
    }

    public void removeLastChar() {
        if (!newConfigName.isEmpty()) {
            newConfigName = newConfigName.substring(0, newConfigName.length() - 1);
        }
    }

    public void clearNewConfigName() {
        newConfigName = "";
    }
}

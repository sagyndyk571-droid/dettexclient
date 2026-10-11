package neverlose.util.config;

import neverlose.util.config.impl.ConfigFileHandler;
import neverlose.util.config.impl.ConfigPath;
import neverlose.util.config.impl.ConfigSerializer;
import neverlose.util.config.impl.autosaver.ConfigAutoSaver;
import neverlose.util.config.impl.consolelogger.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;



public class ConfigSystem {

    private static ConfigSystem instance;

    private final ConfigSerializer serializer;
    private final ConfigFileHandler fileHandler;
    private final ConfigAutoSaver autoSaver;
    private final AtomicBoolean initialized;
    private final AtomicBoolean saving;

    public ConfigSystem() {
        instance = this;
        this.serializer = new ConfigSerializer();
        this.fileHandler = new ConfigFileHandler();
        this.autoSaver = new ConfigAutoSaver(this::save);
        this.initialized = new AtomicBoolean(false);
        this.saving = new AtomicBoolean(false);
    }

    public static ConfigSystem getInstance() {
        return instance;
    }

    public void init() {
        if (initialized.compareAndSet(false, true)) {
            ConfigPath.init();
            fileHandler.createDirectories();
            load();
            autoSaver.start();
            registerShutdownHook();
            Logger.success("AutoConfiguration: System initialized!");
        }
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Logger.info("AutoConfiguration: Shutdown detected, saving...");
            shutdown();
        }, "Neverlose-ConfigShutdown"));
    }

    public void save() {
        save("autoconfig");
    }

    public boolean save(String name) {
        if (!initialized.get()) {
            return false;
        }
        if (!ConfigPath.isValidConfigName(name)) {
            Logger.error("AutoConfiguration: Invalid config name!");
            return false;
        }
        if (!saving.compareAndSet(false, true)) {
            return false;
        }
        try {
            String normalizedName = ConfigPath.normalizeConfigName(name);
            String data = serializer.serialize();
            boolean success = fileHandler.write(normalizedName, data);
            if (success) {
                Logger.success("AutoConfiguration: " + normalizedName + ".json saved successfully!");
            } else {
                Logger.error("AutoConfiguration: " + normalizedName + ".json save failed!");
            }
            return success;
        } catch (Exception e) {
            Logger.error("AutoConfiguration: Save error! " + e.getMessage());
            return false;
        } finally {
            saving.set(false);
        }
    }

    public CompletableFuture<Void> saveAsync() {
        return CompletableFuture.runAsync(this::save);
    }

    public void load() {
        load("autoconfig");
    }

    public boolean load(String name) {
        if (!ConfigPath.isValidConfigName(name)) {
            Logger.error("AutoConfiguration: Invalid config name!");
            return false;
        }

        String normalizedName = ConfigPath.normalizeConfigName(name);
        if (!fileHandler.exists(normalizedName)) {
            if (normalizedName.equalsIgnoreCase("autoconfig")) {
                Logger.info("AutoConfiguration: No config found, creating new...");
                save();
            }
            return false;
        }

        try {
            String data = fileHandler.read(normalizedName);
            if (data != null && !data.isEmpty()) {
                serializer.deserialize(data);
                if (!normalizedName.equalsIgnoreCase("autoconfig")) {
                    fileHandler.write("autoconfig", data);
                }
                Logger.success("AutoConfiguration: " + normalizedName + ".json loaded successfully!");
                return true;
            }
        } catch (Exception e) {
            Logger.error("AutoConfiguration: Load error! " + e.getMessage());
        }
        return false;
    }

    public void shutdown() {
        if (!initialized.get()) {
            return;
        }
        autoSaver.shutdown();
        save();
        Logger.success("AutoConfiguration: Shutdown complete!");
    }

    public void reload() {
        load();
        Logger.success("AutoConfiguration: Config reloaded!");
    }

    public boolean exists(String name) {
        return ConfigPath.isValidConfigName(name) && fileHandler.exists(ConfigPath.normalizeConfigName(name));
    }

    public List<String> listConfigs(boolean includeAutoConfig) {
        List<String> configs = new ArrayList<>();
        try {
            Path configDir = ConfigPath.getConfigDirectory();
            if (!Files.exists(configDir)) {
                return configs;
            }

            try (var stream = Files.list(configDir)) {
                stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .map(path -> path.getFileName().toString())
                        .map(name -> name.substring(0, name.length() - 5))
                        .filter(name -> includeAutoConfig || !name.equalsIgnoreCase("autoconfig"))
                        .sorted(Comparator.comparing(String::toLowerCase))
                        .forEach(configs::add);
            }
        } catch (Exception ignored) {
        }
        return configs;
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public boolean isSaving() {
        return saving.get();
    }

    public ConfigAutoSaver getAutoSaver() {
        return autoSaver;
    }
}

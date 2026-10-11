package neverlose.util.config.impl;

import java.nio.file.Path;
import java.nio.file.Paths;



public class ConfigPath {

    private static final String ROOT_DIR = "Neverlose";
    private static final String CONFIG_DIR = "configs";
    private static final String AUTO_DIR = "autocfg";
    private static final String CONFIG_FILE = "autoconfig.json";

    private static Path runDirectory;

    public static void init() {
        runDirectory = Paths.get("").toAbsolutePath();
    }

    public static Path getConfigDirectory() {
        if (runDirectory == null) {
            init();
        }
        return runDirectory.resolve(ROOT_DIR).resolve(CONFIG_DIR).resolve(AUTO_DIR);
    }

    public static Path getConfigFile() {
        return getConfigDirectory().resolve(CONFIG_FILE);
    }

    public static Path getConfigFile(String name) {
        return getConfigDirectory().resolve(toFileName(name));
    }

    public static String normalizeConfigName(String name) {
        if (name == null) {
            return "";
        }

        String normalized = name.trim();
        if (normalized.endsWith(".json")) {
            normalized = normalized.substring(0, normalized.length() - 5);
        }
        return normalized;
    }

    public static boolean isValidConfigName(String name) {
        String normalized = normalizeConfigName(name);
        return !normalized.isEmpty() && normalized.matches("[A-Za-z0-9_-]{1,32}");
    }

    private static String toFileName(String name) {
        String normalized = normalizeConfigName(name);
        if (normalized.isEmpty() || normalized.equalsIgnoreCase("autoconfig")) {
            return CONFIG_FILE;
        }
        if (!isValidConfigName(normalized)) {
            throw new IllegalArgumentException("Invalid config name: " + name);
        }
        return normalized + ".json";
    }
}

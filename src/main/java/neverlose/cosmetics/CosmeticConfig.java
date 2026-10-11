package neverlose.cosmetics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CosmeticConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = CosmeticPaths.GAME_DIRECTORY
            .resolve("Neverlose")
            .resolve("configs")
            .resolve("cosmetics.json");

    private CosmeticConfig() {
    }

    public static State load() {
        try {
            if (!Files.exists(FILE)) {
                return new State(true, null, null, null);
            }
            JsonObject json = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
            return new State(
                    !json.has("enabled") || json.get("enabled").getAsBoolean(),
                    stringOrNull(json, "model"),
                    stringOrNull(json, "pet"),
                    stringOrNull(json, "accessory")
            );
        } catch (Exception ignored) {
            return new State(true, null, null, null);
        }
    }

    public static void save(CosmeticManager manager) {
        try {
            Files.createDirectories(FILE.getParent());
            JsonObject json = new JsonObject();
            json.addProperty("enabled", manager.isEnabled());
            addModel(json, "model", manager.getActiveModel());
            addModel(json, "pet", manager.getActivePet());
            addModel(json, "accessory", manager.getActiveAccessory());
            Files.writeString(FILE, GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private static void addModel(JsonObject json, String key, CosmeticModel model) {
        if (model == null) {
            json.add(key, null);
        } else {
            json.addProperty(key, model.getId());
        }
    }

    private static String stringOrNull(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : null;
    }

    public record State(boolean enabled, String model, String pet, String accessory) {
    }
}

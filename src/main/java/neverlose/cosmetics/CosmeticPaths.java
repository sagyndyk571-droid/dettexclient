package neverlose.cosmetics;

import net.minecraft.client.MinecraftClient;

import java.nio.file.Path;

public final class CosmeticPaths {
    public static final String MOD_ID = "neverlose";
    public static final Path GAME_DIRECTORY = MinecraftClient.getInstance().runDirectory.toPath();
    public static final Path COSMETICS_DIRECTORY = GAME_DIRECTORY.resolve("Neverlose").resolve("cosmetics");

    private CosmeticPaths() {
    }
}

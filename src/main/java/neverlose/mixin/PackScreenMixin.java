package neverlose.mixin;

import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.util.Util;
import neverlose.util.security.ClientUnhook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Mixin(PackScreen.class)
public class PackScreenMixin {
    @Inject(method = "method_29670", at = @At("HEAD"), cancellable = true, require = 0)
    private void openSafeModePackFolder(ButtonWidget button, CallbackInfo ci) {
        if (!ClientUnhook.isHidden()) {
            return;
        }

        Path path = getTLauncherGamePath();
        try {
            Files.createDirectories(path);
        } catch (IOException ignored) {
        }

        Util.getOperatingSystem().open(path);
        ci.cancel();
    }

    private Path getTLauncherGamePath() {
        String home = System.getProperty("user.home");
        if (home == null || home.isBlank()) {
            return Paths.get(".tlauncher", "legacy", "Minecraft", "game").toAbsolutePath();
        }
        return Paths.get(home, ".tlauncher", "legacy", "Minecraft", "game");
    }
}

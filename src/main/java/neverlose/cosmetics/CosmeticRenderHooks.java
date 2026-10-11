package neverlose.cosmetics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

public final class CosmeticRenderHooks {
    private CosmeticRenderHooks() {
    }

    public static boolean replacesVanillaModel(LivingEntityRenderState state) {
        CosmeticManager manager = CosmeticManager.getInstance();
        manager.ensureInitialized();
        CosmeticModel model = manager.getActiveModel();
        if (!manager.isEnabled() || model == null || !model.isRenderable()) return false;
        if (!(state instanceof PlayerEntityRenderState playerState)) return false;

        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null && playerState.id == client.player.getId();
    }
}

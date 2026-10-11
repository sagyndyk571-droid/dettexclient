package neverlose.cosmetics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import neverlose.util.security.ClientUnhook;

public class CosmeticFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    private final MinecraftClient client = MinecraftClient.getInstance();

    public CosmeticFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                       PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        if (ClientUnhook.isHidden()) {
            return;
        }

        CosmeticManager manager = CosmeticManager.getInstance();
        manager.ensureInitialized();
        if (!manager.isEnabled() || client.world == null || client.player == null) return;

        Entity entity = client.world.getEntityById(state.id);
        if (!(entity instanceof PlayerEntity player) || player != client.player) return;
        if (client.options.getPerspective().isFirstPerson()) return;

        float tickDelta = Math.min(1f, client.getRenderTickCounter().getDynamicDeltaTicks());
        renderCosmetic(matrices, queue, light, manager.getActiveModel(), CosmeticType.MODEL, tickDelta, player);
        renderCosmetic(matrices, queue, light, manager.getActivePet(), CosmeticType.PET, tickDelta, player);
        renderCosmetic(matrices, queue, light, manager.getActiveAccessory(), CosmeticType.ACCESSORY, tickDelta, player);
    }

    private void renderCosmetic(MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                                CosmeticModel model, CosmeticType type, float tickDelta, PlayerEntity player) {
        if (model == null || type == CosmeticType.ACCESSORY && !model.isBodyOnlyAccessory()) return;

        if (model.isRenderFaulted()) return;

        matrices.push();
        try {
            // Feature renderers receive the vanilla player-space flip and model-origin offset.
            // Cancel both so Blockbench coordinates use the same upright space as the source renderer.
            matrices.translate(0f, 1.501f, 0f);
            matrices.scale(-1f, -1f, 1f);
            CosmeticAttachmentTransforms.apply(matrices, type, model);
            CosmeticManager.getInstance().getAnimator().apply(
                    model,
                    tickDelta,
                    player.getHandSwingProgress(tickDelta),
                    player.limbAnimator.getAnimationProgress(tickDelta),
                    player.limbAnimator.getAmplitude(tickDelta)
            );
            CosmeticManager.getInstance().getRenderer().render(
                    matrices,
                    queue,
                    model,
                    light,
                    OverlayTexture.DEFAULT_UV,
                    false,
                    CosmeticRenderSpace.ATTACHMENT
            );
        } catch (RuntimeException | LinkageError error) {
            model.markRenderFault(error);
        } finally {
            matrices.pop();
        }
    }
}

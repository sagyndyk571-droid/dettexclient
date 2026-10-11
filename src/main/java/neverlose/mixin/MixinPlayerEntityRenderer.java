package neverlose.mixin;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import neverlose.modules.impl.render.ChinaHatFeatureRenderer;
import neverlose.cosmetics.CosmeticFeatureRenderer;
import neverlose.modules.impl.render.Esp;

@Mixin(PlayerEntityRenderer.class)
public abstract class MixinPlayerEntityRenderer {
    @SuppressWarnings("unchecked")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void addChinaHatLayer(EntityRendererFactory.Context context, boolean slim, CallbackInfo ci) {
        ((LivingEntityRendererAccessor<PlayerEntityRenderState, PlayerEntityModel>) (Object) this)
                .invokeAddFeature(new ChinaHatFeatureRenderer((FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>) (Object) this));
        ((LivingEntityRendererAccessor<PlayerEntityRenderState, PlayerEntityModel>) (Object) this)
                .invokeAddFeature(new CosmeticFeatureRenderer((FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>) (Object) this));
    }

    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void hideBelownameScore(PlayerEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (Esp.getInstance().isState()) {
            ci.cancel();
        }
    }

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
    private void onUpdateRenderState(PlayerLikeEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (Esp.getInstance().isState()) {
            state.playerName = null;
            state.displayName = null;
        }
    }
}

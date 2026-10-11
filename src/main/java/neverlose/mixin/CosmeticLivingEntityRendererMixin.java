package neverlose.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import neverlose.cosmetics.CosmeticRenderHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class CosmeticLivingEntityRendererMixin {
    @Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true)
    private void neverlose$hideVanillaPlayerModel(LivingEntityRenderState state,
                                                   boolean showBody,
                                                   boolean translucent,
                                                   boolean showOutline,
                                                   CallbackInfoReturnable<RenderLayer> cir) {
        if (CosmeticRenderHooks.replacesVanillaModel(state)) {
            cir.setReturnValue(null);
        }
    }

}

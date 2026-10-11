package neverlose.mixin;

import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.CloudRenderer;
import net.minecraft.util.math.Vec3d;
import neverlose.modules.impl.render.FpsBoost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CloudRenderer.class, priority = 500)
public abstract class CloudRendererMixin {

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void neverlose$noClouds(int color, CloudRenderMode cloudRenderMode, float cloudHeight,
                                    Vec3d cameraPos, long timeMilliseconds, float tickProgress, CallbackInfo ci) {
        FpsBoost fpsBoost = FpsBoost.getInstance();
        if (fpsBoost != null && fpsBoost.shouldSkipClouds()) {
            ci.cancel();
        }
    }
}
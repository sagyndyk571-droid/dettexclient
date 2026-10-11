package neverlose.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.RenderTickCounter;
import neverlose.debob;
import neverlose.util.timer.TimerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTickCounter.Dynamic.class)
public abstract class RenderTickCounterDynamicMixin {
    @ModifyExpressionValue(
            method = "beginRenderTick(J)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/floats/FloatUnaryOperator;apply(F)F"
            )
    )
    private float neverlose$applyTimer(float millisPerTick) {
        return millisPerTick / TimerManager.getTimer();
    }

    @Inject(method = "beginRenderTick(J)I", at = @At("HEAD"))
    private void neverlose$invalidateBlurSource(long targetMillis, CallbackInfoReturnable<Integer> cir) {
        debob instance = debob.getInstance();
        if (instance != null && instance.getManager() != null && instance.getManager().getRenderCore() != null) {
            instance.getManager().getRenderCore().getBlurPipeline().invalidateSource();
        }
    }
}

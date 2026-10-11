package neverlose.mixin;

import net.minecraft.client.render.chunk.AbstractChunkRenderData;
import net.minecraft.util.Util;
import neverlose.modules.impl.render.ChunkAnimator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.render.chunk.ChunkBuilder$BuiltChunk")
public abstract class ChunkBuilderBuiltChunkMixin {
    @Shadow
    private long field_64231;
    @Shadow
    private long field_64453;

    @Inject(method = "method_76548", at = @At("HEAD"), cancellable = true, require = 0)
    private void neverlose$setChunkAnimationDuration(long duration, CallbackInfo ci) {
        ChunkAnimator animation = ChunkAnimator.getInstance();
        if (animation == null || !animation.shouldAnimateChunks()) {
            return;
        }

        field_64453 = Math.max(duration, getChunkAnimationDurationMs(animation));
        ci.cancel();
    }

    @Inject(method = {"setCurrentRenderData", "method_72049"}, at = @At("TAIL"), require = 0)
    private void neverlose$restartChunkAnimation(AbstractChunkRenderData data, CallbackInfo ci) {
        ChunkAnimator animation = ChunkAnimator.getInstance();
        if (animation == null || !animation.shouldAnimateChunks()) {
            return;
        }

        field_64231 = Util.getMeasuringTimeMs();
        field_64453 = getChunkAnimationDurationMs(animation);
        animation.markChunkUpdated(this);
    }

    @Inject(method = "scheduleRebuild(Z)V", at = @At("TAIL"), require = 0)
    private void neverlose$markChunkRebuild(boolean important, CallbackInfo ci) {
        ChunkAnimator animation = ChunkAnimator.getInstance();
        if (animation != null && animation.shouldAnimateChunks()) {
            animation.markChunkUpdated(this);
        }
    }

    @Inject(method = "method_76298", at = @At("RETURN"), cancellable = true, require = 0)
    private void neverlose$easeChunkVisibility(long currentTime, CallbackInfoReturnable<Float> cir) {
        ChunkAnimator animation = ChunkAnimator.getInstance();
        if (animation == null || !animation.shouldAnimateChunks()) {
            return;
        }

        cir.setReturnValue(animation.applySelectedEasing(cir.getReturnValue()));
    }

    private long getChunkAnimationDurationMs(ChunkAnimator animation) {
        return (long) animation.getChunkDurationMs();
    }
}

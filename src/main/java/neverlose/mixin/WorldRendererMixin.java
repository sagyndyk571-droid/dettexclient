package neverlose.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import neverlose.IMinecraft;
import neverlose.modules.impl.render.ChunkAnimator;
import neverlose.modules.impl.render.NoRender;
import neverlose.modules.impl.render.SkyShader;

@Mixin(value = WorldRenderer.class, priority = 500)
public class WorldRendererMixin implements IMinecraft {

    @WrapOperation(
            method = "renderBlockLayers",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk;getOrigin()Lnet/minecraft/util/math/BlockPos;"),
            require = 0
    )
    private BlockPos neverlose$animateChunkOrigin(ChunkBuilder.BuiltChunk builtChunk, Operation<BlockPos> originalCall) {
        BlockPos original = originalCall.call(builtChunk);
        ChunkAnimator animation = ChunkAnimator.getInstance();
        if (animation == null || !animation.shouldAnimateChunks() || builtChunk == null) {
            return original;
        }

        float visibility = builtChunk.method_76298(Util.getMeasuringTimeMs());
        float offset = animation.getChunkOffset(builtChunk, visibility);
        if (offset <= 0.35f) {
            return original;
        }
        return original.add(0, -Math.round(offset), 0);
    }

    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true, require = 0)
    private void onRenderSky(FrameGraphBuilder frameGraphBuilder, Camera camera, float tickDelta, FogData fog, CallbackInfo ci) {
        SkyShader skyShader = SkyShader.getInstance();
        if (skyShader != null && skyShader.isState()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true, require = 0)
    private void onRenderClouds(FrameGraphBuilder frameGraphBuilder, Matrix4f matrix, Matrix4f projection,
                                CloudRenderMode cloudRenderMode, Vec3d cameraPos, float ticks, int color,
                                float cloudHeight, CallbackInfo ci) {
        SkyShader skyShader = SkyShader.getInstance();
        if (skyShader != null && skyShader.isState()) {
            ci.cancel();
        }
    }

    @Inject(method = "hasBlindnessOrDarkness", at = @At("HEAD"), cancellable = true, require = 0)
    private void onHasBlindnessOrDarkness(Camera camera, CallbackInfoReturnable<Boolean> cir) {
        NoRender noRender = NoRender.getInstance();
        if (noRender == null || !noRender.isState()) return;

        Entity entity = camera.getFocusedEntity();
        if (!(entity instanceof LivingEntity livingEntity)) return;

        boolean hasBlindness = livingEntity.hasStatusEffect(StatusEffects.BLINDNESS);
        boolean hasDarkness = livingEntity.hasStatusEffect(StatusEffects.DARKNESS);

        if (noRender.modeSetting.isSelected("Bad Effects") && hasBlindness && !hasDarkness) {
            cir.setReturnValue(false);
        }

        if (noRender.modeSetting.isSelected("Darkness") && hasDarkness && !hasBlindness) {
            cir.setReturnValue(false);
        }

        if (noRender.modeSetting.isSelected("Bad Effects") && noRender.modeSetting.isSelected("Darkness")) {
            cir.setReturnValue(false);
        }
    }
}

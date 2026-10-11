package neverlose.mixin;

import net.minecraft.client.render.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.joml.Vector4f;
import neverlose.modules.impl.render.Ambience;

import java.nio.ByteBuffer;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    @Shadow
    abstract void applyFog(ByteBuffer buffer, int bufPos, Vector4f fogColor, float environmentalStart, float environmentalEnd, float renderDistanceStart, float renderDistanceEnd, float skyEnd, float cloudEnd);

    @Redirect(method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"))
    private void onApplyFogBuffer(FogRenderer instance, ByteBuffer buffer, int bufPos, Vector4f fogColor, float environmentalStart, float environmentalEnd, float renderDistanceStart, float renderDistanceEnd, float skyEnd, float cloudEnd) {
        Ambience ambience = Ambience.getInstance();
        if (ambience != null && ambience.isState() && ambience.customFog.isValue()) {
            int color = ambience.fogColor.getColor();
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            fogColor.set(r, g, b, fogColor.w);

            environmentalStart = (float) ambience.fogStart.getValue();
            environmentalEnd = (float) ambience.fogEnd.getValue();
            renderDistanceStart = (float) ambience.fogStart.getValue();
            renderDistanceEnd = (float) ambience.fogEnd.getValue();
        }
        this.applyFog(buffer, bufPos, fogColor, environmentalStart, environmentalEnd, renderDistanceStart, renderDistanceEnd, skyEnd, cloudEnd);
    }
}

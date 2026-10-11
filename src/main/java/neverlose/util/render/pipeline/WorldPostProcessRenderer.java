package neverlose.util.render.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import neverlose.modules.impl.render.Ambience;
import neverlose.modules.impl.render.Fog;
import neverlose.modules.impl.render.Saturation;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public final class WorldPostProcessRenderer {
    private static final int UNIFORM_SIZE = 192;
    private static RenderPipeline pipeline;
    private static GpuBuffer uniformBuffer;
    private static GpuTexture copyTexture;
    private static GpuTextureView copyTextureView;
    private static GpuTexture depthTexture;
    private static GpuTextureView depthTextureView;
    private static int lastWidth;
    private static int lastHeight;

    private WorldPostProcessRenderer() {
    }

    public static void renderWorldEffects(Matrix4f projection, Matrix4f view) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null || client.getFramebuffer() == null) {
            return;
        }

        Fog fog = Fog.getInstance();
        Saturation saturation = Saturation.getInstance();
        Ambience ambience = Ambience.getInstance();

        boolean useFogBlur = fog != null && fog.shouldApplyBlur();
        boolean useSaturation = saturation != null && saturation.shouldApply();
        boolean useSnow = ambience != null && ambience.shouldApplySnow();
        if (!useFogBlur && !useSaturation && !useSnow) {
            return;
        }

        ensurePipeline();
        if (pipeline == null || uniformBuffer == null) {
            return;
        }

        Framebuffer framebuffer = client.getFramebuffer();
        if (framebuffer.getColorAttachment() == null || framebuffer.getColorAttachmentView() == null) {
            return;
        }

        int width = Math.max(1, framebuffer.textureWidth);
        int height = Math.max(1, framebuffer.textureHeight);
        ensureTextures(width, height);
        if (copyTexture == null || copyTextureView == null) {
            return;
        }
        boolean hasDepth = depthTexture != null && depthTextureView != null && framebuffer.getDepthAttachment() != null;

        float saturationValue = useSaturation ? saturation.getSaturationValue() : 1.0f;
        float blurRadius = useFogBlur ? fog.getBlurRadius() : 0.0f;
        float blurStrength = useFogBlur ? fog.getBlurStrength() : 0.0f;
        float snowStrength = useSnow ? ambience.getSnowCoverStrength() : 0.0f;
        boolean snowOn = useSnow && hasDepth;

        Matrix4f projInv = new Matrix4f(projection).invert();
        Matrix4f viewInv = new Matrix4f(view).invert();
        float upX = view.m10();
        float upY = view.m11();
        float upZ = view.m12();
        float upLen = (float) Math.sqrt(upX * upX + upY * upY + upZ * upZ);
        if (upLen < 1.0e-6f) {
            upX = 0.0f;
            upY = 1.0f;
            upZ = 0.0f;
        } else {
            upX /= upLen;
            upY /= upLen;
            upZ /= upLen;
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.copyTextureToTexture(
                framebuffer.getColorAttachment(),
                copyTexture,
                0, 0, 0, 0, 0,
                width, height
        );
        if (hasDepth) {
            encoder.copyTextureToTexture(
                    framebuffer.getDepthAttachment(),
                    depthTexture,
                    0, 0, 0, 0, 0,
                    width, height
            );
        }

        ByteBuffer data = MemoryUtil.memAlloc(UNIFORM_SIZE);
        data.putFloat(width).putFloat(height).putFloat(saturationValue).putFloat(0.0f);
        data.putFloat(blurRadius).putFloat(blurStrength).putFloat(useFogBlur ? 1.0f : 0.0f).putFloat(0.0f);
        data.putFloat(snowStrength).putFloat(0.45f).putFloat(0.82f).putFloat(snowOn ? 1.0f : 0.0f);
        projInv.get(data);
        viewInv.get(data);
        data.putFloat(upX).putFloat(upY).putFloat(upZ).putFloat(0.0f);
        data.flip();

        encoder.writeToBuffer(uniformBuffer.slice(), data);
        MemoryUtil.memFree(data);

        GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        GpuSampler nearest = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);
        try (RenderPass pass = encoder.createRenderPass(
                () -> "neverlose:world_post_process",
                framebuffer.getColorAttachmentView(),
                OptionalInt.empty(),
                framebuffer.getDepthAttachmentView(),
                OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            pass.bindTexture("Sampler0", copyTextureView, sampler);
            if (hasDepth) {
                pass.bindTexture("Sampler1", depthTextureView, nearest);
            }
            pass.setUniform("PostData", uniformBuffer);
            pass.draw(0, 6);
        }
    }

    public static void close() {
        if (uniformBuffer != null) {
            uniformBuffer.close();
            uniformBuffer = null;
        }
        if (copyTextureView != null) {
            copyTextureView.close();
            copyTextureView = null;
        }
        if (copyTexture != null) {
            copyTexture.close();
            copyTexture = null;
        }
        if (depthTextureView != null) {
            depthTextureView.close();
            depthTextureView = null;
        }
        if (depthTexture != null) {
            depthTexture.close();
            depthTexture = null;
        }
        pipeline = null;
        lastWidth = 0;
        lastHeight = 0;
    }

    private static void ensurePipeline() {
        if (pipeline == null) {
            pipeline = RenderPipeline.builder()
                    .withLocation(Identifier.of("neverlose", "pipeline/world_post_process"))
                    .withVertexShader(Identifier.of("neverlose", "core/post/world/post_fullscreen"))
                    .withFragmentShader(Identifier.of("neverlose", "core/post/world/world_post"))
                    .withVertexFormat(VertexFormat.builder().build(), VertexFormat.DrawMode.TRIANGLES)
                    .withSampler("Sampler0")
                    .withSampler("Sampler1")
                    .withUniform("PostData", UniformType.UNIFORM_BUFFER)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build();
        }

        if (uniformBuffer == null) {
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "neverlose:world_post_process_uniform",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    UNIFORM_SIZE
            );
        }
    }

    private static void ensureTextures(int width, int height) {
        if (copyTexture != null && lastWidth == width && lastHeight == height) {
            return;
        }

        if (copyTextureView != null) {
            copyTextureView.close();
            copyTextureView = null;
        }
        if (copyTexture != null) {
            copyTexture.close();
            copyTexture = null;
        }
        if (depthTextureView != null) {
            depthTextureView.close();
            depthTextureView = null;
        }
        if (depthTexture != null) {
            depthTexture.close();
            depthTexture = null;
        }

        copyTexture = RenderSystem.getDevice().createTexture(
                () -> "neverlose:world_post_process_copy",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8,
                width, height, 1, 1
        );
        copyTextureView = RenderSystem.getDevice().createTextureView(copyTexture);

        depthTexture = RenderSystem.getDevice().createTexture(
                () -> "neverlose:world_post_process_depth",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
                TextureFormat.DEPTH32,
                width, height, 1, 1
        );
        depthTextureView = RenderSystem.getDevice().createTextureView(depthTexture);

        lastWidth = width;
        lastHeight = height;
    }
}

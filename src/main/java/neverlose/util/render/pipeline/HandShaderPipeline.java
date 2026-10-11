package neverlose.util.render.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalInt;

public class HandShaderPipeline {

    private static final Identifier PIPELINE_ID = Identifier.of("neverlose", "pipeline/hand_shader");
    private static final Identifier VERTEX_SHADER = Identifier.of("neverlose", "core/hand_shader");
    private static final Identifier FRAGMENT_SHADER = Identifier.of("neverlose", "core/hand_shader");

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(VERTEX_SHADER)
                    .withFragmentShader(FRAGMENT_SHADER)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("HandShaderData", UniformType.UNIFORM_BUFFER)
                    .withSampler("AfterSampler")
                    .withSampler("BeforeSampler")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f(0, 0, 0);
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final int BUFFER_SIZE = 48;

    private GpuBuffer uniformBuffer;
    private GpuBuffer dummyVertexBuffer;
    private ByteBuffer dataBuffer;
    private boolean initialized;

    private void ensureInitialized() {
        if (initialized) return;

        dataBuffer = MemoryUtil.memAlloc(BUFFER_SIZE);

        ByteBuffer dummyData = MemoryUtil.memAlloc(4);
        dummyData.putInt(0);
        dummyData.flip();
        dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "minecraft:hand_shader_dummy_vertex",
                GpuBuffer.USAGE_VERTEX,
                dummyData
        );
        MemoryUtil.memFree(dummyData);

        initialized = true;
    }

    public void render(GpuTextureView targetView, GpuTextureView afterView, GpuTextureView beforeView,
                       int width, int height, float blurRadius, float threshold,
                       int tintColor, float opacity, int mode, float time) {
        if (targetView == null || afterView == null || beforeView == null || width <= 0 || height <= 0) {
            return;
        }

        ensureInitialized();
        prepareUniformData(width, height, blurRadius, threshold, tintColor, opacity, mode, time);

        int size = dataBuffer.remaining();
        if (uniformBuffer == null || uniformBuffer.size() < size) {
            if (uniformBuffer != null) uniformBuffer.close();
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "minecraft:hand_shader_uniform",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    size
            );
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

        GpuSampler linearSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "minecraft:hand_shader_pass",
                targetView,
                OptionalInt.empty())) {

            renderPass.setPipeline(PIPELINE);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);
            renderPass.bindTexture("AfterSampler", afterView, linearSampler);
            renderPass.bindTexture("BeforeSampler", beforeView, linearSampler);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setUniform("HandShaderData", uniformBuffer);

            renderPass.draw(0, 6);
        }
    }

    private void prepareUniformData(int width, int height, float blurRadius, float threshold,
                                    int tintColor, float opacity, int mode, float time) {
        dataBuffer.clear();

        dataBuffer.putFloat(width);
        dataBuffer.putFloat(height);
        dataBuffer.putFloat(blurRadius);
        dataBuffer.putFloat(threshold);

        float colorAlpha = ((tintColor >> 24) & 0xFF) / 255.0f;
        float r = ((tintColor >> 16) & 0xFF) / 255.0f;
        float g = ((tintColor >> 8) & 0xFF) / 255.0f;
        float b = (tintColor & 0xFF) / 255.0f;

        dataBuffer.putFloat(r);
        dataBuffer.putFloat(g);
        dataBuffer.putFloat(b);
        dataBuffer.putFloat(Math.max(0.0f, Math.min(1.0f, opacity * colorAlpha)));

        dataBuffer.putFloat(mode);
        dataBuffer.putFloat(time);
        dataBuffer.putFloat(0.0f);
        dataBuffer.putFloat(0.0f);

        dataBuffer.flip();
    }

    public void close() {
        if (uniformBuffer != null) {
            uniformBuffer.close();
            uniformBuffer = null;
        }
        if (dummyVertexBuffer != null) {
            dummyVertexBuffer.close();
            dummyVertexBuffer = null;
        }
        if (dataBuffer != null) {
            MemoryUtil.memFree(dataBuffer);
            dataBuffer = null;
        }
        initialized = false;
    }
}

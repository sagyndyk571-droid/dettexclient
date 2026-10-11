package neverlose.modules.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.WorldRenderEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.ColorSetting;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkyShader extends ModuleStructure {
    static final int SKY_UNIFORM_SIZE = 64;
    static SkyShader instance;
    static RenderPipeline waterPipeline;
    static RenderPipeline causticPipeline;
    static GpuBuffer uniformBuffer;

    final SelectSetting mode = new SelectSetting("Режим", "Тип шейдера неба")
            .value("Water", "Caustic")
            .selected("Water");

    final SliderSettings speed = new SliderSettings("Скорость", "Скорость анимации")
            .range(0.1f, 5.0f)
            .setValue(1.0f);

    final SliderSettings scale = new SliderSettings("Размер", "Масштаб узора")
            .range(1.0f, 20.0f)
            .setValue(5.0f);

    final SliderSettings intensity = new SliderSettings("Интенсивность", "Сила свечения")
            .range(0.001f, 0.05f)
            .setValue(0.01f);

    final SliderSettings alpha = new SliderSettings("Прозрачность", "Прозрачность шейдера")
            .range(0.3f, 1.0f)
            .setValue(1.0f);

    final ColorSetting color = new ColorSetting("Цвет", "Цвет шейдера")
            .value(ColorSetting.DEFAULT_COLOR);

    long startMillis = -1L;

    public SkyShader() {
        super("SkyShader", "Shader sky overlay", ModuleCategory.RENDER);
        instance = this;
        settings(mode, speed, scale, intensity, alpha, color);
    }

    public static SkyShader getInstance() {
        return instance;
    }

    @Override
    public void deactivate() {
        startMillis = -1L;
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        renderSkyShader();
    }

    public void renderSkyShader() {
        if (!isState() || mc.player == null || mc.world == null) {
            return;
        }

        ensurePipeline();
        RenderPipeline pipeline = mode.isSelected("Caustic") ? causticPipeline : waterPipeline;
        if (pipeline == null || uniformBuffer == null) {
            return;
        }

        if (startMillis < 0L) {
            startMillis = System.currentTimeMillis();
        }

        ByteBuffer buffer = createUniformData();
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.writeToBuffer(uniformBuffer.slice(), buffer);
        MemoryUtil.memFree(buffer);

        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer framebuffer = client.getFramebuffer();
        if (framebuffer == null) {
            return;
        }

        try (RenderPass pass = encoder.createRenderPass(
                () -> "neverlose:sky_shader",
                framebuffer.getColorAttachmentView(),
                OptionalInt.empty(),
                framebuffer.getDepthAttachmentView(),
                OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            pass.setUniform("SkyData", uniformBuffer);
            pass.draw(0, 6);
        }
    }

    private ByteBuffer createUniformData() {
        float time = (System.currentTimeMillis() - startMillis) / 1000.0f;
        float fw = mc.getWindow().getFramebufferWidth();
        float fh = mc.getWindow().getFramebufferHeight();
        int shaderColor = color.getColorNoAlpha();

        float cr = ((shaderColor >> 16) & 0xFF) / 255f;
        float cg = ((shaderColor >> 8) & 0xFF) / 255f;
        float cb = (shaderColor & 0xFF) / 255f;

        net.minecraft.client.render.Camera camera = mc.gameRenderer.getCamera();
        float yawRad = (float) Math.toRadians(-camera.getYaw());
        float pitchRad = (float) Math.toRadians(camera.getPitch());
        float fov = (float) mc.options.getFov().getValue().intValue();

        ByteBuffer buffer = MemoryUtil.memAlloc(SKY_UNIFORM_SIZE);
        buffer.putFloat(fw).putFloat(fh).putFloat(time).putFloat(fov);
        buffer.putFloat(cr).putFloat(cg).putFloat(cb).putFloat(alpha.getValue());
        buffer.putFloat(speed.getValue()).putFloat(scale.getValue()).putFloat(intensity.getValue()).putFloat(0f);
        buffer.putFloat(yawRad).putFloat(pitchRad).putFloat(0f).putFloat(0f);
        buffer.flip();
        return buffer;
    }

    private static void ensurePipeline() {
        if (waterPipeline != null && causticPipeline != null && uniformBuffer != null) {
            return;
        }

        if (waterPipeline == null) {
            waterPipeline = createPipeline("water", Identifier.of("neverlose", "core/post/sky/water"));
        }
        if (causticPipeline == null) {
            causticPipeline = createPipeline("caustic", Identifier.of("neverlose", "core/post/sky/caustic"));
        }
        if (uniformBuffer == null) {
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "neverlose:sky_shader_uniform",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    SKY_UNIFORM_SIZE);
        }
    }

    private static RenderPipeline createPipeline(String name, Identifier fragmentShader) {
        return RenderPipeline.builder()
                .withLocation(Identifier.of("neverlose", "pipeline/sky_shader_" + name))
                .withVertexShader(Identifier.of("neverlose", "core/post/sky/sky"))
                .withFragmentShader(fragmentShader)
                .withVertexFormat(VertexFormat.builder().build(), VertexFormat.DrawMode.TRIANGLES)
                .withUniform("SkyData", UniformType.UNIFORM_BUFFER)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .build();
    }
}

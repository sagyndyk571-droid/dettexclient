package neverlose.util.render.shader;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.option.Perspective;
import neverlose.util.render.pipeline.HandShaderPipeline;

public class HandShaderRenderer {

    private static HandShaderRenderer instance;

    private final MinecraftClient client;
    private HandShaderPipeline pipeline;

    private GpuTexture sceneBeforeTexture;
    private GpuTextureView sceneBeforeTextureView;
    private GpuTexture sceneAfterTexture;
    private GpuTextureView sceneAfterTextureView;

    private int lastWidth;
    private int lastHeight;

    private boolean enabled;
    private boolean initialized;
    private boolean capturing;

    private float blurRadius = 12.0f;
    private float threshold = 0.01f;
    private float opacity = 0.6f;
    private int color = 0xFF46AAFF;
    private int mode;

    public HandShaderRenderer() {
        this.client = MinecraftClient.getInstance();
        instance = this;
    }

    public static HandShaderRenderer getInstance() {
        if (instance == null) {
            instance = new HandShaderRenderer();
        }
        return instance;
    }

    private void ensureInitialized() {
        if (initialized) return;

        if (pipeline != null) {
            pipeline.close();
        }

        pipeline = new HandShaderPipeline();
        lastWidth = 0;
        lastHeight = 0;
        initialized = true;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            ensureInitialized();
        } else {
            capturing = false;
        }
    }

    public void setBlurRadius(float blurRadius) {
        this.blurRadius = blurRadius;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    private boolean canCapture() {
        return enabled
                && client.player != null
                && client.world != null
                && client.options.getPerspective() == Perspective.FIRST_PERSON;
    }

    private void ensureTextures(int width, int height) {
        if (width == lastWidth && height == lastHeight && sceneBeforeTexture != null && sceneAfterTexture != null) {
            return;
        }

        cleanupTextures();

        sceneBeforeTexture = RenderSystem.getDevice().createTexture(
                () -> "minecraft:hand_shader_scene_before",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
                TextureFormat.RGBA8,
                width, height, 1, 1
        );
        sceneBeforeTextureView = RenderSystem.getDevice().createTextureView(sceneBeforeTexture);

        sceneAfterTexture = RenderSystem.getDevice().createTexture(
                () -> "minecraft:hand_shader_scene_after",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
                TextureFormat.RGBA8,
                width, height, 1, 1
        );
        sceneAfterTextureView = RenderSystem.getDevice().createTextureView(sceneAfterTexture);

        lastWidth = width;
        lastHeight = height;
    }

    public void captureSceneBeforeHands() {
        if (!canCapture()) return;

        ensureInitialized();

        Framebuffer framebuffer = client.getFramebuffer();
        if (framebuffer == null || framebuffer.getColorAttachment() == null) return;

        int width = framebuffer.textureWidth;
        int height = framebuffer.textureHeight;
        ensureTextures(width, height);

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.copyTextureToTexture(
                framebuffer.getColorAttachment(),
                sceneBeforeTexture,
                0, 0, 0, 0, 0,
                width, height
        );

        capturing = true;
    }

    public void captureSceneAfterHands() {
        if (!canCapture() || !capturing) return;

        Framebuffer framebuffer = client.getFramebuffer();
        if (framebuffer == null || framebuffer.getColorAttachment() == null) {
            capturing = false;
            return;
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.copyTextureToTexture(
                framebuffer.getColorAttachment(),
                sceneAfterTexture,
                0, 0, 0, 0, 0,
                lastWidth, lastHeight
        );
    }

    public void renderEffect() {
        if (!canCapture() || !capturing || pipeline == null) {
            capturing = false;
            return;
        }

        Framebuffer framebuffer = client.getFramebuffer();
        if (framebuffer == null || framebuffer.getColorAttachmentView() == null) {
            capturing = false;
            return;
        }

        float time = (System.currentTimeMillis() % 100_000L) / 1000.0f;
        pipeline.render(
                framebuffer.getColorAttachmentView(),
                sceneAfterTextureView,
                sceneBeforeTextureView,
                lastWidth,
                lastHeight,
                blurRadius,
                threshold,
                color,
                opacity,
                mode,
                time
        );

        capturing = false;
    }

    private void cleanupTextures() {
        if (sceneBeforeTextureView != null) {
            sceneBeforeTextureView.close();
            sceneBeforeTextureView = null;
        }
        if (sceneBeforeTexture != null) {
            sceneBeforeTexture.close();
            sceneBeforeTexture = null;
        }
        if (sceneAfterTextureView != null) {
            sceneAfterTextureView.close();
            sceneAfterTextureView = null;
        }
        if (sceneAfterTexture != null) {
            sceneAfterTexture.close();
            sceneAfterTexture = null;
        }
    }

    public void invalidate() {
        cleanupTextures();
        if (pipeline != null) {
            pipeline.close();
            pipeline = null;
        }
        lastWidth = 0;
        lastHeight = 0;
        initialized = false;
        capturing = false;
    }
}

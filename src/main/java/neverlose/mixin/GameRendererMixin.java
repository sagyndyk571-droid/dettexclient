package neverlose.mixin;

import com.holdmylua.source.LuaTestHMI;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RotationAxis;
import neverlose.debob;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import neverlose.client.draggables.Drag;
import neverlose.events.api.EventManager;
import neverlose.events.impl.FovEvent;
import neverlose.events.impl.WorldRenderEvent;
import neverlose.modules.impl.player.NoEntityTrace;
import neverlose.modules.impl.render.AspectRatio;
import neverlose.modules.impl.render.ChunkAnimator;
import neverlose.modules.impl.render.Hud;
import neverlose.modules.impl.render.NoRender;
import neverlose.screens.clickgui.ClickGui;
import neverlose.util.render.Render3D;
import neverlose.util.render.pipeline.WorldPostProcessRenderer;
import neverlose.util.security.ClientUnhook;

@Mixin(value = GameRenderer.class, priority = 500)
public abstract class GameRendererMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    @Final
    private Camera camera;

    @Shadow
    @Final
    GuiRenderState guiState;

    @Shadow
    @Final
    private GuiRenderer guiRenderer;

    @Shadow
    @Final
    private FogRenderer fogRenderer;

    @Unique
    private final MatrixStack matrices = new MatrixStack();
    @Unique
    private float neverlose$animatedFov = Float.NaN;
    @Unique
    private float neverlose$fovFrom = Float.NaN;
    @Unique
    private float neverlose$fovTarget = Float.NaN;
    @Unique
    private long neverlose$fovStartTime;

    @Shadow
    protected abstract void bobView(MatrixStack matrices, float tickDelta);

    @Shadow
    protected abstract void tiltViewWhenHurt(MatrixStack matrices, float tickDelta);

    @Shadow
    public abstract float getFov(Camera camera, float tickDelta, boolean changingFov);

    @Inject(method = "close", at = @At("RETURN"))
    private void onClose(CallbackInfo ci) {
        if (debob.getInstance() != null && debob.getInstance().getManager() != null && debob.getInstance().getManager().getRenderCore() != null) {
            debob.getInstance().getManager().getRenderCore().close();
        }
        WorldPostProcessRenderer.close();
    }
    @Inject(method={"render"}, at={@At(value="HEAD")})
    private void deltaTime(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        float currentTime = (float) GLFW.glfwGetTime();
        LuaTestHMI.deltaTime = currentTime - LuaTestHMI.prevTime;
        LuaTestHMI.prevTime = currentTime;
        LuaTestHMI.deltaTime = MinecraftClient.getInstance().isPaused() ? 0.0f : (float)Math.min(0.05, (double)LuaTestHMI.deltaTime);
    }
    @ModifyExpressionValue(method = "getFov", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I", remap = false))
    private int hookGetFov(int original) {
        FovEvent event = new FovEvent();
        EventManager.callEvent(event);
        if (event.isCancelled()) return event.getFov();
        return original;
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void neverlose$animateFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        ChunkAnimator animation = ChunkAnimator.getInstance();
        float targetFov = cir.getReturnValue();
        if (animation == null || !animation.shouldAnimateCameraZoom() || !Float.isFinite(targetFov)) {
            neverlose$animatedFov = Float.NaN;
            neverlose$fovFrom = Float.NaN;
            neverlose$fovTarget = Float.NaN;
            neverlose$fovStartTime = 0L;
            return;
        }

        long now = System.currentTimeMillis();
        if (!Float.isFinite(neverlose$animatedFov) || !Float.isFinite(neverlose$fovTarget)) {
            neverlose$animatedFov = targetFov;
            neverlose$fovFrom = targetFov;
            neverlose$fovTarget = targetFov;
            neverlose$fovStartTime = now;
            return;
        }

        if (Math.abs(targetFov - neverlose$fovTarget) > 1.2f) {
            neverlose$fovFrom = neverlose$animatedFov;
            neverlose$fovTarget = targetFov;
            neverlose$fovStartTime = now;
        }

        float progress = animation.getScreenProgress(now - neverlose$fovStartTime);
        neverlose$animatedFov = neverlose$fovFrom + (neverlose$fovTarget - neverlose$fovFrom) * progress;
        cir.setReturnValue(neverlose$animatedFov);
    }

    @ModifyExpressionValue(method = "getBasicProjectionMatrix", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;getFramebufferHeight()I"))
    private int neverlose$hookAspectRatio(int original) {
        AspectRatio aspectRatio = AspectRatio.getInstance();
        if (aspectRatio == null || !aspectRatio.isState() || client == null || client.getWindow() == null) {
            return original;
        }

        int framebufferWidth = Math.max(1, client.getWindow().getFramebufferWidth());
        float vanillaAspect = framebufferWidth / (float) Math.max(1, original);
        float customAspect = Math.max(0.1f, aspectRatio.getAspectRatio(vanillaAspect));
        return Math.max(1, Math.round(framebufferWidth / customAspect));
    }

    @Inject(method = "updateCrosshairTarget", at = @At("HEAD"), cancellable = true)
    private void updateCrosshairTargetHook(float tickProgress, CallbackInfo ci) {
        NoEntityTrace noEntityTrace = NoEntityTrace.getInstance();
        if (noEntityTrace != null && noEntityTrace.shouldIgnoreEntityTrace()) {
            Entity entity = this.client.getCameraEntity();
            if (entity != null && this.client.world != null && this.client.player != null) {
                double range = Math.max(
                        this.client.player.getBlockInteractionRange(),
                        this.client.player.getEntityInteractionRange()
                );
                this.client.crosshairTarget = entity.raycast(range, tickProgress, false);
                this.client.targetedEntity = null;
                ci.cancel();
            }
        }
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = {"ldc=hand"}))
    public void hookWorldRender(RenderTickCounter tickCounter, CallbackInfo ci, @Local(ordinal = 0) Matrix4f projection, @Local(ordinal = 1) Matrix4f view, @Local(ordinal = 0) float tickDelta, @Local MatrixStack matrixStack) {
        if (client.world == null || client.player == null) return;
        if (ClientUnhook.isHidden()) return;

        WorldPostProcessRenderer.renderWorldEffects(projection, view);

        MatrixStack worldSpaceStack = new MatrixStack();
        worldSpaceStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        worldSpaceStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));

        Render3D.lastProjMat.set(client.gameRenderer.getBasicProjectionMatrix(getFov(camera, tickDelta, true)));
        Render3D.lastModMat.set(RenderSystem.getModelViewMatrix());
        Render3D.lastWorldSpaceMatrix.set(worldSpaceStack.peek().getPositionMatrix());

        Render3D.setLastWorldSpaceEntry(matrixStack.peek());
        Render3D.setLastTickDelta(tickDelta);
        Render3D.setLastCameraPos(camera.getCameraPos());
        Render3D.setLastCameraRotation(new Quaternionf(camera.getRotation()));

        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix().mul(view);

        matrices.push();
        tiltViewWhenHurt(matrices, camera.getLastTickProgress());
        if (client.options.getBobView().getValue()) {
            bobView(matrices, camera.getLastTickProgress());
        }
        modelViewStack.mul(matrices.peek().getPositionMatrix().invert(new Matrix4f()));
        matrices.pop();

        WorldRenderEvent event = new WorldRenderEvent(matrixStack, tickDelta);
        EventManager.callEvent(event);
        Render3D.onWorldRender(event);

        modelViewStack.popMatrix();
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void onTiltViewWhenHurt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("Damage")) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "renderWorld", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F", ordinal = 0))
    private float onNauseaDistortion(float original) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("Nausea")) {
            return 0.0F;
        }
        return original;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER))
    private void afterGuiRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        if (client.world == null || client.player == null) return;
        if (ClientUnhook.isHidden()) return;
        if (isLoadingScreen(client.currentScreen)) return;
        if (client.getOverlay() != null) return;
        if (!shouldRenderOnTop(client.currentScreen)) return;

        guiState.clear();

        int mouseX = (int) client.mouse.getScaledX(client.getWindow());
        int mouseY = (int) client.mouse.getScaledY(client.getWindow());
        float tickDelta = tickCounter.getTickProgress(false);

        DrawContext context = new DrawContext(client, guiState, mouseX, mouseY);

        Hud hud = Hud.getInstance();
        if (hud != null && hud.isState()) {
            boolean isChatScreen = client.currentScreen instanceof ChatScreen;
            Drag.onDraw(context, mouseX, mouseY, tickDelta, isChatScreen);
        }

        if (client.currentScreen instanceof ClickGui clickGui) {
            clickGui.renderOverlay(context, tickCounter);
        }

        guiRenderer.render(fogRenderer.getFogBuffer(FogRenderer.FogType.NONE));
    }

    @Unique
    private boolean shouldRenderOnTop(Screen screen) {
        if (screen == null) return true;
        if (screen instanceof ClickGui) return true;
        if (screen instanceof ChatScreen) return true;
        return false;
    }

    @Unique
    private boolean isLoadingScreen(Screen screen) {
        if (screen == null) return false;
        String className = screen.getClass().getSimpleName().toLowerCase();
        String fullName = screen.getClass().getName().toLowerCase();
        if (className.contains("loading")) return true;
        if (className.contains("progress")) return true;
        if (className.contains("connecting")) return true;
        if (className.contains("downloading")) return true;
        if (className.contains("terrain")) return true;
        if (className.contains("generating")) return true;
        if (className.contains("saving")) return true;
        if (className.contains("reload")) return true;
        if (className.contains("resource")) return true;
        if (className.contains("pack")) return true;
        if (fullName.contains("mojang")) return true;
        return false;
    }
}

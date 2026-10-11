package neverlose.mixin;

import com.holdmylua.source.global.GlobalsStorage;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import neverlose.debob;
import neverlose.util.hmi.HandMyItemsRuntime;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import neverlose.IMinecraft;
import neverlose.events.api.EventManager;
import neverlose.events.impl.CrosshairEvent;
import neverlose.events.impl.DrawEvent;
import neverlose.events.impl.HotbarItemRenderEvent;
import neverlose.modules.impl.render.Hud;
import neverlose.modules.impl.render.ChunkAnimator;
import neverlose.modules.impl.render.NoRender;
import neverlose.screens.clickgui.ClickGui;
import neverlose.util.render.Render2D;
import neverlose.util.security.ClientUnhook;
import org.joml.Matrix3x2fStack;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin implements IMinecraft {

    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    public abstract TextRenderer getTextRenderer();
    @Unique
    private int richCurrentHotbarIndex = 0;
    @Unique
    private long neverlose$hotbarStartTime;
    @Unique
    private boolean neverlose$animatedHotbarFrame;

    @Inject(method = "renderHotbar", at = @At("HEAD"))
    private void onRenderHotbarStart(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        richCurrentHotbarIndex = 0;
        ChunkAnimator animation = ChunkAnimator.getInstance();
        if (animation == null || !animation.shouldAnimateHotbar()) {
            neverlose$hotbarStartTime = 0L;
            neverlose$animatedHotbarFrame = false;
            return;
        }

        if (neverlose$hotbarStartTime == 0L) {
            neverlose$hotbarStartTime = System.currentTimeMillis();
        }

        float progress = animation.getScreenProgress(System.currentTimeMillis() - neverlose$hotbarStartTime);
        float scale = 0.965f + progress * 0.035f;
        float yOffset = (1.0f - progress) * 15.0f;
        float centerX = client.getWindow().getScaledWidth() / 2.0f;
        float anchorY = client.getWindow().getScaledHeight() - 12.0f;

        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(centerX, anchorY + yOffset);
        matrices.scale(scale, scale);
        matrices.translate(-centerX, -anchorY);
        neverlose$animatedHotbarFrame = true;
    }

    @Inject(method = "renderHotbar", at = @At("RETURN"))
    private void neverlose$animateHotbarTail(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (neverlose$animatedHotbarFrame) {
            context.getMatrices().popMatrix();
            neverlose$animatedHotbarFrame = false;
        }
    }

    @WrapOperation(
            method = "renderHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHotbarItem(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;I)V"
            )
    )
    private void onRenderHotbarItem(InGameHud instance, DrawContext context, int x, int y, RenderTickCounter tickCounter, PlayerEntity player, ItemStack stack, int seed, Operation<Void> original) {
        int hotbarIndex = richCurrentHotbarIndex;

        if (richCurrentHotbarIndex < 9) {
            richCurrentHotbarIndex++;
        }

        HotbarItemRenderEvent event = new HotbarItemRenderEvent(stack, hotbarIndex);
        EventManager.callEvent(event);
        original.call(instance, context, x, y, tickCounter, player, event.getStack(), seed);
    }

    @Inject(method = "renderNauseaOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderNauseaOverlay(DrawContext context, float nauseaStrength, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("Nausea")) {
            ci.cancel();
        }
    }
    @Inject(method={"render"}, at={@At(value="HEAD")})
    private void debugText(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (HandMyItemsRuntime.isActive() && !GlobalsStorage.debugTextRenderer.get().isEmpty()) {
            int y = 10;
            for (String s : GlobalsStorage.debugTextRenderer.get()) {
                context.drawCenteredTextWithShadow(this.getTextRenderer(), s, 10, y, 255);
                y += 10;
            }
            GlobalsStorage.debugTextRenderer.clear();
        }
    }
    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
    private void onRenderScoreboard(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("Scoreboard")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderBossBarHud", at = @At("HEAD"), cancellable = true)
    private void onRenderBossBar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("BossBar")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (ClientUnhook.isHidden()) return;
        if (this.client.options.hudHidden) return;
        if (client.world == null || client.player == null) return;
        if (client.getOverlay() != null) return;

        CrosshairEvent event = new CrosshairEvent(context, tickCounter.getTickProgress(false));
        EventManager.callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void onRenderCustomHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (ClientUnhook.isHidden()) return;
        if (this.client.options.hudHidden) return;
        if (client.world == null || client.player == null) return;
        if (client.getOverlay() != null) return;

        Screen screen = client.currentScreen;

        if (isLoadingScreen(screen)) return;

        context.createNewRootLayer();
        Render2D.beginOverlay();
        context.getMatrices().pushMatrix();

        DrawEvent event = new DrawEvent(context, drawEngine, tickCounter.getTickProgress(false));
        EventManager.callEvent(event);

        context.getMatrices().popMatrix();

        if (shouldRenderHud(screen)) {
            int mouseX = (int) client.mouse.getScaledX(client.getWindow());
            int mouseY = (int) client.mouse.getScaledY(client.getWindow());
            float tickDelta = tickCounter.getTickProgress(false);

            Hud hud = Hud.getInstance();
            if (hud != null && hud.isState() && debob.getInstance() != null
                    && debob.getInstance().getManager() != null
                    && debob.getInstance().getManager().getHudManager() != null) {
                debob.getInstance().getManager().getHudManager().render(context, tickDelta, mouseX, mouseY);
            }
        }

        Render2D.endOverlay();
    }

    @Unique
    private boolean shouldRenderHud(Screen screen) {
        if (screen == null) return true;
        if (screen instanceof ClickGui) return false;
        if (screen instanceof ChatScreen) return false;
        if (isLoadingScreen(screen)) return false;
        return true;
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

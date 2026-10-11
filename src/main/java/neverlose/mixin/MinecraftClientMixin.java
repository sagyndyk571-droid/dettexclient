package neverlose.mixin;

import com.holdmylua.source.access.LivingEntityAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.session.Session;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import neverlose.util.hmi.HandMyItemsRuntime;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import neverlose.debob;
import neverlose.events.api.EventManager;
import neverlose.events.impl.GameLeftEvent;
import neverlose.events.impl.HotBarUpdateEvent;
import neverlose.events.impl.SetScreenEvent;
import neverlose.modules.impl.combat.NoInteract;
import neverlose.modules.impl.render.Hud;
import neverlose.screens.clickgui.ClickGui;
import neverlose.screens.menu.MainMenuScreen;
import neverlose.util.config.ConfigSystem;
import neverlose.util.render.font.FontRenderer;
import neverlose.util.security.ClientUnhook;
import neverlose.util.session.SessionChanger;
import neverlose.util.window.WindowStyle;

import static neverlose.IMinecraft.mc;

@Mixin(value = MinecraftClient.class, priority = 500)
public abstract class MinecraftClientMixin {
    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @Shadow
    @Nullable
    public ClientPlayerInteractionManager interactionManager;

    @Shadow
    @Final
    public GameRenderer gameRenderer;

    @Shadow
    public ClientWorld world;

    private static boolean fontsInitialized = false;

    @Shadow
    @Mutable
    private Session session;

    private void setSession(Session newSession) {
        this.session = newSession;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        new debob().init();
        SessionChanger.setSessionSetter(this::setSession);
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void onStop(CallbackInfo ci) {
        ConfigSystem configSystem = ConfigSystem.getInstance();
        if (configSystem != null) {
            configSystem.shutdown();
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (!fontsInitialized && screen != null) {
            try {
                FontRenderer fontRenderer = debob.getInstance().getManager().getRenderCore().getFontRenderer();
                if (fontRenderer != null && !fontRenderer.isInitialized()) {
                    fontRenderer.initialize();
                    fontsInitialized = true;
                }
            } catch (Exception ignored) {}
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void redirectTitleScreen(Screen screen, CallbackInfo ci) {
        if (ClientUnhook.isHidden()) {
            return;
        }
        if (screen instanceof TitleScreen && !(screen instanceof MainMenuScreen)) {
            ci.cancel();
            ((MinecraftClient)(Object)this).setScreen(new MainMenuScreen());
        }
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;Z)V", at = @At("HEAD"))
    private void onDisconnect(Screen screen, boolean transferring, CallbackInfo info) {
        if (world != null) {
            EventManager.callEvent(GameLeftEvent.get());
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        Hud hud = Hud.getInstance();
        if (hud != null && hud.isState()) {
            if (debob.getInstance() != null
                    && debob.getInstance().getManager() != null
                    && debob.getInstance().getManager().getHudManager() != null) {
                debob.getInstance().getManager().getHudManager().tick();
            }
        }
    }

    @Inject(method = "setScreen", at = @At(value = "HEAD"), cancellable = true)
    public void setScreenHook(Screen screen, CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;

        if (ClientUnhook.isHidden() && screen instanceof ClickGui) {
            ci.cancel();
            return;
        }

        if (client.currentScreen instanceof ClickGui clickGui) {
            if (clickGui.isClosing() && screen == null) {
                ci.cancel();
                return;
            }
        }

        SetScreenEvent event = new SetScreenEvent(screen);
        EventManager.callEvent(event);

        debob instance = debob.getInstance();

        Screen eventScreen = event.getScreen();
        if (screen != eventScreen) {
            mc.setScreen(eventScreen);
            ci.cancel();
        }
    }
    @Inject(method={"doAttack"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/network/ClientPlayerEntity;swingHand(Lnet/minecraft/util/Hand;)V")})
    public void doAttackMix(CallbackInfoReturnable<Boolean> cir) {
        if (!HandMyItemsRuntime.isActive()) {
            return;
        }
        ClientPlayerEntity clientPlayerEntity = this.player;
        if (clientPlayerEntity instanceof LivingEntityAccessor) {
            LivingEntityAccessor mixin = (LivingEntityAccessor)clientPlayerEntity;
            mixin.hMI5_0$resetMainHandSwing(false);
        }
    }

    @Redirect(method={"doItemUse"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/network/ClientPlayerEntity;swingHand(Lnet/minecraft/util/Hand;)V"))
    private void doItemUse(ClientPlayerEntity instance, Hand hand) {
        if (HandMyItemsRuntime.isActive() && instance instanceof LivingEntityAccessor) {
            LivingEntityAccessor accessor = (LivingEntityAccessor)instance;
            if (hand == Hand.MAIN_HAND) {
                accessor.hMI5_0$resetMainHandSwing(true);
            } else {
                accessor.hMI5_0$resetOffHandSwing(true);
            }
        }
        instance.swingHand(hand);
    }
    @Inject(method = "getWindowTitle", at = @At("RETURN"), cancellable = true)
    private void getWindowTitle(CallbackInfoReturnable<String> cir) {
        if (ClientUnhook.isHidden()) {
            cir.setReturnValue("Minecraft 1.21.11");
            return;
        }
        cir.setReturnValue("DettexClient BETA");
    }

    @Inject(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"), cancellable = true)
    public void handleInputEventsHook(CallbackInfo ci) {
        HotBarUpdateEvent event = new HotBarUpdateEvent();
        EventManager.callEvent(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Hand;values()[Lnet/minecraft/util/Hand;"), cancellable = true)
    public void doItemUseHook(CallbackInfo ci) {
        if (NoInteract.getInstance().isState()) {
            for (Hand hand : Hand.values()) {
                if (player.getStackInHand(hand).isEmpty()) continue;
                ActionResult result = interactionManager.interactItem(player, hand);
                if (result.isAccepted()) {
                    if (result instanceof ActionResult.Success success && success.swingSource().equals(ActionResult.SwingSource.CLIENT)) {
                        gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                        player.swingHand(hand);
                    }
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "onResolutionChanged", at = @At("TAIL"))
    private void applyDarkMode(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        WindowStyle.setDarkMode(client.getWindow().getHandle());
    }
}

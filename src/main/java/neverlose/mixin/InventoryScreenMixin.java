package neverlose.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import neverlose.modules.impl.render.ChunkAnimator;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Unique
    private long neverlose$openTime;
    @Unique
    private boolean neverlose$animatedInventoryFrame;

    @Inject(method = "init", at = @At("TAIL"))
    private void addDropAllButton(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        InventoryScreen screen = (InventoryScreen) (Object) this;

        int x = screen.width / 2 - 40;
        int y = screen.height / 2 - 120;

        ButtonWidget dropAllButton = ButtonWidget.builder(
                Text.of("Выкинуть всё"),
                button -> dropAllItems(mc)
        ).position(x, y).size(80, 20).build();

        screen.addDrawableChild(dropAllButton);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void neverlose$animateInventoryHead(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ChunkAnimator animation = ChunkAnimator.getInstance();
        if (animation == null || !animation.shouldAnimateInventory()) {
            neverlose$animatedInventoryFrame = false;
            return;
        }

        if (neverlose$openTime == 0L) {
            neverlose$openTime = System.currentTimeMillis();
        }

        InventoryScreen screen = (InventoryScreen) (Object) this;
        float progress = animation.getScreenProgress(System.currentTimeMillis() - neverlose$openTime);
        float scale = 0.92f + progress * 0.08f;
        float alphaLift = (1.0f - progress) * 6.0f;

        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(screen.width / 2.0f, screen.height / 2.0f - alphaLift);
        matrices.scale(scale, scale);
        matrices.translate(-screen.width / 2.0f, -screen.height / 2.0f);
        neverlose$animatedInventoryFrame = true;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void neverlose$animateInventoryTail(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (neverlose$animatedInventoryFrame) {
            context.getMatrices().popMatrix();
            neverlose$animatedInventoryFrame = false;
        }
    }

    private void dropAllItems(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        if (player == null || player.currentScreenHandler == null) return;

        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                mc.interactionManager.clickSlot(
                        player.currentScreenHandler.syncId,
                        i,
                        1,
                        SlotActionType.THROW,
                        player
                );
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                mc.interactionManager.clickSlot(
                        player.currentScreenHandler.syncId,
                        i + 36,
                        1,
                        SlotActionType.THROW,
                        player
                );
            }
        }
    }
}

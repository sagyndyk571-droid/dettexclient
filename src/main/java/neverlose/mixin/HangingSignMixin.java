/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.Model
 *  net.minecraft.client.model.Model$SinglePartModel
 *  net.minecraft.client.render.OverlayTexture
 *  net.minecraft.client.render.block.entity.HangingSignBlockEntityRenderer
 *  net.minecraft.client.render.command.ModelCommandRenderer$CrumblingOverlayCommand
 *  net.minecraft.client.render.command.OrderedRenderCommandQueue
 *  net.minecraft.client.texture.SpriteHolder
 *  net.minecraft.client.util.SpriteIdentifier
 *  net.minecraft.client.util.math.MatrixStack
 *  net.minecraft.util.Unit
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package neverlose.mixin;

import java.util.Objects;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.block.entity.HangingSignBlockEntityRenderer;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Unit;
import neverlose.util.hmi.HandMyItemsRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={HangingSignBlockEntityRenderer.class})
public abstract class HangingSignMixin {
    @Inject(method={"renderAsItem"}, at={@At(value="HEAD")}, cancellable=true)
    private static void render(SpriteHolder spriteHolder, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, int j, Model.SinglePartModel singlePartModel, SpriteIdentifier spriteIdentifier, CallbackInfo ci) {
        if (!HandMyItemsRuntime.isActive()) {
            return;
        }
        matrixStack.push();
        HangingSignBlockEntityRenderer.setAngles((MatrixStack)matrixStack, (float)0.0f);
        matrixStack.scale(1.0f, -1.0f, -1.0f);
        Unit var10002 = Unit.INSTANCE;
        Objects.requireNonNull(singlePartModel);
        orderedRenderCommandQueue.submitModel((Model)singlePartModel, (Object)var10002, matrixStack, spriteIdentifier.getRenderLayer(arg_0 -> ((Model.SinglePartModel)singlePartModel).getLayer(arg_0)), i, OverlayTexture.DEFAULT_UV, -1, spriteHolder.getSprite(spriteIdentifier), 0, (ModelCommandRenderer.CrumblingOverlayCommand)null);
        matrixStack.pop();
        ci.cancel();
    }
}

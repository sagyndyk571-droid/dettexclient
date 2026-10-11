/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.network.AbstractClientPlayerEntity
 *  net.minecraft.client.network.ClientPlayerEntity
 *  net.minecraft.client.render.OverlayVertexConsumer
 *  net.minecraft.client.render.RenderLayer
 *  net.minecraft.client.render.RenderLayers
 *  net.minecraft.client.render.TexturedRenderLayers
 *  net.minecraft.client.render.VertexConsumer
 *  net.minecraft.client.render.VertexConsumerProvider
 *  net.minecraft.client.render.VertexConsumers
 *  net.minecraft.client.render.command.ItemCommandRenderer
 *  net.minecraft.client.render.item.ItemRenderState$Glint
 *  net.minecraft.client.render.item.ItemRenderer
 *  net.minecraft.client.render.model.BakedQuad
 *  net.minecraft.client.util.math.MatrixStack
 *  net.minecraft.client.util.math.MatrixStack$Entry
 *  net.minecraft.item.ItemDisplayContext
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.math.ColorHelper
 *  net.minecraft.util.math.MatrixUtil
 *  org.joml.Matrix4f
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package neverlose.mixin;

import com.holdmylua.source.global.DispatcherStorage;
import com.holdmylua.source.global.GlobalsStorage;
import com.holdmylua.source.global.item_model.ItemModelContext;
import com.holdmylua.source.global.item_model.ItemModelStorage;
import com.holdmylua.source.lua_runtime.ModelScriptCache;
import com.holdmylua.source.lua_runtime.ScriptHolder;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.OverlayVertexConsumer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumers;
import net.minecraft.client.render.command.ItemCommandRenderer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MatrixUtil;
import neverlose.util.hmi.HandMyItemsRuntime;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={ItemCommandRenderer.class})
public abstract class ItemRendererMixin {
    @Unique
    private static int getTint(int[] tints, int index) {
        return index >= 0 && index < tints.length ? tints[index] : -1;
    }

    @Unique
    private static boolean useTranslucentGlint(RenderLayer renderLayer) {
        return MinecraftClient.usesImprovedTransparency() && renderLayer == TexturedRenderLayers.getItemTranslucentCull();
    }

    @Redirect(method={"render"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/render/item/ItemRenderer;renderItem(Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II[ILjava/util/List;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/item/ItemRenderState$Glint;)V"))
    private static void render(ItemDisplayContext displayContext, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, int[] tints, List<BakedQuad> quads, RenderLayer layer, ItemRenderState.Glint glint) {
        VertexConsumer vertexConsumer;
        if (glint == ItemRenderState.Glint.SPECIAL) {
            MatrixStack.Entry entry = matrices.peek().copy();
            if (displayContext == ItemDisplayContext.GUI) {
                MatrixUtil.scale((Matrix4f)entry.getPositionMatrix(), (float)0.5f);
            } else if (displayContext.isFirstPerson()) {
                MatrixUtil.scale((Matrix4f)entry.getPositionMatrix(), (float)0.75f);
            }
            vertexConsumer = ItemRendererMixin.getSpecialItemGlintConsumer(vertexConsumers, layer, entry);
        } else {
            vertexConsumer = ItemRenderer.getItemGlintConsumer((VertexConsumerProvider)vertexConsumers, (RenderLayer)layer, (boolean)true, (glint != ItemRenderState.Glint.NONE ? 1 : 0) != 0);
        }
        if (!HandMyItemsRuntime.isActive()) {
            ItemRendererMixin.renderBakedQuadsWithoutHmi(matrices, vertexConsumer, quads, tints, light, overlay);
            return;
        }
        ItemRendererMixin.customRenderBakedQuads(matrices, vertexConsumer, quads, tints, light, overlay, displayContext);
    }

    @Unique
    private static void renderBakedQuadsWithoutHmi(MatrixStack matrices, VertexConsumer vertexConsumer, List<BakedQuad> quads, int[] tints, int light, int overlay) {
        MatrixStack.Entry entry = matrices.peek();
        for (BakedQuad bakedQuad : quads) {
            float alpha;
            float red;
            float green;
            float blue;
            if (bakedQuad.hasTint()) {
                int tint = ItemRendererMixin.getTint(tints, bakedQuad.tintIndex());
                alpha = (float)ColorHelper.getAlpha(tint) / 255.0f;
                red = (float)ColorHelper.getRed(tint) / 255.0f;
                green = (float)ColorHelper.getGreen(tint) / 255.0f;
                blue = (float)ColorHelper.getBlue(tint) / 255.0f;
            } else {
                alpha = 1.0f;
                red = 1.0f;
                green = 1.0f;
                blue = 1.0f;
            }
            vertexConsumer.quad(entry, bakedQuad, red, green, blue, alpha, light, overlay);
        }
    }

    @Unique
    private static void customRenderBakedQuads(MatrixStack matrices, VertexConsumer vertexConsumer, List<BakedQuad> quads, int[] tints, int light, int overlay, ItemDisplayContext displayContext) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        int index = 0;
        if ((displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) && MinecraftClient.getInstance().getEntityRenderDispatcher().gameOptions.getPerspective().isFirstPerson() && player == MinecraftClient.getInstance().player) {
            ItemModelContext context = ItemModelStorage.get();
            ItemStack renderedItem = DispatcherStorage.getRenderedItem();
            ScriptHolder.itemModelCache.executeModel(context, renderedItem, (AbstractClientPlayerEntity)player, GlobalsStorage.modelPartAnimator);
            for (ModelScriptCache cache : ScriptHolder.itemModelAddonsCache) {
                cache.executeModel(context, renderedItem, (AbstractClientPlayerEntity)player, GlobalsStorage.modelPartAnimator);
            }
        }
        for (BakedQuad bakedQuad : quads) {
            float j;
            float h;
            float g;
            float f;
            if (bakedQuad.hasTint()) {
                int i = ItemRendererMixin.getTint(tints, bakedQuad.tintIndex());
                f = (float)ColorHelper.getAlpha((int)i) / 255.0f;
                g = (float)ColorHelper.getRed((int)i) / 255.0f;
                h = (float)ColorHelper.getGreen((int)i) / 255.0f;
                j = (float)ColorHelper.getBlue((int)i) / 255.0f;
            } else {
                f = 1.0f;
                g = 1.0f;
                h = 1.0f;
                j = 1.0f;
            }
            matrices.push();
            GlobalsStorage.modelPartAnimator.applyPoses(index, matrices);
            MatrixStack.Entry entry = matrices.peek();
            vertexConsumer.quad(entry, bakedQuad, g, h, j, f, light, overlay);
            matrices.pop();
            ++index;
        }
        GlobalsStorage.modelPartAnimator.clear();
    }

    @Unique
    private static VertexConsumer getSpecialItemGlintConsumer(VertexConsumerProvider consumers, RenderLayer layer, MatrixStack.Entry matrix) {
        return VertexConsumers.union((VertexConsumer)new OverlayVertexConsumer(consumers.getBuffer(ItemRendererMixin.useTranslucentGlint(layer) ? RenderLayers.glintTranslucent() : RenderLayers.glint()), matrix, 0.0078125f), (VertexConsumer)consumers.getBuffer(layer));
    }
}

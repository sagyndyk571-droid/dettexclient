/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.render.item.property.numeric.UseDurationProperty
 *  net.minecraft.client.world.ClientWorld
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.HeldItemContext
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package neverlose.mixin;

import com.holdmylua.source.global.GlobalsStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.property.numeric.UseDurationProperty;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import neverlose.util.hmi.HandMyItemsRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={UseDurationProperty.class})
public class UseDurationMixin {
    @Inject(method={"getValue"}, at={@At(value="RETURN")}, cancellable=true)
    private void swapProperty(ItemStack stack, ClientWorld world, HeldItemContext context, int seed, CallbackInfoReturnable<Float> cir) {
        if (HandMyItemsRuntime.isActive() && MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.getActiveItem() == stack && GlobalsStorage.useDuration.containsKey(stack.getItem().toString())) {
            cir.setReturnValue(Float.valueOf(Float.parseFloat(GlobalsStorage.useDuration.get(stack.getItem().toString()).toString())));
        }
    }
}

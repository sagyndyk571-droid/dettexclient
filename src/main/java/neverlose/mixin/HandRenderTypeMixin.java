/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Mutable
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package neverlose.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets={"net/minecraft/client/render/item/HeldItemRenderer$HandRenderType"})
public abstract class HandRenderTypeMixin {
    @Shadow
    @Mutable
    private boolean renderMainHand;
    @Shadow
    @Mutable
    private boolean renderOffHand;

    @Inject(method={"<init>"}, at={@At(value="RETURN")})
    private void makeBothHandsRenderTrue(String string, int i, boolean renderMainHand, boolean renderOffHand, CallbackInfo ci) {
        this.renderMainHand = true;
        this.renderOffHand = true;
    }
}


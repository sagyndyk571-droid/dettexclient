/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.render.command.RenderDispatcher
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package neverlose.mixin;

import com.holdmylua.source.global.DispatcherStorage;
import com.holdmylua.source.global.item_model.ItemModelStorage;
import net.minecraft.client.render.command.RenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={RenderDispatcher.class})
public class ItemRenderStateMixin {
    @Inject(method={"render"}, at={@At(value="TAIL")})
    private void tester(CallbackInfo ci) {
        DispatcherStorage.clear();
        ItemModelStorage.clear();
    }
}


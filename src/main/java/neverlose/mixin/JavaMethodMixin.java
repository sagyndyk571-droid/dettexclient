/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.luaj.vm2.LuaValue
 *  org.luaj.vm2.Varargs
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package neverlose.mixin;

import com.holdmylua.source.annotation.Safe;
import java.lang.reflect.Method;
import java.util.Map;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets={"org/luaj/vm2/lib/jse/JavaMethod"}, remap=false)
public class JavaMethodMixin {
    @Shadow
    @Final
    private Method method;
    @Shadow
    @Final
    private static Map methods;

    @Inject(method={"invokeMethod"}, at={@At(value="RETURN")}, cancellable=true)
    private void isSafe(Object par1, Varargs par2, CallbackInfoReturnable<LuaValue> cir) {
        if (!(this.method.isAnnotationPresent(Safe.class) || this.method.getName().equals("getOrDefault") || this.method.getName().equals("put"))) {
            cir.setReturnValue(LuaValue.NIL);
        }
    }

    static {
        System.out.println("HMI Lua safety layer loaded!");
    }
}

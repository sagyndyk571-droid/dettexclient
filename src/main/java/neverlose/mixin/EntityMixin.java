package neverlose.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import neverlose.IMinecraft;
import neverlose.events.api.EventManager;
import neverlose.events.impl.BoundingBoxControlEvent;
import neverlose.events.impl.MovePostEvent;
import neverlose.events.impl.PlayerVelocityStrafeEvent;
import neverlose.modules.impl.combat.aura.AngleConnection;

@Mixin(Entity.class)
public abstract class EntityMixin implements IMinecraft {

    @Shadow private Box boundingBox;
    @Shadow public float yaw;
    @Unique
    private boolean client$local;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(EntityType<?> type, World world, CallbackInfo ci) {
        client$local = (Entity)(Object)this instanceof ClientPlayerEntity;
    }
   
    @Unique
    private final MinecraftClient client = MinecraftClient.getInstance();
  
    @Inject(method = "getBoundingBox", at = @At("HEAD"), cancellable = true)
    public final void getBoundingBox(CallbackInfoReturnable<Box> cir) {
        BoundingBoxControlEvent event = new BoundingBoxControlEvent(boundingBox, (Entity) (Object) this);
        EventManager.callEvent(event);
        cir.setReturnValue(event.getBox());
    }
    
    @Redirect(method = "updateVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d hookVelocity(Vec3d movementInput, float speed, float yaw) {
        if ((Object) this == mc.player) {
            PlayerVelocityStrafeEvent event = new PlayerVelocityStrafeEvent(movementInput, speed, yaw, Entity.movementInputToVelocity(movementInput, speed, yaw));
            EventManager.callEvent(event);
            return event.getVelocity();
        }
        return Entity.movementInputToVelocity(movementInput, speed, yaw);
    }

    @Inject(method = "updateVelocity", at = @At("TAIL"))
    private void neverlose$movePost(float speed, Vec3d movementInput, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (mc.player != null && entity.getId() == mc.player.getId()) {
            EventManager.callEvent(new MovePostEvent(speed, movementInput));
        }
    }
    
    @ModifyVariable(method = "getRotationVector(FF)Lnet/minecraft/util/math/Vec3d;", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float modifyPitch(float pitch) {
        if ((Object) this instanceof ClientPlayerEntity && AngleConnection.INSTANCE.getCurrentAngle() !=null) {
            return AngleConnection.INSTANCE.getCurrentAngle().getPitch();
        }
        return pitch;
    }
}

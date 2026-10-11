package neverlose.mixin;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.entity.Entity;
import neverlose.modules.impl.render.FpsBoost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityRenderManager.class, priority = 500)
public abstract class EntityRenderManagerMixin {

    @Inject(method = "shouldRender(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/Frustum;DDD)Z", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void neverlose$cullEntities(E entity, Frustum frustum,
                                                         double cameraX, double cameraY, double cameraZ,
                                                         CallbackInfoReturnable<Boolean> cir) {
        if (entity == net.minecraft.client.MinecraftClient.getInstance().player) return;

        FpsBoost fpsBoost = FpsBoost.getInstance();
        if (fpsBoost == null || !fpsBoost.isState()) return;

        double dx = entity.getX() - cameraX;
        double dy = entity.getY() - cameraY;
        double dz = entity.getZ() - cameraZ;

        if (fpsBoost.shouldCullEntity(entity, dx * dx + dy * dy + dz * dz)) {
            cir.setReturnValue(false);
        }
    }
}
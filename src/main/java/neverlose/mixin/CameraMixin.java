/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.client.render.Camera
 *  net.minecraft.util.math.RotationAxis
 *  net.minecraft.util.math.Vec3d
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package neverlose.mixin;

import com.holdmylua.source.access.CameraAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import neverlose.events.api.EventManager;
import neverlose.events.impl.CameraEvent;
import neverlose.events.impl.CameraPositionEvent;
import neverlose.modules.impl.combat.aura.Angle;
import neverlose.modules.impl.render.ChunkAnimator;
import neverlose.util.hmi.HandMyItemsRuntime;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Camera.class})
public abstract class CameraMixin
        implements CameraAccessor {
    @Shadow
    @Final
    private Quaternionf rotation;
    @Shadow
    private float pitch;
    @Shadow
    private float yaw;
    @Shadow
    @Final
    private static Vector3f HORIZONTAL;
    @Shadow
    @Final
    private static Vector3f VERTICAL;
    @Shadow
    @Final
    private static Vector3f DIAGONAL;
    @Shadow
    @Final
    private Vector3f horizontalPlane;
    @Shadow
    @Final
    private Vector3f verticalPlane;
    @Shadow
    @Final
    private Vector3f diagonalPlane;
    @Shadow
    private Vec3d pos;
    @Unique
    private float pitchM = 0.0f;
    @Unique
    private float yawM = 0.0f;
    @Unique
    private float rollM = 0.0f;
    @Unique
    private float xM = 0.0f;
    @Unique
    private float yM = 0.0f;
    @Unique
    private float zM = 0.0f;
    @Unique
    private boolean neverlose$lastFirstPerson = true;
    @Unique
    private long neverlose$perspectiveStartTime;
    @Unique
    private Vec3d neverlose$perspectiveFrom;
    @Shadow
    protected abstract void setPos(Vec3d var1);

    @Shadow @Final private BlockPos.Mutable blockPos;

    @Shadow public abstract void setRotation(float yaw, float pitch);

    @Shadow protected abstract void moveBy(float f, float g, float h);

    @Shadow protected abstract float clipToSpace(float f);
    @Override
    public void hMI5_0$applyRotation() {
        this.rotation.add((Quaternionfc)RotationAxis.POSITIVE_X.rotationDegrees(this.pitchM));
        this.rotation.add((Quaternionfc)RotationAxis.POSITIVE_Y.rotationDegrees(this.yawM));
        this.rotation.add((Quaternionfc)RotationAxis.POSITIVE_Z.rotationDegrees(this.rollM));
    }

    @Override
    public void hMI5_0$setRotationValues(float pitch, float yaw, float roll) {
        this.pitchM = pitch;
        this.yawM = yaw;
        this.rollM = roll;
    }

    @Override
    public void hMI5_0$setPosValues(float x, float y, float z) {
        this.xM = x;
        this.yM = y;
        this.zM = z;
    }
    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V", shift = At.Shift.AFTER), cancellable = true)
    private void updateHook(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
        CameraEvent event = new CameraEvent(false, 4, new Angle(yaw, pitch));
        EventManager.callEvent(event);
        Angle angle = event.getAngle();
        if (event.isCancelled() && focusedEntity instanceof ClientPlayerEntity player && !player.isSleeping() && thirdPerson) {
            float pitch = inverseView ? -angle.getPitch() : angle.getPitch();
            float yaw = angle.getYaw() - (inverseView ? 180 : 0);
            float distance = event.getDistance();
            setRotation(yaw, pitch);
            moveBy(event.isCameraClip() ? -distance : -clipToSpace(distance), 0.0F, 0.0F);
            ci.cancel();
        }
    }

    @Inject(method = "setPos(Lnet/minecraft/util/math/Vec3d;)V", at = @At("HEAD"), cancellable = true)
    private void posHook(Vec3d pos, CallbackInfo ci) {
        CameraPositionEvent event = new CameraPositionEvent(pos);
        EventManager.callEvent(event);
        this.pos = pos = neverlose$animatePerspectivePosition(event.getPos());
        blockPos.set(pos.x,pos.y,pos.z);
        ci.cancel();
    }

    @Unique
    private Vec3d neverlose$animatePerspectivePosition(Vec3d targetPos) {
        MinecraftClient client = MinecraftClient.getInstance();
        ChunkAnimator animation = ChunkAnimator.getInstance();
        if (client == null || animation == null || !animation.shouldAnimatePerspective()) {
            neverlose$perspectiveStartTime = 0L;
            neverlose$perspectiveFrom = null;
            neverlose$lastFirstPerson = client == null || client.options.getPerspective().isFirstPerson();
            return targetPos;
        }

        boolean firstPerson = client.options.getPerspective().isFirstPerson();
        if (firstPerson) {
            neverlose$perspectiveStartTime = 0L;
            neverlose$perspectiveFrom = null;
            neverlose$lastFirstPerson = true;
            return targetPos;
        }

        long now = System.currentTimeMillis();
        if (firstPerson != neverlose$lastFirstPerson) {
            neverlose$lastFirstPerson = firstPerson;
            neverlose$perspectiveStartTime = now;
            neverlose$perspectiveFrom = this.pos == null ? targetPos : this.pos;
        }

        if (neverlose$perspectiveStartTime == 0L || neverlose$perspectiveFrom == null) {
            return targetPos;
        }

        float progress = animation.getScreenProgress(now - neverlose$perspectiveStartTime);
        if (progress >= 0.995f) {
            neverlose$perspectiveStartTime = 0L;
            neverlose$perspectiveFrom = null;
            return targetPos;
        }
        return neverlose$perspectiveFrom.lerp(targetPos, progress);
    }
    @Inject(method={"setRotation"}, at={@At(value="HEAD")}, cancellable=true)
    public void setRotationMixin(float yaw, float pitch, CallbackInfo ci) {
        if (!HandMyItemsRuntime.isActive()) {
            return;
        }
        if (!FabricLoader.getInstance().isModLoaded("do_a_barrel_roll")) {
            this.pitch = pitch;
            this.yaw = yaw;
            this.rotation.rotationYXZ((float)Math.PI - (yaw + this.yawM) * ((float)Math.PI / 180), (-pitch + this.pitchM) * ((float)Math.PI / 180), this.rollM * ((float)Math.PI / 180));
            HORIZONTAL.rotate((Quaternionfc)this.rotation, this.horizontalPlane);
            VERTICAL.rotate((Quaternionfc)this.rotation, this.verticalPlane);
            DIAGONAL.rotate((Quaternionfc)this.rotation, this.diagonalPlane);
            ci.cancel();
        }
    }

    @Inject(method={"setPos(DDD)V"}, at={@At(value="HEAD")}, cancellable=true)
    public void setRotationMixin(double x, double y, double z, CallbackInfo ci) {
        if (!HandMyItemsRuntime.isActive()) {
            return;
        }
        if (!FabricLoader.getInstance().isModLoaded("do_a_barrel_roll")) {
            this.setPos(new Vec3d(x += (double)this.xM, y += (double)this.yM, z += (double)this.zM));
            ci.cancel();
        }
    }
}

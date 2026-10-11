package neverlose.cosmetics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import neverlose.util.render.сliemtpipeline.ClientPipelines;
import org.joml.Matrix4f;

import java.util.LinkedHashSet;
import java.util.Set;

public class CosmeticRenderer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final float SCALE = 1.8f / 24f;
    private static final int MAX_BONE_DEPTH = 128;

    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue,
                       CosmeticModel model, int light, int overlay) {
        render(matrices, queue, model, light, overlay, false, CosmeticRenderSpace.PLAYER_MODEL);
    }

    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue,
                       CosmeticModel model, int light, int overlay, boolean flipY) {
        render(matrices, queue, model, light, overlay, flipY, CosmeticRenderSpace.PLAYER_MODEL);
    }

    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue,
                       CosmeticModel model, int light, int overlay, boolean flipY,
                       CosmeticRenderSpace space) {
        if (model == null || model.isRenderFaulted() || mc.world == null) return;

        try {
            model.loadTexture();
            if (model.getRenderRoots().isEmpty()) {
                model.markRenderFault("Model has no renderable bones");
                return;
            }

            Set<CosmeticTextureSlot> slots = collectTextureSlots(model);
            if (slots.isEmpty()) {
                model.markRenderFault("Model has no usable textures");
                return;
            }

            float scaleY = flipY ? -SCALE : SCALE;
            for (CosmeticTextureSlot slot : slots) {
                queue.submitCustom(matrices, ClientPipelines.COSMETIC_MODEL.apply(slot.getId()), (entry, consumer) -> {
                    if (model.isRenderFaulted()) return;
                    try {
                        Matrix4f rootMatrix = new Matrix4f(entry.getPositionMatrix()).scale(SCALE, scaleY, SCALE);
                        for (CosmeticBone root : model.getRenderRoots()) {
                            renderBone(consumer, rootMatrix, model, root, slot, 0);
                        }
                    } catch (RuntimeException | LinkageError error) {
                        model.markRenderFault(error);
                    }
                });
            }
        } catch (RuntimeException | LinkageError error) {
            model.markRenderFault(error);
        }
    }

    private Set<CosmeticTextureSlot> collectTextureSlots(CosmeticModel model) {
        Set<CosmeticTextureSlot> slots = new LinkedHashSet<>();
        for (CosmeticBone root : model.getRenderRoots()) {
            collectTextureSlots(model, root, slots, 0);
        }
        return slots;
    }

    private void collectTextureSlots(CosmeticModel model, CosmeticBone bone,
                                     Set<CosmeticTextureSlot> slots, int depth) {
        if (depth > MAX_BONE_DEPTH) {
            throw new IllegalStateException("Cosmetic bone hierarchy is too deep");
        }
        if (model.shouldSkipBone(bone)) return;

        for (CosmeticCube cube : bone.getCubes()) {
            for (CosmeticCube.CubeFace face : cube.getFaces()) {
                if (face == null) continue;
                CosmeticTextureSlot slot = model.getTextureSlot(face.getTextureIndex());
                if (slot == null) continue;
                if (!slot.isLoaded()) model.ensureTextureSlotLoaded(slot);
                if (slot.isLoaded()) slots.add(slot);
            }
        }
        for (CosmeticBone child : bone.getChildren()) {
            collectTextureSlots(model, child, slots, depth + 1);
        }
    }

    private void renderBone(VertexConsumer consumer, Matrix4f parent, CosmeticModel model,
                            CosmeticBone bone, CosmeticTextureSlot targetSlot, int depth) {
        if (depth > MAX_BONE_DEPTH) {
            throw new IllegalStateException("Cosmetic bone hierarchy is too deep");
        }
        if (model.shouldSkipBone(bone)) return;

        Matrix4f boneMatrix = new Matrix4f(parent);
        applyBoneTransform(boneMatrix, bone);

        for (CosmeticCube cube : bone.getCubes()) {
            renderCube(consumer, boneMatrix, model, cube, targetSlot);
        }
        for (CosmeticBone child : bone.getChildren()) {
            renderBone(consumer, boneMatrix, model, child, targetSlot, depth + 1);
        }
    }

    private static void applyBoneTransform(Matrix4f matrix, CosmeticBone bone) {
        float px = bone.getPivot().x;
        float py = bone.getPivot().y;
        float pz = bone.getPivot().z;

        float apx = bone.getAnimPosition().x;
        float apy = bone.getAnimPosition().y;
        float apz = bone.getAnimPosition().z;
        if (apx != 0f || apy != 0f || apz != 0f) {
            matrix.translate(apx, apy, apz);
        }

        float rx = bone.getDefaultRotation().x + bone.getAnimRotation().x;
        float ry = bone.getDefaultRotation().y + bone.getAnimRotation().y;
        float rz = bone.getDefaultRotation().z + bone.getAnimRotation().z;
        if (rx != 0f || ry != 0f || rz != 0f) {
            matrix.translate(px, py, pz);
            matrix.rotate(RotationAxis.POSITIVE_X.rotationDegrees(rx));
            matrix.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(ry));
            matrix.rotate(RotationAxis.POSITIVE_Z.rotationDegrees(rz));
            matrix.translate(-px, -py, -pz);
        }
    }

    private void renderCube(VertexConsumer consumer, Matrix4f parent, CosmeticModel model,
                            CosmeticCube cube, CosmeticTextureSlot targetSlot) {
        Matrix4f cubeMatrix = new Matrix4f(parent);

        float ox = cube.getOrigin().x;
        float oy = cube.getOrigin().y;
        float oz = cube.getOrigin().z;
        float rx = cube.getRotation().x;
        float ry = cube.getRotation().y;
        float rz = cube.getRotation().z;
        if (rx != 0f || ry != 0f || rz != 0f) {
            cubeMatrix.translate(ox, oy, oz);
            cubeMatrix.rotate(RotationAxis.POSITIVE_X.rotationDegrees(rx));
            cubeMatrix.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(ry));
            cubeMatrix.rotate(RotationAxis.POSITIVE_Z.rotationDegrees(rz));
            cubeMatrix.translate(-ox, -oy, -oz);
        }

        float inflate = cube.getInflate();
        float x1 = cube.getFrom().x - inflate;
        float y1 = cube.getFrom().y - inflate;
        float z1 = cube.getFrom().z - inflate;
        float x2 = cube.getTo().x + inflate;
        float y2 = cube.getTo().y + inflate;
        float z2 = cube.getTo().z + inflate;
        if (!allFinite(x1, y1, z1, x2, y2, z2)) {
            throw new IllegalArgumentException("Cosmetic contains non-finite cube coordinates");
        }

        CosmeticCube.CubeFace[] faces = cube.getFaces();
        for (int side = 0; side < faces.length; side++) {
            CosmeticCube.CubeFace face = faces[side];
            if (face == null || model.getTextureSlot(face.getTextureIndex()) != targetSlot) continue;
            quadFace(consumer, cubeMatrix, face, targetSlot.getWidth(), targetSlot.getHeight(),
                    x1, y1, z1, x2, y2, z2, side);
        }
    }

    private static boolean allFinite(float... values) {
        for (float value : values) {
            if (!Float.isFinite(value)) return false;
        }
        return true;
    }

    private void quadFace(VertexConsumer buf, Matrix4f matrix,
                          CosmeticCube.CubeFace face, float textureWidth, float textureHeight,
                          float x1, float y1, float z1, float x2, float y2, float z2, int side) {
        float u1 = face.getU1();
        float v1 = face.getV1();
        float u2 = face.getU2();
        float v2 = face.getV2();
        if (!allFinite(u1, v1, u2, v2) || textureWidth <= 0f || textureHeight <= 0f) return;

        switch (side) {
            case 0 -> quad(buf, matrix, textureWidth, textureHeight,
                    x2, y2, z1, u1, v1, x1, y2, z1, u2, v1,
                    x1, y1, z1, u2, v2, x2, y1, z1, u1, v2);
            case 1 -> quad(buf, matrix, textureWidth, textureHeight,
                    x2, y2, z2, u1, v1, x2, y2, z1, u2, v1,
                    x2, y1, z1, u2, v2, x2, y1, z2, u1, v2);
            case 2 -> quad(buf, matrix, textureWidth, textureHeight,
                    x1, y2, z2, u1, v1, x2, y2, z2, u2, v1,
                    x2, y1, z2, u2, v2, x1, y1, z2, u1, v2);
            case 3 -> quad(buf, matrix, textureWidth, textureHeight,
                    x1, y2, z1, u1, v1, x1, y2, z2, u2, v1,
                    x1, y1, z2, u2, v2, x1, y1, z1, u1, v2);
            case 4 -> quad(buf, matrix, textureWidth, textureHeight,
                    x1, y2, z2, u1, v1, x2, y2, z2, u2, v1,
                    x2, y2, z1, u2, v2, x1, y2, z1, u1, v2);
            case 5 -> quad(buf, matrix, textureWidth, textureHeight,
                    x1, y1, z1, u1, v1, x2, y1, z1, u2, v1,
                    x2, y1, z2, u2, v2, x1, y1, z2, u1, v2);
            default -> {
            }
        }
    }

    private void quad(VertexConsumer buf, Matrix4f matrix, float textureWidth, float textureHeight,
                      float x1, float y1, float z1, float u1, float v1,
                      float x2, float y2, float z2, float u2, float v2,
                      float x3, float y3, float z3, float u3, float v3,
                      float x4, float y4, float z4, float u4, float v4) {
        buf.vertex(matrix, x1, y1, z1).texture(u1 / textureWidth, v1 / textureHeight).color(1f, 1f, 1f, 1f);
        buf.vertex(matrix, x2, y2, z2).texture(u2 / textureWidth, v2 / textureHeight).color(1f, 1f, 1f, 1f);
        buf.vertex(matrix, x3, y3, z3).texture(u3 / textureWidth, v3 / textureHeight).color(1f, 1f, 1f, 1f);
        buf.vertex(matrix, x4, y4, z4).texture(u4 / textureWidth, v4 / textureHeight).color(1f, 1f, 1f, 1f);
    }
}

package neverlose.modules.impl.render;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import neverlose.util.render.сliemtpipeline.ClientPipelines;
import org.joml.Matrix4f;

public class ChinaHatFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    private static final float PI2 = (float) (Math.PI * 2.0);
    private static final int FILL_SEGMENTS = 144;
    private static final int OUTLINE_SEGMENTS = 144;
    private static final int GLOW_SEGMENTS = 96;

    public ChinaHatFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                       PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        ChinaHat chinaHat = ChinaHat.getInstance();
        if (chinaHat == null || !chinaHat.shouldRender(state)) {
            return;
        }
        boolean hasHelmet = state.equippedHeadStack != null && !state.equippedHeadStack.isEmpty();
        float scale = state.baby ? 0.62f : 1.2f;
        float helmetOffset = hasHelmet ? 0.075f * scale : 0.0f;
        float brimY = (-0.415f) * scale - helmetOffset;
        float radius = 0.45f * scale;
        float height = 0.22f * scale;

        matrices.push();
        getContextModel().head.applyTransform(matrices);

        submitGlow(matrices, queue, chinaHat, radius, brimY);
        submitFill(matrices, queue, chinaHat, radius, brimY, height);
        submitOutline(matrices, queue, chinaHat, radius, brimY);

        matrices.pop();
    }

    private void submitFill(MatrixStack matrices, OrderedRenderCommandQueue queue, ChinaHat chinaHat,
                            float radius, float brimY, float height) {
        queue.submitCustom(matrices, ClientPipelines.CHINA_HAT, (entry, consumer) -> {
            Matrix4f matrix = entry.getPositionMatrix();
            int centerColor = chinaHat.getGradientColor(0, FILL_SEGMENTS, 210);
            float apexY = brimY - height;

            for (int i = 0; i < FILL_SEGMENTS; i++) {
                float angle = i * PI2 / FILL_SEGMENTS;
                float nextAngle = (i + 1) * PI2 / FILL_SEGMENTS;

                int edgeColor = chinaHat.getGradientColor(i, FILL_SEGMENTS, 205);
                int nextEdgeColor = chinaHat.getGradientColor(i + 1, FILL_SEGMENTS, 205);

                vertex(consumer, matrix, 0.0f, apexY, 0.0f, centerColor);
                vertex(consumer, matrix, -MathHelper.sin(angle) * radius, brimY, MathHelper.cos(angle) * radius, edgeColor);
                vertex(consumer, matrix, -MathHelper.sin(nextAngle) * radius, brimY, MathHelper.cos(nextAngle) * radius, nextEdgeColor);
            }
        });
    }

    private void submitOutline(MatrixStack matrices, OrderedRenderCommandQueue queue, ChinaHat chinaHat,
                               float radius, float brimY) {
        queue.submitCustom(matrices, ClientPipelines.CHINA_HAT_OUTLINE, (entry, consumer) -> {
            Matrix4f matrix = entry.getPositionMatrix();

            for (int i = 0; i <= OUTLINE_SEGMENTS; i++) {
                float angle = i * PI2 / OUTLINE_SEGMENTS;
                int color = chinaHat.getGradientColor(i, OUTLINE_SEGMENTS, 255);
                vertex(consumer, matrix, -MathHelper.sin(angle) * radius, brimY, MathHelper.cos(angle) * radius, color);
            }
        });
    }

    private void submitGlow(MatrixStack matrices, OrderedRenderCommandQueue queue, ChinaHat chinaHat,
                            float radius, float brimY) {
        queue.submitCustom(matrices, ClientPipelines.CHINA_HAT_GLOW, (entry, consumer) -> {
            Matrix4f matrix = entry.getPositionMatrix();
            float innerRadius = radius * 0.92f;
            float outerRadius = radius * 1.20f;

            for (int i = 0; i < GLOW_SEGMENTS; i++) {
                float angle = i * PI2 / GLOW_SEGMENTS;
                float nextAngle = (i + 1) * PI2 / GLOW_SEGMENTS;

                int innerColor = chinaHat.getGradientColor(i, GLOW_SEGMENTS, 54);
                int nextInnerColor = chinaHat.getGradientColor(i + 1, GLOW_SEGMENTS, 54);
                int outerColor = chinaHat.getGradientColor(i, GLOW_SEGMENTS, 0);
                int nextOuterColor = chinaHat.getGradientColor(i + 1, GLOW_SEGMENTS, 0);

                vertex(consumer, matrix, -MathHelper.sin(angle) * innerRadius, brimY, MathHelper.cos(angle) * innerRadius, innerColor);
                vertex(consumer, matrix, -MathHelper.sin(angle) * outerRadius, brimY, MathHelper.cos(angle) * outerRadius, outerColor);
                vertex(consumer, matrix, -MathHelper.sin(nextAngle) * outerRadius, brimY, MathHelper.cos(nextAngle) * outerRadius, nextOuterColor);
                vertex(consumer, matrix, -MathHelper.sin(nextAngle) * innerRadius, brimY, MathHelper.cos(nextAngle) * innerRadius, nextInnerColor);
            }
        });
    }

    private void vertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, int color) {
        consumer.vertex(matrix, x, y, z).color(color);
    }
}

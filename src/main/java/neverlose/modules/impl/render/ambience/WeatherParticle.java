package neverlose.modules.impl.render.ambience;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import neverlose.modules.impl.render.worldparticles.ParticleRenderer;

public class WeatherParticle {

    public static final Identifier SNOW_TEXTURE = Identifier.of("neverlose", "textures/world/snowflake.png");
    public static final Identifier SPLASH_TEXTURE = Identifier.of("neverlose", "textures/world/dashbloom.png");

    public enum Kind {
        SNOW,
        RAIN,
        SPLASH
    }

    public final Kind kind;

    public double x, y, z;
    public double vx, vy, vz;

    public float size;
    public float rotation;
    public float rotSpeed;

    public float age;
    public float maxLife;

    public boolean dead;

    public WeatherParticle(Kind kind) {
        this.kind = kind;
    }

    public void update(float dt) {
        if (kind == Kind.SPLASH) {
            age += dt;
            if (age >= maxLife) dead = true;
            return;
        }

        x += vx * dt;
        y += vy * dt;
        z += vz * dt;
        rotation += rotSpeed * dt;
    }

    public void render(MatrixStack matrices, VertexConsumerProvider immediate, Vec3d cam, float yaw, float pitch) {
        float rx = (float) (x - cam.x);
        float ry = (float) (y - cam.y);
        float rz = (float) (z - cam.z);

        if (kind == Kind.RAIN) {
            matrices.push();
            matrices.translate(rx, ry, rz);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));

            Matrix4f m = matrices.peek().getPositionMatrix();
            VertexConsumer b = immediate.getBuffer(ParticleRenderer.getQuadsLayer());

            float hw = 0.024f;
            float hh = size;
            int a = 185;
            b.vertex(m, -hw, -hh, 0).color(198, 222, 255, a);
            b.vertex(m, -hw, hh, 0).color(198, 222, 255, a);
            b.vertex(m, hw, hh, 0).color(198, 222, 255, a);
            b.vertex(m, hw, -hh, 0).color(198, 222, 255, a);

            matrices.pop();
            return;
        }

        if (kind == Kind.SNOW) {
            matrices.push();
            matrices.translate(rx, ry, rz);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));

            Matrix4f m = matrices.peek().getPositionMatrix();
            VertexConsumer b = immediate.getBuffer(ParticleRenderer.getGlowLayer(SNOW_TEXTURE));

            float half = size;
            int a = 205;
            b.vertex(m, -half, -half, 0).texture(0, 0).color(255, 255, 255, a);
            b.vertex(m, -half, half, 0).texture(0, 1).color(255, 255, 255, a);
            b.vertex(m, half, half, 0).texture(1, 1).color(255, 255, 255, a);
            b.vertex(m, half, -half, 0).texture(1, 0).color(255, 255, 255, a);

            matrices.pop();
            return;
        }

        // SPLASH — flat ripple lying on the ground.
        float progress = maxLife <= 0f ? 1f : Math.min(1f, age / maxLife);
        float s = size * (0.35f + 1.15f * progress);
        int a = (int) (175 * (1f - progress));
        if (a <= 0) return;

        matrices.push();
        matrices.translate(rx, ry, rz);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));

        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer b = immediate.getBuffer(ParticleRenderer.getGlowLayer(SPLASH_TEXTURE));

        b.vertex(m, -s, -s, 0).texture(0, 0).color(185, 212, 255, a);
        b.vertex(m, -s, s, 0).texture(0, 1).color(185, 212, 255, a);
        b.vertex(m, s, s, 0).texture(1, 1).color(185, 212, 255, a);
        b.vertex(m, s, -s, 0).texture(1, 0).color(185, 212, 255, a);

        matrices.pop();
    }
}

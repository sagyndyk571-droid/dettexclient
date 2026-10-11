package neverlose.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.Vec3d;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.PacketEvent;
import neverlose.events.impl.TickEvent;
import neverlose.events.impl.WorldChangeEvent;
import neverlose.events.impl.WorldLoadEvent;
import neverlose.events.impl.WorldRenderEvent;
import neverlose.modules.impl.render.particles.Particle3D;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.ColorSetting;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.Instance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeadChams extends ModuleStructure {

    public static DeadChams getInstance() {
        return Instance.get(DeadChams.class);
    }

    final List<Particle3D> particles = new ArrayList<>();
    final Set<Integer> processedDeathEntities = new HashSet<>();

    public SelectSetting mode = new SelectSetting("Форма", "Форма эффекта смерти")
            .value("Кубы", "Корона", "Куб", "Доллар", "Сердце", "Молния", "Линия", "Ромб", "Снежинка", "Звезда", "Звезда 2", "Треугольник", "Рандом")
            .selected("Звезда");

    public SelectSetting glowMode = new SelectSetting("Свечение", "Поведение свечения")
            .value("Bloom", "Bloom Sample", "Оба")
            .selected("Bloom Sample");

    public SliderSettings deathAmount = new SliderSettings("Плотность", "Количество фрагментов при смерти")
            .range(80, 420)
            .setValue(260);

    public SliderSettings spread = new SliderSettings("Разлёт", "Сила разлёта фрагментов")
            .range(0.5f, 3.0f)
            .setValue(1.0f);

    public SliderSettings speed = new SliderSettings("Импульс", "Скорость движения фрагментов")
            .range(0.1f, 3.0f)
            .setValue(2.0f);

    public SliderSettings lifeTime = new SliderSettings("Длительность", "Время жизни фрагментов")
            .range(0.5f, 10f)
            .setValue(2.5f);

    public SliderSettings size = new SliderSettings("Масштаб", "Размер фрагментов")
            .range(0.1f, 1.0f)
            .setValue(1f);

    public BooleanSetting randomColor = new BooleanSetting("Случайный цвет", "Каждый фрагмент получает случайный оттенок")
            .setValue(false);

    public ColorSetting deathColor = new ColorSetting("Цвет распада", "Основной цвет эффекта смерти")
            .value(0xFF58B8FF)
            .visible(() -> !randomColor.isValue());

    private static final float GLOW_SIZE = 7.5f;
    private static final float GRAVITY_STRENGTH = 0.04f;

    private static final int[] RANDOM_COLORS = {
            0xFFFF0000,
            0xFFFF7F00,
            0xFFFFFF00,
            0xFF00FF00,
            0xFF00FFFF,
            0xFF0000FF,
            0xFF8B00FF,
            0xFFFF00FF,
            0xFFFF1493,
            0xFFFFFFFF,
            0xFF00FF7F,
            0xFFFF6347
    };

    public DeadChams() {
        super("DeadChams", "Эффект распада при смерти", ModuleCategory.RENDER);
        settings(mode, glowMode, deathAmount, spread, speed, lifeTime, size, randomColor, deathColor);
    }

    @Override
    public void deactivate() {
        clearAll();
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        clearAll();
    }

    @EventHandler
    public void onWorldChange(WorldChangeEvent e) {
        clearAll();
    }

    private void clearAll() {
        particles.clear();
        processedDeathEntities.clear();
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.getType() != PacketEvent.Type.RECEIVE || mc.world == null) {
            return;
        }

        if (!(e.getPacket() instanceof EntityStatusS2CPacket packet)) {
            return;
        }

        Entity entity = packet.getEntity(mc.world);
        if (entity == null) {
            return;
        }

        if (packet.getStatus() == 35) {
            onTotemPop(entity);
            return;
        }

        if (packet.getStatus() == 3 && processedDeathEntities.add(entity.getId())) {
            onEntityDeath(entity);
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) {
            clearAll();
            return;
        }

        Set<Integer> activeEntityIds = new HashSet<>();
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }

            activeEntityIds.add(entity.getId());
            if (living.getHealth() <= 0.0f && processedDeathEntities.add(entity.getId())) {
                onEntityDeath(entity);
            }
        }

        processedDeathEntities.removeIf(id -> !activeEntityIds.contains(id));

        Iterator<Particle3D> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle3D particle = iterator.next();
            particle.update();
            if (particle.isDead()) {
                iterator.remove();
            }
        }
    }

    public void onTotemPop(Entity entity) {
        onEntityDeath(entity);
    }

    public void onEntityDeath(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return;
        }

        double width = Math.max(entity.getWidth(), 0.55f);
        double height = Math.max(entity.getHeight(), 0.6f);
        double yaw = Math.toRadians(-entity.getYaw());
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        int count = deathAmount.getInt();

        if (height / width > 2.2) {
            spawnDeathBox(entity, count * 18 / 100, 0.0, height * 0.88, 0.0, width * 0.85, height * 0.18, width * 0.85, cos, sin);
            spawnDeathBox(entity, count * 30 / 100, 0.0, height * 0.58, 0.0, width * 0.82, height * 0.38, width * 0.48, cos, sin);
            spawnDeathBox(entity, count * 13 / 100, -width * 0.58, height * 0.55, 0.0, width * 0.22, height * 0.5, width * 0.22, cos, sin);
            spawnDeathBox(entity, count * 13 / 100, width * 0.58, height * 0.55, 0.0, width * 0.22, height * 0.5, width * 0.22, cos, sin);
            spawnDeathBox(entity, count * 13 / 100, -width * 0.22, height * 0.2, 0.0, width * 0.26, height * 0.4, width * 0.24, cos, sin);
            spawnDeathBox(entity, count * 13 / 100, width * 0.22, height * 0.2, 0.0, width * 0.26, height * 0.4, width * 0.24, cos, sin);
        } else {
            spawnDeathBox(entity, count * 48 / 100, 0.0, height * 0.48, 0.0, width * 1.15, height * 0.42, width * 1.75, cos, sin);
            spawnDeathBox(entity, count * 16 / 100, 0.0, height * 0.66, width * 1.02, width * 0.72, height * 0.34, width * 0.58, cos, sin);
            spawnDeathBox(entity, count * 9 / 100, -width * 0.38, height * 0.16, -width * 0.48, width * 0.22, height * 0.32, width * 0.22, cos, sin);
            spawnDeathBox(entity, count * 9 / 100, width * 0.38, height * 0.16, -width * 0.48, width * 0.22, height * 0.32, width * 0.22, cos, sin);
            spawnDeathBox(entity, count * 9 / 100, -width * 0.38, height * 0.16, width * 0.48, width * 0.22, height * 0.32, width * 0.22, cos, sin);
            spawnDeathBox(entity, count * 9 / 100, width * 0.38, height * 0.16, width * 0.48, width * 0.22, height * 0.32, width * 0.22, cos, sin);
        }
    }

    private void spawnDeathBox(Entity entity, int count, double centerX, double centerY, double centerZ,
                               double sizeX, double sizeY, double sizeZ, double cos, double sin) {
        if (count <= 0) return;

        double halfX = Math.max(sizeX * 0.5, 0.001);
        double halfY = Math.max(sizeY * 0.5, 0.001);
        double halfZ = Math.max(sizeZ * 0.5, 0.001);
        float spreadValue = spread.getValue() * 0.032f;
        float speedMult = getSpeedMultiplier();

        for (int i = 0; i < count; i++) {
            double localX = centerX + (Math.random() * 2.0 - 1.0) * halfX;
            double localY = centerY + (Math.random() * 2.0 - 1.0) * halfY;
            double localZ = centerZ + (Math.random() * 2.0 - 1.0) * halfZ;

            if (Math.random() < 0.72) {
                switch (ThreadLocalRandom.current().nextInt(6)) {
                    case 0 -> localX = centerX - halfX;
                    case 1 -> localX = centerX + halfX;
                    case 2 -> localY = centerY - halfY;
                    case 3 -> localY = centerY + halfY;
                    case 4 -> localZ = centerZ - halfZ;
                    default -> localZ = centerZ + halfZ;
                }
            }

            localX += (Math.random() - 0.5) * sizeX * 0.05;
            localY += (Math.random() - 0.5) * sizeY * 0.05;
            localZ += (Math.random() - 0.5) * sizeZ * 0.05;

            double worldX = entity.getX() + localX * cos - localZ * sin;
            double worldZ = entity.getZ() + localX * sin + localZ * cos;
            Vec3d pos = new Vec3d(worldX, entity.getY() + localY, worldZ);

            double outX = localX / Math.max(entity.getWidth(), 0.001);
            double outZ = localZ / Math.max(entity.getWidth(), 0.001);
            double velocityLocalX = (outX * 0.7 + (Math.random() - 0.5) * 0.35) * spreadValue * speedMult;
            double velocityLocalZ = (outZ * 0.7 + (Math.random() - 0.5) * 0.35) * spreadValue * speedMult;

            Vec3d velocity = new Vec3d(
                    velocityLocalX * cos - velocityLocalZ * sin,
                    (0.015 + Math.random() * 0.075) * speedMult,
                    velocityLocalX * sin + velocityLocalZ * cos
            );

            particles.add(new Particle3D(
                    pos,
                    velocity,
                    getDeathParticleColor(),
                    size.getValue() * 0.52f,
                    lifeTime.getValue() * 1.25f
            ).setGravity(getGravity() * 0.28f).setVelocityMultiplier(0.965f).setMode(getParticleMode()).setGlowMode(getGlowMode()).setFreezeTicks(18 + ThreadLocalRandom.current().nextInt(14)).setSpinning(true));
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (particles.isEmpty()) return;

        MatrixStack stack = e.getStack();
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        float partialTicks = e.getPartialTicks();

        for (Particle3D particle : particles) {
            particle.render(stack, immediate, GLOW_SIZE, partialTicks);
        }

        immediate.draw();
    }

    private int getDeathParticleColor() {
        if (randomColor.isValue()) {
            return RANDOM_COLORS[ThreadLocalRandom.current().nextInt(RANDOM_COLORS.length)];
        }
        return deathColor.getColor();
    }

    private float getGravity() {
        return (1.0f - 0.9f) * GRAVITY_STRENGTH;
    }

    private float getSpeedMultiplier() {
        return speed.getValue();
    }

    private Particle3D.ParticleMode getParticleMode() {
        return switch (mode.getSelected()) {
            case "Кубы" -> Particle3D.ParticleMode.CUBES;
            case "Корона" -> Particle3D.ParticleMode.CROWN;
            case "Куб" -> Particle3D.ParticleMode.CUBE_BLAST;
            case "Доллар" -> Particle3D.ParticleMode.DOLLAR;
            case "Сердце" -> Particle3D.ParticleMode.HEART;
            case "Молния" -> Particle3D.ParticleMode.LIGHTNING;
            case "Линия" -> Particle3D.ParticleMode.LINE;
            case "Ромб" -> Particle3D.ParticleMode.RHOMBUS;
            case "Снежинка" -> Particle3D.ParticleMode.SNOWFLAKE;
            case "Звезда" -> Particle3D.ParticleMode.STAR;
            case "Звезда 2" -> Particle3D.ParticleMode.STAR_ALT;
            case "Треугольник" -> Particle3D.ParticleMode.TRIANGLE;
            case "Рандом" -> Particle3D.ParticleMode.RANDOM;
            default -> Particle3D.ParticleMode.CUBES;
        };
    }

    private Particle3D.GlowMode getGlowMode() {
        return switch (glowMode.getSelected()) {
            case "Bloom" -> Particle3D.GlowMode.BLOOM;
            case "Bloom Sample" -> Particle3D.GlowMode.BLOOM_SAMPLE;
            case "Оба" -> Particle3D.GlowMode.BOTH;
            default -> Particle3D.GlowMode.BOTH;
        };
    }
}

package neverlose.modules.impl.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.*;
import net.minecraft.entity.projectile.thrown.*;
import net.minecraft.item.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.DrawEvent;
import neverlose.events.impl.WorldRenderEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.ColorSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.ColorUtil;
import neverlose.util.Instance;
import neverlose.util.render.Render2D;
import neverlose.util.render.Render3D;
import neverlose.util.render.font.Fonts;
import neverlose.util.render.item.ItemRender;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ProjectileTracer extends ModuleStructure {

    public static ProjectileTracer getInstance() {
        return Instance.get(ProjectileTracer.class);
    }


    private final BooleanSetting showEnderPearls = new BooleanSetting(
            "Эндер-перлы", "Показывать траектории эндер-перлов").setValue(true);

    private final BooleanSetting showArrows = new BooleanSetting(
            "Стрелы", "Показывать траектории стрел + предсказание лука").setValue(true);

    private final BooleanSetting showThrown = new BooleanSetting(
            "Брошенные предметы", "Снежки, яйца, зелья, трезубцы").setValue(true);

    private final BooleanSetting showPrediction = new BooleanSetting(
            "Предсказание броска", "Показывать куда полетит снаряд в руке").setValue(true);

    private final BooleanSetting showDistance = new BooleanSetting(
            "Дистанция", "Показывать дистанцию до точки приземления").setValue(true);

    private final BooleanSetting showExplosionRadius = new BooleanSetting(
            "Радиус зелья", "Показывать радиус действия взрывных зелий").setValue(true);

    private final BooleanSetting wallhack = new BooleanSetting(
            "Сквозь стены", "Траектории и маркеры видны через блоки").setValue(false);

    private final BooleanSetting onlyOwn = new BooleanSetting(
            "Только свои", "Показывать только снаряды своего игрока").setValue(false);

    private final SliderSettings trailLength = new SliderSettings(
            "Длина следа", "Количество точек в следе").range(8, 64).setValue(24);

    private final ColorSetting pearlColor = new ColorSetting(
            "Цвет перлов", "Цвет трассы эндер-перла")
            .value(new Color(80, 175, 255, 210).getRGB());

    private final ColorSetting arrowColor = new ColorSetting(
            "Цвет стрел", "Цвет трассы стрел")
            .value(new Color(220, 210, 80, 210).getRGB());

    private final ColorSetting thrownColor = new ColorSetting(
            "Цвет прочего", "Цвет трассы прочих снарядов")
            .value(new Color(100, 160, 230, 210).getRGB());


    private static final int MAX_TRAIL = 64;


    private final Map<Integer, TrailData> trails = new ConcurrentHashMap<>();


    private final List<LandingLabel> pendingLabels = Collections.synchronizedList(new ArrayList<>());


    public ProjectileTracer() {
        super("ProjectileTracer", "Трассы снарядов и предсказание броска", ModuleCategory.RENDER);
        settings(pearlColor);
    }

    @Override
    public void deactivate() {
        trails.clear();
        pendingLabels.clear();
    }


    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (mc.world == null || mc.player == null) return;
        pendingLabels.clear();

        float delta = event.getPartialTicks();
        List<Integer> active = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (!isTrackedProjectile(entity)) continue;
            if (onlyOwn.isValue() && !isOwnProjectile(entity)) continue;

            active.add(entity.getId());
            TrailData td = trails.computeIfAbsent(entity.getId(), k -> new TrailData());

            double ix = MathHelper.lerp(delta, entity.lastRenderX, entity.getX());
            double iy = MathHelper.lerp(delta, entity.lastRenderY, entity.getY());
            double iz = MathHelper.lerp(delta, entity.lastRenderZ, entity.getZ());
            Vec3d interp = new Vec3d(ix, iy, iz);


            if (td.points.isEmpty() || td.points.get(td.points.size() - 1).squaredDistanceTo(interp) > 0.01) {
                td.points.add(interp);
                if (td.points.size() > MAX_TRAIL) td.points.remove(0);
            }
            td.lastPos  = interp;
            td.velocity = entity.getVelocity();
            td.entity   = entity;

            renderProjectileTrail(entity, td);
        }


        trails.keySet().removeIf(id -> !active.contains(id));

        if (showPrediction.isValue()) {
            renderPrediction(delta);
        }
    }


    @EventHandler
    public void onDraw(DrawEvent event) {
        DrawContext context = event.getDrawContext();
        for (LandingLabel label : new ArrayList<>(pendingLabels)) {
            float[] screen = worldToScreen(label.worldPos());
            if (screen == null) continue;
            drawPredictionLabel(context, screen[0], screen[1], label);
        }
    }


    private void renderProjectileTrail(Entity entity, TrailData td) {
        int     baseColor = getEntityColor(entity);
        int     len       = Math.max(2, (int) trailLength.getValue());
        boolean wh        = wallhack.isValue();

        List<Vec3d> pts  = td.points;
        int         size = pts.size();
        if (size < 2) return;

        int start = Math.max(0, size - len);


        for (int i = start; i < size - 1; i++) {
            float t  = (float)(i - start) / Math.max(1, size - 1 - start);
            int   c1 = ColorUtil.multAlpha(baseColor, t * 0.75f);
            int   c2 = ColorUtil.multAlpha(baseColor, Math.min(1f, t + 0.08f) * 0.75f);
            float w  = 0.8f + t * 1.4f;

            if (wh) Render3D.drawLineGradientNoDepth(pts.get(i), pts.get(i + 1), c1, c2, w);
            else    Render3D.drawLineGradient(pts.get(i), pts.get(i + 1), c1, c2, w, false);
        }


        boolean isPotion = entity instanceof ThrownItemEntity te &&
                (te.getStack().getItem() instanceof SplashPotionItem ||
                        te.getStack().getItem() instanceof LingeringPotionItem);


        LandingResult land = simulateLanding(td.lastPos, td.velocity, getEntityGravity(entity));
        if (land != null) {
            drawLandingMarker(land.pos(), land.face(), baseColor, isPotion, wh, getEntityIcon(entity), land.ticks());
        }
    }


    private void drawLandingMarker(Vec3d pos, Direction face, int color, boolean isPotion, boolean wh, ItemStack icon, int ticks) {
        final float R   = 0.42f;
        final int   SEG = 36;

        int outerC = ColorUtil.multAlpha(color, 0.95f);
        int glowC  = ColorUtil.multAlpha(color, 0.20f);
        int crossC = ColorUtil.multAlpha(color, 0.55f);


        for (int i = 0; i < SEG; i++) {
            Vec3d p1 = onFace(pos, face, R * 1.40f, 2 * Math.PI * i       / SEG, 0.012);
            Vec3d p2 = onFace(pos, face, R * 1.40f, 2 * Math.PI * (i + 1) / SEG, 0.012);
            if (wh) Render3D.drawLineGradientNoDepth(p1, p2, glowC, glowC, 3.0f);
            else    Render3D.drawLineGradient(p1, p2, glowC, glowC, 3.0f, false);
        }


        for (int i = 0; i < SEG; i++) {
            Vec3d p1 = onFace(pos, face, R, 2 * Math.PI * i       / SEG, 0.022);
            Vec3d p2 = onFace(pos, face, R, 2 * Math.PI * (i + 1) / SEG, 0.022);
            if (wh) Render3D.drawLineGradientNoDepth(p1, p2, outerC, outerC, 1.7f);
            else    Render3D.drawLineGradient(p1, p2, outerC, outerC, 1.7f, false);
        }


        float  cr = R * 0.65f;
        Vec3d ca = onFace(pos, face, cr,  0,             0.026);
        Vec3d cb = onFace(pos, face, cr,  Math.PI,       0.026);
        Vec3d cc = onFace(pos, face, cr,  Math.PI / 2,   0.026);
        Vec3d cd = onFace(pos, face, cr, -Math.PI / 2,   0.026);
        if (wh) {
            Render3D.drawLineGradientNoDepth(ca, cb, crossC, crossC, 1.1f);
            Render3D.drawLineGradientNoDepth(cc, cd, crossC, crossC, 1.1f);
        } else {
            Render3D.drawLineGradient(ca, cb, crossC, crossC, 1.1f, false);
            Render3D.drawLineGradient(cc, cd, crossC, crossC, 1.1f, false);
        }


        if (isPotion && showExplosionRadius.isValue()) {
            drawExplosionRadius(pos, face, color, 4.0f, wh);
        }


        if (showDistance.isValue() && mc.player != null) {
            pendingLabels.add(new LandingLabel(labelWorldPos(pos, face), formatLandingTime(ticks), color, icon.copy()));
        }
    }


    private void drawExplosionRadius(Vec3d pos, Direction face, int color, float radius, boolean wh) {
        final int SEG   = 48;
        int       ringC = ColorUtil.multAlpha(color, 0.50f);
        int       spokeStart = ColorUtil.multAlpha(color, 0.20f);
        int       spokeEnd   = ColorUtil.multAlpha(color, 0.04f);


        for (int i = 0; i < SEG; i++) {
            Vec3d p1 = onFace(pos, face, radius, 2 * Math.PI * i       / SEG, 0.03);
            Vec3d p2 = onFace(pos, face, radius, 2 * Math.PI * (i + 1) / SEG, 0.03);
            if (wh) Render3D.drawLineGradientNoDepth(p1, p2, ringC, ringC, 1.0f);
            else    Render3D.drawLineGradient(p1, p2, ringC, ringC, 1.0f, false);
        }


        Vec3d center = onFace(pos, face, 0, 0, 0.026);
        for (int i = 0; i < 12; i++) {
            Vec3d edge = onFace(pos, face, radius, 2 * Math.PI * i / 12, 0.03);
            if (wh) Render3D.drawLineGradientNoDepth(center, edge, spokeStart, spokeEnd, 0.5f);
            else    Render3D.drawLineGradient(center, edge, spokeStart, spokeEnd, 0.5f, false);
        }
    }


    private void renderPrediction(float delta) {
        if (mc.player == null) return;
        ItemStack main = mc.player.getMainHandStack();
        ItemStack off  = mc.player.getOffHandStack();
        tryPredictItem(main, delta);
        if (main.isEmpty()) tryPredictItem(off, delta);
    }

    private void tryPredictItem(ItemStack stack, float delta) {
        if (stack.isEmpty()) return;
        Item  item    = stack.getItem();
        boolean wh    = wallhack.isValue();
        Vec3d eyePos  = mc.player.getCameraPosVec(delta);
        Vec3d look    = mc.player.getRotationVec(delta);

        if (item instanceof EnderPearlItem && showEnderPearls.isValue()) {

            drawPredictedPath(eyePos, look.multiply(1.5), 0.03, pearlColor.getColor(), false, wh, stack);

        } else if (item instanceof SnowballItem && showThrown.isValue()) {
            drawPredictedPath(eyePos, look.multiply(1.5), 0.03, thrownColor.getColor(), false, wh, stack);

        } else if (item instanceof EggItem && showThrown.isValue()) {
            drawPredictedPath(eyePos, look.multiply(1.5), 0.03, thrownColor.getColor(), false, wh, stack);

        } else if ((item instanceof SplashPotionItem || item instanceof LingeringPotionItem) && showThrown.isValue()) {

            drawPredictedPath(eyePos, look.multiply(0.5), 0.05, thrownColor.getColor(), true, wh, stack);

        } else if (item instanceof BowItem && showArrows.isValue()) {
            int useTime = mc.player.getItemUseTime();
            if (useTime < 3) return;
            float pull = BowItem.getPullProgress(useTime);
            if (pull <= 0.05f) return;

            drawPredictedPath(eyePos, look.multiply(pull * 3.0), 0.05, arrowColor.getColor(), false, wh, stack);

        } else if (item instanceof CrossbowItem && showArrows.isValue()) {
            if (!CrossbowItem.isCharged(stack)) return;
            float speed = 3.15f;
            int   ac    = arrowColor.getColor();


            drawPredictedPath(eyePos, look.multiply(speed), 0.05, ac, false, wh, stack);


            if (hasMultishot(stack)) {
                int sideColor = ColorUtil.multAlpha(ac, 0.60f);
                Vec3d left  = rotateAroundY(look, Math.toRadians(10)).multiply(speed);
                Vec3d right = rotateAroundY(look, Math.toRadians(-10)).multiply(speed);
                drawPredictedPath(eyePos, left,  0.05, sideColor, false, wh, stack);
                drawPredictedPath(eyePos, right, 0.05, sideColor, false, wh, stack);
            }

        } else if (item instanceof TridentItem && showThrown.isValue()) {
            drawPredictedPath(eyePos, look.multiply(2.5), 0.05, thrownColor.getColor(), false, wh, stack);
        }
    }


    private void drawPredictedPath(Vec3d startPos, Vec3d startVel, double gravity,
                                   int color, boolean isPotion, boolean wh, ItemStack icon) {
        List<Vec3d>   pts  = new ArrayList<>();
        Vec3d         pos  = startPos;
        Vec3d         vel  = startVel;
        LandingResult land = null;

        for (int i = 0; i < 200; i++) {
            pts.add(pos);
            Vec3d nextPos = pos.add(vel);

            if (mc.world != null) {
                BlockHitResult hit = mc.world.raycast(new RaycastContext(
                        pos, nextPos,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE,
                        mc.player));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    pts.add(hit.getPos());
                    land = new LandingResult(hit.getPos(), hit.getSide(), i + 1);
                    break;
                }
            }

            vel = new Vec3d(vel.x * 0.99, vel.y - gravity, vel.z * 0.99);
            pos = nextPos;
            if (i == 199) land = new LandingResult(pos, Direction.UP, 200);
        }


        for (int i = 0; i < pts.size() - 1; i++) {
            if (i % 2 == 0) {
                float t    = (float) i / Math.max(1, pts.size());
                int   segC = ColorUtil.multAlpha(color, 0.25f + t * 0.65f);
                float w    = 0.7f + t * 1.0f;
                if (wh) Render3D.drawLineGradientNoDepth(pts.get(i), pts.get(i + 1), segC, segC, w);
                else    Render3D.drawLineGradient(pts.get(i), pts.get(i + 1), segC, segC, w, false);
            }
        }

        if (land != null) {
            drawLandingMarker(land.pos(), land.face(), color, isPotion, wh, icon, land.ticks());
        }
    }



    private LandingResult simulateLanding(Vec3d pos, Vec3d vel, double gravity) {
        if (mc.world == null) return null;
        for (int i = 0; i < 300; i++) {
            Vec3d nextPos = pos.add(vel);
            BlockHitResult hit = mc.world.raycast(new RaycastContext(
                    pos, nextPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mc.player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                return new LandingResult(hit.getPos(), hit.getSide(), i + 1);
            }
            vel = new Vec3d(vel.x * 0.99, vel.y - gravity, vel.z * 0.99);
            pos = nextPos;
        }
        return new LandingResult(pos, Direction.UP, 300);
    }



    private void drawPredictionLabel(DrawContext context, float sx, float sy, LandingLabel label) {
        float textSize = 4.2f;
        float iconScale = 0.42f;
        float iconSize = 16.0f * iconScale;
        float height = 9.5f;
        float width = 34.0f;
        float x = sx - width / 2.0f;
        float y = sy - height - 2.0f;

        Render2D.rect(x, y, width, height, new Color(0, 0, 0, 188).getRGB(), 1.5f);

        if (!label.icon().isEmpty()) {
            ItemRender.drawItemWithContext(context, label.icon(), x + 2.0f, y + 1.35f, iconScale, 1.0f);
        }

        Fonts.BOLD.draw(label.text(), x + iconSize + 4.2f, y + 3.0f, textSize, 0xFFE9EEF5);
    }


    private Vec3d onFace(Vec3d center, Direction face, float radius, double angle, double surfaceOffset) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double ox  = face.getOffsetX() * surfaceOffset;
        double oy  = face.getOffsetY() * surfaceOffset;
        double oz  = face.getOffsetZ() * surfaceOffset;

        return switch (face) {
            case UP, DOWN ->
                    new Vec3d(center.x + ox + cos * radius, center.y + oy,             center.z + oz + sin * radius);
            case NORTH, SOUTH ->
                    new Vec3d(center.x + ox + cos * radius, center.y + oy + sin * radius, center.z + oz);
            case EAST, WEST ->
                    new Vec3d(center.x + ox,             center.y + oy + sin * radius, center.z + oz + cos * radius);
        };
    }

    private Vec3d labelWorldPos(Vec3d pos, Direction face) {
        return switch (face) {
            case UP    -> pos.add( 0.0,  0.65,  0.0);
            case DOWN  -> pos.add( 0.0, -0.65,  0.0);
            case NORTH -> pos.add( 0.0,  0.45, -0.55);
            case SOUTH -> pos.add( 0.0,  0.45,  0.55);
            case EAST  -> pos.add( 0.55, 0.45,  0.0);
            case WEST  -> pos.add(-0.55, 0.45,  0.0);
        };
    }

    private String formatLandingTime(int ticks) {
        return String.format("%.1fs", Math.max(1, ticks) / 20.0f);
    }

    private ItemStack getEntityIcon(Entity entity) {
        if (entity instanceof EnderPearlEntity) return Items.ENDER_PEARL.getDefaultStack();
        if (entity instanceof SpectralArrowEntity) return Items.SPECTRAL_ARROW.getDefaultStack();
        if (entity instanceof ArrowEntity) return Items.ARROW.getDefaultStack();
        if (entity instanceof TridentEntity) return Items.TRIDENT.getDefaultStack();
        if (entity instanceof SnowballEntity) return Items.SNOWBALL.getDefaultStack();
        if (entity instanceof EggEntity) return Items.EGG.getDefaultStack();
        if (entity instanceof ThrownItemEntity thrown) return thrown.getStack();
        return ItemStack.EMPTY;
    }

    private Vec3d rotateAroundY(Vec3d v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3d(v.x * cos + v.z * sin, v.y, -v.x * sin + v.z * cos);
    }


    private double getEntityGravity(Entity e) {
        if (e instanceof EnderPearlEntity)  return 0.03;
        if (e instanceof SnowballEntity)    return 0.03;
        if (e instanceof EggEntity)         return 0.03;
        return 0.05;
    }


    private boolean isTrackedProjectile(Entity e) {
        if (e instanceof EnderPearlEntity)                                          return showEnderPearls.isValue();
        if (e instanceof ArrowEntity || e instanceof SpectralArrowEntity
                || e instanceof TridentEntity)                                      return showArrows.isValue();
        if (e instanceof SnowballEntity || e instanceof EggEntity
                || e instanceof ThrownItemEntity)                                   return showThrown.isValue();
        return false;
    }


    private boolean isOwnProjectile(Entity e) {
        if (mc.player == null) return false;
        if (e instanceof ProjectileEntity pe) return pe.getOwner() == mc.player;
        return false;
    }

    private int getEntityColor(Entity e) {
        if (e instanceof EnderPearlEntity)                                          return pearlColor.getColor();
        if (e instanceof ArrowEntity || e instanceof SpectralArrowEntity
                || e instanceof TridentEntity)                                      return arrowColor.getColor();
        return thrownColor.getColor();
    }


    private boolean hasMultishot(ItemStack stack) {
        if (mc.world == null) return false;
        try {
            var enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
            if (enchants == null) return false;
            var reg   = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            var entry = reg.getOptional(Enchantments.MULTISHOT);
            return entry.map(e -> enchants.getLevel(e) > 0).orElse(false);
        } catch (Exception ignored) {
            return false;
        }
    }


    private float[] worldToScreen(Vec3d worldPos) {
        if (mc.getWindow() == null) return null;
        try {
            Vec3d cam = Render3D.lastCameraPos;
            org.joml.Vector4f p = new org.joml.Vector4f(
                    (float)(worldPos.x - cam.x),
                    (float)(worldPos.y - cam.y),
                    (float)(worldPos.z - cam.z), 1.0f);
            p.mul(Render3D.lastWorldSpaceMatrix).mul(Render3D.lastProjMat);
            if (p.w <= 0) return null;
            float nx = p.x / p.w, ny = p.y / p.w;
            if (nx < -1 || nx > 1 || ny < -1 || ny > 1) return null;
            float sw = Render2D.getFixedScaledWidth();
            float sh = Render2D.getFixedScaledHeight();
            return new float[]{ (nx + 1f) / 2f * sw, (1f - ny) / 2f * sh };
        } catch (Exception ignored) {
            return null;
        }
    }


    private record LandingResult(Vec3d pos, Direction face, int ticks) {}


    private record LandingLabel(Vec3d worldPos, String text, int color, ItemStack icon) {}

    private static class TrailData {
        final List<Vec3d> points = new ArrayList<>();
        Vec3d  lastPos  = Vec3d.ZERO;
        Vec3d  velocity = Vec3d.ZERO;
        Entity entity   = null;
    }
}

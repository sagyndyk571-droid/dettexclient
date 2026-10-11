package neverlose.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.WorldChangeEvent;
import neverlose.events.impl.WorldRenderEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.ColorSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.ColorUtil;
import neverlose.util.render.Render3D;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Trails extends ModuleStructure {
    final List<Point> points = new ArrayList<>();

    final SliderSettings lifeTime = new SliderSettings("Время жизни", "Сколько держится полоса")
            .range(300, 2000)
            .setValue(800);

    final SliderSettings lineWidth = new SliderSettings("Толщина линий", "Толщина верхней и нижней обводки")
            .range(0.5f, 4.0f)
            .setValue(1.8f);

    final SliderSettings fillAlpha = new SliderSettings("Прозрачность", "Прозрачность заливки полосы")
            .range(0.05f, 0.75f)
            .setValue(0.42f);

    final BooleanSetting thirdPersonOnly = new BooleanSetting("Только от 3 лица", "Не рисовать в первом лице")
            .setValue(true);

    final BooleanSetting wallhack = new BooleanSetting("Сквозь стены", "Полоса видна через блоки")
            .setValue(false);

    final ColorSetting color = new ColorSetting("Цвет", "Цвет полосы")
            .value(ColorSetting.DEFAULT_COLOR);

    public Trails() {
        super("Trails", "Полоса за игроком", ModuleCategory.RENDER);
        settings(lifeTime, lineWidth, fillAlpha, thirdPersonOnly, wallhack, color);
    }

    @Override
    public void activate() {
        clear();
    }

    @Override
    public void deactivate() {
        clear();
    }

    @EventHandler
    public void onWorldChange(WorldChangeEvent event) {
        clear();
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (mc.player == null || mc.world == null) {
            clear();
            return;
        }
        if (thirdPersonOnly.isValue() && mc.options.getPerspective().isFirstPerson()) {
            clear();
            return;
        }

        long now = System.currentTimeMillis();
        trimExpired(now);
        addPoint(event.getPartialTicks(), now);
        renderTrail(now);
    }

    private void clear() {
        points.clear();
    }

    private void trimExpired(long now) {
        long maxAge = (long) lifeTime.getValue();
        points.removeIf(point -> now - point.createdAt() > maxAge);
    }

    private void addPoint(float tickDelta, long now) {
        double x = MathHelper.lerp(tickDelta, mc.player.lastRenderX, mc.player.getX());
        double y = MathHelper.lerp(tickDelta, mc.player.lastRenderY, mc.player.getY());
        double z = MathHelper.lerp(tickDelta, mc.player.lastRenderZ, mc.player.getZ());
        Vec3d pos = new Vec3d(x, y, z);

        if (!points.isEmpty() && points.get(points.size() - 1).pos().squaredDistanceTo(pos) < 0.0009) {
            return;
        }

        points.add(new Point(pos, now));
        while (points.size() > 180) {
            points.remove(0);
        }
    }

    private void renderTrail(long now) {
        if (points.size() < 2) {
            return;
        }

        int baseColor = color.getColor();
        boolean noDepth = wallhack.isValue();
        float width = lineWidth.getValue();
        long maxAge = Math.max(1L, (long) lifeTime.getValue());

        Iterator<Point> iterator = points.iterator();
        Point previous = iterator.next();
        while (iterator.hasNext()) {
            Point current = iterator.next();
            float previousAlpha = getAlpha(previous, now, maxAge);
            float currentAlpha = getAlpha(current, now, maxAge);

            Vec3d previousBottom = previous.pos().add(0.0, 0.0005, 0.0);
            Vec3d currentBottom = current.pos().add(0.0, 0.0005, 0.0);
            Vec3d previousTop = previous.pos().add(0.0, mc.player.getHeight(), 0.0);
            Vec3d currentTop = current.pos().add(0.0, mc.player.getHeight(), 0.0);

            int fillPrevious = ColorUtil.multAlpha(baseColor, previousAlpha * fillAlpha.getValue());
            int fillCurrent = ColorUtil.multAlpha(baseColor, currentAlpha * fillAlpha.getValue());
            int linePrevious = ColorUtil.multAlpha(baseColor, previousAlpha);
            int lineCurrent = ColorUtil.multAlpha(baseColor, currentAlpha);
            int glowPrevious = ColorUtil.multAlpha(baseColor, previousAlpha * 0.28f);
            int glowCurrent = ColorUtil.multAlpha(baseColor, currentAlpha * 0.28f);

            drawQuad(previousTop, currentTop, currentBottom, previousBottom,
                    fillPrevious, fillCurrent, fillCurrent, fillPrevious, noDepth);
            drawQuad(previousBottom, currentBottom, currentTop, previousTop,
                    fillPrevious, fillCurrent, fillCurrent, fillPrevious, noDepth);
            drawLine(previousTop, currentTop, glowPrevious, glowCurrent, width * 2.7f, noDepth);
            drawLine(previousBottom, currentBottom, glowPrevious, glowCurrent, width * 2.2f, noDepth);
            drawLine(previousTop, currentTop, linePrevious, lineCurrent, width, noDepth);
            drawLine(previousBottom, currentBottom, linePrevious, lineCurrent, Math.max(0.7f, width * 0.8f), noDepth);

            previous = current;
        }
    }

    private float getAlpha(Point point, long now, long maxAge) {
        float age = MathHelper.clamp((now - point.createdAt()) / (float) maxAge, 0.0f, 1.0f);
        float fade = 1.0f - age;
        return fade * fade;
    }

    private void drawQuad(Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4, int c1, int c2, int c3, int c4, boolean noDepth) {
        if (noDepth) {
            Render3D.drawGradientQuadNoDepth(p1, p2, p3, p4, c1, c2, c3, c4);
        } else {
            Render3D.drawGradientQuad(p1, p2, p3, p4, c1, c2, c3, c4, false);
        }
    }

    private void drawLine(Vec3d start, Vec3d end, int colorStart, int colorEnd, float width, boolean noDepth) {
        if (noDepth) {
            Render3D.drawLineGradientNoDepth(start, end, colorStart, colorEnd, width);
        } else {
            Render3D.drawLineGradient(start, end, colorStart, colorEnd, width, false);
        }
    }

    private record Point(Vec3d pos, long createdAt) {
    }
}

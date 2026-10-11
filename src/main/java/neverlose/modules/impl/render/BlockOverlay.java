package neverlose.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.WorldRenderEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.ColorSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.ColorUtil;
import neverlose.util.Instance;
import neverlose.util.render.Render3D;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlockOverlay extends ModuleStructure {
    static final long ANIMATION_MS = 170L;

    final ColorSetting color = new ColorSetting("Color", "Overlay color")
            .value(ColorSetting.DEFAULT_COLOR);

    final SliderSettings lineWidth = new SliderSettings("Line width", "Outline width")
            .range(0.5f, 4.0f)
            .setValue(2.0f);

    final SliderSettings alpha = new SliderSettings("Alpha", "Fill alpha")
            .range(35, 220)
            .setValue(145);

    final SliderSettings glow = new SliderSettings("Glow", "Soft outer glow")
            .range(0.0f, 3.0f)
            .setValue(1.0f);

    final BooleanSetting fill = new BooleanSetting("Fill", "Render filled block mask")
            .setValue(true);

    final BooleanSetting outline = new BooleanSetting("Outline", "Render block outline")
            .setValue(true);

    final BooleanSetting throughWalls = new BooleanSetting("Through walls", "Render through blocks")
            .setValue(true);

    BlockPos lastBlock;
    Box fromBox;
    Box targetBox;
    long animationStart;

    public static BlockOverlay getInstance() {
        return Instance.get(BlockOverlay.class);
    }

    public BlockOverlay() {
        super("BlockOverlay", "Block Overlay", ModuleCategory.RENDER);
        settings(color, lineWidth, alpha, glow, fill, outline, throughWalls);
    }

    @Override
    public void deactivate() {
        resetTarget();
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (mc.world == null || mc.player == null) {
            resetTarget();
            return;
        }

        BlockPos pos = getCrosshairBlock();
        if (pos == null) {
            resetTarget();
            return;
        }

        Box currentBox = getBlockBox(pos);
        if (currentBox == null) {
            resetTarget();
            return;
        }

        updateAnimatedTarget(pos, currentBox);
        Box renderBox = getAnimatedBox().expand(0.002);
        int baseColor = ColorUtil.reAlphaInt(color.getColorNoAlpha(), Math.round(alpha.getValue()));
        boolean depth = !throughWalls.isValue();

        if (glow.getValue() > 0.0f) {
            renderGlow(renderBox, baseColor, depth);
        }

        if (fill.isValue()) {
            Render3D.drawBox(renderBox, baseColor, lineWidth.getValue(), false, true, depth);
        }

        if (outline.isValue()) {
            int outlineColor = ColorUtil.reAlphaInt(color.getColorNoAlpha(), 255);
            Render3D.drawBox(renderBox, outlineColor, lineWidth.getValue(), true, false, depth);
        }
    }

    private BlockPos getCrosshairBlock() {
        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockPos pos = hit.getBlockPos();
        return mc.world.getBlockState(pos).isAir() ? null : pos;
    }

    private Box getBlockBox(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        VoxelShape shape = state.getOutlineShape(mc.world, pos);
        if (shape.isEmpty()) {
            return new Box(pos);
        }
        return shape.getBoundingBox().offset(pos);
    }

    private void updateAnimatedTarget(BlockPos pos, Box currentBox) {
        if (lastBlock == null || fromBox == null || targetBox == null) {
            lastBlock = pos;
            fromBox = currentBox;
            targetBox = currentBox;
            animationStart = System.currentTimeMillis() - ANIMATION_MS;
            return;
        }

        if (!pos.equals(lastBlock)) {
            fromBox = getAnimatedBox();
            targetBox = currentBox;
            lastBlock = pos;
            animationStart = System.currentTimeMillis();
            return;
        }

        targetBox = currentBox;
    }

    private Box getAnimatedBox() {
        if (fromBox == null || targetBox == null) {
            return Box.from(Vec3d.ZERO);
        }

        float progress = Math.min(1.0f, (System.currentTimeMillis() - animationStart) / (float) ANIMATION_MS);
        float t = easeOutCubic(progress);
        return lerp(fromBox, targetBox, t);
    }

    private Box lerp(Box from, Box to, float t) {
        return new Box(
                lerp(from.minX, to.minX, t),
                lerp(from.minY, to.minY, t),
                lerp(from.minZ, to.minZ, t),
                lerp(from.maxX, to.maxX, t),
                lerp(from.maxY, to.maxY, t),
                lerp(from.maxZ, to.maxZ, t)
        );
    }

    private double lerp(double from, double to, float t) {
        return from + (to - from) * t;
    }

    private float easeOutCubic(float t) {
        float inv = 1.0f - t;
        return 1.0f - inv * inv * inv;
    }

    private void renderGlow(Box box, int baseColor, boolean depth) {
        int glowColor = ColorUtil.multAlpha(baseColor, 0.28f);
        float amount = glow.getValue();
        for (int i = 1; i <= 3; i++) {
            double expand = i * 0.0125 * amount;
            Render3D.drawBox(box.expand(expand), glowColor, lineWidth.getValue() + i, true, true, depth);
        }
    }

    private void resetTarget() {
        lastBlock = null;
        fromBox = null;
        targetBox = null;
        animationStart = 0L;
    }
}

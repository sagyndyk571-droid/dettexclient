package neverlose.util.render;

/**
 * Numeric scale context for HUD elements.
 *
 * <p>While active, every {@link Render2D} primitive and font draw is transformed
 * around an element origin. The transform is applied to coordinates and sizes
 * (not to a render matrix), so MSDF text and SDF shapes are re-rasterized at the
 * target size and stay perfectly crisp.</p>
 */
public final class HudRender {

    private static int depth = 0;
    private static float scale = 1f;
    private static float originX = 0f;
    private static float originY = 0f;

    private HudRender() {}

    public static void push(float elementX, float elementY, float elementScale) {
        if (elementScale <= 0f) {
            elementScale = 1f;
        }
        if (depth == 0) {
            originX = elementX;
            originY = elementY;
            scale = elementScale;
        }
        depth++;
    }

    public static void pop() {
        if (depth > 0) {
            depth--;
        }
        if (depth == 0) {
            scale = 1f;
            originX = 0f;
            originY = 0f;
        }
    }

    public static boolean isActive() {
        return depth > 0 && scale != 1f;
    }

    public static float scale() {
        return isActive() ? scale : 1f;
    }

    public static float x(float value) {
        return isActive() ? originX + (value - originX) * scale : value;
    }

    public static float y(float value) {
        return isActive() ? originY + (value - originY) * scale : value;
    }

    public static float len(float value) {
        return isActive() ? value * scale : value;
    }

    public static float[] lengths(float[] values) {
        if (!isActive() || values == null) {
            return values;
        }
        float[] out = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = values[i] * scale;
        }
        return out;
    }
}

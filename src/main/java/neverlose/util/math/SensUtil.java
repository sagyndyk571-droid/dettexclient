package neverlose.util.math;

import neverlose.IMinecraft;

public final class SensUtil implements IMinecraft {
    public static float getSens(float rotation) {
        return getDeltaMouse(rotation) * getGCDValue();
    }

    public static float getSensRandom(float rotation) {
        return getDeltaMouse(rotation) * getGCDValueR();
    }

    public static float getGCDValue() {
        return (float) ((double) getGCD() * 0.15);
    }

    public static float getGCDValueR() {
        return (float) ((double) getGCDR() * 0.15);
    }

    public static float getGCDR() {
        double mouseSensitivity = mc.options.getMouseSensitivity().getValue();
        return (float) (Math.pow(mouseSensitivity * 0.8 + 0.24, 3.0) * 7.0);
    }

    public static float getGCD() {
        double mouseSensitivity = mc.options.getMouseSensitivity().getValue();
        return (float) (Math.pow(mouseSensitivity * 0.6 + 0.2, 3.0) * 8.0);
    }

    public static float getDeltaMouse(float delta) {
        return Math.round(delta / getGCDValue());
    }

    private SensUtil() {
    }
}

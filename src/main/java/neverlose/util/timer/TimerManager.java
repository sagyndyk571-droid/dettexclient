package neverlose.util.timer;

public final class TimerManager {
    private static volatile float timer = 1.0f;

    private TimerManager() {
    }

    public static void setTimer(float value) {
        if (!Float.isFinite(value)) {
            timer = 1.0f;
            return;
        }
        timer = Math.max(0.05f, Math.min(10.0f, value));
    }

    public static float getTimer() {
        return timer;
    }

    public static void reset() {
        timer = 1.0f;
    }
}

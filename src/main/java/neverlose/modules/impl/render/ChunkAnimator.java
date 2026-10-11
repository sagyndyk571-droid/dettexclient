package neverlose.modules.impl.render;

import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.MultiSelectSetting;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.modules.module.setting.implement.SliderSettings;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class ChunkAnimator extends ModuleStructure {
    private static ChunkAnimator instance;
    private final Map<Object, Long> chunkStarts = Collections.synchronizedMap(new WeakHashMap<>());

    private static final String PLAYER_LIST = "Player list";
    private static final String INVENTORY = "Inventory";
    private static final String CAMERA_ZOOM = "Camera zoom";
    private static final String CHUNKS = "Chunks";
    private static final String PERSPECTIVE = "Perspective";
    private static final String HOTBAR = "Hotbar";

    private final MultiSelectSetting mode = new MultiSelectSetting("Animate", "Elements to animate")
            .value(PLAYER_LIST, INVENTORY, CAMERA_ZOOM, CHUNKS, PERSPECTIVE, HOTBAR)
            .selected(PLAYER_LIST, INVENTORY, CAMERA_ZOOM, CHUNKS, PERSPECTIVE, HOTBAR);

    private final SelectSetting chunkAnimation = new SelectSetting("Easing", "Animation easing")
            .value("Quart", "Circ", "Sine", "Cubic")
            .selected("Quart")
            .visible(() -> mode.isSelected(CHUNKS) || mode.isSelected(INVENTORY) || mode.isSelected(PLAYER_LIST) || mode.isSelected(HOTBAR));

    private final SliderSettings speed = new SliderSettings("Speed", "Animation speed")
            .range(2, 10)
            .setValue(6)
            .visible(() -> mode.isSelected(CHUNKS) || mode.isSelected(INVENTORY) || mode.isSelected(PLAYER_LIST) || mode.isSelected(HOTBAR));

    public ChunkAnimator() {
        super("Animation", "Animates selected client actions", ModuleCategory.RENDER);
        instance = this;
        settings(mode, chunkAnimation, speed);
    }

    public static ChunkAnimator getInstance() {
        return instance;
    }

    public float getSpeed() {
        return speed.getValue();
    }

    public boolean shouldAnimateInventory() {
        return isState() && mode.isSelected(INVENTORY);
    }

    public boolean shouldAnimatePlayerList() {
        return isState() && mode.isSelected(PLAYER_LIST);
    }

    public boolean shouldAnimateCameraZoom() {
        return isState() && mode.isSelected(CAMERA_ZOOM);
    }

    public boolean shouldAnimatePerspective() {
        return isState() && mode.isSelected(PERSPECTIVE);
    }

    public boolean shouldAnimateChunks() {
        return isState() && mode.isSelected(CHUNKS);
    }

    public boolean shouldAnimateHotbar() {
        return isState() && mode.isSelected(HOTBAR);
    }

    public float getScreenProgress(long elapsedMs) {
        float duration = Math.max(90.0f, 290.0f - speed.getValue() * 18.0f);
        float progress = Math.max(0.0f, Math.min(1.0f, elapsedMs / duration));
        return applySelectedEasing(progress);
    }

    public float getYOffset(float visibility) {
        if (!shouldAnimateChunks()) {
            return 0.0f;
        }
        float progress = Math.max(0.0f, Math.min(1.0f, visibility));
        float amplitude = 28.0f + speed.getValue() * 5.0f;
        return (1.0f - applySelectedEasing(progress)) * amplitude;
    }

    public void markChunkUpdated(Object chunk) {
        if (chunk == null) {
            return;
        }
        chunkStarts.put(chunk, System.currentTimeMillis());
    }

    public float getChunkOffset(Object chunk, float vanillaVisibility) {
        if (!shouldAnimateChunks()) {
            return 0.0f;
        }

        long now = System.currentTimeMillis();
        Long startedAt = chunkStarts.computeIfAbsent(chunk, ignored -> now);
        float duration = getChunkDurationMs();
        float ownProgress = Math.max(0.0f, Math.min(1.0f, (now - startedAt) / duration));
        float vanillaProgress = Math.max(0.0f, Math.min(1.0f, vanillaVisibility));
        float progress = Math.min(ownProgress, vanillaProgress);

        if (ownProgress >= 0.995f && vanillaProgress >= 0.995f) {
            chunkStarts.remove(chunk);
        }

        float amplitude = 28.0f + speed.getValue() * 5.0f;
        return (1.0f - applySelectedEasing(progress)) * amplitude;
    }

    public float getChunkDurationMs() {
        return Math.max(320.0f, 1050.0f - speed.getValue() * 70.0f);
    }

    public float applySelectedEasing(float progress) {
        return switch (chunkAnimation.getSelected()) {
            case "Circ" -> (float) Math.sqrt(1.0f - Math.pow(progress - 1.0f, 2.0f));
            case "Sine" -> (float) Math.sin((progress * Math.PI) / 2.0f);
            case "Cubic" -> 1.0f - (float) Math.pow(1.0f - progress, 3.0f);
            default -> 1.0f - (float) Math.pow(1.0f - progress, 4.0f);
        };
    }
}

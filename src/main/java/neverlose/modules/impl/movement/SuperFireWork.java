package neverlose.modules.impl.movement;

import com.adl.nativeprotect.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.FireworkEvent;
import neverlose.modules.impl.combat.aura.AngleConnection;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.modules.module.setting.implement.SliderSettings;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SuperFireWork extends ModuleStructure {
    SelectSetting modeSetting = new SelectSetting("Режим", "Выберите тип режима")
            .value("BravoHvH", "ReallyWorld", "SlimeWorld", "PulseHVH", "Custom");

    SliderSettings customSpeedSetting = new SliderSettings("Скорость", "Скорость для Custom режима")
            .range(1.5f, 3f)
            .setValue(1.963f)
            .visible(() -> modeSetting.isSelected("Custom"));

    BooleanSetting nearBoostSetting = new BooleanSetting("", "");

    SliderSettings yaw05 = slimeSlider("Yaw 0-5°", 1.63f);
    SliderSettings yaw510 = slimeSlider("Yaw 5-10°", 1.62f);
    SliderSettings yaw1015 = slimeSlider("Yaw 10-15°", 1.69f);
    SliderSettings yaw1520 = slimeSlider("Yaw 15-20°", 1.76f);
    SliderSettings yaw2025 = slimeSlider("Yaw 20-25°", 1.77f);
    SliderSettings yaw2530 = slimeSlider("Yaw 25-30°", 1.83f);
    SliderSettings yaw3035 = slimeSlider("Yaw 30-35°", 1.92f);
    SliderSettings yaw3540 = slimeSlider("Yaw 35-40°", 1.96f);
    SliderSettings yaw4045 = slimeSlider("Yaw 40-45°", 1.94f);

    SliderSettings pitch05 = slimeSlider("Pitch 0-5°", 1.62f);
    SliderSettings pitch510 = slimeSlider("Pitch 5-10°", 1.63f);
    SliderSettings pitch1015 = slimeSlider("Pitch 10-15°", 1.63f);
    SliderSettings pitch1520 = slimeSlider("Pitch 15-20°", 1.66f);
    SliderSettings pitch2025 = slimeSlider("Pitch 20-25°", 1.8f);
    SliderSettings pitch2530 = slimeSlider("Pitch 25-30°", 1.84f);
    SliderSettings pitch3035 = slimeSlider("Pitch 30-35°", 1.97f);
    SliderSettings pitch3540 = slimeSlider("Pitch 35-40°", 1.99f);
    SliderSettings pitch4045 = slimeSlider("Pitch 40-45°", 1.99f);

    SliderSettings[] slimeYawSettings = {
            yaw05, yaw510, yaw1015, yaw1520, yaw2025, yaw2530, yaw3035, yaw3540, yaw4045
    };

    SliderSettings[] slimePitchSettings = {
            pitch05, pitch510, pitch1015, pitch1520, pitch2025, pitch2530, pitch3035, pitch3540, pitch4045
    };

    public SuperFireWork() {
        super("SuperFireWork", "Super FireWork", ModuleCategory.MOVEMENT);
        settings(modeSetting, customSpeedSetting,
                yaw05, yaw510, yaw1015, yaw1520, yaw2025, yaw2530, yaw3035, yaw3540, yaw4045,
                pitch05, pitch510, pitch1015, pitch1520, pitch2025, pitch2530, pitch3035, pitch3540, pitch4045);
    }

    @EventHandler
   @Native
    public void onFirework(FireworkEvent e) {
        if (mc.player == null || !mc.player.isGliding()) return;

        float yaw = AngleConnection.INSTANCE.getRotation().getYaw() % 360f;
        if (yaw < 0) yaw += 360f;

        if (modeSetting.isSelected("ReallyWorld")) {
            handleReallyWorldMode(e, yaw);
        } else if (modeSetting.isSelected("SlimeWorld")) {
            handleSlimeWorldMode(e);
        } else if (modeSetting.isSelected("BravoHvH")) {
            handleBravoHvHMode(e, yaw);
        } else if (modeSetting.isSelected("PulseHVH")) {
            handlePulseHVHMode(e, yaw);
        } else if (modeSetting.isSelected("Custom")) {
            handleCustomMode(e, yaw);
        }
    }

   @Native
    private void handleReallyWorldMode(FireworkEvent e, float yaw) {
        float[] diagonals = {45f, 135f, 225f, 315f};
        float closestDiff = 180f;

        for (float d : diagonals) {
            float diff = Math.abs(yaw - d);
            diff = Math.min(diff, 360f - diff);
            if (diff < closestDiff) closestDiff = diff;
        }

        double speedXZ = 1.5;
        double speedY = 1.5;

        if (closestDiff <= 4) {
            speedXZ = 2.2;
        } else if (closestDiff <= 8) {
            speedXZ = 2.06;
        } else if (closestDiff <= 12) {
            speedXZ = 1.98;
        } else if (closestDiff <= 16) {
            speedXZ = 1.87;
        } else if (closestDiff <= 20) {
            speedXZ = 1.8;
        } else if (closestDiff <= 24) {
            speedXZ = 1.74;
        } else if (closestDiff <= 28) {
            speedXZ = 1.7;
        } else if (closestDiff <= 32) {
            speedXZ = 1.65;
        } else if (closestDiff <= 36) {
            speedXZ = 1.63;
        } else {
            speedXZ = 1.61;
            speedY = 1.61;
        }

        applyFireworkVelocity(e, speedXZ, speedY);
    }

    @Native
    private void handleSlimeWorldMode(FireworkEvent e) {
        float yaw = convertAngleToRange(MathHelper.wrapDegrees(AngleConnection.INSTANCE.getRotation().getYaw()));
        float pitch = convertAngleToRange(Math.abs(AngleConnection.INSTANCE.getRotation().getPitch()));

        float speedXZ = getSlimeSpeed(yaw, slimeYawSettings);
        float speedY = getSlimeSpeed(pitch, slimePitchSettings);

        if (speedY > speedXZ) {
            speedXZ = speedY;
        }

        applyFireworkVelocity(e, speedXZ, speedY);
    }

   @Native
    private void handleBravoHvHMode(FireworkEvent e, float yaw) {
        boolean isDiagonal = checkDiagonal(yaw, 16f);
        boolean nearPlayer = checkNearPlayer(4f);

        double speedXZ;
        double speedY = 1.66;

        if (isDiagonal) {
            speedXZ = 1.963;
        } else if (nearBoostSetting.isValue() && nearPlayer) {
            speedXZ = 1.82;
            speedY = 1.67;
        } else {
            speedXZ = 1.675;
        }

        applyFireworkVelocity(e, speedXZ, speedY);
    }

   @Native
    private void handlePulseHVHMode(FireworkEvent e, float yaw) {
        boolean isDiagonal = checkDiagonal(yaw, 16f);
        boolean nearPlayer = checkNearPlayer(5f);

        double speedXZ;
        double speedY = 2.66;

        if (isDiagonal) {
            speedXZ = 1.963;
        } else if (nearBoostSetting.isValue() && nearPlayer) {
            speedXZ = 1.82;
            speedY = 1.67;
        } else {
            speedXZ = 1.675;
        }

        applyFireworkVelocity(e, speedXZ, speedY);
    }

   @Native
    private void handleCustomMode(FireworkEvent e, float yaw) {
        boolean isDiagonal = checkDiagonal(yaw, 16f);
        boolean nearPlayer = checkNearPlayer(5f);

        double speedXZ;
        double speedY = 1.66;

        if (isDiagonal) {
            speedXZ = customSpeedSetting.getValue();
        } else if (nearBoostSetting.isValue() && nearPlayer) {
            speedXZ = customSpeedSetting.getValue() - 0.1f;
            speedY = 1.67;
        } else {
            speedXZ = 1.675;
        }

        applyFireworkVelocity(e, speedXZ, speedY);
    }

    private SliderSettings slimeSlider(String name, float value) {
        return new SliderSettings(name, "SlimeWorld firework speed")
                .range(1.5f, 2.5f)
                .setValue(value)
                .visible(() -> modeSetting.isSelected("SlimeWorld"));
    }

    @Native
    private float getSlimeSpeed(float angle, SliderSettings[] settings) {
        int index = (int) (angle / 5.0f);
        if (index >= settings.length) {
            index = settings.length - 1;
        }
        if (index < 0) {
            index = 0;
        }
        return settings[index].getValue();
    }

    @Native
    private float convertAngleToRange(float angle) {
        float absAngle = Math.abs(angle);

        if (absAngle > 90.0f) {
            absAngle = 180.0f - absAngle;
        }

        if (absAngle > 45.0f) {
            absAngle = 90.0f - absAngle;
        }

        return absAngle;
    }

    @Native
    private boolean checkDiagonal(float yaw, float threshold) {
        for (float d : new float[]{45f, 135f, 225f, 315f}) {
            float diff = Math.abs(yaw - d);
            diff = Math.min(diff, 360f - diff);
            if (diff <= threshold) {
                return true;
            }
        }
        return false;
    }

    @Native
    private boolean checkNearPlayer(float distance) {
        if (!nearBoostSetting.isValue() || mc.world == null) return false;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (player.distanceTo(mc.player) <= distance) {
                return true;
            }
        }
        return false;
    }

   @Native
    private void applyFireworkVelocity(FireworkEvent e, double speedXZ, double speedY) {
        Vec3d rotationVector = AngleConnection.INSTANCE.getMoveRotation().toVector();
        Vec3d currentVelocity = e.getVector();

        e.setVector(currentVelocity.add(
                rotationVector.x * 0.1 + (rotationVector.x * speedXZ - currentVelocity.x) * 0.5,
                rotationVector.y * 0.1 + (rotationVector.y * speedY - currentVelocity.y) * 0.5,
                rotationVector.z * 0.1 + (rotationVector.z * speedXZ - currentVelocity.z) * 0.5
        ));
    }
}

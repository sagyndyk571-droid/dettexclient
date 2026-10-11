package neverlose.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.HandAnimationEvent;
import neverlose.events.impl.SwingDurationEvent;
import neverlose.modules.impl.combat.Aura;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SwingAnimation extends ModuleStructure {
    public static SwingAnimation getInstance() {
        return Instance.get(SwingAnimation.class);
    }

    SelectSetting swingType = new SelectSetting("Тип взмаха", "Выберите тип взмаха")
            .value("Chop", "Swipe", "Down", "Smooth", "Smooth 2", "Power", "Feast", "Twist", "Default");
    SliderSettings hitStrengthSetting = new SliderSettings("Сила взмаха", "Сила анимации взмаха")
            .range(0.5F, 3.0F).setValue(1.0F);
    SliderSettings swingSpeedSetting = new SliderSettings("Длительность взмаха", "Длительность анимации удара")
            .range(0.5F, 4.0F).setValue(1.0F);

    BooleanSetting onlySwing = new BooleanSetting("Только при взмахе", "Показывает анимацию только при взмахе")
            .setValue(false);

    BooleanSetting onlyAura = new BooleanSetting("Только при включенной КиллАуре", "Показывает анимацию только при включенной киллауре")
            .setValue(false);

    public SwingAnimation() {
        super("SwingAnimation", "Swing Animation", ModuleCategory.RENDER);
        settings(swingType, hitStrengthSetting, swingSpeedSetting, onlySwing, onlyAura);
    }


    public void applyHandsWithItemsAnimation(MatrixStack matrix, Arm arm, float swingProgress) {
        if (onlyAura.isValue() && !(Aura.getInstance().isState() && Aura.getInstance().target != null)) {
            return;
        }
        if (onlySwing.isValue() && mc.player.handSwingTicks == 0) {
            return;
        }

        float side = arm == Arm.RIGHT ? 1.0F : -1.0F;
        float sin1 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        float sinSmooth = (float) (Math.sin(swingProgress * Math.PI) * 0.5F);
        float strength = MathHelper.clamp(hitStrengthSetting.getValue(), 0.5F, 2.0F);

        matrix.translate(side * -0.018F * sin1 * strength, -0.02F * sin2 * strength, -0.012F * sin2 * strength);
        switch (swingType.getSelected()) {
            case "Chop" -> {
                matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -10.0F * strength));
                matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * sin2 * -6.0F * strength));
            }
            case "Twist" -> {
                matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * sin2 * 8.0F * strength));
                matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * (sin1 - sin2) * 8.0F * strength));
                matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -9.0F * strength));
            }
            case "Swipe" -> {
                matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * sin1 * -9.0F * strength));
                matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * sin2 * -7.0F * strength));
                matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -8.0F * strength));
            }
            case "Down" -> {
                matrix.translate(0.0F, -0.025F * sin2 * strength, 0.0F);
                matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -12.0F * strength));
            }
            case "Smooth" -> {
                matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * sin1 * -5.0F * strength));
                matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -8.0F * strength));
            }
            case "Smooth 2" -> matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -8.0F * strength));
            case "Power" -> {
                matrix.translate(side * (-sinSmooth * sinSmooth * sin1) * 0.035F * strength, 0.0F, -0.02F * sin2 * strength);
                matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * sin2 * -7.0F * strength));
                matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -12.0F * strength));
            }
            case "Feast" -> {
                matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * sin2 * 10.0F * strength));
                matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -7.0F * strength));
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onSwingDuration(SwingDurationEvent e) {
        if (onlyAura.isValue() ? Aura.getInstance().isState() && Aura.getInstance().target != null : true) {
            e.setAnimation(swingSpeedSetting.getValue());
            e.cancel();
        }
    }

    @NonFinal
    private float spinAngle = 0.0F;
    @NonFinal
    private float spinBackTimer = 0.0F;
    @NonFinal
    private boolean wasSwinging = false;

    @EventHandler
    public void onHandAnimation(HandAnimationEvent e) {

        boolean isMainHand = e.getHand().equals(Hand.MAIN_HAND);
        if (isMainHand) {
            MatrixStack matrix = e.getMatrices();
            float swingProgress = e.getSwingProgress();
            int i = mc.player.getMainArm().equals(Arm.RIGHT) ? 1 : -1;
            float sin1 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
            float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
            float sinSmooth = (float) (Math.sin(swingProgress * Math.PI) * 0.5F);
            float strength = hitStrengthSetting.getValue();

            if (onlyAura.isValue() ? Aura.getInstance().isState() && Aura.getInstance().target != null : true) {
                if (onlySwing.isValue() ? mc.player.handSwingTicks != 0 : true) {
                    switch (swingType.getSelected()) {
                        case "Chop" -> {
                            matrix.translate(0.56F * i, -0.44F, -0.72F);
                            matrix.translate(0.0F, 0.33F * -0.6F, 0.0F);
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * i));
                            float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
                            float f2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f2 * -20.0F * i * strength));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f2 * -80.0F * strength));
                            matrix.translate(0.4F, 0.2F, 0.2F);
                            matrix.translate(-0.5F, 0.08F, 0.0F);
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
                        }
                        case "Twist" -> {
                            matrix.translate(i * 0.56F, -0.36F, -0.72F);
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(80 * i));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -90 * strength));
                            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((sin1 - sin2) * 60 * i * strength));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30));
                            matrix.translate(0, -0.1F, 0.05F);
                        }
                        case "Swipe" -> {
                            matrix.translate(0.56F * i, -0.32F, -0.72F);
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(70 * i));
                            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20 * i));
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((sin2 * sin1) * -5 * strength));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees((sin2 * sin1) * -120 * strength));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70));
                        }
                        case "Default" -> {
                            matrix.translate(i * 0.56F, -0.52F - (sin2 * 0.5F * strength), -0.72F);
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 * i));
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45 * i));
                        }
                        case "Down" -> {
                            matrix.translate(i * 0.56F, -0.32F, -0.72F);
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76 * i));
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5 * strength));
                            matrix.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100 * strength));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155 * strength));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100));
                        }
                        case "Smooth" -> {
                            matrix.translate(i * 0.56F, -0.42F, -0.72F);
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * (45.0F + sin1 * -20.0F * strength)));
                            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * sin2 * -20.0F * strength));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F * strength));
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * -45.0F));
                            matrix.translate(0, -0.1, 0);
                        }
                        case "Smooth 2" -> {
                            matrix.translate(i * 0.56F, -0.42F, -0.72F);
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F * strength));
                            matrix.translate(0, -0.1, 0);
                        }
                        case "Power" -> {
                            matrix.translate(i * 0.56F, -0.32F, -0.72F);
                            matrix.translate((-sinSmooth * sinSmooth * sin1) * i * strength, 0, 0);
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(61 * i));
                            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * strength));
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((sin2 * sin1) * -5 * strength));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees((sin2 * sin1) * -30 * strength));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sinSmooth * -60 * strength));
                        }
                        case "Feast" -> {
                            matrix.translate(i * 0.56F, -0.32F, -0.72F);
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 75 * i * strength));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -45 * strength));
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80));
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35 * i));
                        }
                    }
                } else {
                    matrix.translate(i * 0.56F, -0.52F, -0.72F);
                }
            } else {
                return;
            }
            e.cancel();
        }
    }
}

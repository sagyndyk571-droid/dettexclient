package neverlose.modules.impl.misc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.consume.UseAction;
import neverlose.events.api.EventHandler;
import neverlose.events.impl.TickEvent;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.BooleanSetting;
import neverlose.modules.module.setting.implement.MultiSelectSetting;
import neverlose.modules.module.setting.implement.SelectSetting;
import neverlose.modules.module.setting.implement.SliderSettings;
import neverlose.util.Instance;
import neverlose.util.repository.friend.FriendUtils;

import java.util.Random;

public class ShieldDesync extends ModuleStructure {

    public static ShieldDesync getInstance() {
        return Instance.get(ShieldDesync.class);
    }

    private final SelectSetting mode = new SelectSetting("Режим", "Режим работы щита")
            .value("Anti-Axe", "Spam", "Auto-Hold")
            .selected("Spam");

    private final MultiSelectSetting targetType = new MultiSelectSetting("Настройка целей", "Кого учитывать")
            .value("Игроки", "Мобы", "Животные", "Друзья", "Стойки для брони")
            .selected("Игроки", "Мобы");

    private final SelectSetting targetPriority = new SelectSetting("Приоритет цели", "Выбор цели")
            .value("Дистанция", "HP")
            .selected("Дистанция");

    private final SliderSettings range = new SliderSettings("Дальность", "Радиус активации (блоков)")
            .range(0f, 6f)
            .setValue(3.5f);

    private final SliderSettings spamDelayMin = new SliderSettings("Задержка мин (мс)", "Минимальная задержка спама")
            .range(50f, 1000f)
            .setValue(150f)
            .visible(() -> mode.isSelected("Spam"));

    private final SliderSettings spamDelayMax = new SliderSettings("Задержка макс (мс)", "Максимальная задержка спама")
            .range(50f, 1500f)
            .setValue(400f)
            .visible(() -> mode.isSelected("Spam"));

    private final BooleanSetting pauseWhenUsingItem = new BooleanSetting(
            "Пауза при юзании предмета",
            "Оффает щит, пока игрок юзает (еду, зелья, лук)")
            .setValue(true);

    private final Random random = new Random();

    private boolean weControllingKey = false;
    private long    nextToggleMs  = 0;
    private boolean spamPressed   = false;
    private int axeGrace = 0;
    private static final int AXE_GRACE_TICKS = 8;

    public ShieldDesync() {
        super("ShieldDesync", "Защита щита от топора", ModuleCategory.COMBAT);
        settings(mode, targetType, targetPriority, range, spamDelayMin, spamDelayMax, pauseWhenUsingItem);
    }

    private void setShield(boolean press) {
        weControllingKey = press;
        mc.options.useKey.setPressed(press);
    }

    private void releaseControl() {
        if (weControllingKey) {
            mc.options.useKey.setPressed(false);
            weControllingKey = false;
        }
        spamPressed = false;
        axeGrace    = 0;
    }

    @Override
    public void deactivate() {
        if (weControllingKey) {
            mc.options.useKey.setPressed(false);
            weControllingKey = false;
        }
        spamPressed = false;
        axeGrace    = 0;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (!(mc.player.getOffHandStack().getItem() instanceof ShieldItem)) {
            releaseControl();
            return;
        }

        if (pauseWhenUsingItem.isValue()) {
            ItemStack main = mc.player.getMainHandStack();
            boolean mainNeedsUse = !main.isEmpty()
                    && !(main.getItem() instanceof ShieldItem)
                    && main.getUseAction() != UseAction.NONE;
            boolean playerIsUsing = mc.player.isUsingItem()
                    && !(mc.player.getActiveItem().getItem() instanceof ShieldItem);
            if (mainNeedsUse || playerIsUsing) {
                releaseControl();
                return;
            }
        }

        LivingEntity target = findTarget();
        if (target == null) {
            releaseControl();
            return;
        }

        if (entityHoldsAxe(target)) {
            axeGrace = AXE_GRACE_TICKS;
        } else if (axeGrace > 0) {
            axeGrace--;
        }
        boolean danger = axeGrace > 0;

        switch (mode.getSelected()) {

            case "Anti-Axe" -> {
                if (danger) {

                    setShield(false);
                } else {

                    weControllingKey = false;
                }
            }

            case "Spam" -> {
                if (danger) {
                    setShield(false);
                    spamPressed  = false;
                    nextToggleMs = System.currentTimeMillis() + 120L;
                    return;
                }

                long now = System.currentTimeMillis();
                if (now >= nextToggleMs) {
                    spamPressed = !spamPressed;
                    float lo    = spamDelayMin.getValue();
                    float hi    = Math.max(lo + 10f, spamDelayMax.getValue());
                    long  delay = (long)(lo + random.nextFloat() * (hi - lo));
                    nextToggleMs = now + delay;
                    setShield(spamPressed);
                }
            }

            case "Auto-Hold" -> {
                if (danger) {
                    setShield(false);
                } else {
                    setShield(true);
                }
            }
        }
    }

    private LivingEntity findTarget() {
        if (mc.world == null || mc.player == null) return null;

        LivingEntity best      = null;
        float        bestScore = Float.MAX_VALUE;
        float        maxRange  = range.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player)   continue;
            if (!living.isAlive())     continue;
            if (!isValidTarget(living)) continue;

            float dist = mc.player.distanceTo(living);
            if (dist > maxRange) continue;

            float score = targetPriority.isSelected("HP") ? living.getHealth() : dist;
            if (score < bestScore) {
                bestScore = score;
                best      = living;
            }
        }
        return best;
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity instanceof PlayerEntity player) {
            if (FriendUtils.isFriend(player)) return targetType.isSelected("Друзья");
            return targetType.isSelected("Игроки");
        }
        if (entity instanceof ArmorStandEntity) return targetType.isSelected("Стойки для брони");
        if (entity instanceof AnimalEntity)     return targetType.isSelected("Животные");
        if (entity instanceof MobEntity)        return targetType.isSelected("Мобы");
        return false;
    }

    private boolean entityHoldsAxe(LivingEntity e) {
        return e.getMainHandStack().getItem() instanceof AxeItem
                || e.getOffHandStack().getItem() instanceof AxeItem;
    }
}
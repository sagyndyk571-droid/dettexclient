package neverlose.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import neverlose.modules.module.ModuleStructure;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.implement.ColorSetting;
import neverlose.util.ColorUtil;
import neverlose.util.repository.friend.FriendUtils;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChinaHat extends ModuleStructure {
    static ChinaHat instance;

    public final ColorSetting color1 = new ColorSetting("Color 1", "First gradient color")
            .value(ColorSetting.DEFAULT_COLOR);

    public final ColorSetting color2 = new ColorSetting("Color 2", "Second gradient color")
            .value(ColorSetting.DEFAULT_COLOR);

    public ChinaHat() {
        super("ChinaHat", "China Hat", ModuleCategory.RENDER);
        instance = this;
        settings(color1, color2);
    }

    public static ChinaHat getInstance() {
        return instance;
    }

    public boolean shouldRender(PlayerEntity player) {
        if (player == null || !player.isAlive()) {
            return false;
        }

        if (player == mc.player) {
            return !mc.options.getPerspective().isFirstPerson();
        }

        return FriendUtils.isFriend(player);
    }

    public boolean shouldRender(PlayerEntityRenderState state) {
        if (!isState() || mc.world == null || mc.player == null || state == null) {
            return false;
        }

        Entity entity = mc.world.getEntityById(state.id);
        return entity instanceof PlayerEntity player && shouldRender(player);
    }

    public int getGradientColor(int index, int size, int alpha) {
        long time = System.currentTimeMillis();
        float timeOffset = (time / 200.0f) % size;
        int adjustedIndex = (int) ((index + timeOffset) % size);
        float progress = (float) adjustedIndex / size;

        int color;
        if (progress < 0.5f) {
            color = ColorUtil.interpolateColor(color1.getColor(), color2.getColor(), progress * 2.0f);
        } else {
            color = ColorUtil.interpolateColor(color2.getColor(), color1.getColor(), (progress - 0.5f) * 2.0f);
        }

        return ColorUtil.replAlpha(color, alpha);
    }
}

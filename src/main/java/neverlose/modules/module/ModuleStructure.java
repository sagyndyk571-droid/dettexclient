package neverlose.modules.module;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.MinecraftClient;
import neverlose.debob;
import org.lwjgl.glfw.GLFW;
import neverlose.IMinecraft;
import neverlose.events.api.EventManager;
import neverlose.events.impl.ModuleToggleEvent;
import neverlose.modules.module.category.ModuleCategory;
import neverlose.modules.module.setting.SettingRepository;
import neverlose.modules.impl.render.Hud;
import neverlose.screens.hud.Notifications;
import neverlose.util.animations.Animation;
import neverlose.util.animations.Decelerate;
import neverlose.util.animations.Direction;
import neverlose.util.security.ClientUnhook;

import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleStructure extends SettingRepository implements IMinecraft {
    String name;
    String description;
    ModuleCategory category;
    Animation animation = new Decelerate().setValue(1.0).setMs(100);

    public ModuleStructure(String name, ModuleCategory category) {
        this.name = name;
        this.category = category;
        this.description = "";
    }

    public ModuleStructure(String name, String description, ModuleCategory category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    @NonFinal
    int key = GLFW.GLFW_KEY_UNKNOWN, type = 1;

    @NonFinal
    public boolean state;

    @NonFinal
    public boolean favorite;

    @NonFinal
    private static boolean toggleFeedbackSuppressed;

    public static void runWithoutToggleFeedback(Runnable runnable) {
        boolean previous = toggleFeedbackSuppressed;
        toggleFeedbackSuppressed = true;
        try {
            runnable.run();
        } finally {
            toggleFeedbackSuppressed = previous;
        }
    }

    public void setKey(int key) {
        this.key = key;
    }

    public void switchState() {
        setState(!state);
    }

    public void setState(boolean state) {
        if (state && ClientUnhook.isHidden()) {
            return;
        }
        animation.setDirection(state ? Direction.FORWARDS : Direction.BACKWARDS);
        if (state != this.state) {
            this.state = state;
            handleStateChange();
        }
    }

    public void switchFavorite() {
        setFavorite(!favorite);
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    private void handleStateChange() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player != null && mc.world != null) {
            Hud hud = Hud.getInstance();
            Notifications notifications = Notifications.getInstance();

            if (!toggleFeedbackSuppressed && hud != null && hud.isState() && notifications != null) {
                if (hud.interfaceSettings.isSelected("Notifications") && hud.notifyModule.isValue()) {
                    if (state) {
                        notifications.addNotification("Feature "  + name + " - enabled!", 2000);
                    } else {
                        notifications.addNotification("Feature " + name + " - disabled!", 2000);
                    }
                }
            }

            if (state) {
                activate();
            } else {
                deactivate();
            }
        }
        toggleSilent(state);
        if (!toggleFeedbackSuppressed) {
            EventManager.callEvent(new ModuleToggleEvent(this, state));
        }
    }

    private void toggleSilent(boolean activate) {
        try {
            var eventManager = debob.getInstance().getManager().getEventManager();
            if (eventManager != null) {
                if (activate) {
                    eventManager.register(this);
                } else {
                    eventManager.unregister(this);
                }
            }
        } catch (Exception ignored) {}
    }

    public void activate() {
    }

    public void deactivate() {
    }

    public List<neverlose.modules.module.setting.Setting> getSettings() {
        return settings();
    }

    public boolean isVisual() {
        return false;
    }

    public String getDescription() {
        return description;
    }

    public boolean isState() {
        return state;
    }

    public String getName() {
        return name;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public int getKey() {
        return key;
    }
}

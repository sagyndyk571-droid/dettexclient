package neverlose.client.draggables;

import net.minecraft.client.gui.DrawContext;
import neverlose.events.impl.PacketEvent;
import neverlose.modules.impl.render.Hud;
import neverlose.screens.hud.*;
import neverlose.util.config.impl.drag.DragConfig;

import java.util.ArrayList;
import java.util.List;

public class HudManager {

    private final List<HudElement> elements = new ArrayList<>();
    private boolean initialized = false;

    public HudManager() {
    }

    public void initElements() {
        if (initialized) return;

        register(new Watermark());
        register(new HotKeys());
        register(new neverlose.screens.hud.ArrayList());
        register(new Notifications());
        register(new Potions());
        register(new TargetHud());
        register(new Info());
        register(new Inventory());
        register(new CoolDowns());
        register(new MusicInfo());
        register(new Staff());

        initialized = true;

        DragConfig.getInstance().load();
    }

    public void register(HudElement element) {
        elements.add(element);
    }

    public void onPacket(PacketEvent e) {
        for (HudElement element : elements) {
            element.onPacket(e);
        }
    }

    public void render(DrawContext context, float tickDelta, int mouseX, int mouseY) {
        Hud hud = Hud.getInstance();
        if (hud == null || !hud.isState()) return;

        if (isHud2(hud)) {
            Hud2Renderer.render(context, tickDelta, mouseX, mouseY);
            return;
        }

        for (HudElement element : elements) {
            if (isElementEnabled(element)) {
                element.render(context, tickDelta);
            }
        }
    }

    public void tick() {
        for (HudElement element : elements) {
            if (isElementEnabled(element)) {
                element.tick();
            }
        }

        Hud hud = Hud.getInstance();
        if (hud != null && isHud2(hud)) {
            Hud2Renderer.tick();
        }
    }

    private boolean isHud2(Hud hud) {
        return hud != null && hud.hudStyle != null && hud.hudStyle.isSelected("HUD 2");
    }

    private boolean isElementEnabled(HudElement element) {
        Hud hud = Hud.getInstance();
        if (hud == null || !hud.isState()) return false;

        String name = element.getName();
        return hud.interfaceSettings.isSelected(name);
    }

    public HudElement getElementAt(double mouseX, double mouseY) {
        for (int i = elements.size() - 1; i >= 0; i--) {
            HudElement element = elements.get(i);
            if (isElementEnabled(element) && element.visible()) {
                float width = element.getWidth() * element.getHudScale();
                float height = element.getHeight() * element.getHudScale();
                if (mouseX >= element.getX() && mouseX <= element.getX() + width &&
                        mouseY >= element.getY() && mouseY <= element.getY() + height) {
                    return element;
                }
            }
        }
        return null;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (HudElement element : elements) {
            if (isElementEnabled(element)) {
                if (element.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void saveConfig() {
        DragConfig.getInstance().save();
    }

    public void loadConfig() {
        DragConfig.getInstance().load();
    }

    public List<HudElement> getElements() {
        return elements;
    }

    public List<HudElement> getEnabledElements() {
        List<HudElement> enabled = new ArrayList<>();
        for (HudElement element : elements) {
            if (isElementEnabled(element)) {
                enabled.add(element);
            }
        }
        return enabled;
    }

    public boolean isInitialized() {
        return initialized;
    }
}

package neverlose.manager;

import com.adl.nativeprotect.Native;
import lombok.Getter;
import neverlose.client.draggables.HudManager;
import neverlose.command.CommandManager;
import neverlose.events.api.EventManager;
import neverlose.modules.impl.combat.aura.attack.StrikerConstructor;
import neverlose.modules.module.*;
import neverlose.screens.clickgui.ClickGui;
import neverlose.util.config.ConfigSystem;
import neverlose.util.config.impl.bind.BindConfig;
import neverlose.util.config.impl.blockesp.BlockESPConfig;
import neverlose.util.config.impl.drag.DragConfig;
import neverlose.util.config.impl.friend.FriendConfig;
import neverlose.util.config.impl.prefix.PrefixConfig;
import neverlose.util.config.impl.proxy.ProxyConfig;
import neverlose.util.config.impl.staff.StaffConfig;
import neverlose.util.modules.ModuleProvider;
import neverlose.util.modules.ModuleSwitcher;
import neverlose.util.render.shader.RenderCore;
import neverlose.util.render.shader.Scissor;
import neverlose.util.render.font.FontInitializer;
import neverlose.util.repository.macro.MacroRepository;
import neverlose.util.repository.way.WayRepository;
import neverlose.util.tps.TPSCalculate;



@Getter
public class Manager {
    public StrikerConstructor attackPerpetrator = new StrikerConstructor();
    private EventManager eventManager;
    private RenderCore renderCore;
    private Scissor scissor;
    private ModuleProvider moduleProvider;
    @Getter
    private ModuleRepository moduleRepository;
    private ModuleSwitcher moduleSwitcher;
    private ClickGui clickgui;
    private ConfigSystem configSystem;
    private CommandManager commandManager;
    private TPSCalculate tpsCalculate;
    private HudManager hudManager = new HudManager();
    private final MediaPlayer mediaPlayer = new MediaPlayer();

        public MediaPlayer getMediaPlayer() {
            return mediaPlayer;
        }
    public void init() {
        MacroRepository.getInstance().init();
        WayRepository.getInstance().init();
        BlockESPConfig.getInstance().load();
        FriendConfig.getInstance().load();
        PrefixConfig.getInstance().load();
        StaffConfig.getInstance().load();
        ProxyConfig.getInstance().load();
        DragConfig.getInstance().load();
        BindConfig.getInstance();

        FontInitializer.register();
        tpsCalculate = new TPSCalculate();

        clickgui = new ClickGui();
        eventManager = new EventManager();
        renderCore = new RenderCore();
        scissor = new Scissor();
        hudManager = new HudManager();
        hudManager.initElements();
        moduleRepository = new ModuleRepository();
        moduleRepository.setup();
        moduleProvider = new ModuleProvider(moduleRepository.modules());
        moduleSwitcher = new ModuleSwitcher(moduleRepository.modules(), eventManager);
        configSystem = new ConfigSystem();
        configSystem.init();

        commandManager = new CommandManager();
        commandManager.init();
    }
}

package neverlose;

import com.adl.nativeprotect.Native;
import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import neverlose.manager.Manager;
import neverlose.util.sounds.SoundManager;

public class debob implements ClientModInitializer {

    @Getter
    private static debob instance;
    public String debob = "debob paster";
    public String danil = "poshel naxui pidor";
    public String KODEK = "top proter get luchshia prota";
    public String Dister = "bratanchik";
    @Getter
    private Manager manager;
    
    public static debob getInstance() {
        return instance;
    }
    
    public Manager getManager() {
        return manager;
    }
    @Override
    public void onInitializeClient() {
        registerHandMyItemsPack();
        SoundManager.init();
    }
    private void registerHandMyItemsPack() {
        FabricLoader.getInstance().getModContainer("copyright").ifPresent(container ->
                ResourceLoader.registerBuiltinPack(
                        Identifier.of("copyright", "handmyitems"),
                        container,
                        Text.literal("HandMyItems"),
                        PackActivationType.DEFAULT_ENABLED
                )
        );
    }
    public void init() {
        instance = this;
        manager = new Manager();
        manager.init();
    }
}

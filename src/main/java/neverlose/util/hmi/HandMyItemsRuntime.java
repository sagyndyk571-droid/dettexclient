package neverlose.util.hmi;

import com.holdmylua.source.global.DispatcherStorage;
import com.holdmylua.source.global.GlobalsStorage;
import com.holdmylua.source.global.item_model.ItemModelStorage;
import com.holdmylua.source.lua_runtime.LuaScriptCache;
import com.holdmylua.source.lua_runtime.ModelScriptCache;
import com.holdmylua.source.lua_runtime.ScriptHolder;
import com.holdmylua.source.lua_runtime.resource_controller.LuaAnimationResourceLoader;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.resource.ResourceType;

import java.io.IOException;
import java.util.ArrayList;

public final class HandMyItemsRuntime {
    private static final LuaAnimationResourceLoader LOADER = new LuaAnimationResourceLoader();
    private static boolean initialized;
    private static boolean active;

    private HandMyItemsRuntime() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
                .registerReloadListener((IdentifiableResourceReloadListener) LOADER);
        initialized = true;
    }

    public static boolean isActive() {
        return active;
    }

    public static void enable() {
        active = true;
        initialize();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getResourceManager() != null) {
            LOADER.reload(client.getResourceManager());
        }
    }

    public static void disable() {
        active = false;
        clearRuntimeState();
    }

    private static void clearRuntimeState() {
        try {
            ScriptHolder.handScriptCache = new LuaScriptCache("return");
            ScriptHolder.handRelativeScriptCache = new LuaScriptCache("return");
            ScriptHolder.itemScriptCache = new LuaScriptCache("return");
            ScriptHolder.itemModelCache = new ModelScriptCache("return");
        } catch (IOException exception) {
            throw new RuntimeException("Failed to reset HandMyItems scripts", exception);
        }

        ScriptHolder.handAddonsCache = new ArrayList<>();
        ScriptHolder.handRelativeAddonsCache = new ArrayList<>();
        ScriptHolder.itemAddonsCache = new ArrayList<>();
        ScriptHolder.itemModelAddonsCache = new ArrayList<>();

        GlobalsStorage.renderAsBlock.clear();
        GlobalsStorage.translateItem.clear();
        GlobalsStorage.applyBlockRotation.clear();
        GlobalsStorage.itemSwingSpeed.clear();
        GlobalsStorage.useDuration.clear();
        GlobalsStorage.usingItem.clear();
        GlobalsStorage.registry.clear();
        GlobalsStorage.particles.clear();
        GlobalsStorage.modelPartAnimator.clear();
        GlobalsStorage.debugTextRenderer.clear();
        GlobalsStorage.mainHandItem = Items.AIR.getDefaultStack();
        GlobalsStorage.offHandItem = Items.AIR.getDefaultStack();
        GlobalsStorage.renderedStack = Items.AIR.getDefaultStack();
        DispatcherStorage.clear();
        ItemModelStorage.clear();
    }
}

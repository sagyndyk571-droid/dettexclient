/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.luaj.vm2.Globals
 *  org.luaj.vm2.LoadState
 *  org.luaj.vm2.LuaValue
 *  org.luaj.vm2.compiler.LuaC
 *  org.luaj.vm2.lib.Bit32Lib
 *  org.luaj.vm2.lib.CoroutineLib
 *  org.luaj.vm2.lib.PackageLib
 *  org.luaj.vm2.lib.StringLib
 *  org.luaj.vm2.lib.TableLib
 *  org.luaj.vm2.lib.jse.JseBaseLib
 *  org.luaj.vm2.lib.jse.JseMathLib
 */
package com.holdmylua.source.lua_runtime;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.CoroutineLib;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

public class LuaScriptManager {
    private static final LuaScriptManager INSTANCE = new LuaScriptManager();
    public final Globals sharedGlobals = LuaScriptManager.standardGlobals();

    private LuaScriptManager() {
    }

    public static LuaScriptManager getInstance() {
        return INSTANCE;
    }

    private static Globals standardGlobals() {
        Globals globals = new Globals();
        globals.load((LuaValue)new JseBaseLib());
        globals.load((LuaValue)new PackageLib());
        globals.load((LuaValue)new Bit32Lib());
        globals.load((LuaValue)new TableLib());
        globals.load((LuaValue)new StringLib());
        globals.load((LuaValue)new CoroutineLib());
        globals.load((LuaValue)new JseMathLib());
        LoadState.install((Globals)globals);
        LuaC.install((Globals)globals);
        return globals;
    }
}


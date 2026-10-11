package neverlose.modules.module.setting.implement;

import neverlose.modules.module.ModuleStructure;

/**
 * BindSetting, привязанный к клавише включения модуля (module.getKey() / setKey()).
 * Используется в окошке Bind, чтобы показывать кнопку бинда, а не настройки модуля (типа «Только криты»).
 */
public class ModuleKeyBindSetting extends BindSetting {

    private final ModuleStructure module;

    public ModuleKeyBindSetting(ModuleStructure module) {
        super("Key", "");
        this.module = module;
    }

    @Override
    public int getKey() {
        return module.getKey();
    }

    @Override
    public BindSetting setKey(int key) {
        module.setKey(key);
        return this;
    }

    @Override
    public int getType() {
        return module.getType();
    }

    @Override
    public BindSetting setType(int type) {
        module.setType(type);
        return this;
    }
}

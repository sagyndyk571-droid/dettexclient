/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.registry.Registries
 *  net.minecraft.util.Identifier
 */
package com.holdmylua.source.scripting.script_wrappers;

import com.holdmylua.source.annotation.Safe;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class JSItems {
    @Safe
    public Item get(String name) {
        Identifier id = Identifier.of((String)name);
        return (Item)Registries.ITEM.get(id);
    }

    @Safe
    public String checkItemName(ItemStack item) {
        return item.getCustomName().toString();
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.sound.SoundEvent
 */
package com.holdmylua.source.scripting.custom_api;

import com.holdmylua.source.annotation.Safe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;

public class Sound {
    private SoundEvent sound;

    @Safe
    public void play(float volume, float pitch) {
        MinecraftClient.getInstance().player.playSound(this.sound, volume, pitch);
    }
}


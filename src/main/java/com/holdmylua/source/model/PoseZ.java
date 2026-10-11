/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.util.math.MatrixStack
 */
package com.holdmylua.source.model;

import com.holdmylua.source.model.interfaces.Poses;
import com.holdmylua.source.model.parents.AbstractPose;
import net.minecraft.client.util.math.MatrixStack;

public class PoseZ
extends AbstractPose
implements Poses {
    float amount;

    public PoseZ(float amount) {
        this.amount = amount;
    }

    @Override
    public void applyPose(MatrixStack matrices) {
        matrices.translate(0.0f, 0.0f, this.amount);
    }
}


/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.util.ARGB;
import org.joml.Vector3fc;

/** Converts vanilla particle colours for Bedrock's packed colour fields. */
public final class VanillaParticles {
    private VanillaParticles() {
    }

    public static int color(ColorParticleOption particle) {
        return ARGB.colorFromFloat(particle.getAlpha(), particle.getRed(), particle.getGreen(), particle.getBlue());
    }

    public static int color(SpellParticleOption particle) {
        return ARGB.colorFromFloat(1, particle.getRed(), particle.getGreen(), particle.getBlue()) & 0xFFFFFF;
    }

    public static int rgb(Vector3fc color) {
        return ARGB.colorFromFloat(1, color.x(), color.y(), color.z()) & 0xFFFFFF;
    }
}

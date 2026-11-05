/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import io.netty.buffer.Unpooled;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SpellParticleOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VanillaParticlesTest {
    @BeforeAll
    static void bootstrap() {
        VanillaBootstrap.defaultRegistries();
    }

    @Test
    void preservesAllChannelValuesThroughTheNativeColorParticleCodec() {
        var codec = ColorParticleOption.streamCodec(ParticleTypes.ENTITY_EFFECT);
        var buffer = Unpooled.buffer();
        try {
            for (int channel = 0; channel <= 255; channel++) {
                int color = channel << 24 | (255 - channel) << 16 | (channel * 3 & 255) << 8 | channel * 7 & 255;
                codec.encode(buffer, ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, color));
                assertEquals(color, VanillaParticles.color(codec.decode(buffer)), "packed colour for channel " + channel);
                assertEquals(0, buffer.readableBytes());
                buffer.clear();
            }
        } finally {
            buffer.release();
        }
    }

    @Test
    void preservesDustAndSpellRgbValues() {
        for (int channel = 0; channel <= 255; channel++) {
            int color = channel << 16 | (channel * 3 & 255) << 8 | channel * 7 & 255;
            var dust = new DustParticleOptions(color, 1);
            assertEquals(color, VanillaParticles.rgb(dust.getColor()));
            var transition = new DustColorTransitionOptions(color, 0x123456, 1);
            assertEquals(color, VanillaParticles.rgb(transition.getFromColor()));
            assertEquals(0x123456, VanillaParticles.rgb(transition.getToColor()));
            assertEquals(color, VanillaParticles.color(SpellParticleOption.create(ParticleTypes.EFFECT, color, 1)));
        }
    }
}

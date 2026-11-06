/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.PalettedContainerFactory;

/** Decodes chunk sections with the client's native buffer and palette readers. */
public final class VanillaChunks {
    private VanillaChunks() {
    }

    public static Section[] readSections(ClientboundLevelChunkPacketData data, RegistryAccess registries, int sectionCount) {
        if (sectionCount < 0) {
            throw new IllegalArgumentException("Negative section count: " + sectionCount);
        }
        var factory = PalettedContainerFactory.create(registries);
        var buffer = data.getReadBuffer();
        try {
            var sections = new Section[sectionCount];
            for (int i = 0; i < sectionCount; i++) {
                short nonEmptyBlockCount = buffer.readShort();
                short fluidCount = buffer.readShort();
                var states = VanillaBlockPalette.read(buffer);
                var biomes = factory.createForBiomes();
                biomes.read(buffer);
                sections[i] = new Section(nonEmptyBlockCount, fluidCount, states, biomes);
            }
            if (buffer.isReadable()) {
                throw new IllegalArgumentException("Trailing chunk section data: " + buffer.readableBytes() + " bytes");
            }
            return sections;
        } finally {
            buffer.release();
        }
    }

    public record Section(short nonEmptyBlockCount, short fluidCount, VanillaBlockPalette states,
                          PalettedContainerRO<Holder<Biome>> biomes) {
        public boolean hasOnlyAir() {
            return nonEmptyBlockCount == 0;
        }
    }
}

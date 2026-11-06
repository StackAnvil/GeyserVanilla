/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;

/** Decodes chunk sections with the client's native section and palette readers. */
public final class VanillaChunks {
    private VanillaChunks() {
    }

    public static LevelChunkSection[] readSections(ClientboundLevelChunkPacketData data, RegistryAccess registries, int sectionCount) {
        if (sectionCount < 0) {
            throw new IllegalArgumentException("Negative section count: " + sectionCount);
        }
        var factory = PalettedContainerFactory.create(registries);
        var buffer = data.getReadBuffer();
        try {
            var sections = new LevelChunkSection[sectionCount];
            for (int i = 0; i < sectionCount; i++) {
                var section = new LevelChunkSection(factory);
                section.read(buffer);
                sections[i] = section;
            }
            if (buffer.isReadable()) {
                throw new IllegalArgumentException("Trailing chunk section data: " + buffer.readableBytes() + " bytes");
            }
            return sections;
        } finally {
            buffer.release();
        }
    }
}

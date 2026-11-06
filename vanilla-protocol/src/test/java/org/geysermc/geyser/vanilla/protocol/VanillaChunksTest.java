/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.GlobalPalette;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.SingleValuePalette;
import net.minecraft.world.level.levelgen.Heightmap;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VanillaChunksTest {
    @Test
    void decodesSingletonLocalAndGlobalNativeChunkPalettes() throws Throwable {
        var registries = VanillaBootstrap.defaultRegistries();
        var factory = PalettedContainerFactory.create(registries);
        var biomeRegistry = registries.lookupOrThrow(Registries.BIOME);
        var biomeValues = biomeRegistry.listElements().limit(2).toList();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            for (int distinct : new int[]{1, 2, 17, 512}) {
                var original = new LevelChunkSection(factory);
                for (int index = 0; index < 4096; index++) {
                    original.setBlockState(index & 15, index >> 8, index >> 4 & 15, Block.stateById(index % distinct));
                }
                var biomes = factory.createForBiomes();
                for (int index = 0; index < 64; index++) {
                    biomes.set(index & 3, index >> 4, index >> 2 & 3, biomeValues.get(index % 2));
                }
                original = new LevelChunkSection(original.getStates(), biomes);
                original.recalcBlockCounts();
                original.write(buffer);
                byte[] bytes = new byte[buffer.readableBytes()];
                buffer.readBytes(bytes);
                var packetData = data(bytes);
                ClientboundLevelChunkPacketData.STREAM_CODEC.encode(wire, packetData);
                packetData = ClientboundLevelChunkPacketData.STREAM_CODEC.decode(wire);
                var decoded = VanillaChunks.readSections(packetData, registries, 1)[0];
                var view = VanillaPalettes.view(decoded.getStates());
                if (distinct == 1) assertInstanceOf(SingleValuePalette.class, view.palette());
                if (distinct == 512) assertInstanceOf(GlobalPalette.class, view.palette());
                for (int index = 0; index < 4096; index++) {
                    var expected = Block.stateById(index % distinct);
                    assertSame(expected, decoded.getBlockState(index & 15, index >> 8, index >> 4 & 15));
                    assertSame(expected, view.palette().valueFor(view.storage().get(index)));
                }
                var biomeView = VanillaPalettes.view(decoded.getBiomes());
                for (int index = 0; index < 64; index++) {
                    assertEquals(biomeValues.get(index % 2), biomeView.palette().valueFor(biomeView.storage().get(index)));
                }
                assertEquals(original.hasOnlyAir(), decoded.hasOnlyAir());
                assertFalse(wire.isReadable());
                wire.clear();
                buffer.clear();
            }
        } finally {
            wire.release();
            buffer.release();
        }
    }

    @Test
    void rejectsTruncatedTrailingAndInvalidSectionCounts() throws Throwable {
        var registries = VanillaBootstrap.defaultRegistries();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new LevelChunkSection(PalettedContainerFactory.create(registries)).write(buffer);
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            var valid = data(bytes);
            assertThrows(IllegalArgumentException.class, () -> VanillaChunks.readSections(valid, registries, -1));
            assertThrows(IllegalArgumentException.class, () -> VanillaChunks.readSections(valid, registries, 0));
            var truncated = data(java.util.Arrays.copyOf(bytes, bytes.length - 1));
            assertThrows(RuntimeException.class, () -> VanillaChunks.readSections(truncated, registries, 1));
            assertEquals(1, VanillaChunks.readSections(valid, registries, 1).length);
        } finally {
            buffer.release();
        }
    }

    private static ClientboundLevelChunkPacketData data(byte[] sections) throws Throwable {
        var constructor = MethodHandles.privateLookupIn(ClientboundLevelChunkPacketData.class, MethodHandles.lookup())
            .findConstructor(ClientboundLevelChunkPacketData.class, MethodType.methodType(void.class, Map.class, byte[].class, List.class));
        return (ClientboundLevelChunkPacketData) constructor.invoke(Map.<Heightmap.Types, long[]>of(), sections, List.of());
    }
}

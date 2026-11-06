/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.chunk.GlobalPalette;
import net.minecraft.world.level.chunk.SingleValuePalette;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class VanillaBlockPaletteTest {
    private static final int CUSTOM_ID = 1_000_001;

    @Test
    void keepsCustomSingletonAndLocalIdsThroughPaletteChanges() {
        VanillaBootstrap.initialize();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeByte(0);
            buffer.writeVarInt(CUSTOM_ID);
            var palette = VanillaBlockPalette.read(buffer);
            assertFalse(buffer.isReadable());
            assertInstanceOf(SingleValuePalette.class, palette.view().palette());
            assertEquals(CUSTOM_ID, palette.get(15, 15, 15));
            palette.set(0, 0, 0, 1);
            for (int i = 0; i < 1024; i++) {
                palette.set(1, 0, 0, CUSTOM_ID);
            }
            assertEquals(2, palette.view().palette().getSize());
            assertEquals(1, palette.get(0, 0, 0));
            assertEquals(CUSTOM_ID, palette.get(15, 15, 15));

            // Exercise the native identity-based hash palette, then promotion to global storage.
            for (int i = 2; i < 50; i++) {
                palette.set(i & 15, i >> 8, i >> 4 & 15, CUSTOM_ID + i);
            }
            int size = palette.view().palette().getSize();
            for (int i = 0; i < 1024; i++) {
                palette.set(1, 0, 0, CUSTOM_ID);
            }
            assertEquals(size, palette.view().palette().getSize());
            for (int i = 50; i < 512; i++) {
                palette.set(i & 15, i >> 8, i >> 4 & 15, CUSTOM_ID + i);
            }
            assertInstanceOf(GlobalPalette.class, palette.view().palette());
            assertEquals(1, palette.get(0, 0, 0));
            assertEquals(CUSTOM_ID, palette.get(1, 0, 0));
            assertEquals(CUSTOM_ID, palette.get(15, 15, 15));
            for (int i = 2; i < 512; i++) {
                assertEquals(CUSTOM_ID + i, palette.get(i & 15, i >> 8, i >> 4 & 15));
            }
        } finally {
            buffer.release();
        }
    }

    @Test
    void readsCustomLocalPaletteAndRetainsCanonicalValuesOnUpdate() {
        VanillaBootstrap.initialize();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeByte(4);
            buffer.writeVarInt(2);
            buffer.writeVarInt(1);
            buffer.writeVarInt(CUSTOM_ID);
            var storage = new SimpleBitStorage(4, 4096);
            for (int i = 0; i < 4096; i++) {
                storage.set(i, i % 2);
            }
            buffer.writeFixedSizeLongArray(storage.getRaw());
            var palette = VanillaBlockPalette.read(buffer);
            assertFalse(buffer.isReadable());
            for (int i = 0; i < 4096; i++) {
                assertEquals(i % 2 == 0 ? 1 : CUSTOM_ID, palette.get(i & 15, i >> 8, i >> 4 & 15));
            }
            for (int i = 0; i < 1024; i++) {
                palette.set(0, 0, 0, CUSTOM_ID);
            }
            assertEquals(2, palette.view().palette().getSize());
            assertEquals(CUSTOM_ID, palette.get(0, 0, 0));
            assertEquals(1, palette.get(2, 0, 0));
        } finally {
            buffer.release();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {9, 16, 17, 21, 31})
    void readsExactGlobalWireWidthAndWidensForLaterUpdates(int bits) {
        VanillaBootstrap.initialize();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            int highest = (int) ((1L << bits) - 1);
            buffer.writeByte(bits);
            var storage = new SimpleBitStorage(bits, 4096);
            for (int i = 0; i < 4096; i++) {
                storage.set(i, i % 2 == 0 ? 1 : highest);
            }
            buffer.writeFixedSizeLongArray(storage.getRaw());
            buffer.writeInt(0x12345678);
            var palette = VanillaBlockPalette.read(buffer);
            assertEquals(0x12345678, buffer.readInt());
            assertFalse(buffer.isReadable());
            assertInstanceOf(GlobalPalette.class, palette.view().palette());
            int next = bits == 31 ? 2 : highest + 1;
            palette.set(0, 0, 0, next);
            assertEquals(next, palette.get(0, 0, 0));
            for (int i = 1; i < 4096; i++) {
                assertEquals(i % 2 == 0 ? 1 : highest, palette.get(i & 15, i >> 8, i >> 4 & 15));
            }
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsNegativeIdsAndInvalidWidths() {
        VanillaBootstrap.initialize();
        var palette = new VanillaBlockPalette();
        assertThrows(IllegalArgumentException.class, () -> palette.set(0, 0, 0, -1));
        assertEquals(0, palette.get(0, 0, 0));
        var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(new byte[] {32}));
        try {
            assertThrows(IllegalArgumentException.class, () -> VanillaBlockPalette.read(buffer));
        } finally {
            buffer.release();
        }
    }
}

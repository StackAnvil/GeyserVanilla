/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.GlobalPalette;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.Strategy;

import java.util.Iterator;
import java.util.stream.IntStream;

/** Native block palette storage that also retains non-vanilla Java runtime IDs. */
public final class VanillaBlockPalette {
    private final RawIds ids;
    private PalettedContainer<Integer> container;
    private int bits;

    public VanillaBlockPalette() {
        this(Integer.SIZE - Integer.numberOfLeadingZeros(Block.BLOCK_STATE_REGISTRY.size() - 1));
    }

    private VanillaBlockPalette(int bits) {
        this.bits = bits;
        this.ids = new RawIds(bits);
        this.container = new PalettedContainer<>(ids.byId(0), Strategy.createForBlockStates(ids));
    }

    public static VanillaBlockPalette read(FriendlyByteBuf buffer) {
        int wireBits = buffer.getUnsignedByte(buffer.readerIndex());
        if (wireBits > 31) {
            throw new IllegalArgumentException("Invalid block palette width: " + wireBits);
        }
        // Native readers allocate global storage from the map size, rather than the wire width.
        var palette = wireBits > 8 ? new VanillaBlockPalette(wireBits) : new VanillaBlockPalette();
        palette.container.read(buffer);
        return palette;
    }

    public int get(int x, int y, int z) {
        var view = view();
        int value = view.storage().get((y << 4 | z) << 4 | x);
        return view.palette() instanceof GlobalPalette ? value : view.palette().valueFor(value);
    }

    public void set(int x, int y, int z, int blockId) {
        if (blockId < 0) {
            throw new IllegalArgumentException("Negative block state ID: " + blockId);
        }
        int requiredBits = Integer.SIZE - Integer.numberOfLeadingZeros(Math.max(blockId, ids.maximumId));
        if (requiredBits > bits) {
            // A later block update can exceed the width of the server's original global palette.
            bits = requiredBits;
            ids.bits = bits;
            var replacement = new PalettedContainer<>(ids.byId(get(0, 0, 0)), Strategy.createForBlockStates(ids));
            for (int index = 0; index < 4096; index++) {
                replacement.set(index & 15, index >> 8, index >> 4 & 15,
                    ids.byId(get(index & 15, index >> 8, index >> 4 & 15)));
            }
            container = replacement;
        }
        if (view().palette() instanceof GlobalPalette) {
            container.set(x, y, z, blockId);
        } else {
            container.set(x, y, z, ids.byId(blockId));
        }
        if (view().palette() instanceof GlobalPalette) {
            ids.values.clear();
        }
    }

    public VanillaPalettes.View<Integer> view() {
        return VanillaPalettes.view(container);
    }

    private static final class RawIds implements IdMap<Integer> {
        // Vanilla palettes compare values by identity. Reuse boxes for both reads and updates.
        private final Int2ObjectOpenHashMap<Integer> values = new Int2ObjectOpenHashMap<>();
        private int maximumId;
        private int bits;

        private RawIds(int bits) {
            this.bits = bits;
        }

        @Override
        public int getId(Integer value) {
            return value;
        }

        @Override
        public Integer byId(int id) {
            if (id < 0) {
                return null;
            }
            maximumId = Math.max(maximumId, id);
            return values.computeIfAbsent(id, Integer::valueOf);
        }

        @Override
        public int size() {
            return (int) Math.min(Integer.MAX_VALUE, 1L << bits);
        }

        @Override
        public Iterator<Integer> iterator() {
            return IntStream.range(0, size()).boxed().iterator();
        }
    }
}

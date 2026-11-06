/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.geysermc.geyser.vanilla.runtime.VanillaClassLoader;

/** Retains custom wire IDs in native block states without expanding vanilla's dense tables. */
public final class VanillaBlockStates {
    private static volatile Binding binding;

    private VanillaBlockStates() {
    }

    static synchronized void initialize() {
        if (binding == null && Block.class.getClassLoader() instanceof VanillaClassLoader) {
            binding = new Binding(Block.BLOCK_STATE_REGISTRY, Block.BLOCK_STATE_REGISTRY.size(),
                CacheBuilder.newBuilder().weakValues().build(CacheLoader.from(WireState::new)));
        }
    }

    /** Called from the isolated native IdMapper before its normal dense lookup. */
    public static Object byId(Object mapper, int id) {
        Binding current = binding;
        return current != null && mapper == current.mapper() && id >= current.vanillaSize()
            ? current.custom().getUnchecked(id) : null;
    }

    /** Called from the isolated native IdMapper before its normal identity lookup. */
    public static int getId(Object mapper, Object state) {
        Binding current = binding;
        return current != null && mapper == current.mapper() && state instanceof WireState wire ? wire.id : -1;
    }

    private record Binding(Object mapper, int vanillaSize, LoadingCache<Integer, WireState> custom) {
    }

    private static final class WireState extends BlockState {
        private final int id;

        WireState(int id) {
            // The engine runs no native world. Geyser's block model supplies custom
            // behavior; this native state retains the identity used by Mojang's codecs.
            super(Blocks.AIR, new Property<?>[0], new Comparable<?>[0]);
            this.id = id;
            initCache();
        }
    }
}

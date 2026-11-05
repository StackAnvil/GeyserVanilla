/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;

/** Evaluates block restrictions without a client world or global tag updates. */
public final class VanillaAdventurePredicates {
    private static final VarHandle PREDICATES;

    static {
        try {
            PREDICATES = MethodHandles.privateLookupIn(AdventureModePredicate.class, MethodHandles.lookup())
                .findVarHandle(AdventureModePredicate.class, "predicates", List.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private VanillaAdventurePredicates() {
    }

    @SuppressWarnings("unchecked")
    public static List<BlockPredicate> predicates(AdventureModePredicate predicate) {
        return List.copyOf((List<BlockPredicate>) PREDICATES.get(predicate));
    }

    /** Checks state restrictions; block entity NBT and components require a separate world lookup. */
    public static boolean matchesState(BlockPredicate predicate, BlockState state, RegistryAccess registries) {
        if (predicate.blocks().isPresent()) {
            // BlockState.is(HolderSet) uses the block's global holder. Server tags use connection holders.
            var holder = registries.lookupOrThrow(Registries.BLOCK)
                .get(BuiltInRegistries.BLOCK.getKey(state.getBlock())).orElseThrow();
            if (!predicate.blocks().orElseThrow().contains(holder)) {
                return false;
            }
        }
        return predicate.properties().map(properties -> properties.matches(state)).orElse(true);
    }
}

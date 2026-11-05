/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.tags.TagNetworkSerialization;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class VanillaAdventurePredicatesTest {
    @Test
    void matchesConnectionHoldersAndTagsInsteadOfGlobalBlockHolders() {
        var context = new VanillaRegistryContext();
        VanillaTestRegistries.packets().forEach(context::accept);
        context.accept(VanillaTestRegistries.tags());
        var registries = context.finishConfiguration();
        var blocks = registries.lookupOrThrow(Registries.BLOCK);
        var stone = blocks.get(Identifier.withDefaultNamespace("stone")).orElseThrow();
        var direct = new BlockPredicate(Optional.of(HolderSet.direct(stone)), Optional.empty(),
            Optional.empty(), DataComponentMatchers.ANY);
        assertNotSame(Blocks.STONE.builtInRegistryHolder(), stone);
        assertTrue(VanillaAdventurePredicates.matchesState(direct, Blocks.STONE.defaultBlockState(), registries));
        assertFalse(VanillaAdventurePredicates.matchesState(direct, Blocks.DIRT.defaultBlockState(), registries));

        var logs = blocks.getOrThrow(TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace("logs")));
        var tagged = new BlockPredicate(Optional.of(logs), Optional.empty(), Optional.empty(), DataComponentMatchers.ANY);
        assertTrue(VanillaAdventurePredicates.matchesState(tagged, Blocks.OAK_LOG.defaultBlockState(), registries));
        assertFalse(VanillaAdventurePredicates.matchesState(tagged, Blocks.STONE.defaultBlockState(), registries));
    }

    @Test
    void checksPropertiesAndExposesAnImmutablePredicateList() {
        var registries = VanillaBootstrap.defaultRegistries();
        var properties = StatePropertiesPredicate.Builder.properties()
            .hasProperty(BlockStateProperties.AXIS, Direction.Axis.X).build();
        var block = new BlockPredicate(Optional.empty(), properties, Optional.empty(), DataComponentMatchers.ANY);
        var adventure = new AdventureModePredicate(List.of(block));
        var predicates = VanillaAdventurePredicates.predicates(adventure);
        assertEquals(List.of(block), predicates);
        assertThrows(UnsupportedOperationException.class, predicates::clear);
        assertTrue(VanillaAdventurePredicates.matchesState(block,
            Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X), registries));
        assertFalse(VanillaAdventurePredicates.matchesState(block,
            Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y), registries));
        assertFalse(VanillaAdventurePredicates.matchesState(block, Blocks.STONE.defaultBlockState(), registries));
    }

    @Test
    void keepsServerBlockTagRestrictionsIndependent() {
        var first = new VanillaRegistryContext();
        var second = new VanillaRegistryContext();
        VanillaTestRegistries.packets().forEach(packet -> {
            first.accept(packet);
            second.accept(packet);
        });
        var key = TagKey.create(Registries.BLOCK, Identifier.parse("geyser:test_restriction"));
        var defaults = VanillaBootstrap.defaultRegistries();
        int stoneId = defaults.lookupOrThrow(Registries.BLOCK).getId(Blocks.STONE);
        int dirtId = defaults.lookupOrThrow(Registries.BLOCK).getId(Blocks.DIRT);
        first.accept(withBlockTag(key.location(), stoneId));
        second.accept(withBlockTag(key.location(), dirtId));
        var firstRegistries = first.finishConfiguration();
        var secondRegistries = second.finishConfiguration();
        var firstPredicate = new BlockPredicate(Optional.of(firstRegistries.lookupOrThrow(Registries.BLOCK).getOrThrow(key)),
            Optional.empty(), Optional.empty(), DataComponentMatchers.ANY);
        var secondPredicate = new BlockPredicate(Optional.of(secondRegistries.lookupOrThrow(Registries.BLOCK).getOrThrow(key)),
            Optional.empty(), Optional.empty(), DataComponentMatchers.ANY);
        assertTrue(VanillaAdventurePredicates.matchesState(firstPredicate, Blocks.STONE.defaultBlockState(), firstRegistries));
        assertFalse(VanillaAdventurePredicates.matchesState(secondPredicate, Blocks.STONE.defaultBlockState(), secondRegistries));
        assertTrue(VanillaAdventurePredicates.matchesState(secondPredicate, Blocks.DIRT.defaultBlockState(), secondRegistries));
        assertFalse(VanillaAdventurePredicates.matchesState(firstPredicate, Blocks.DIRT.defaultBlockState(), firstRegistries));
        assertTrue(defaults.lookupOrThrow(Registries.BLOCK).get(key).isEmpty());
    }

    private static ClientboundUpdateTagsPacket withBlockTag(Identifier key, int id) {
        var tags = new HashMap<>(VanillaTestRegistries.tags().tags());
        var blockTags = new HashMap<>(tags.get(Registries.BLOCK).tags());
        blockTags.put(key, IntList.of(id));
        tags.put(Registries.BLOCK, new TagNetworkSerialization.NetworkPayload(blockTags));
        return new ClientboundUpdateTagsPacket(tags);
    }
}

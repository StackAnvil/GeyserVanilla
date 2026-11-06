/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagNetworkSerialization.NetworkPayload;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VanillaCustomItemsTest {
    private static final Identifier IDENTIFIER = Identifier.parse("geyser_test:sparse_item");
    private static int id;

    @BeforeAll
    static void register() {
        VanillaBootstrap.defaultRegistries();
        id = BuiltInRegistries.ITEM.size() + 400;
        VanillaCustomItems.register(IDENTIFIER, id);
    }

    @Test
    void preservesSparseIdsWithoutChangingBuiltInRegistries() {
        var access = new VanillaRegistryContext().finishConfiguration();
        var registry = access.lookupOrThrow(Registries.ITEM);
        var holder = registry.get(id).orElseThrow();
        assertEquals(IDENTIFIER, holder.key().identifier());
        assertEquals(id, registry.getId(holder.value()));
        assertSame(holder, registry.asHolderIdMap().byId(id));
        assertEquals(id, registry.asHolderIdMap().getId(holder));
        assertTrue(registry.get(id - 1).isEmpty());
        assertNull(registry.byId(id - 1));
        assertEquals(registry.size(), registry.listElements().count());
        registry.forEach(item -> assertNotNull(item));
        assertFalse(BuiltInRegistries.ITEM.containsKey(IDENTIFIER));
        assertTrue(BuiltInRegistries.ITEM.getResourceKey(holder.value()).isEmpty());
        assertEquals(IDENTIFIER, holder.value().builtInRegistryHolder().key().identifier());
        assertThrows(IllegalStateException.class, () -> new Item(new Item.Properties().setId(
            ResourceKey.create(Registries.ITEM, Identifier.parse("geyser_test:unregistered")))));
        // This also verifies that the custom constructor did not leave a global initializer
        // referring to a key absent from the built-in registry.
        assertDoesNotThrow(() -> BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
            .build(VanillaBootstrap.defaultRegistries()));
        assertThrows(IllegalArgumentException.class, () -> VanillaItems.stack(access, id - 1, 1, null));
    }

    @Test
    void decodesNativeItemsWithEachConnectionsDefaultsAndTags() {
        var first = new VanillaRegistryContext();
        var second = new VanillaRegistryContext();
        var tag = TagKey.create(Registries.ITEM, Identifier.parse("geyser_test:custom"));
        first.accept(new ClientboundUpdateTagsPacket(Map.of(Registries.ITEM,
            new NetworkPayload(Map.of(tag.location(), IntArrayList.of(id))))));
        var firstAccess = first.finishConfiguration();
        var secondAccess = second.finishConfiguration();
        var firstHolder = firstAccess.lookupOrThrow(Registries.ITEM).get(id).orElseThrow();
        var secondHolder = secondAccess.lookupOrThrow(Registries.ITEM).get(id).orElseThrow();
        firstHolder.bindComponents(DataComponentMap.builder().addAll(firstHolder.components())
            .set(DataComponents.MAX_STACK_SIZE, 16).build());
        secondHolder.bindComponents(DataComponentMap.builder().addAll(secondHolder.components())
            .set(DataComponents.MAX_STACK_SIZE, 32).build());
        var edits = new GeyserDataComponents();
        edits.put(DataComponents.CUSTOM_NAME, Component.literal("custom stack"));
        ItemStack stack = VanillaItems.stack(firstAccess, id, 3, edits);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), firstAccess);
        try {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
            var other = new RegistryFriendlyByteBuf(buffer.retainedDuplicate(), secondAccess);
            try {
                ItemStack decoded = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
                ItemStack decodedOther = ItemStack.OPTIONAL_STREAM_CODEC.decode(other);
                assertEquals(id, VanillaItems.id(decoded));
                assertEquals(id, VanillaItems.id(decodedOther));
                assertEquals(3, decoded.getCount());
                assertEquals("custom stack", decoded.get(DataComponents.CUSTOM_NAME).getString());
                assertEquals(16, decoded.getMaxStackSize());
                assertEquals(32, decodedOther.getMaxStackSize());
                assertTrue(decoded.is(tag));
                assertFalse(decodedOther.is(tag));
                assertNotSame(decoded.typeHolder(), decodedOther.typeHolder());
                assertEquals(0, buffer.readableBytes());
            } finally {
                other.release();
            }
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsConflictingDefinitions() {
        assertThrows(IllegalArgumentException.class, () -> VanillaCustomItems.register(IDENTIFIER, id + 1));
        assertThrows(IllegalArgumentException.class, () -> VanillaCustomItems.register(
            Identifier.parse("geyser_test:duplicate_id"), id));
        assertThrows(IllegalArgumentException.class, () -> VanillaCustomItems.register(
            Identifier.parse("geyser_test:vanilla_id"), 1));
        assertThrows(IllegalArgumentException.class, () -> VanillaCustomItems.register(
            Identifier.withDefaultNamespace("stone"), id + 1));
    }
}

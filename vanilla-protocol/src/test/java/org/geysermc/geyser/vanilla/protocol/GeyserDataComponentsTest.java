/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.HashedStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeyserDataComponentsTest {
    @BeforeAll
    static void bootstrap() {
        VanillaBootstrap.defaultRegistries();
    }

    @Test
    void distinguishesRemovedComponentsFromUnchangedDefaults() {
        var prototype = net.minecraft.core.component.DataComponentMap.builder()
            .set(DataComponents.MAX_STACK_SIZE, 64).build();
        var edits = new GeyserDataComponents();
        assertFalse(edits.contains(DataComponents.MAX_STACK_SIZE));
        assertEquals(64, edits.toVanilla().get(prototype, DataComponents.MAX_STACK_SIZE));

        edits.put(DataComponents.MAX_STACK_SIZE, 16);
        assertEquals(16, edits.toVanilla().get(prototype, DataComponents.MAX_STACK_SIZE));
        edits.put(DataComponents.MAX_STACK_SIZE, null);
        assertTrue(edits.contains(DataComponents.MAX_STACK_SIZE));
        assertNull(edits.toVanilla().get(prototype, DataComponents.MAX_STACK_SIZE));
        assertTrue(edits.toVanilla().split().removed().contains(DataComponents.MAX_STACK_SIZE));

        edits.remove(DataComponents.MAX_STACK_SIZE);
        assertFalse(edits.contains(DataComponents.MAX_STACK_SIZE));
        assertEquals(64, edits.toVanilla().get(prototype, DataComponents.MAX_STACK_SIZE));
    }

    @Test
    void preservesEditsThroughNativeWireCodec() {
        var edits = new GeyserDataComponents();
        edits.put(DataComponents.CUSTOM_NAME, Component.literal("Vanilla inventory"));
        edits.put(DataComponents.DAMAGE, 150);
        edits.put(DataComponents.ENCHANTMENTS, null);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), VanillaBootstrap.defaultRegistries());
        try {
            DataComponentPatch.STREAM_CODEC.encode(buffer, edits.toVanilla());
            var decoded = GeyserDataComponents.from(DataComponentPatch.STREAM_CODEC.decode(buffer));
            assertEquals(edits, decoded);
            assertEquals(edits.hashCode(), decoded.hashCode());
            assertEquals("Vanilla inventory", decoded.get(DataComponents.CUSTOM_NAME).getString());
            assertTrue(decoded.contains(DataComponents.ENCHANTMENTS));
            assertNull(decoded.get(DataComponents.ENCHANTMENTS));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void copiesAndMergedSnapshotsDoNotShareMutableEdits() {
        var source = new GeyserDataComponents();
        source.put(DataComponents.DAMAGE, 300);
        var copy = source.copy();
        var snapshot = source.immutableCopy();
        source.put(DataComponents.DAMAGE, 400);
        assertEquals(300, copy.get(DataComponents.DAMAGE));
        assertEquals(copy, snapshot);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.put(DataComponents.DAMAGE, 10));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.types().clear());

        copy.putAll(source);
        assertEquals(400, copy.get(DataComponents.DAMAGE));
        source.remove(DataComponents.DAMAGE);
        assertEquals(400, copy.get(DataComponents.DAMAGE));
    }

    @Test
    void inventoryHashesDetectChangedValuesAndRemovals() {
        var registries = VanillaBootstrap.defaultRegistries();
        var edits = new GeyserDataComponents();
        edits.put(DataComponents.CUSTOM_NAME, Component.literal("Original"));
        edits.put(DataComponents.ENCHANTMENTS, null);
        var holder = registries.lookupOrThrow(Registries.ITEM)
            .get(Identifier.withDefaultNamespace("diamond_sword")).orElseThrow();
        ItemStack stack = new ItemStack(holder, 1, edits.toVanilla());
        var hash = VanillaItemHashing.hashStack(registries, stack);
        var hasher = VanillaItemHashing.hasher(registries);
        assertTrue(hash.matches(stack.copy(), hasher));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Changed"));
        assertFalse(hash.matches(stack, hasher));

        stack = new ItemStack(holder, 1, DataComponentPatch.builder()
            .set(DataComponents.CUSTOM_NAME, Component.literal("Original")).build());
        assertFalse(hash.matches(stack, hasher));
        assertSame(HashedStack.EMPTY, VanillaItemHashing.hashStack(registries, null));
        assertSame(HashedStack.EMPTY, VanillaItemHashing.hashStack(registries, ItemStack.EMPTY));
    }

    @Test
    void createsStacksWithTheRequestedRegistryHolder() {
        var context = new VanillaRegistryContext();
        VanillaTestRegistries.packets().forEach(context::accept);
        context.accept(VanillaTestRegistries.tags());
        var registries = context.finishConfiguration();
        var holder = registries.lookupOrThrow(Registries.ITEM)
            .get(Identifier.withDefaultNamespace("diamond_sword")).orElseThrow();
        int id = registries.lookupOrThrow(Registries.ITEM).getId(holder.value());
        var edits = new GeyserDataComponents();
        edits.put(DataComponents.DAMAGE, 50);
        ItemStack stack = VanillaItems.stack(registries, id, 2, edits);

        assertSame(holder, stack.typeHolder());
        assertEquals(id, VanillaItems.id(stack));
        assertEquals(2, stack.getCount());
        assertEquals(50, stack.get(DataComponents.DAMAGE));
        edits.put(DataComponents.DAMAGE, 75);
        assertEquals(50, stack.get(DataComponents.DAMAGE));
        assertSame(ItemStack.EMPTY, VanillaItems.stack(registries, -1, 0, null));
        assertThrows(IllegalArgumentException.class, () -> VanillaItems.stack(registries, Integer.MAX_VALUE, 1, null));
    }
}

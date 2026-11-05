/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.HashedPatchMap;
import net.minecraft.network.HashedStack;
import net.minecraft.util.HashOps;
import net.minecraft.world.item.ItemStack;

/** Inventory verification through the vanilla component codecs and hash operations. */
public final class VanillaItemHashing {
    private VanillaItemHashing() {
    }

    public static HashedPatchMap.HashGenerator hasher(RegistryAccess registries) {
        var operations = registries.createSerializationContext(HashOps.CRC32C_INSTANCE);
        return component -> component.encodeValue(operations).getOrThrow(message ->
            new IllegalArgumentException("Cannot hash component " + component.type() + ": " + message)).asInt();
    }

    public static HashedStack hashStack(RegistryAccess registries, ItemStack stack) {
        return stack == null ? HashedStack.EMPTY : HashedStack.create(stack, hasher(registries));
    }
}

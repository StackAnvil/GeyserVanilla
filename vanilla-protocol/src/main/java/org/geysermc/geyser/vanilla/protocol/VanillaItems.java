/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;

public final class VanillaItems {
    private VanillaItems() {
    }

    public static int id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getId(stack.getItem());
    }

    public static ItemStack stack(RegistryAccess registries, int id, int count, GeyserDataComponents components) {
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        var holder = registries.lookupOrThrow(Registries.ITEM).get(id)
            .orElseThrow(() -> new IllegalArgumentException("Unknown Java item ID " + id));
        return new ItemStack(holder, count, components == null ? DataComponentPatch.EMPTY : components.toVanilla());
    }
}

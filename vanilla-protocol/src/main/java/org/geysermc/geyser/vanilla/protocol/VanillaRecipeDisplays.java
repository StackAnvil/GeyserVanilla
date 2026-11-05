/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.util.stream.Stream;

/** Resolves recipe displays with the connection's registries instead of a client level. */
public final class VanillaRecipeDisplays {
    private VanillaRecipeDisplays() {
    }

    private static ContextMap context(RegistryAccess registries) {
        return ContextMap.builder().set(SlotDisplayContext.REGISTRIES, registries)
            .buildAndValidate(SlotDisplayContext.CONTEXT);
    }

    public static ItemStack firstStack(RegistryAccess registries, SlotDisplay display) {
        return display.resolveForFirstStack(context(registries));
    }

    public static Stream<ItemStack> stacks(RegistryAccess registries, SlotDisplay display) {
        return display.resolve(context(registries), SlotDisplay.ItemStackContentsFactory.INSTANCE);
    }

    public static boolean accepts(RegistryAccess registries, SlotDisplay display, ItemStack stack) {
        return accepts(registries, display, stack, false);
    }

    private static boolean accepts(RegistryAccess registries, SlotDisplay display, ItemStack stack, boolean anyPotion) {
        return switch (display) {
            case SlotDisplay.Empty ignored -> stack.isEmpty();
            case SlotDisplay.Composite composite -> composite.contents().stream()
                .anyMatch(child -> accepts(registries, child, stack, anyPotion));
            case SlotDisplay.WithRemainder remainder -> accepts(registries, remainder.input(), stack, anyPotion);
            case SlotDisplay.ItemSlotDisplay item -> !stack.isEmpty() && stack.getItem() == item.item().value();
            case SlotDisplay.TagSlotDisplay tag -> !stack.isEmpty() && tag.tag().contains(stack.typeHolder());
            case SlotDisplay.ItemStackSlotDisplay item -> matches(stack, item.stack().create(), anyPotion);
            case SlotDisplay.OnlyWithComponent filtered -> stack.get(filtered.component()) != null
                && accepts(registries, filtered.source(), stack, anyPotion);
            case SlotDisplay.DyedSlotDemo dyed -> accepts(registries, dyed.target(), stack, anyPotion);
            case SlotDisplay.WithAnyPotion potion -> accepts(registries, potion.display(), stack, true);
            case SlotDisplay.AnyFuel ignored -> !stack.isEmpty() && stack.get(DataComponents.COOKING_FUEL) != null;
            default -> stacks(registries, display).anyMatch(expected -> matches(stack, expected, anyPotion));
        };
    }

    private static boolean matches(ItemStack stack, ItemStack expected, boolean anyPotion) {
        if (!ItemStack.isSameItem(stack, expected) || stack.getCount() < expected.getCount()) {
            return false;
        }
        if (anyPotion) {
            stack = stack.copy();
            expected = expected.copy();
            stack.remove(DataComponents.POTION_CONTENTS);
            expected.remove(DataComponents.POTION_CONTENTS);
        }
        return ItemStack.isSameItemSameComponents(stack, expected);
    }
}

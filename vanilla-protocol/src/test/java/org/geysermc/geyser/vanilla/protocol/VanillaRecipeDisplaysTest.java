/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VanillaRecipeDisplaysTest {
    private static RegistryAccess registries;

    @BeforeAll
    static void bootstrap() {
        var context = new VanillaRegistryContext();
        VanillaTestRegistries.packets().forEach(context::accept);
        context.accept(VanillaTestRegistries.tags());
        registries = context.finishConfiguration();
    }

    @Test
    void resolvesTagsAndFiltersWithConnectionDefaults() {
        var planks = registries.lookupOrThrow(Registries.ITEM).getOrThrow(
            TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("planks")));
        var tag = new SlotDisplay.TagSlotDisplay(planks);
        var resolved = VanillaRecipeDisplays.stacks(registries, tag).toList();
        assertEquals(planks.size(), resolved.size());
        assertTrue(resolved.stream().allMatch(stack -> planks.contains(stack.typeHolder())));
        assertTrue(VanillaRecipeDisplays.accepts(registries, tag, new ItemStack(item("oak_planks"))));
        assertFalse(VanillaRecipeDisplays.accepts(registries, tag, new ItemStack(item("stone"))));

        var filtered = new SlotDisplay.OnlyWithComponent(new SlotDisplay.Composite(List.of(
            new SlotDisplay.ItemSlotDisplay(item("stone")), new SlotDisplay.ItemSlotDisplay(item("diamond_sword")))),
            DataComponents.MAX_DAMAGE);
        ItemStack sword = VanillaRecipeDisplays.firstStack(registries, filtered);
        assertSame(item("diamond_sword"), sword.typeHolder());
        assertTrue(VanillaRecipeDisplays.accepts(registries, filtered, sword));
        sword.remove(DataComponents.MAX_DAMAGE);
        assertFalse(VanillaRecipeDisplays.accepts(registries, filtered, sword));
    }

    @Test
    void matchesTemplateComponentsAndMinimumCountWithoutRequiringRemainders() {
        var patch = DataComponentPatch.builder().set(DataComponents.CUSTOM_NAME, Component.literal("Required name")).build();
        var template = new ItemStackTemplate(item("stone"), 2, patch);
        var display = new SlotDisplay.WithRemainder(new SlotDisplay.ItemStackSlotDisplay(template),
            new SlotDisplay.ItemSlotDisplay(item("bucket")));
        ItemStack stack = template.create();
        assertTrue(VanillaRecipeDisplays.accepts(registries, display, stack));
        stack.setCount(3);
        assertTrue(VanillaRecipeDisplays.accepts(registries, display, stack));
        stack.setCount(1);
        assertFalse(VanillaRecipeDisplays.accepts(registries, display, stack));
        stack.setCount(2);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Different name"));
        assertFalse(VanillaRecipeDisplays.accepts(registries, display, stack));
    }

    @Test
    void expandsAndMatchesAnyPotionWithoutChangingTheStackOrTemplate() {
        var water = registries.lookupOrThrow(Registries.POTION).get(Identifier.withDefaultNamespace("water")).orElseThrow();
        var healing = registries.lookupOrThrow(Registries.POTION).get(Identifier.withDefaultNamespace("healing")).orElseThrow();
        var patch = DataComponentPatch.builder().set(DataComponents.POTION_CONTENTS, new PotionContents(water)).build();
        var template = new ItemStackTemplate(item("potion"), 1, patch);
        var source = new SlotDisplay.ItemStackSlotDisplay(template);
        var display = new SlotDisplay.WithAnyPotion(source);
        var variants = VanillaRecipeDisplays.stacks(registries, display).toList();
        assertEquals(registries.lookupOrThrow(Registries.POTION).size(), variants.size());
        assertTrue(variants.stream().allMatch(stack -> stack.get(DataComponents.POTION_CONTENTS).potion().orElseThrow()
            .canSerializeIn(registries.lookupOrThrow(Registries.POTION))));
        var stack = template.create();
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(healing));
        assertFalse(VanillaRecipeDisplays.accepts(registries, source, stack));
        assertTrue(VanillaRecipeDisplays.accepts(registries, display, stack));
        assertEquals(new PotionContents(healing), stack.get(DataComponents.POTION_CONTENTS));
        assertEquals(patch, template.components());
    }

    @Test
    void resolvesDyeDemosAndFuelItemsUsingVanillaComponents() {
        var target = new SlotDisplay.ItemSlotDisplay(item("leather_helmet"));
        var dyed = new SlotDisplay.DyedSlotDemo(new SlotDisplay.ItemSlotDisplay(item("red_dye")), target);
        var result = VanillaRecipeDisplays.firstStack(registries, dyed);
        assertSame(item("leather_helmet"), result.typeHolder());
        assertNotNull(result.get(DataComponents.DYED_COLOR));
        assertTrue(VanillaRecipeDisplays.accepts(registries, dyed, result));
        assertNull(VanillaRecipeDisplays.firstStack(registries, target).get(DataComponents.DYED_COLOR));

        var coal = new ItemStack(item("coal"));
        assertTrue(VanillaRecipeDisplays.accepts(registries, SlotDisplay.AnyFuel.INSTANCE, coal));
        assertFalse(VanillaRecipeDisplays.accepts(registries, SlotDisplay.AnyFuel.INSTANCE, new ItemStack(item("stone"))));
        coal.remove(DataComponents.COOKING_FUEL);
        assertFalse(VanillaRecipeDisplays.accepts(registries, SlotDisplay.AnyFuel.INSTANCE, coal));
        var fuels = VanillaRecipeDisplays.stacks(registries, SlotDisplay.AnyFuel.INSTANCE).toList();
        assertFalse(fuels.isEmpty());
        assertTrue(fuels.stream().allMatch(stack -> stack.get(DataComponents.COOKING_FUEL) != null));
    }

    @Test
    void decodesNativeDisplaysAndHandlesEmptyAlternatives() {
        var display = new SlotDisplay.Composite(List.of(SlotDisplay.Empty.INSTANCE,
            new SlotDisplay.ItemSlotDisplay(item("stone"))));
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            SlotDisplay.STREAM_CODEC.encode(buffer, display);
            var decoded = SlotDisplay.STREAM_CODEC.decode(buffer);
            assertSame(item("stone"), VanillaRecipeDisplays.firstStack(registries, decoded).typeHolder());
            assertTrue(VanillaRecipeDisplays.accepts(registries, decoded, ItemStack.EMPTY));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
        assertTrue(VanillaRecipeDisplays.firstStack(registries, SlotDisplay.Empty.INSTANCE).isEmpty());
        assertTrue(VanillaRecipeDisplays.firstStack(registries, new SlotDisplay.Composite(List.of())).isEmpty());
        assertFalse(VanillaRecipeDisplays.accepts(registries, new SlotDisplay.Composite(List.of()), ItemStack.EMPTY));
    }

    private static Holder<Item> item(String name) {
        return registries.lookupOrThrow(Registries.ITEM).get(Identifier.withDefaultNamespace(name)).orElseThrow();
    }
}

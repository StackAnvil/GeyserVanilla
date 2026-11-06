/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.inventory.recipe;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntObjectMutablePair;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.MultiRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.DefaultDescriptor;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemTagDescriptor;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.item.Potion;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.BedrockRequiresTagItem;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.java.BuiltInJavaRegistries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.geyser.session.cache.tags.GeyserHolderSet;
import org.geysermc.geyser.translator.item.ItemTranslator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplay.Composite;
import net.minecraft.world.item.crafting.display.SlotDisplay.DyedSlotDemo;
import net.minecraft.world.item.crafting.display.SlotDisplay.Empty;
import net.minecraft.world.item.crafting.display.SlotDisplay.ItemSlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay.OnlyWithComponent;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay.TagSlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay.WithRemainder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.geysermc.geyser.util.InventoryUtils.LAST_RECIPE_NET_ID;

public class RecipeUtil {

    /**
     * Required to use the specified cartography table recipes
     */
    public static final List<MultiRecipeData> CARTOGRAPHY_RECIPES = List.of(
            MultiRecipeData.of(UUID.fromString("8b36268c-1829-483c-a0f1-993b7156a8f2"), ++LAST_RECIPE_NET_ID), // Map extending
            MultiRecipeData.of(UUID.fromString("442d85ed-8272-4543-a6f1-418f90ded05d"), ++LAST_RECIPE_NET_ID), // Map cloning
            MultiRecipeData.of(UUID.fromString("98c84b38-1085-46bd-b1ce-dd38c159e6cc"), ++LAST_RECIPE_NET_ID), // Map upgrading
            MultiRecipeData.of(UUID.fromString("602234e4-cac1-4353-8bb7-b1ebff70024b"), ++LAST_RECIPE_NET_ID) // Map locking
    );

    public static List<ItemDescriptorWithCount> translateToInput(GeyserSession session, SlotDisplay slotDisplay) {
        if (slotDisplay instanceof Empty) {
            return List.of(ItemDescriptorWithCount.EMPTY);
        }
        if (slotDisplay instanceof WithRemainder remainder) {
            return translateToInput(session, remainder.input());
        }
        if (slotDisplay instanceof OnlyWithComponent filtered) {
            // Bedrock descriptors cannot require a Java data component.
            return translateToInput(session, filtered.source());
        }
        if (slotDisplay instanceof DyedSlotDemo dyed) {
            return translateToInput(session, dyed.target());
        }
        if (slotDisplay instanceof ItemSlotDisplay item) {
            return List.of(fromItem(session, net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(item.item().value())));
        }
        if (slotDisplay instanceof Composite composite) {
            if (composite.contents().size() == 1) {
                return translateToInput(session, composite.contents().getFirst());
            }
            int[] ids = composite.contents().stream().mapToInt(RecipeUtil::plainItemId).sorted().toArray();
            if (ids.length > 0 && ids[0] >= 0) {
                var descriptor = lookupBedrockTag(session, ids);
                if (descriptor != null) {
                    return descriptor;
                }
            }
            return fallbackCompositeMapping(session, composite);
        }
        if (slotDisplay instanceof TagSlotDisplay tag) {
            int[] ids = tag.tag().stream().mapToInt(holder ->
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(holder.value())).sorted().toArray();
            if (ids.length == 0) {
                return List.of(ItemDescriptorWithCount.EMPTY);
            }
            var descriptor = lookupBedrockTag(session, ids);
            return descriptor != null ? descriptor : Arrays.stream(ids).mapToObj(id -> fromItem(session, id)).distinct().toList();
        }
        return org.geysermc.geyser.vanilla.protocol.VanillaRecipeDisplays.stacks(session.vanillaRegistries(), slotDisplay)
            .map(stack -> ItemDescriptorWithCount.fromItem(ItemTranslator.translateToBedrock(session, stack)))
            .distinct().toList();
    }

    private static int plainItemId(SlotDisplay display) {
        if (display instanceof ItemSlotDisplay item) {
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(item.item().value());
        }
        if (display instanceof ItemStackSlotDisplay item && item.stack().count() == 1 && item.stack().components().isEmpty()) {
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(item.stack().item().value());
        }
        return -1;
    }

    public static @Nullable Pair<Item, ItemData> translateToOutput(GeyserSession session, SlotDisplay slotDisplay) {
        ItemStack stack = org.geysermc.geyser.vanilla.protocol.VanillaRecipeDisplays.firstStack(session.vanillaRegistries(), slotDisplay);
        return stack.isEmpty() ? null : Pair.of(BuiltInJavaRegistries.ITEM.getOrThrow(
            org.geysermc.geyser.vanilla.protocol.VanillaItems.id(stack)), ItemTranslator.translateToBedrock(session, stack));
    }

    private static ItemDescriptorWithCount fromItem(GeyserSession session, int item) {
        if (item == Items.AIR_ID) {
            return ItemDescriptorWithCount.EMPTY;
        }
        ItemMapping mapping = session.getItemMappings().getMapping(item);
        return new ItemDescriptorWithCount(new DefaultDescriptor(mapping.getBedrockDefinition(), mapping.getBedrockData()), 1); // Need to check count
    }

    /**
     * Checks to see if this list of items matches with one of this Bedrock version's tags.
     */
    @Nullable
    private static List<ItemDescriptorWithCount> lookupBedrockTag(GeyserSession session, int[] items) {
        var bedrockTags = Registries.TAGS.forVersion(session.getUpstream().getProtocolVersion());
        String bedrockTag = bedrockTags.get(items);
        if (bedrockTag != null) {
            return Collections.singletonList(
                    new ItemDescriptorWithCount(new ItemTagDescriptor(bedrockTag), 1)
            );
        } else {
            return null;
        }
    }

    /**
     * Converts Composite contents to a list of basic ItemDescriptorWithCounts.
     */
    private static List<ItemDescriptorWithCount> fallbackCompositeMapping(GeyserSession session, Composite composite) {
        return composite.contents().stream()
                .map(subDisplay -> translateToInput(session, subDisplay))
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .toList();
    }

    public static Pair<List<List<ItemDescriptorWithCount>>, ItemData> combinations(GeyserSession session, SlotDisplay result, List<SlotDisplay> ingredients) {
        Pair<Item, ItemData> pair = translateToOutput(session, result);
        if (pair == null || !pair.right().isValid()) {
            // Likely modded item Bedrock will complain about
            // Implementation note: ItemData#isValid() may return true for air because count might be > 0 and the air definition may not be ItemDefinition.AIR
            return null;
        }

        ItemData output = pair.right();
        if (!(pair.left() instanceof BedrockRequiresTagItem)) {
            // Strip NBT - tools won't appear in the recipe book otherwise
            output = output.toBuilder().tag(null).build();
        }

        boolean empty = true;
        boolean complexInputs = false;
        List<List<ItemDescriptorWithCount>> inputs = new ArrayList<>(ingredients.size());
        for (SlotDisplay input : ingredients) {
            List<ItemDescriptorWithCount> translated = translateToInput(session, input);
            if (translated == null) {
                continue;
            }
            inputs.add(translated);
            if (translated.size() != 1 || translated.getFirst() != ItemDescriptorWithCount.EMPTY) {
                empty = false;
            }
            complexInputs |= translated.size() > 1;
        }
        if (empty) {
            // Crashes Bedrock 1.19.70 otherwise
            // Fixes https://github.com/GeyserMC/Geyser/issues/3549
            return null;
        }

        if (complexInputs) {
            long size = 1;
            // See how big a cartesian product will get without creating one (Guava throws an error; not really ideal)
            for (List<ItemDescriptorWithCount> list : inputs) {
                size *= list.size();
                if (size > 500) {
                    // Too much. No.
                    complexInputs = false;
                    break;
                }
            }
            if (complexInputs) {
                return Pair.of(Lists.cartesianProduct(inputs), output);
            }
        }

        int totalSimpleRecipes = inputs.stream().mapToInt(List::size).max().orElse(1);

        // Sort inputs to create "uniform" simple recipes, if possible
        inputs = inputs.stream()
                .map(descriptors -> descriptors.stream()
                        .sorted(ItemDescriptorWithCountComparator.INSTANCE)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

        List<List<ItemDescriptorWithCount>> finalRecipes = new ArrayList<>(totalSimpleRecipes);
        for (int i = 0; i < totalSimpleRecipes; i++) {
            int current = i;
            finalRecipes.add(inputs.stream().map(descriptors -> {
                if (descriptors.size() > current) {
                    return descriptors.get(current);
                }
                return descriptors.getFirst();
            }).toList());
        }

        return Pair.of(finalRecipes, output);
    }

    private static class ItemDescriptorWithCountComparator implements Comparator<ItemDescriptorWithCount> {

        static ItemDescriptorWithCountComparator INSTANCE = new ItemDescriptorWithCountComparator();

        @Override
        public int compare(ItemDescriptorWithCount o1, ItemDescriptorWithCount o2) {
            String tag1 = null, tag2 = null;

            // Collect item tags first
            if (o1.getDescriptor() instanceof ItemTagDescriptor itemTagDescriptor) {
                tag1 = itemTagDescriptor.getItemTag();
            }

            if (o2.getDescriptor() instanceof ItemTagDescriptor itemTagDescriptor) {
                tag2 = itemTagDescriptor.getItemTag();
            }

            if (tag1 != null || tag2 != null) {
                if (tag1 != null && tag2 != null) {
                    return tag1.compareTo(tag2); // Just sort based on their string id
                }

                if (tag1 != null) {
                    return -1;
                }

                return 1; // the second is an item tag; which should be r
            }

            if (o1.getDescriptor() instanceof DefaultDescriptor defaultDescriptor1 && o2.getDescriptor() instanceof DefaultDescriptor defaultDescriptor2) {
                return IntComparators.NATURAL_COMPARATOR.compare(defaultDescriptor1.getItemId().getRuntimeId(), defaultDescriptor2.getItemId().getRuntimeId());
            }

            throw new IllegalStateException("Unable to compare unknown item descriptors: " + o1 + " and " + o2);
        }
    }
}

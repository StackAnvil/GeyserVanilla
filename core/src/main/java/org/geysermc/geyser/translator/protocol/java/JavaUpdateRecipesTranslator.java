/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.protocol.java;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.PotionMixData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.RecipeUnlockingRequirement;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.ShapelessRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.SmithingTransformRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.DefaultDescriptor;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.TrimDataPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.recipe.GeyserRecipe;
import org.geysermc.geyser.inventory.recipe.GeyserShapedRecipe;
import org.geysermc.geyser.inventory.recipe.GeyserShapelessRecipe;
import org.geysermc.geyser.inventory.recipe.GeyserStonecutterData;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.GeyserHolderSet;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.MinecraftKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.world.item.crafting.SelectableRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.geysermc.geyser.inventory.recipe.RecipeUtil.CARTOGRAPHY_RECIPES;

/**
 * Used to send all valid recipes from Java to Bedrock.
 * <p>
 * Bedrock REQUIRES a CraftingDataPacket to be sent in order to craft anything.
 */
@Translator(packet = ClientboundUpdateRecipesPacket.class)
public class JavaUpdateRecipesTranslator extends PacketTranslator<ClientboundUpdateRecipesPacket> {

    private static final List<String> NETHERITE_UPGRADES = List.of(
            "minecraft:netherite_sword",
            "minecraft:netherite_shovel",
            "minecraft:netherite_pickaxe",
            "minecraft:netherite_axe",
            "minecraft:netherite_hoe",
            "minecraft:netherite_helmet",
            "minecraft:netherite_chestplate",
            "minecraft:netherite_leggings",
            "minecraft:netherite_boots"
    );



    @Override
    public void translate(GeyserSession session, ClientboundUpdateRecipesPacket packet) {
        int netId = session.getLastRecipeNetId().get();
        CraftingDataPacket craftingDataPacket = new CraftingDataPacket();
        // See JavaFinishConfigurationTranslator - avoid re-sending base recipe data in quick succession
        if (session.isCleanRecipesRequired()) {
            craftingDataPacket.setCleanRecipes(true);
            craftingDataPacket.getMultiData().addAll(CARTOGRAPHY_RECIPES);

            for (GeyserRecipe recipe : session.getCraftingRecipes().values()) {
                if (recipe instanceof GeyserShapedRecipe shapedRecipe) {
                    craftingDataPacket.getShapedData().addAll(shapedRecipe.asRecipeData(session));
                } else if (recipe instanceof GeyserShapelessRecipe shapelessRecipe) {
                    craftingDataPacket.getShapelessData().addAll(shapelessRecipe.asRecipeData(session));
                }
            }
            session.getTrimRecipes().addAllSmithingRecipes(session, craftingDataPacket);
        }

        // As we now populate recipes that can differ,
        // we need to ensure the next crafting packet resets the client's known recipes
        session.setCleanRecipesRequired(true);

        boolean oldSmithingTable;
        var smithingBase = packet.itemSets().get(net.minecraft.world.item.crafting.RecipePropertySet.SMITHING_BASE);
        var smithingTemplate = packet.itemSets().get(net.minecraft.world.item.crafting.RecipePropertySet.SMITHING_TEMPLATE);
        var smithingAddition = packet.itemSets().get(net.minecraft.world.item.crafting.RecipePropertySet.SMITHING_ADDITION);
        if (smithingBase == null || smithingTemplate == null || smithingAddition == null) {
            // We're probably on a version before the smithing table got expanded functionality.
            oldSmithingTable = true;

            ItemMapping template = session.getItemMappings().getStoredItems().upgradeTemplate();

            for (String identifier : NETHERITE_UPGRADES) {
                SmithingTransformRecipeData recipe = SmithingTransformRecipeData.of(identifier + "_smithing",
                    getDescriptorFromId(session, template.getBedrockIdentifier()),
                    getDescriptorFromId(session, identifier.replace("netherite", "diamond")),
                    getDescriptorFromId(session, "minecraft:netherite_ingot"),
                    ItemData.builder().definition(Objects.requireNonNull(session.getItemMappings().getDefinition(identifier))).count(1).build(),
                    "smithing_table",
                    netId++);

                craftingDataPacket.getSmithingTransformData().add(recipe);
            }
        } else {
            oldSmithingTable = false;
            // BDS sends armor trim templates and materials before the CraftingDataPacket (not sure why - eclipse).
            TrimDataPacket trimDataPacket = new TrimDataPacket();
            // This won't work very well for custom trim patterns and materials
            trimDataPacket.getPatterns().addAll(session.getTrimRecipes().bedrockTrimPatterns());
            trimDataPacket.getMaterials().addAll(session.getTrimRecipes().bedrockTrimMaterials());
            session.sendUpstreamPacket(trimDataPacket);
        }
        session.getGeyser().getLogger().debug("Using old smithing table workaround? " + oldSmithingTable);
        session.setOldSmithingTable(oldSmithingTable);

        addPotionMixes(session, packet.itemSets(), craftingDataPacket.getPotionMixData());

        Int2ObjectMap<List<SelectableRecipe.SingleInputEntry<net.minecraft.world.item.crafting.StonecutterRecipe>>> rawStonecutterData = new Int2ObjectOpenHashMap<>();

        var stonecutterRecipes = packet.stonecutterRecipes().entries();
        for (var recipe : stonecutterRecipes) {
            // Hardcoding the heck out of this until we see different examples of how this works.
            if (!(recipe.recipe().optionDisplay() instanceof ItemStackSlotDisplay)) {
                session.getGeyser().getLogger().warning("Ignoring stonecutter recipe for weird output: " + recipe);
                continue;
            }

            recipe.input().items().forEach(ingredient -> rawStonecutterData.computeIfAbsent(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(ingredient.value()), $ -> new ArrayList<>()).add(recipe));
        }

        Int2ObjectMap<GeyserStonecutterData> stonecutterRecipeMap = new Int2ObjectOpenHashMap<>();
        for (Int2ObjectMap.Entry<List<SelectableRecipe.SingleInputEntry<net.minecraft.world.item.crafting.StonecutterRecipe>>> data : rawStonecutterData.int2ObjectEntrySet()) {
            // Implementation note: data used to have to be sorted according to the item translation key.
            // This is no longer necessary as of 1.21.2, and is instead presented in the order the server sends us.
            // (Recipes are ordered differently between Paper and vanilla)
            // See #5150.
            int buttonId = 0;
            for (var recipe : data.getValue()) {
                // As of 1.16.4, all stonecutter recipes have one ingredient option
                int javaInput = data.getIntKey();
                ItemMapping mapping = session.getItemMappings().getMapping(javaInput);
                if (mapping.getJavaItem() == Items.AIR) {
                    // Modded ?
                    continue;
                }
                ItemDescriptorWithCount descriptor = new ItemDescriptorWithCount(new DefaultDescriptor(mapping.getBedrockDefinition(), mapping.getBedrockData()), 1);
                ItemStack javaOutput = ((ItemStackSlotDisplay) recipe.recipe().optionDisplay()).stack().create();
                ItemData output = ItemTranslator.translateToBedrock(session, javaOutput);
                if (!output.isValid()) {
                    // Probably modded items
                    continue;
                }
                int recipeNetId = netId++;
                UUID uuid = UUID.randomUUID();
                // We need to register stonecutting recipes, so they show up on Bedrock
                // (Implementation note: recipe ID creates the order which stonecutting recipes are shown in stonecutter)
                ShapelessRecipeData stonecutterRecipe = ShapelessRecipeData.shapeless("stonecutter_" + javaInput + "_" + buttonId,
                    Collections.singletonList(descriptor), Collections.singletonList(output), uuid, "stonecutter", 0, recipeNetId, RecipeUnlockingRequirement.INVALID);
                craftingDataPacket.getShapelessData().add(stonecutterRecipe);

                // Save the recipe list for reference when crafting
                // Add the net ID as the key and the button required + output for the value
                stonecutterRecipeMap.put(recipeNetId, new GeyserStonecutterData(buttonId++, javaOutput));

                // Currently, stone cutter recipes are not locked/unlocked on Bedrock; so no need to cache their identifiers.
            }
        }

        session.sendUpstreamPacket(craftingDataPacket);
        session.setStonecutterRecipes(stonecutterRecipeMap);
        session.getLastRecipeNetId().set(netId);
    }

    private ItemDescriptorWithCount getDescriptorFromId(GeyserSession session, String bedrockId) {
        ItemDefinition bedrockDefinition = session.getItemMappings().getDefinition(bedrockId);
        if (bedrockDefinition != null) {
            return ItemDescriptorWithCount.fromItem(ItemData.builder().definition(bedrockDefinition).count(1).build());
        }
        GeyserImpl.getInstance().getLogger().debug("Unable to find item with identifier " + bedrockId);
        return ItemDescriptorWithCount.EMPTY;
    }

    private static void addPotionMixes(GeyserSession session, Map<net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.RecipePropertySet>, net.minecraft.world.item.crafting.RecipePropertySet> itemSets, List<PotionMixData> mixes) {
        var brewingInputs = itemSets.get(net.minecraft.world.item.crafting.RecipePropertySet.BREWING_INPUTS);
        var brewingReagents = itemSets.get(net.minecraft.world.item.crafting.RecipePropertySet.BREWING_REAGENTS);

        if (brewingInputs == null || brewingReagents == null) {
            return;
        }

        ItemMappings itemMappings = session.getItemMappings();
        var items = session.vanillaRegistries().lookupOrThrow(net.minecraft.core.registries.Registries.ITEM);
        var inputs = items.listElements().filter(holder -> brewingInputs.test(new ItemStack(holder))).toList();
        var reagents = items.listElements().filter(holder -> brewingReagents.test(new ItemStack(holder))).toList();

        // Bedrock demands an output, but Java doesn't give us one
        // It doesn't matter much what the output is, since the client doesn't display it anywhere: so a simple potion will do
        ItemMapping brewingOutput = itemMappings.getMapping(Items.POTION);

        for (var input : inputs) {
            for (var reagent : reagents) {
                ItemMapping inputMapping = itemMappings.getMapping(items.getId(input.value()));
                ItemMapping reagentMapping = itemMappings.getMapping(items.getId(reagent.value()));
                mixes.add(new PotionMixData(
                    inputMapping.getBedrockDefinition().getRuntimeId(), inputMapping.getBedrockData(),
                    reagentMapping.getBedrockDefinition().getRuntimeId(), reagentMapping.getBedrockData(),
                    brewingOutput.getBedrockDefinition().getRuntimeId(), brewingOutput.getBedrockData()));
            }
        }
    }
}

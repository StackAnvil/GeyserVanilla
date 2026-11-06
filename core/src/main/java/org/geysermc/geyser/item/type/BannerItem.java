/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.item.type;

import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.key.Key;
import net.minecraft.network.chat.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.inventory.item.BannerPattern;
import org.geysermc.geyser.inventory.item.DyeColor;
import org.geysermc.geyser.item.TooltipOptions;
import net.minecraft.world.item.Rarity;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.registry.java.JavaRegistryProvider;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.geyser.registry.java.JavaRegistryLookup;
import org.geysermc.geyser.translator.item.BedrockItemBuilder;
import org.geysermc.geyser.util.MinecraftKey;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatternLayers.Layer;
import net.minecraft.core.component.DataComponents;
import org.geysermc.geyser.vanilla.protocol.GeyserDataComponents;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class BannerItem extends BlockItem {
    /**
     * Holds what a Java ominous banner pattern looks like.
     * <p>
     * Translating the patterns over to Bedrock does not work effectively, but Bedrock has a dedicated type for
     * ominous banners that we set instead. This variable is used to detect Java ominous banner patterns, and apply
     * the correct ominous banner pattern if Bedrock pulls the item from creative.
     */
    private static final List<Pair<BannerPattern, DyeColor>> OMINOUS_BANNER_PATTERN;

    static {
        // Construct what an ominous banner is supposed to look like
        OMINOUS_BANNER_PATTERN = List.of(
                Pair.of(BannerPattern.RHOMBUS, DyeColor.CYAN),
                Pair.of(BannerPattern.STRIPE_BOTTOM, DyeColor.LIGHT_GRAY),
                Pair.of(BannerPattern.STRIPE_CENTER, DyeColor.GRAY),
                Pair.of(BannerPattern.BORDER, DyeColor.LIGHT_GRAY),
                Pair.of(BannerPattern.STRIPE_MIDDLE, DyeColor.BLACK),
                Pair.of(BannerPattern.HALF_HORIZONTAL, DyeColor.LIGHT_GRAY),
                Pair.of(BannerPattern.CIRCLE, DyeColor.LIGHT_GRAY),
                Pair.of(BannerPattern.BORDER, DyeColor.BLACK)
        );
    }

    public static boolean isOminous(JavaRegistryProvider registries, List<Layer> patternLayers) {
        if (OMINOUS_BANNER_PATTERN.size() != patternLayers.size()) {
            return false;
        }
        for (int i = 0; i < OMINOUS_BANNER_PATTERN.size(); i++) {
            Layer patternLayer = patternLayers.get(i);
            Pair<BannerPattern, DyeColor> pair = OMINOUS_BANNER_PATTERN.get(i);
            if (patternLayer.color().getId() != pair.right().ordinal() ||
                    patternLayer.pattern().unwrapKey().isEmpty()) {
                return false;
            }
            Optional<BannerPattern> bannerPattern = patternLayer.pattern().unwrapKey().flatMap(key -> JavaRegistries.BANNER_PATTERN.get(registries, MinecraftKey.key(key.identifier().toString())));
            if (bannerPattern.orElse(null) != pair.left()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isOminous(List<NbtMap> blockEntityPatterns) {
        // Cannot do a simple NBT equals check here because the IDs may not be full resource locations
        // ViaVersion's fault, 1.20.4 -> 1.20.5, but it works on Java so we need to support it.
        if (OMINOUS_BANNER_PATTERN.size() != blockEntityPatterns.size()) {
            return false;
        }
        for (int i = 0; i < OMINOUS_BANNER_PATTERN.size(); i++) {
            NbtMap patternLayer = blockEntityPatterns.get(i);
            Pair<BannerPattern, DyeColor> pair = OMINOUS_BANNER_PATTERN.get(i);
            DyeColor color = DyeColor.getByJavaIdentifier(patternLayer.getString("color"));
            if (color != pair.right()) {
                return false;
            }
            Key id = MinecraftKey.key(patternLayer.getString("pattern")); // Ouch
            BannerPattern bannerPattern = BannerPattern.getByJavaIdentifier(id);
            if (bannerPattern != pair.left()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Convert a list of patterns from Java nbt to Bedrock nbt
     *
     * @param patterns The patterns to convert
     * @return The new converted patterns
     */
    public static NbtList<NbtMap> convertBannerPattern(List<NbtMap> patterns) {
        List<NbtMap> tagsList = new ArrayList<>();
        for (NbtMap patternTag : patterns) {
            NbtMap bedrockBannerPattern = getBedrockBannerPattern(patternTag);
            if (bedrockBannerPattern != null) {
                tagsList.add(bedrockBannerPattern);
            }
        }

        return new NbtList<>(NbtType.COMPOUND, tagsList);
    }

    /**
     * Converts a Java item component for banners into Bedrock item NBT.
     */
    static void convertBannerPattern(JavaRegistryProvider registries, List<Layer> patterns, BedrockItemBuilder builder) {
        if (isOminous(registries, patterns)) {
            // Remove the current patterns and set the ominous banner type
            builder.putInt("Type", 1);
        } else {
            List<NbtMap> patternList = new ArrayList<>(patterns.size());
            for (Layer patternLayer : patterns) {
                patternLayer.pattern().unwrapKey().flatMap(key -> JavaRegistries.BANNER_PATTERN.get(registries, MinecraftKey.key(key.identifier().toString()))).ifPresent(bannerPattern -> {
                    NbtMap tag = NbtMap.builder()
                        .putString("Pattern", bannerPattern.getBedrockIdentifier())
                        .putInt("Color", 15 - patternLayer.color().getId())
                        .build();
                    patternList.add(tag);
                });
            }
            builder.putList("Patterns", NbtType.COMPOUND, patternList);
        }
    }

    /**
     * Convert the Java edition banner pattern nbt to Bedrock edition, null if the pattern doesn't exist
     *
     * @param pattern Java edition pattern nbt
     * @return The Bedrock edition format pattern nbt
     */
    private static NbtMap getBedrockBannerPattern(NbtMap pattern) {
        // ViaVersion 1.20.4 -> 1.20.5 can send without the namespace
        BannerPattern bannerPattern = BannerPattern.getByJavaIdentifier(MinecraftKey.key(pattern.getString("pattern")));
        DyeColor dyeColor = DyeColor.getByJavaIdentifier(pattern.getString("color"));
        if (bannerPattern == null || dyeColor == null) {
            return null;
        }

        return NbtMap.builder()
                .putString("Pattern", bannerPattern.getBedrockIdentifier())
                .putInt("Color", 15 - dyeColor.ordinal())
                .build();
    }

    /**
     * Convert the Bedrock edition banner pattern nbt to Java edition
     *
     * @param pattern Bedrock edition pattern nbt
     * @return The Java edition format pattern layer
     */
    public static Layer getJavaBannerPattern(GeyserSession session, NbtMap pattern) {
        BannerPattern bannerPattern = BannerPattern.getByBedrockIdentifier(pattern.getString("Pattern"));
        DyeColor dyeColor = DyeColor.getById(15 - pattern.getInt("Color"));
        if (bannerPattern == null || dyeColor == null) {
            return null;
        }
        return session.vanillaRegistries().lookupOrThrow(net.minecraft.core.registries.Registries.BANNER_PATTERN)
            .get(net.minecraft.resources.Identifier.parse(bannerPattern.getJavaIdentifier().asString()))
            .map(holder -> new Layer(holder, net.minecraft.world.item.DyeColor.byId(dyeColor.ordinal())))
            .orElse(null);
    }

    public BannerItem(Builder builder, Block block, Block... otherBlocks) {
        super(builder, block, otherBlocks);
    }

    @Override
    public void translateComponentsToBedrock(@NonNull GeyserSession session, @NonNull GeyserDataComponents components, @NonNull TooltipOptions tooltip, @NonNull BedrockItemBuilder builder) {
        super.translateComponentsToBedrock(session, components, tooltip, builder);

        BannerPatternLayers patterns = components.get(DataComponents.BANNER_PATTERNS);
        if (patterns != null) {
            convertBannerPattern(session.javaRegistries(), patterns.layers(), builder);
        }
    }

    @Override
    public void translateNbtToJava(@NonNull GeyserSession session, @NonNull NbtMap bedrockTag, @NonNull GeyserDataComponents components, @NonNull ItemMapping mapping) {
        super.translateNbtToJava(session, bedrockTag, components, mapping);

        if (bedrockTag.getInt("Type") == 1) {
            // Ominous banner pattern
            List<Layer> patternLayers = new ArrayList<>();
            for (Pair<BannerPattern, DyeColor> pair : OMINOUS_BANNER_PATTERN) {
                patternLayers.add(new Layer(session.vanillaRegistries().lookupOrThrow(net.minecraft.core.registries.Registries.BANNER_PATTERN)
                    .get(net.minecraft.resources.Identifier.parse(pair.left().getJavaIdentifier().asString())).orElseThrow(),
                    net.minecraft.world.item.DyeColor.byId(pair.right().ordinal())));
            }

            components.put(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(List.copyOf(patternLayers)));
            // The ominous banner item in the Java creative menu just has banner patterns hidden as of 1.21.5
            components.put(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.BANNER_PATTERNS, true));
            components.put(DataComponents.ITEM_NAME, Component.translatable("block.minecraft.ominous_banner"));
            components.put(DataComponents.RARITY, Rarity.UNCOMMON);
        }
        // Bedrock's creative inventory does not support other patterns as of 1.20.5
    }
}

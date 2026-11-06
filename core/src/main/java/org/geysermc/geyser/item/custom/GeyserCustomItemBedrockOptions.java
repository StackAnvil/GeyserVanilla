/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.item.custom;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemIcons;
import org.geysermc.geyser.api.util.CreativeCategory;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.registry.populator.custom.CustomItemContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record GeyserCustomItemBedrockOptions(@NonNull CustomItemIcons icons, boolean allowOffhand, boolean displayHandheld, int protectionValue,
                                             @NonNull CreativeCategory creativeCategory, @Nullable String creativeGroup, @NonNull Set<Identifier> tags,
                                             @Nullable Integer defaultDyeableColor) implements CustomItemBedrockOptions {

    @Override
    public int protectionValue() {
        return protectionValue == -1 ? 0 : protectionValue;
    }

    public int protectionValue(CustomItemContext context) {
        if (protectionValue == -1 && context.vanillaMapping().isPresent()) {
            ItemAttributeModifiers attributeModifiers = context.components().get(DataComponents.ATTRIBUTE_MODIFIERS);
            Equippable equippable = context.components().get(DataComponents.EQUIPPABLE);
            if (attributeModifiers != null && equippable != null) {
                return (int) attributeModifiers.modifiers().stream()
                    .filter(entry -> entry.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR.unwrapKey().orElseThrow())
                        && entry.slot().test(equippable.slot())
                        && entry.modifier().operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE)
                    .mapToDouble(entry -> entry.modifier().amount())
                    .sum();
            }
        }
        return protectionValue();
    }

    public static class Builder implements CustomItemBedrockOptions.Builder {
        private CustomItemIcons iconData = CustomItemIcons.EMPTY;
        private boolean allowOffhand = true;
        private boolean displayHandheld = false;
        private int protectionValue = -1;
        private CreativeCategory creativeCategory = CreativeCategory.NONE;
        private String creativeGroup = null;
        private Set<Identifier> tags = new HashSet<>();
        private @Nullable Integer dyeableColor;

        @Override
        public Builder icons(CustomItemIcons icons) {
            this.iconData = icons;
            return this;
        }

        @Override
        public Builder allowOffhand(boolean allowOffhand) {
            this.allowOffhand = allowOffhand;
            return this;
        }

        @Override
        public Builder displayHandheld(boolean displayHandheld) {
            this.displayHandheld = displayHandheld;
            return this;
        }

        @Override
        public Builder protectionValue(int protectionValue) {
            if (protectionValue < 0) {
                throw new IllegalArgumentException("protectionValue cannot be negative");
            }
            this.protectionValue = protectionValue;
            return this;
        }

        @Override
        public Builder creativeCategory(@NotNull CreativeCategory creativeCategory) {
            Objects.requireNonNull(creativeCategory, "creativeCategory cannot be null");
            this.creativeCategory = creativeCategory;
            return this;
        }

        @Override
        public Builder creativeGroup(@Nullable String creativeGroup) {
            this.creativeGroup = creativeGroup;
            return this;
        }

        @Override
        public Builder tag(@NotNull Identifier tag) {
            Objects.requireNonNull(tag, "tag cannot be null");
            this.tags.add(tag);
            return this;
        }

        @Override
        public Builder tags(@Nullable Set<Identifier> tags) {
            this.tags = Objects.requireNonNullElseGet(tags, HashSet::new);
            return this;
        }

        @Override
        public Builder dyeable(int defaultColor) {
            if (defaultColor < 0 || defaultColor > 0xFFFFFF) {
                throw new IllegalArgumentException("defaultColor must be between 0x000000 and 0xFFFFFF, was: 0x" + Integer.toString(defaultColor, 16).toUpperCase(Locale.ROOT));
            }
            this.dyeableColor = defaultColor;
            return this;
        }

        @Override
        public CustomItemBedrockOptions build() {
            if (iconData.dyedIcon() != null && dyeableColor == null) {
                throw new IllegalStateException("Set a dyed icon but item is not marked as dyeable");
            }
            return new GeyserCustomItemBedrockOptions(iconData, allowOffhand, displayHandheld, protectionValue,
                creativeCategory, creativeGroup, Set.copyOf(tags), dyeableColor);
        }
    }
}

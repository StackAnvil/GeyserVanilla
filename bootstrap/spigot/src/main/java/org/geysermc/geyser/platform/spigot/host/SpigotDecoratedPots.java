/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.spigot.host;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import org.bukkit.block.Block;
import org.bukkit.block.DecoratedPot;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Reads server-side decoration components without passing server item stacks into the client. */
final class SpigotDecoratedPots {
    private SpigotDecoratedPots() {
    }

    static Map<String, String> assets(Block block) {
        if (!(block.getState() instanceof DecoratedPot pot)) {
            return null;
        }
        try {
            Map<String, String> assets = Native.assets(pot);
            if (assets != null) {
                return assets;
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Older servers expose material decorations instead of component-bearing item templates.
        }
        Map<String, String> assets = new HashMap<>();
        pot.getSherds().forEach((side, material) -> {
            String key = material.getKey().toString();
            if (key.endsWith("_pottery_sherd")) {
                assets.put(side.name().toLowerCase(Locale.ROOT),
                    key.substring(0, key.length() - "_sherd".length()) + "_pattern");
            }
        });
        return assets;
    }

    // Keep current-server game types out of the fallback class's method signatures.
    private static final class Native {
        static Map<String, String> assets(DecoratedPot pot) throws ReflectiveOperationException {
            Object entity = pot.getClass().getMethod("getBlockEntity").invoke(pot);
            if (!(entity instanceof DecoratedPotBlockEntity nativePot)) {
                return null;
            }
            var decorations = nativePot.getDecorations();
            Map<String, String> assets = new HashMap<>();
            decorations.back().ifPresent(stack -> put(assets, "back", stack));
            decorations.left().ifPresent(stack -> put(assets, "left", stack));
            decorations.right().ifPresent(stack -> put(assets, "right", stack));
            decorations.front().ifPresent(stack -> put(assets, "front", stack));
            return assets;
        }

        private static void put(Map<String, String> assets, String side, ItemStackTemplate stack) {
            var pattern = stack.get(DataComponents.PROVIDES_POTTERY_PATTERN);
            if (pattern != null) {
                assets.put(side, pattern.value().assetId().toString());
            }
        }
    }
}

/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.spigot.host;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.minecraft.world.level.block.entity.PotDecorations;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.DecoratedPot;
import org.geysermc.geyser.vanilla.protocol.VanillaBootstrap;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpigotDecoratedPotsTest {
    public interface NativePotAccess {
        Object getBlockEntity();
    }

    @Test
    void readsCustomAndRemovedComponentsInsteadOfOnlySherdMaterials() {
        VanillaBootstrap.defaultRegistries();
        var custom = new ItemStackTemplate(Items.BRICK, DataComponentPatch.builder()
            .set(DataComponents.PROVIDES_POTTERY_PATTERN,
                Holder.direct(new DecoratedPotPattern(Identifier.parse("custom:my_pattern")))).build());
        var removed = new ItemStackTemplate(Items.ANGLER_POTTERY_SHERD, DataComponentPatch.builder()
            .remove(DataComponents.PROVIDES_POTTERY_PATTERN).build());
        var nativePot = mock(DecoratedPotBlockEntity.class);
        when(nativePot.getDecorations()).thenReturn(new PotDecorations(Optional.of(custom), Optional.of(removed),
            Optional.empty(), Optional.of(new ItemStackTemplate(Items.ANGLER_POTTERY_SHERD))));
        var pot = mock(DecoratedPot.class, withSettings().extraInterfaces(NativePotAccess.class));
        when(((NativePotAccess) pot).getBlockEntity()).thenReturn(nativePot);
        var block = mock(Block.class);
        when(block.getState()).thenReturn(pot);

        assertEquals(Map.of("back", "custom:my_pattern", "front", "minecraft:angler_pottery_pattern"),
            SpigotDecoratedPots.assets(block));
        verify(pot, never()).getSherds();
    }

    @Test
    void supportsMaterialDecorationsOnOlderServers() {
        var pot = mock(DecoratedPot.class);
        when(pot.getSherds()).thenReturn(Map.of(DecoratedPot.Side.BACK, Material.BRICK,
            DecoratedPot.Side.LEFT, Material.ARCHER_POTTERY_SHERD,
            DecoratedPot.Side.FRONT, Material.ARMS_UP_POTTERY_SHERD));
        var block = mock(Block.class);
        when(block.getState()).thenReturn(pot);
        assertEquals(Map.of("left", "minecraft:archer_pottery_pattern", "front", "minecraft:arms_up_pottery_pattern"),
            SpigotDecoratedPots.assets(block));
    }

    @Test
    void loadsTheFallbackWithoutCurrentMinecraftClasses() throws Exception {
        var pot = mock(DecoratedPot.class);
        when(pot.getSherds()).thenReturn(Map.of(DecoratedPot.Side.FRONT, Material.ARCHER_POTTERY_SHERD));
        var block = mock(Block.class);
        when(block.getState()).thenReturn(pot);
        String name = SpigotDecoratedPots.class.getName();
        URL location = SpigotDecoratedPots.class.getProtectionDomain().getCodeSource().getLocation();
        try (var olderServer = new URLClassLoader(new URL[] {location}, getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String candidate, boolean resolve) throws ClassNotFoundException {
                if (candidate.startsWith("net.minecraft.")) {
                    throw new ClassNotFoundException(candidate);
                }
                if (candidate.equals(name) || candidate.startsWith(name + "$")) {
                    Class<?> loaded = findLoadedClass(candidate);
                    if (loaded == null) {
                        loaded = findClass(candidate);
                    }
                    if (resolve) {
                        resolveClass(loaded);
                    }
                    return loaded;
                }
                return super.loadClass(candidate, resolve);
            }
        }) {
            Class<?> reader = olderServer.loadClass(name);
            var assets = reader.getDeclaredMethod("assets", Block.class);
            assets.setAccessible(true);
            assertEquals(Map.of("front", "minecraft:archer_pottery_pattern"), assets.invoke(null, block));
        }
    }

    @Test
    void skipsBlocksThatAreNoLongerDecoratedPots() {
        var block = mock(Block.class);
        when(block.getState()).thenReturn(mock(org.bukkit.block.BlockState.class));
        assertNull(SpigotDecoratedPots.assets(block));
    }
}

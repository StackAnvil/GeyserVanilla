/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class VanillaRegistryContextTest {
    @Test
    void loadsServerDataWithoutRebindingGlobalDefaults() {
        var defaults = VanillaBootstrap.defaultRegistries();
        var originalHolder = defaults.lookupOrThrow(Registries.ITEM)
            .get(Identifier.withDefaultNamespace("goat_horn")).orElseThrow();
        var originalComponents = originalHolder.components();
        List<ClientboundRegistryDataPacket> packets = VanillaTestRegistries.packets();
        var first = new VanillaRegistryContext();
        var second = new VanillaRegistryContext();
        packets.forEach(packet -> {
            first.accept(withInstrumentDescription(packet, "first server"));
            second.accept(withInstrumentDescription(packet, "second server"));
        });
        var tags = VanillaTestRegistries.tags();
        first.accept(tags);
        second.accept(tags);
        var firstAccess = first.finishConfiguration();
        var secondAccess = second.finishConfiguration();
        var key = Identifier.withDefaultNamespace("goat_horn");
        ItemStack firstItem = new ItemStack(firstAccess.lookupOrThrow(Registries.ITEM).get(key).orElseThrow());
        ItemStack secondItem = new ItemStack(secondAccess.lookupOrThrow(Registries.ITEM).get(key).orElseThrow());

        assertEquals("first server", firstItem.get(DataComponents.INSTRUMENT).instrument().value().description().getString());
        assertEquals("second server", secondItem.get(DataComponents.INSTRUMENT).instrument().value().description().getString());
        assertSame(originalComponents, originalHolder.components());
        assertSame(defaults.lookupOrThrow(Registries.ITEM).get(key).orElseThrow(), originalHolder);
        assertNotSame(firstItem.typeHolder(), secondItem.typeHolder());
        assertTrue(firstItem.get(DataComponents.INSTRUMENT).instrument().canSerializeIn(firstAccess.lookupOrThrow(Registries.INSTRUMENT)));
        assertTrue(secondItem.get(DataComponents.INSTRUMENT).instrument().canSerializeIn(secondAccess.lookupOrThrow(Registries.INSTRUMENT)));
    }

    private static ClientboundRegistryDataPacket withInstrumentDescription(ClientboundRegistryDataPacket packet, String description) {
        if (!packet.registry().equals(Registries.INSTRUMENT)) {
            return packet;
        }
        return new ClientboundRegistryDataPacket(packet.registry(), packet.entries().stream().map(entry -> {
            CompoundTag data = (CompoundTag) entry.data().orElseThrow().copy();
            data.putString("description", description);
            return new RegistrySynchronization.PackedRegistryEntry(entry.id(), Optional.of(data));
        }).toList());
    }
}

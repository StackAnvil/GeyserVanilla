/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagNetworkSerialization.NetworkPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class VanillaTestRegistries {
    private VanillaTestRegistries() {
    }

    static List<ClientboundRegistryDataPacket> packets() {
        var source = VanillaBootstrap.defaultRegistries();
        List<ClientboundRegistryDataPacket> packets = new ArrayList<>();
        RegistrySynchronization.packRegistries(source.createSerializationContext(NbtOps.INSTANCE), source,
            Set.of(), (key, entries) -> packets.add(new ClientboundRegistryDataPacket(key, entries)));
        return packets;
    }

    static ClientboundUpdateTagsPacket tags() {
        var source = VanillaBootstrap.defaultRegistries();
        Map<ResourceKey<? extends Registry<?>>, NetworkPayload> tags = new HashMap<>();
        source.registries().filter(entry -> BuiltInRegistries.REGISTRY.containsKey(entry.key().identifier())
            || RegistrySynchronization.isNetworkable(entry.key())).forEach(entry -> tags.put(entry.key(), tags(entry.value())));
        return new ClientboundUpdateTagsPacket(tags);
    }

    private static <T> NetworkPayload tags(Registry<T> registry) {
        Map<Identifier, it.unimi.dsi.fastutil.ints.IntList> tags = new HashMap<>();
        registry.getTags().forEach(tag -> {
            var ids = new IntArrayList();
            tag.forEach(holder -> ids.add(registry.getId(holder.value())));
            tags.put(tag.key().location(), ids);
        });
        return new NetworkPayload(tags);
    }
}

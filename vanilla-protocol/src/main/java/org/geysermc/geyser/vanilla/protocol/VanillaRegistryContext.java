/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.tags.TagNetworkSerialization.NetworkPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Maintains server registry data without rebinding the process-wide vanilla registry holders. */
public final class VanillaRegistryContext {
    private Map<ResourceKey<? extends Registry<?>>, List<RegistrySynchronization.PackedRegistryEntry>> contents = Map.of();
    private final Map<ResourceKey<? extends Registry<?>>, List<RegistrySynchronization.PackedRegistryEntry>> pending = new HashMap<>();
    private final Map<ResourceKey<? extends Registry<?>>, NetworkPayload> tags = new HashMap<>();
    private RegistryAccess.Frozen registries;

    public VanillaRegistryContext() {
        registries = VanillaBootstrap.defaultRegistries();
    }

    public void accept(ClientboundRegistryDataPacket packet) {
        pending.computeIfAbsent(packet.registry(), ignored -> new ArrayList<>()).addAll(packet.entries());
    }

    public void accept(ClientboundUpdateTagsPacket packet) {
        tags.putAll(packet.tags());
    }

    public RegistryAccess.Frozen registries() {
        return registries;
    }

    public RegistryAccess.Frozen finishConfiguration() {
        if (!pending.isEmpty()) {
            Map<ResourceKey<? extends Registry<?>>, List<RegistrySynchronization.PackedRegistryEntry>> snapshot = new HashMap<>();
            pending.forEach((key, entries) -> snapshot.put(key, List.copyOf(entries)));
            contents = Map.copyOf(snapshot);
            pending.clear();
        }

        // Vanilla's graphical client shares its static holders because it connects to one server.
        // A proxy must keep tags and default item components independent for every upstream session.
        List<Registry<?>> staticRegistries = new ArrayList<>();
        BuiltInRegistries.REGISTRY.forEach(registry -> staticRegistries.add(copyStaticRegistry(registry)));
        RegistryAccess.Frozen staticAccess = new RegistryAccess.ImmutableRegistryAccess(staticRegistries).freeze();

        if (contents.isEmpty()) {
            // A tag-only reconfiguration before registry synchronization retains vanilla defaults.
            registries = new RegistryAccess.ImmutableRegistryAccess(Stream.concat(staticAccess.registries(),
                VanillaBootstrap.defaultRegistries().registries().filter(entry -> !staticAccess.listRegistryKeys().anyMatch(entry.key()::equals))))
                .freeze();
        } else {
            Map<ResourceKey<? extends Registry<?>>, RegistryDataLoader.NetworkedRegistryData> network = new HashMap<>();
            contents.forEach((key, entries) -> network.put(key,
                new RegistryDataLoader.NetworkedRegistryData(entries, tags.getOrDefault(key, NetworkPayload.EMPTY))));
            RegistryAccess.Frozen remote = RegistryDataLoader.load(network, ResourceProvider.EMPTY,
                staticAccess.listRegistries().toList(), RegistryDataLoader.SYNCHRONIZED_REGISTRIES, Runnable::run).join();
            registries = new RegistryAccess.ImmutableRegistryAccess(Stream.concat(staticAccess.registries(), remote.registries())).freeze();
        }

        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries).forEach(pending -> pending.apply());
        return registries;
    }

    private <T> Registry<T> copyStaticRegistry(Registry<T> original) {
        MappedRegistry<T> copy = new MappedRegistry<>(original.key(), original.registryLifecycle());
        original.listElements().forEach(holder -> copy.register(holder.key(), holder.value(),
            original.registrationInfo(holder.key()).orElse(RegistrationInfo.BUILT_IN)));
        NetworkPayload networkTags = tags.get(original.key());
        if (networkTags != null) {
            copy.bindTags(TagLoader.loadTagsFromNetwork(networkTags, copy));
        } else {
            Map<TagKey<T>, List<Holder<T>>> originalTags = new HashMap<>();
            original.getTags().forEach(tag -> originalTags.put(tag.key(), tag.stream()
                .map(holder -> (Holder<T>) copy.get(holder.unwrapKey().orElseThrow()).orElseThrow()).toList()));
            copy.bindTags(originalTags);
        }
        return copy.freeze();
    }
}

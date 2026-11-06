/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.session.cache.registry;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.geyser.registry.java.JavaRegistry;
import org.geysermc.geyser.registry.java.JavaRegistryKey;
import org.geysermc.geyser.registry.java.JavaRegistryProvider;
import org.geysermc.geyser.registry.java.MutableJavaRegistry;
import org.geysermc.geyser.registry.java.SimpleJavaRegistry;
import org.geysermc.geyser.registry.java.reader.JavaRegistryReaders;
import org.geysermc.geyser.registry.java.reader.KeyDependentJavaRegistryReader;
import org.geysermc.geyser.registry.java.reader.RegistryEntryContext;
import org.geysermc.geyser.util.MinecraftKey;
import net.minecraft.core.RegistrySynchronization.PackedRegistryEntry;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;

import java.util.List;
import java.util.ArrayList;

/**
 * Stores any information sent via Java registries. May not contain all data in a given registry - we'll strip what's
 * unneeded.
 *
 * <p>Crafted as of 1.20.5 for easy "add new registry" functionality in the future.</p>
 */
final class RegistryCache {
    private final Reference2ObjectMap<JavaRegistryKey<?>, MutableJavaRegistry<?>> registries;
    private final Reference2ObjectMap<JavaRegistryKey<?>, List<PackedRegistryEntry>> configurationRegistryData;

    RegistryCache() {
        this.registries = new Reference2ObjectOpenHashMap<>(JavaRegistryReaders.networkRegistries().size());
        this.configurationRegistryData = new Reference2ObjectOpenHashMap<>(JavaRegistryReaders.networkRegistries().size());
        for (JavaRegistryKey<?> registry : JavaRegistryReaders.networkRegistries()) {
            registries.put(registry, new SimpleJavaRegistry<>(registry));
        }
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable JavaRegistry<T> registry(JavaRegistryKey<T> registryKey) {
        return (JavaRegistry<T>) registries.get(registryKey);
    }

    /**
     * Clears all held registry data. Should be called at the start of the configuration phase.
     */
    public void clear() {
        registries.values().forEach(MutableJavaRegistry::clear);
        configurationRegistryData.clear();
    }

    /**
     * Loads a {@link ClientboundRegistryDataPacket}. Registers entries of this packet, waiting to be parsed by {@link #parseAndBind(JavaRegistryProvider)}.
     *
     * @param packet the packet with registry data to load
     */
    public void load(ClientboundRegistryDataPacket packet) {
        JavaRegistryKey<?> registryKey = JavaRegistries.fromKey(MinecraftKey.vanillaToKey(packet.registry().identifier()));
        if (registryKey != null && JavaRegistryReaders.shouldParseRegistry(registryKey)) {
            MutableJavaRegistry<?> registry = registries.get(registryKey);
            // Register all entries, they will be parsed bound at the end of the configuration phase by parseAndBind
            // We could theoretically already freeze the registry at this stage, but I am not sure if vanilla Java allows receiving
            // multiple registry data packets for the same registry
            packet.entries().forEach(entry -> registry.register(MinecraftKey.vanillaToKey(entry.id())));
            this.configurationRegistryData.computeIfAbsent(registryKey, ignored -> new ArrayList<>()).addAll(packet.entries());
        } else {
            GeyserImpl.getInstance().getLogger().debug("Ignoring registry of type " + packet.registry());
        }
    }

    /**
     * Freezes all registries, then parses all entries and binds all registries.
     *
     * @param registryProvider the {@link JavaRegistryProvider}
     */
    public void parseAndBind(JavaRegistryProvider registryProvider) {
        // First, freeze all registries
        registries.values().forEach(MutableJavaRegistry::freeze);

        // Then, go over all received entries, this time binding them. We do this at this stage to ensure references to other dynamic registries work.
        // Registries for which no entries were received will not be left unbound: a registry automatically considers itself bound if it was empty upon freezing
        configurationRegistryData.forEach((registryKey, registryEntries) -> loadRegistry(registryProvider, registryKey, registryEntries));
        // Finally, we can clear the raw registry data
        configurationRegistryData.clear();
    }

    private <T> void loadRegistry(JavaRegistryProvider registryProvider, JavaRegistryKey<T> registryKey,
                                  List<PackedRegistryEntry> entries) {
        // Generics ftw
        MutableJavaRegistry<T> registry = (MutableJavaRegistry<T>) registries.get(registryKey);
        // Must have a reader at this point
        KeyDependentJavaRegistryReader<T> reader = JavaRegistryReaders.getReader(registryKey).orElseThrow();

        // Bind the registry, parsing all the registries
        registry.bind(id -> reader.read(new RegistryEntryContext(registryProvider, entries.get(id))));
    }

}

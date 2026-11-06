/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Native item definitions for Geyser's non-vanilla item API. Wire IDs may contain gaps. */
public final class VanillaCustomItems {
    private static final Map<Identifier, Definition> DEFINITIONS = new HashMap<>();
    private static final Map<Item, Definition> BY_ITEM = new IdentityHashMap<>();
    private static final Map<Integer, Definition> BY_ID = new HashMap<>();
    private static final VarHandle FROZEN;
    private static final VarHandle INTRUSIVE_HOLDERS;
    private static final VarHandle INITIALIZERS;
    private static final VarHandle INITIALIZER;

    static {
        try {
            var lookup = MethodHandles.lookup();
            var registryLookup = MethodHandles.privateLookupIn(MappedRegistry.class, lookup);
            FROZEN = registryLookup.findVarHandle(MappedRegistry.class, "frozen", boolean.class);
            INTRUSIVE_HOLDERS = registryLookup.findVarHandle(MappedRegistry.class, "unregisteredIntrusiveHolders", Map.class);
            INITIALIZERS = MethodHandles.privateLookupIn(DataComponentInitializers.class, lookup)
                .findVarHandle(DataComponentInitializers.class, "initializers", List.class);
            Class<?> entry = Class.forName("net.minecraft.core.component.DataComponentInitializers$InitializerEntry");
            INITIALIZER = MethodHandles.privateLookupIn(entry, lookup)
                .findVarHandle(entry, "initializer", DataComponentInitializers.Initializer.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private VanillaCustomItems() {
    }

    public static synchronized void register(Identifier identifier, int id) {
        VanillaBootstrap.defaultRegistries();
        if (id < BuiltInRegistries.ITEM.size() || BuiltInRegistries.ITEM.containsKey(identifier)) {
            throw new IllegalArgumentException("Custom item conflicts with a vanilla item: " + identifier + " (" + id + ")");
        }
        if (DEFINITIONS.containsKey(identifier) || BY_ID.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate custom item: " + identifier + " (" + id + ")");
        }
        var key = ResourceKey.create(Registries.ITEM, identifier);
        Definition definition = construct(key, id);
        DEFINITIONS.put(identifier, definition);
        BY_ITEM.put(definition.item(), definition);
        BY_ID.put(id, definition);
    }

    @SuppressWarnings("unchecked")
    private static Definition construct(ResourceKey<Item> key, int id) {
        var global = BuiltInRegistries.ITEM;
        var owner = new MappedRegistry<Item>(Registries.ITEM, Lifecycle.stable(), true);
        // Item's constructor hardcodes the built-in registry and the global initializer list.
        // Redirect its intrusive holder into an owned registry during construction, then restore
        // both globals. No custom entry, holder, or initializer remains in the built-in registry.
        synchronized (global) {
            if (!(boolean) FROZEN.get(global)) {
                throw new IllegalStateException("Custom items must be registered after vanilla bootstrap");
            }
            Map<?, ?> previousHolders = (Map<?, ?>) INTRUSIVE_HOLDERS.get(global);
            List<Object> initializers = (List<Object>) INITIALIZERS.get(BuiltInRegistries.DATA_COMPONENT_INITIALIZERS);
            int previousSize = initializers.size();
            try {
                INTRUSIVE_HOLDERS.set(global, new IdentityHashMap<Item, Holder.Reference<Item>>() {
                    @Override
                    public Holder.Reference<Item> computeIfAbsent(Item item,
                            Function<? super Item, ? extends Holder.Reference<Item>> ignored) {
                        return owner.createIntrusiveHolder(item);
                    }
                });
                FROZEN.set(global, false);
                Item item = new Item(new Item.Properties().setId(key));
                if (initializers.size() != previousSize + 1) {
                    throw new IllegalStateException("Unexpected vanilla item initializer count");
                }
                var initializer = (DataComponentInitializers.Initializer<Item>) INITIALIZER.get(initializers.get(previousSize));
                var holder = owner.register(key, item, RegistrationInfo.BUILT_IN);
                owner.freeze();
                var definition = new Definition(key, id, item, initializer);
                holder.bindComponents(definition.components(VanillaBootstrap.defaultRegistries()));
                return definition;
            } finally {
                initializers.subList(previousSize, initializers.size()).clear();
                INTRUSIVE_HOLDERS.set(global, previousHolders);
                FROZEN.set(global, true);
            }
        }
    }

    static synchronized int id(Item item) {
        Definition definition = BY_ITEM.get(item);
        return definition == null ? BuiltInRegistries.ITEM.getId(item) : definition.id();
    }

    static synchronized MappedRegistry<Item> registry(Registry<Item> original) {
        var registry = new ItemRegistry(original.registryLifecycle());
        original.listElements().forEach(holder -> registry.register(holder.key(), holder.value(),
            original.registrationInfo(holder.key()).orElse(RegistrationInfo.BUILT_IN)));
        BY_ID.values().stream().sorted(java.util.Comparator.comparingInt(Definition::id)).forEach(definition ->
            registry.register(definition.key(), definition.item(), RegistrationInfo.BUILT_IN));
        return registry;
    }

    static synchronized void initializeComponents(RegistryAccess registries) {
        // Registration temporarily captures a constructor initializer. Keep configuration
        // initialization under the same lock so it can only see the original global list.
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries).forEach(pending -> pending.apply());
        Registry<Item> items = registries.lookupOrThrow(Registries.ITEM);
        DEFINITIONS.values().forEach(definition -> items.get(definition.key()).orElseThrow()
            .bindComponents(definition.components(registries)));
    }

    public static synchronized boolean isCustom(Item item) {
        return BY_ITEM.containsKey(item);
    }

    private record Definition(ResourceKey<Item> key, int id, Item item,
                              DataComponentInitializers.Initializer<Item> initializer) {
        DataComponentMap components(RegistryAccess registries) {
            var builder = DataComponentMap.builder();
            initializer.run(builder, registries, key);
            return builder.build();
        }
    }

    // Native registries retain dense storage for iteration and component lookups. Only the
    // network ID lookup changes, so missing IDs never become null registry entries.
    private static final class ItemRegistry extends MappedRegistry<Item> {
        private final Map<Integer, Holder.Reference<Item>> wireIds = new HashMap<>();
        private final Map<Item, Integer> itemIds = new IdentityHashMap<>();

        ItemRegistry(Lifecycle lifecycle) {
            super(Registries.ITEM, lifecycle);
        }

        @Override
        public Holder.Reference<Item> register(ResourceKey<Item> key, Item item, RegistrationInfo info) {
            int id = VanillaCustomItems.id(item);
            if (id < 0 || wireIds.containsKey(id)) {
                throw new IllegalArgumentException("Duplicate or unknown Java item ID " + id);
            }
            var holder = super.register(key, item, info);
            wireIds.put(id, holder);
            itemIds.put(item, id);
            return holder;
        }

        @Override
        public int getId(Item item) {
            return itemIds.getOrDefault(item, -1);
        }

        @Override
        public Item byId(int id) {
            var holder = wireIds.get(id);
            return holder == null ? null : holder.value();
        }

        @Override
        public Optional<Holder.Reference<Item>> get(int id) {
            return Optional.ofNullable(wireIds.get(id));
        }
    }
}

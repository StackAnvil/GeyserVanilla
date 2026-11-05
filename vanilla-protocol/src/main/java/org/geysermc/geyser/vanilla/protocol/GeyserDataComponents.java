/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Mutable inventory edits using vanilla component types and values. Null values remove defaults. */
public final class GeyserDataComponents implements DataComponentGetter {
    private final Map<DataComponentType<?>, Object> edits;

    public GeyserDataComponents() {
        this(new IdentityHashMap<>());
    }

    private GeyserDataComponents(Map<DataComponentType<?>, Object> edits) {
        this.edits = edits;
    }

    public static GeyserDataComponents from(DataComponentMap components) {
        GeyserDataComponents result = new GeyserDataComponents();
        components.forEach(result::put);
        return result;
    }

    public static GeyserDataComponents from(DataComponentPatch patch) {
        DataComponentPatch.SplitResult split = patch.split();
        GeyserDataComponents result = from(split.added());
        split.removed().forEach(type -> result.edits.put(type, null));
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(DataComponentType<? extends T> type) {
        return (T) edits.get(type);
    }

    /** Includes explicit removals, so callers can distinguish them from unchanged defaults. */
    public boolean contains(DataComponentType<?> type) {
        return edits.containsKey(type);
    }

    public <T> void put(DataComponentType<T> type, T value) {
        edits.put(type, value);
    }

    public <T> void put(TypedDataComponent<T> component) {
        put(component.type(), component.value());
    }

    public void putAll(GeyserDataComponents components) {
        edits.putAll(components.edits);
    }

    /** Clears an edit, allowing the default component to apply again. */
    @SuppressWarnings("unchecked")
    public <T> T remove(DataComponentType<T> type) {
        return (T) edits.remove(type);
    }

    public Set<DataComponentType<?>> types() {
        return Collections.unmodifiableSet(edits.keySet());
    }

    public boolean isEmpty() {
        return edits.isEmpty();
    }

    public int size() {
        return edits.size();
    }

    public GeyserDataComponents copy() {
        return new GeyserDataComponents(new IdentityHashMap<>(edits));
    }

    public GeyserDataComponents immutableCopy() {
        return new GeyserDataComponents(Collections.unmodifiableMap(new IdentityHashMap<>(edits)));
    }

    public DataComponentPatch toVanilla() {
        DataComponentPatch.Builder builder = DataComponentPatch.builder();
        edits.forEach((type, value) -> {
            if (value == null) {
                builder.remove(type);
            } else {
                builder.set(TypedDataComponent.createUnchecked(type, value));
            }
        });
        return builder.build();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof GeyserDataComponents components
            && toVanilla().equals(components.toVanilla());
    }

    @Override
    public int hashCode() {
        return toVanilla().hashCode();
    }

    @Override
    public String toString() {
        return toVanilla().toString();
    }
}

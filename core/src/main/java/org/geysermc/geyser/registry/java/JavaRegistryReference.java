/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.registry.java;

import java.util.Objects;
import java.util.function.IntFunction;

/** A reference or inline value in Geyser's Bedrock translation registries. */
public sealed interface JavaRegistryReference<T> permits JavaRegistryReference.ById, JavaRegistryReference.Inline {
    T resolve(IntFunction<T> registry);

    default int networkId() {
        throw new IllegalStateException("An inline registry value has no network ID");
    }

    static <T> JavaRegistryReference<T> byId(int id) {
        return new ById<>(id);
    }

    static <T> JavaRegistryReference<T> inline(T value) {
        return new Inline<>(value);
    }

    record ById<T>(int networkId) implements JavaRegistryReference<T> {
        public ById {
            if (networkId < 0) {
                throw new IllegalArgumentException("Negative registry ID: " + networkId);
            }
        }

        @Override
        public T resolve(IntFunction<T> registry) {
            return registry.apply(networkId);
        }
    }

    record Inline<T>(T value) implements JavaRegistryReference<T> {
        public Inline {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public T resolve(IntFunction<T> registry) {
            return value;
        }
    }
}

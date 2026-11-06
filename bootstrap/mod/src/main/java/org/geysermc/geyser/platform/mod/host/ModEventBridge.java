/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.mod.host;

/** Shared by transformed game classes and the current engine. */
public final class ModEventBridge {
    private static volatile ModEvents events;

    private ModEventBridge() {
    }

    public static ModEvents events() {
        return events;
    }

    public static synchronized void install(ModEvents replacement) {
        if (events != null && events != replacement) {
            throw new IllegalStateException("A mod engine is already registered");
        }
        events = replacement;
    }

    public static synchronized void clear(ModEvents owner) {
        if (events == owner) {
            events = null;
        }
    }
}

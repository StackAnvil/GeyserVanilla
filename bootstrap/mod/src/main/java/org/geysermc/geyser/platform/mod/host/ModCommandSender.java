/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.mod.host;

import java.util.UUID;

/** An opaque host command source whose game objects never enter the engine. */
public interface ModCommandSender {
    String name();

    boolean isConsole();

    UUID playerUuid();

    void sendPlain(String message);

    void sendJson(String message);
}

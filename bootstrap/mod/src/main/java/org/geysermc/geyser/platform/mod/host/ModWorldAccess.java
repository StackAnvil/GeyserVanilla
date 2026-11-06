/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.mod.host;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** World operations whose arguments and results can cross the client-loader boundary. */
public interface ModWorldAccess {
    int protocolVersion();

    int blockAt(UUID player, int x, int y, int z);

    int defaultGameMode();

    void decoratedPotAssets(UUID player, int x, int y, int z, Consumer<Map<String, String>> result);
}

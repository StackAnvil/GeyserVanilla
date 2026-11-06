/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.mod.host;

import java.util.Map;
import java.util.UUID;

/** Gameplay notifications containing only IDs, coordinates, and shared sender handles. */
public interface ModEvents {
    Map<UUID, Integer> playerRenderDistances();

    void blockPlaced(UUID player, int x, int y, int z, int blockState);

    /** Each attached block occupies four entries: x, y, z, and the Java block state ID. */
    void pistonMoved(UUID[] players, int x, int y, int z, int direction, boolean sticky, boolean extending, int[] blocks);

    void playerJoined(ModCommandSender player);
}

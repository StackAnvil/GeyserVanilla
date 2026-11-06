/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.spigot.host;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.SocketAddress;
import java.util.Map;

/** Server-owned networking and world access; no game or Netty objects cross this boundary. */
public interface SpigotHost extends AutoCloseable {
    SocketAddress initialize(boolean disableCompression, boolean debug) throws Exception;

    void initializeWorldAdapter() throws Exception;

    int blockAt(World world, int x, int y, int z);

    String[] biomeIdentifiers(boolean withTags);

    int[] allBlockStates();

    @Nullable Map<String, String> decoratedPotAssets(Block block);

    @Override
    void close();
}

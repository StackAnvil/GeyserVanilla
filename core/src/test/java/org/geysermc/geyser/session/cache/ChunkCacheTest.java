/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.session.cache;

import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.level.WorldManager;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.vanilla.protocol.VanillaBlockPalette;
import org.geysermc.geyser.vanilla.protocol.VanillaBootstrap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChunkCacheTest {
    @Test
    void retainsCustomIdsAcrossChunkLoadUpdatesAndUnload() {
        VanillaBootstrap.initialize();
        var session = mock(GeyserSession.class);
        var geyser = mock(GeyserImpl.class);
        var world = mock(WorldManager.class);
        when(session.getGeyser()).thenReturn(geyser);
        when(geyser.getWorldManager()).thenReturn(world);
        var cache = new ChunkCache(session);
        cache.setMinY(-64);
        cache.setHeightY(32);
        var section = new VanillaBlockPalette();
        section.set(15, 0, 0, 1_000_001);
        cache.addToCache(-1, 2, new VanillaBlockPalette[] {section, null});

        assertEquals(1_000_001, cache.getBlockAt(-1, -64, 32));
        cache.updateBlock(-1, -64, 32, 2_000_001);
        assertEquals(2_000_001, cache.getBlockAt(-1, -64, 32));
        cache.updateBlock(-16, -33, 47, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, cache.getBlockAt(-16, -33, 47));
        assertEquals(0, cache.getBlockAt(-16, -32, 47));
        assertEquals(0, cache.getBlockAt(-16, -65, 47));

        cache.removeChunk(-1, 2);
        assertEquals(0, cache.getBlockAt(-1, -64, 32));
        assertEquals(0, cache.getBlockAt(-16, -33, 47));
        cache.addToCache(-1, 2, new VanillaBlockPalette[] {section, null});
        cache.clear();
        assertEquals(0, cache.getBlockAt(-1, -64, 32));
    }

    @Test
    void leavesStorageToAHostWithItsOwnChunkCache() {
        var session = mock(GeyserSession.class);
        var geyser = mock(GeyserImpl.class);
        var world = mock(WorldManager.class);
        when(session.getGeyser()).thenReturn(geyser);
        when(geyser.getWorldManager()).thenReturn(world);
        when(world.hasOwnChunkCache()).thenReturn(true);
        var cache = new ChunkCache(session);
        assertFalse(cache.isCache());
        cache.addToCache(0, 0, null);
        cache.updateBlock(0, 0, 0, 1_000_001);
        cache.removeChunk(0, 0);
        cache.clear();
        assertEquals(0, cache.getBlockAt(0, 0, 0));
    }
}

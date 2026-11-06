/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class VanillaNetworkTest {
    @Test
    @Timeout(15)
    void stopsOnlyOwnedWorkersAndCanCreateANewGroup() throws Exception {
        var hostGroup = net.minecraft.server.network.EventLoopGroupHolder.remote(false).eventLoopGroup();
        var first = VanillaNetwork.tcpBootstrap().config().group();
        assertNotSame(hostGroup, first);
        assertSame(first, VanillaNetwork.tcpBootstrap().config().group());
        assertTrue(first.submit(() -> Thread.currentThread().getName()).get(5, TimeUnit.SECONDS)
            .startsWith("Geyser vanilla TCP"));
        VanillaNetwork.shutdown();
        assertTrue(first.isTerminated());
        assertFalse(hostGroup.isShuttingDown());
        var second = VanillaNetwork.tcpBootstrap().config().group();
        assertNotSame(first, second);
        VanillaNetwork.shutdown();
        VanillaNetwork.shutdown();
        assertTrue(second.isTerminated());
        assertFalse(hostGroup.isShuttingDown());
    }
}

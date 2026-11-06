/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.spigot;

import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.network.java.GeyserInjector;
import org.geysermc.geyser.network.java.LocalSession;
import org.geysermc.geyser.platform.spigot.host.SpigotHost;

import java.net.InetAddress;

public final class GeyserSpigotInjector extends GeyserInjector {
    private final SpigotHost host;

    public GeyserSpigotInjector(SpigotHost host) {
        this.host = host;
    }

    @Override
    protected void initializeLocalChannel0(GeyserBootstrap bootstrap) throws Exception {
        serverSocketAddress = host.initialize(bootstrap.config().advanced().java().disableCompression(), bootstrap.config().debugMode());
        // Some older server initializers need a first connection before their pipeline is complete.
        LocalSession.connectStatus(serverSocketAddress, InetAddress.getLoopbackAddress().getHostAddress(),
            bootstrap.config().java().address(), bootstrap.config().java().port()).exceptionally(failure -> {
                bootstrap.getGeyserLogger().debug("Initial local status request failed", failure);
                return null;
            });
    }

    @Override
    public void shutdown() {
        host.close();
        super.shutdown();
    }
}

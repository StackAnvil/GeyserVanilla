/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.bungeecord;

import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.network.java.GeyserInjector;
import org.geysermc.geyser.platform.bungeecord.host.BungeeHost;

public final class GeyserBungeeInjector extends GeyserInjector {
    private final BungeeHost host;

    public GeyserBungeeInjector(BungeeHost host) {
        this.host = host;
    }

    @Override
    protected void initializeLocalChannel0(GeyserBootstrap bootstrap) throws Exception {
        host.reloadHandler(() -> {
            Thread thread = Thread.currentThread();
            ClassLoader previous = thread.getContextClassLoader();
            thread.setContextClassLoader(getClass().getClassLoader());
            try {
                var settings = bootstrap.config().advanced().java();
                serverSocketAddress = host.rebind(settings.disableCompression(), settings.bungeeListener(), settings.useHaproxyProtocol());
            } catch (Exception failure) {
                serverSocketAddress = null;
                bootstrap.getGeyserLogger().error("Could not rebind the BungeeCord listener", failure);
            } finally {
                thread.setContextClassLoader(previous);
            }
        });
        var settings = bootstrap.config().advanced().java();
        serverSocketAddress = host.initialize(settings.disableCompression(), settings.bungeeListener(), settings.useHaproxyProtocol());
    }

    @Override
    public void shutdown() {
        host.close();
        super.shutdown();
    }
}

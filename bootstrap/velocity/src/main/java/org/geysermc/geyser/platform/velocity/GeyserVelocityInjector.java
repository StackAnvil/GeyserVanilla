/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.velocity;

import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.network.java.GeyserInjector;
import org.geysermc.geyser.vanilla.runtime.VanillaHostInjector;

/** Keeps host Netty classes outside the native engine. */
public final class GeyserVelocityInjector extends GeyserInjector {
    private final VanillaHostInjector host;

    public GeyserVelocityInjector(VanillaHostInjector host) {
        this.host = host;
    }

    @Override
    protected void initializeLocalChannel0(GeyserBootstrap bootstrap) throws Exception {
        serverSocketAddress = host.initialize(bootstrap.config().advanced().java().disableCompression());
    }

    @Override
    public void shutdown() {
        host.close();
        super.shutdown();
    }
}

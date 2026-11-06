/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.runtime;

import java.net.SocketAddress;

/** Creates an injected listener with host classes and exposes only its serialized transport. */
public interface VanillaHostInjector extends AutoCloseable {
    SocketAddress initialize(boolean disableCompression) throws Exception;

    @Override
    void close();
}

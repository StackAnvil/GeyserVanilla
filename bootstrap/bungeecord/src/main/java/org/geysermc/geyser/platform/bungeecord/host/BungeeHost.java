/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.bungeecord.host;

import java.net.SocketAddress;

/** Host listener configuration and reload notifications without host Netty types. */
public interface BungeeHost extends AutoCloseable {
    SocketAddress initialize(boolean disableCompression, String listener, boolean proxyProtocol) throws Exception;

    SocketAddress rebind(boolean disableCompression, String listener, boolean proxyProtocol) throws Exception;

    void reloadHandler(Runnable handler);

    @Override
    void close();
}

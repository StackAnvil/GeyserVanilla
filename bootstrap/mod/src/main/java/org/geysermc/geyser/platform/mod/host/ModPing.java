/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.mod.host;

import java.net.InetSocketAddress;

/** Status data produced by the host, including other mods' status hooks. */
public interface ModPing {
    Result query(InetSocketAddress address);

    record Result(String description, int maximum, int online) {
    }
}

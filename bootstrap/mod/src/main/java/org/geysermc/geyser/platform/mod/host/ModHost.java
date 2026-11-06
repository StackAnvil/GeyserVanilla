/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.mod.host;

import org.geysermc.geyser.api.util.PlatformType;

import java.io.InputStream;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/** Host services used by the isolated mod engine. */
public interface ModHost {
    PlatformType platformType();
    Path dataFolder();
    boolean dedicated();
    String serverVersion();
    String serverName();
    String bindAddress();
    int port();
    Path floodgateFolder();
    InputStream resource(String name);
    Map<String, Object> metadata();
    ModWorldAccess world();
    ModPing ping();
    ModCommandSender playerSender(UUID player);
    ModCommandSender consoleSender();
    Object commandManager(Function<ModCommandSender, Object> map, Function<Object, ModCommandSender> reverse);
    String commandRegistryType();
    SocketAddress initializeNetwork(boolean disableCompression, boolean debug) throws Exception;
    void closeNetwork();
}

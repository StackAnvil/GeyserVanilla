/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.mod.host;

import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.platform.mod.GeyserServerPortGetter;
import org.geysermc.geyser.vanilla.runtime.VanillaHostedRuntime;

import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Host-owned mod lifecycle. Only shared API and JDK values reach the engine. */
public abstract class ModRuntime implements ModHost, AutoCloseable {
    private static ModRuntime instance;
    private MinecraftServer server;
    private ModHostInjector injector;
    private VanillaHostedRuntime runtime;
    private Object engine;
    private ModEvents events;

    public static ModRuntime instance() {
        return instance;
    }

    protected final synchronized void initialize() {
        try {
            instance = this;
            Path cache = Path.of(System.getProperty("geyser.vanilla.cache", dataFolder().resolve("cache/vanilla").toString()));
            runtime = VanillaHostedRuntime.open(getClass(), cache, message -> LogManager.getLogger("geyser").info(message),
                List.of("net.fabricmc.", "net.neoforged.", "org.slf4j.", "org.apache.logging.log4j.",
                    "org.incendo.", "io.leangen.geantyref.", "org.geysermc.geyser.platform.mod.host.",
                    "org.geysermc.geyser.platform.neoforge.host."));
            engine = runtime.create("org.geysermc.geyser.platform.mod.ModEngine", new Class<?>[] {ModHost.class}, this);
            invoke("onGeyserInitialize", new Class<?>[0]);
            invoke("registerCommands", new Class<?>[0]);
            events = (ModEvents) invoke("events", new Class<?>[0]);
            if (events != null) {
                ModEventBridge.install(events);
            }
        } catch (Exception | Error failure) {
            try { close(); } catch (Exception | Error cleanup) {
                if (failure != cleanup) failure.addSuppressed(cleanup);
            }
            throw new IllegalStateException("Could not initialize GeyserVanilla mod runtime", failure);
        }
    }

    public final synchronized void serverStarted(MinecraftServer server) {
        this.server = server;
        try {
            invoke("onGeyserEnable", new Class<?>[0]);
        } catch (RuntimeException | Error failure) {
            try { close(); } catch (Exception | Error cleanup) {
                if (failure != cleanup) failure.addSuppressed(cleanup);
            }
            throw failure;
        }
    }

    public final synchronized void serverStopped() {
        if (dedicated()) {
            close();
        } else {
            invoke("onGeyserDisable", new Class<?>[0]);
            server = null;
        }
    }

    public final synchronized String lanStarted(MinecraftServer server, String language) {
        serverStarted(server);
        return (String) invoke("lanMessage", new Class<?>[] {String.class}, language);
    }

    public final void gatherPermissions(BiConsumer<String, TriState> registrar) {
        invoke("gatherPermissions", new Class<?>[] {BiConsumer.class}, registrar);
    }

    public final void gatherUndefinedPermissions(BiConsumer<String, TriState> registrar) {
        invoke("gatherUndefinedPermissions", new Class<?>[] {BiConsumer.class}, registrar);
    }

    private synchronized Object invoke(String method, Class<?>[] parameters, Object... arguments) {
        if (engine == null) {
            return null;
        }
        try {
            return runtime.invoke(engine, method, parameters, arguments);
        } catch (Exception failure) {
            throw new IllegalStateException("Mod engine operation failed: " + method, failure);
        }
    }

    protected final MinecraftServer server() { return server; }
    public String serverVersion() { return server == null ? SharedConstants.getCurrentVersion().name() : server.getServerVersion(); }
    public String serverName() { return server == null ? platformType().platformName() : server.getServerModName(); }
    public String bindAddress() { return server == null || server.getLocalIp() == null ? "" : server.getLocalIp(); }
    public int port() { return server == null ? 0 : ((GeyserServerPortGetter) server).geyser$getServerPort(); }
    public ModWorldAccess world() { return new ServerWorldAccess(server); }
    public ModPing ping() { return new ServerPing(server, failure -> LogManager.getLogger("geyser").debug("Host ping failed", failure)); }
    public ModCommandSender consoleSender() { return new ServerCommandSender(server.createCommandSourceStack()); }

    public ModCommandSender playerSender(UUID uuid) {
        var player = server == null ? null : server.getPlayerList().getPlayer(uuid);
        return player == null ? null : new ServerCommandSender(player.createCommandSourceStack());
    }

    @Override
    public Map<String, Object> metadata() {
        Map<String, Object> info = new HashMap<>();
        info.put("platformName", serverName());
        info.put("minecraftVersion", serverVersion());
        info.put("serverIP", bindAddress());
        info.put("serverPort", port());
        info.put("onlineMode", server != null && server.usesAuthentication());
        return info;
    }

    @Override
    public synchronized SocketAddress initializeNetwork(boolean disableCompression, boolean debug) throws Exception {
        if (injector != null) {
            throw new IllegalStateException("Mod networking is already injected");
        }
        injector = new ModHostInjector(server);
        return injector.initialize(disableCompression, debug);
    }

    @Override
    public synchronized void closeNetwork() {
        if (injector != null) {
            injector.close();
            injector = null;
        }
    }

    @Override
    public final synchronized void close() {
        ModEventBridge.clear(events);
        events = null;
        Object closingEngine = engine;
        VanillaHostedRuntime closingRuntime = runtime;
        engine = null;
        runtime = null;
        try (closingRuntime) {
            if (closingRuntime != null && closingEngine != null) {
                closingRuntime.invoke(closingEngine, "onGeyserShutdown", new Class<?>[0]);
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Could not stop the mod engine", failure);
        } finally {
            closeNetwork();
            server = null;
            if (instance == this) instance = null;
        }
    }
}

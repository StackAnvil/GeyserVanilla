/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.viaproxy;

import io.netty.channel.AbstractChannel;
import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.reflect.stream.RStream;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.ViaProxyPlugin;
import net.raphimc.viaproxy.plugins.events.Client2ProxyChannelInitializeEvent;
import net.raphimc.viaproxy.plugins.events.ConsoleCommandEvent;
import net.raphimc.viaproxy.plugins.events.ProxyStartEvent;
import net.raphimc.viaproxy.plugins.events.ProxyStopEvent;
import net.raphimc.viaproxy.plugins.events.ShouldVerifyOnlineModeEvent;
import net.raphimc.viaproxy.plugins.events.ViaProxyLoadedEvent;
import net.raphimc.viaproxy.plugins.events.types.ITyped;
import org.apache.logging.log4j.LogManager;
import org.geysermc.geyser.vanilla.runtime.VanillaHostedRuntime;

import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Bridges proxy events without exposing host Netty or authentication objects to the client. */
public final class GeyserViaProxyEntrypoint extends ViaProxyPlugin {
    private VanillaHostedRuntime runtime;
    private Object engine;

    @Override
    public synchronized void onEnable() {
        try {
            Path cache = Path.of(System.getProperty("geyser.vanilla.cache",
                PluginManager.PLUGINS_DIR.toPath().resolve("Geyser/cache/vanilla").toString()));
            runtime = VanillaHostedRuntime.open(getClass(), cache, message -> LogManager.getLogger("Geyser").info(message),
                List.of("net.raphimc.viaproxy.", "net.raphimc.vialegacy.", "com.viaversion.",
                    "net.lenni0451.", "org.slf4j.", "org.apache.logging.log4j."));
            engine = runtime.create("org.geysermc.geyser.platform.viaproxy.GeyserViaProxyPlugin", new Class<?>[0]);
            ViaProxy.EVENT_MANAGER.register(this);
        } catch (Exception | Error failure) {
            try {
                close();
            } catch (Exception cleanup) {
                if (cleanup != failure) {
                    failure.addSuppressed(cleanup);
                }
            }
            throw new IllegalStateException("Could not start GeyserVanilla", failure);
        }
    }

    @EventHandler
    private void onViaProxyLoaded(ViaProxyLoadedEvent event) {
        start("onViaProxyLoaded");
    }

    @EventHandler
    private void onProxyStart(ProxyStartEvent event) {
        start("onGeyserEnable");
    }

    @EventHandler
    private void onProxyStop(ProxyStopEvent event) {
        invoke("onGeyserDisable", new Class<?>[0]);
    }

    @EventHandler
    private void onConsoleCommand(ConsoleCommandEvent event) {
        if (Boolean.TRUE.equals(invoke("runConsoleCommand", new Class<?>[] {String.class, String[].class},
            event.getCommand(), event.getArgs()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onShouldVerifyOnlineMode(ShouldVerifyOnlineModeEvent event) {
        var profile = event.getProxyConnection().getGameProfile();
        if (profile != null && Boolean.TRUE.equals(invoke("isBedrockPlayer", new Class<?>[] {UUID.class, String.class},
            profile.getId(), profile.getName()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onClient2ProxyChannelInitialize(Client2ProxyChannelInitializeEvent event) {
        if (event.getType() != ITyped.Type.POST || event.isLegacyPassthrough()
            || System.getProperty("geyser.viaproxy.disableIpPassthrough") != null) {
            return;
        }
        var realAddress = (SocketAddress) invoke("realAddress", new Class<?>[] {SocketAddress.class},
            event.getChannel().remoteAddress());
        if (realAddress != null && event.getChannel() instanceof AbstractChannel) {
            RStream.of(AbstractChannel.class, event.getChannel()).fields().by("remoteAddress").set(realAddress);
        }
    }

    private synchronized Object invoke(String method, Class<?>[] parameters, Object... arguments) {
        if (engine == null) {
            return null;
        }
        try {
            return runtime.invoke(engine, method, parameters, arguments);
        } catch (Exception failure) {
            throw new IllegalStateException("Could not handle ViaProxy event: " + method, failure);
        }
    }

    private synchronized void start(String method) {
        try {
            invoke(method, new Class<?>[0]);
        } catch (RuntimeException | Error failure) {
            try {
                close();
            } catch (Exception cleanup) {
                if (cleanup != failure) {
                    failure.addSuppressed(cleanup);
                }
            }
            throw failure;
        }
    }

    @Override
    public void onDisable() {
        try {
            close();
        } catch (Exception failure) {
            throw new IllegalStateException("Could not stop GeyserVanilla", failure);
        }
    }

    private synchronized void close() throws Exception {
        ViaProxy.EVENT_MANAGER.unregister(this);
        Object closingEngine = engine;
        VanillaHostedRuntime closingRuntime = runtime;
        engine = null;
        runtime = null;
        try (closingRuntime) {
            if (closingRuntime != null && closingEngine != null) {
                closingRuntime.invoke(closingEngine, "onGeyserShutdown", new Class<?>[0]);
            }
        }
    }
}

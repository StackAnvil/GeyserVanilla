/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ListenerBoundEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.platform.velocity.host.VelocityHostInjector;
import org.geysermc.geyser.vanilla.runtime.VanillaHostInjector;
import org.geysermc.geyser.vanilla.runtime.VanillaHostedRuntime;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;

/** Host entrypoint. Only the isolated delegate resolves Geyser core and native game classes. */
@Plugin(id = "geyser", name = "GeyserVanilla-Velocity", version = GeyserImpl.VERSION,
    url = "https://github.com/StackAnvil/GeyserVanilla", authors = "StackAnvil")
public final class GeyserVelocityEntrypoint {
    private final ProxyServer server;
    private final PluginContainer container;
    private final Logger logger;
    private final VelocityHostInjector injector;
    private VanillaHostedRuntime runtime;
    private Object engine;

    @Inject
    public GeyserVelocityEntrypoint(ProxyServer server, PluginContainer container, Logger logger) {
        this.server = server;
        this.container = container;
        this.logger = logger;
        this.injector = new VelocityHostInjector(server);
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) throws Exception {
        try {
            Path cache = Path.of(System.getProperty("geyser.vanilla.cache", "plugins/Geyser-Velocity/cache/vanilla"));
            runtime = VanillaHostedRuntime.open(getClass(), cache, logger::info,
                List.of("com.velocitypowered.", "com.google.inject.", "net.kyori.", "org.slf4j.",
                    "org.apache.logging.log4j.", "org.incendo.", "io.leangen.geantyref.",
                    "org.geysermc.geyser.platform.velocity.host."));
            engine = runtime.create("org.geysermc.geyser.platform.velocity.GeyserVelocityPlugin",
                new Class<?>[] {ProxyServer.class, PluginContainer.class, Logger.class, VanillaHostInjector.class},
                server, container, logger, injector);
            runtime.invoke(engine, "onGeyserInitialize", new Class<?>[0]);
        } catch (Exception | Error failure) {
            try {
                close();
            } catch (Exception cleanup) {
                failure.addSuppressed(cleanup);
            }
            throw failure;
        }
    }

    @Subscribe
    public void onProxyBound(ListenerBoundEvent event) throws Exception {
        if (engine != null) {
            try {
                runtime.invoke(engine, "onProxyBound", new Class<?>[] {ListenerBoundEvent.class}, event);
            } catch (Exception | Error failure) {
                try {
                    close();
                } catch (Exception cleanup) {
                    failure.addSuppressed(cleanup);
                }
                throw failure;
            }
        }
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) throws Exception {
        close();
    }

    private void close() throws Exception {
        Object closingEngine = engine;
        VanillaHostedRuntime closingRuntime = runtime;
        engine = null;
        runtime = null;
        try (closingRuntime; injector) {
            if (closingRuntime != null && closingEngine != null) {
                closingRuntime.invoke(closingEngine, "onGeyserShutdown", new Class<?>[0]);
            }
        }
    }
}

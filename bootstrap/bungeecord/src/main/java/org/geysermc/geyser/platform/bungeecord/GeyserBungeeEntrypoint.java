/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.bungeecord;

import net.md_5.bungee.api.plugin.Plugin;
import org.geysermc.geyser.platform.bungeecord.host.BungeeHost;
import org.geysermc.geyser.platform.bungeecord.host.BungeeHostInjector;
import org.geysermc.geyser.vanilla.runtime.VanillaHostedRuntime;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Owns the host lifecycle while the native client and Geyser core run in a separate loader. */
public final class GeyserBungeeEntrypoint extends Plugin {
    private BungeeHostInjector host;
    private VanillaHostedRuntime runtime;
    private Object engine;
    private volatile boolean closing;

    @Override
    public synchronized void onLoad() {
        try {
            host = new BungeeHostInjector(this);
            Path cache = Path.of(System.getProperty("geyser.vanilla.cache", getDataFolder().toPath().resolve("cache/vanilla").toString()));
            runtime = VanillaHostedRuntime.open(getClass(), cache, getLogger()::info,
                List.of("net.md_5.", "org.slf4j.", "org.incendo.", "io.leangen.geantyref.",
                    "org.geysermc.geyser.platform.bungeecord.host."));
            engine = runtime.create("org.geysermc.geyser.platform.bungeecord.GeyserBungeePlugin",
                new Class<?>[] {Plugin.class, BungeeHost.class}, this, host);
            runtime.invoke(engine, "onGeyserInitialize", new Class<?>[0]);
        } catch (Exception | Error failure) {
            throw failed(failure);
        }
    }

    @Override
    public synchronized void onEnable() {
        if (engine == null) {
            return;
        }
        try {
            runtime.invoke(engine, "onEnable", new Class<?>[0]);
            awaitStartupCompletion(0);
        } catch (Exception | Error failure) {
            throw failed(failure);
        }
    }

    private synchronized void awaitStartupCompletion(int tries) {
        if (closing || engine == null) {
            return;
        }
        try {
            Field listeners = getProxy().getClass().getDeclaredField("listeners");
            listeners.setAccessible(true);
            boolean ready = !((Collection<?>) listeners.get(getProxy())).isEmpty();
            if (!ready && tries < 20) {
                getProxy().getScheduler().schedule(this, () -> awaitStartupCompletion(tries + 1),
                    tries + 1, TimeUnit.SECONDS);
                return;
            }
            if (!ready) {
                getLogger().warning("Proxy listeners are still unavailable. Starting Geyser now.");
            }
            runtime.invoke(engine, "onGeyserEnable", new Class<?>[0]);
        } catch (Exception | Error failure) {
            throw failed(failure);
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

    private IllegalStateException failed(Throwable failure) {
        try {
            close();
        } catch (Exception cleanup) {
            if (cleanup != failure) {
                failure.addSuppressed(cleanup);
            }
        }
        return new IllegalStateException("Could not start GeyserVanilla", failure);
    }

    private synchronized void close() throws Exception {
        closing = true;
        getProxy().getScheduler().cancel(this);
        Object closingEngine = engine;
        VanillaHostedRuntime closingRuntime = runtime;
        BungeeHostInjector closingHost = host;
        engine = null;
        runtime = null;
        host = null;
        try (closingRuntime; closingHost) {
            if (closingRuntime != null && closingEngine != null) {
                closingRuntime.invoke(closingEngine, "onGeyserShutdown", new Class<?>[0]);
            }
        }
    }
}

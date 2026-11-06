/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.spigot;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.geyser.platform.spigot.host.SpigotHost;
import org.geysermc.geyser.platform.spigot.host.SpigotHostInjector;
import org.geysermc.geyser.vanilla.runtime.VanillaHostedRuntime;

import java.nio.file.Path;
import java.util.List;

/** Owns the Bukkit plugin identity while Geyser and the client use an isolated loader. */
public final class GeyserSpigotEntrypoint extends JavaPlugin implements Listener {
    private SpigotHostInjector host;
    private VanillaHostedRuntime runtime;
    private Object engine;

    @Override
    public synchronized void onLoad() {
        try {
            host = new SpigotHostInjector(this);
            Path cache = Path.of(System.getProperty("geyser.vanilla.cache", getDataFolder().toPath().resolve("cache/vanilla").toString()));
            runtime = VanillaHostedRuntime.open(getClass(), cache, getLogger()::info,
                List.of("org.bukkit.", "io.papermc.", "com.destroystokyo.paper.", "net.md_5.", "com.viaversion.",
                    "org.slf4j.", "org.apache.logging.log4j.", "org.incendo.", "io.leangen.geantyref.",
                    "me.lucko.commodore.", "org.geysermc.erosion.bukkit.", "org.geysermc.geyser.adapters.",
                    "org.geysermc.geyser.platform.spigot.host."));
            engine = runtime.create("org.geysermc.geyser.platform.spigot.GeyserSpigotPlugin",
                new Class<?>[] {JavaPlugin.class, SpigotHost.class}, this, host);
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
            if (isEnabled()) {
                getServer().getPluginManager().registerEvents(this, this);
            }
        } catch (Exception | Error failure) {
            throw failed(failure);
        }
    }

    @EventHandler
    public synchronized void onServerLoaded(ServerLoadEvent event) {
        if (engine == null) {
            return;
        }
        try {
            runtime.invoke(engine, "onServerLoaded", new Class<?>[] {boolean.class}, event.getType() == ServerLoadEvent.LoadType.RELOAD);
        } catch (Exception | Error failure) {
            throw failed(failure);
        }
    }

    private IllegalStateException failed(Throwable failure) {
        try {
            close();
        } catch (Exception | Error cleanup) {
            if (cleanup != failure) {
                failure.addSuppressed(cleanup);
            }
        }
        return new IllegalStateException("Could not start GeyserVanilla", failure);
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
        Object closingEngine = engine;
        VanillaHostedRuntime closingRuntime = runtime;
        SpigotHostInjector closingHost = host;
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

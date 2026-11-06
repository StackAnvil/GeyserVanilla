/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.mod;

import org.geysermc.geyser.FloodgateKeyLoader;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.command.CommandRegistry;
import org.geysermc.geyser.command.CommandSourceConverter;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.configuration.GeyserPluginConfig;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.level.WorldManager;
import org.geysermc.geyser.network.java.GeyserInjector;
import org.geysermc.geyser.ping.GeyserLegacyPingPassthrough;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.geysermc.geyser.platform.mod.command.ModCommandSource;
import org.geysermc.geyser.platform.mod.host.ModCommandSender;
import org.geysermc.geyser.platform.mod.host.ModEvents;
import org.geysermc.geyser.platform.mod.host.ModHost;
import org.geysermc.geyser.platform.mod.world.GeyserModWorldManager;
import org.geysermc.geyser.text.GeyserLocale;
import org.incendo.cloud.CommandManager;

import java.io.InputStream;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.function.BiConsumer;

/** Geyser bootstrap whose game classes belong entirely to the downloaded client. */
public final class ModEngine implements GeyserBootstrap {
    private final ModHost host;
    private final GeyserModLogger logger = new GeyserModLogger();
    private GeyserImpl geyser;
    private GeyserPluginConfig config;
    private CommandRegistry commands;
    private IGeyserPingPassthrough ping;
    private WorldManager world;
    private GeyserInjector injector;
    private Path floodgateKey;
    private boolean started;

    public ModEngine(ModHost host) {
        this.host = host;
    }

    @Override
    public void onGeyserInitialize() {
        GeyserLocale.init(this);
        config = loadConfig(GeyserPluginConfig.class);
        if (config != null) {
            geyser = GeyserImpl.load(this);
        }
    }

    @SuppressWarnings("unchecked")
    public void registerCommands() throws Exception {
        if (geyser == null) {
            return;
        }
        var converter = new CommandSourceConverter<>(ModCommandSender.class, host::playerSender,
            host::consoleSender, ModCommandSource::new);
        var cloud = (CommandManager<GeyserCommandSource>) host.commandManager(converter::map,
            source -> converter.reverse((GeyserCommandSource) source));
        if (host.commandRegistryType().isEmpty()) {
            commands = new CommandRegistry(geyser, cloud, false);
        } else {
            commands = (CommandRegistry) Class.forName(host.commandRegistryType(), true, getClass().getClassLoader())
                .getConstructor(GeyserImpl.class, CommandManager.class).newInstance(geyser, cloud);
        }
    }

    public ModEvents events() {
        return geyser == null ? null : new ModEngineEvents(geyser);
    }

    public void gatherPermissions(BiConsumer<String, TriState> register) {
        if (geyser != null) {
            geyser.eventBus().fire((GeyserRegisterPermissionsEvent) register::accept);
        }
    }

    public void gatherUndefinedPermissions(BiConsumer<String, TriState> register) throws Exception {
        if (commands != null && !host.commandRegistryType().isEmpty()) {
            commands.getClass().getMethod("gatherUndefinedPermissions", BiConsumer.class).invoke(commands, register);
        }
    }

    @Override
    public void onGeyserEnable() {
        if (geyser == null) {
            return;
        }
        if (started && !geyser.isReloading()) {
            // Reopening a client LAN world reuses registries and command registrations.
            geyser.reloadGeyser();
            return;
        }
        boolean reloading = geyser.isReloading();
        if (reloading) {
            config = loadConfig(GeyserPluginConfig.class);
            if (config == null) {
                return;
            }
        }
        world = new GeyserModWorldManager(host.world());
        if (!reloading && host.dedicated()) {
            injector = new GeyserInjector() {
                @Override
                protected void initializeLocalChannel0(GeyserBootstrap bootstrap) throws Exception {
                    serverSocketAddress = host.initializeNetwork(config.advanced().java().disableCompression(), config.debugMode());
                }

                @Override
                public void shutdown() {
                    host.closeNetwork();
                    super.shutdown();
                }
            };
            injector.initializeLocalChannel(this);
        }
        GeyserImpl.start();
        started = true;
        ping = config.motd().integratedPingPassthrough()
            ? new ModPingPassthrough(host.ping()) : GeyserLegacyPingPassthrough.init(geyser);
    }

    @Override
    public void onGeyserDisable() {
        if (geyser != null && geyser.isEnabled()) {
            geyser.disable();
        }
        world = null;
        ping = null;
    }

    @Override
    public void onGeyserShutdown() {
        try {
            if (geyser != null) {
                geyser.shutdown();
                geyser = null;
            }
        } finally {
            if (injector != null) {
                injector.shutdown();
                injector = null;
            }
        }
    }

    public String lanMessage(String language) {
        GeyserLocale.loadGeyserLocale(language);
        return GeyserLocale.getPlayerLocaleString("geyser.core.start.ip_suppressed", language,
            String.valueOf(geyser.bedrockListener().port()));
    }

    public PlatformType platformType() { return host.platformType(); }
    public GeyserPluginConfig config() { return config; }
    public GeyserModLogger getGeyserLogger() { return logger; }
    public CommandRegistry getCommandRegistry() { return commands; }
    public IGeyserPingPassthrough getGeyserPingPassthrough() { return ping; }
    public WorldManager getWorldManager() { return world; }
    public Path getConfigFolder() { return host.dataFolder(); }
    public BootstrapDumpInfo getDumpInfo() { return new ModDumpInfo(host.metadata()); }
    public String getMinecraftServerVersion() { return host.serverVersion(); }
    public String getServerBindAddress() { return host.bindAddress(); }
    public int getServerPort() { return host.port(); }
    public SocketAddress getSocketAddress() { return injector == null ? null : injector.getServerSocketAddress(); }
    public InputStream getResourceOrNull(String resource) { return host.resource(resource); }
    public String getServerPlatform() { return host.serverName(); }
    public Path getFloodgateKeyPath() { return floodgateKey; }

    @Override
    public boolean testFloodgatePluginPresent() {
        Path folder = host.floodgateFolder();
        if (folder == null) {
            return false;
        }
        floodgateKey = FloodgateKeyLoader.getKeyPath(config, folder, getConfigFolder(), logger);
        return true;
    }
}

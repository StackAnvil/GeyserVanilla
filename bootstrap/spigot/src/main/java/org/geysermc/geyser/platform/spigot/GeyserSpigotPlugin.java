/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.platform.spigot;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.data.MappingData;
import com.viaversion.viaversion.api.protocol.ProtocolPathEntry;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.FloodgateKeyLoader;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.command.CommandRegistry;
import org.geysermc.geyser.command.CommandSourceConverter;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.configuration.GeyserPluginConfig;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.level.WorldManager;
import org.geysermc.geyser.network.bedrock.GameProtocol;
import org.geysermc.geyser.ping.GeyserLegacyPingPassthrough;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.geysermc.geyser.platform.spigot.host.SpigotHost;
import org.geysermc.geyser.platform.spigot.command.SpigotCommandRegistry;
import org.geysermc.geyser.platform.spigot.command.SpigotCommandSource;
import org.geysermc.geyser.platform.spigot.world.GeyserPistonListener;
import org.geysermc.geyser.platform.spigot.world.GeyserSpigotBlockPlaceListener;
import org.geysermc.geyser.platform.spigot.world.manager.GeyserSpigotLegacyNativeWorldManager;
import org.geysermc.geyser.platform.spigot.world.manager.GeyserSpigotNativeWorldManager;
import org.geysermc.geyser.platform.spigot.world.manager.GeyserSpigotWorldManager;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.metrics.MetricsPlatform;
import org.incendo.cloud.bukkit.BukkitCommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class GeyserSpigotPlugin implements GeyserBootstrap {
    private final JavaPlugin plugin;
    private final SpigotHost host;

    private CommandRegistry commandRegistry;
    private GeyserPluginConfig geyserConfig;
    private GeyserSpigotInjector geyserInjector;
    private final GeyserSpigotLogger geyserLogger;
    private IGeyserPingPassthrough geyserSpigotPingPassthrough;
    private GeyserSpigotWorldManager geyserWorldManager;

    private GeyserImpl geyser;

    /**
     * The Minecraft server version, formatted as <code>1.#.#</code>
     */
    private String minecraftVersion;

    public GeyserSpigotPlugin(JavaPlugin plugin, SpigotHost host) {
        this.plugin = plugin;
        this.host = host;
        this.geyserLogger = GeyserPaperLogger.supported()
            ? new GeyserPaperLogger(plugin, plugin.getLogger()) : new GeyserSpigotLogger(plugin.getLogger());
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    @Override
    public void onGeyserInitialize() {
        GeyserLocale.init(this);

        try {
            // AvailableCommandsSerializer_v291 complains otherwise - affects at least 1.8
            Class.forName("io.netty.buffer.ByteBuf", false, plugin.getClass().getClassLoader()).getMethod("writeShortLE", int.class);
            // Only available in 1.13.x
            Class.forName("org.bukkit.event.server.ServerLoadEvent");
            // We depend on this as a fallback in certain scenarios
            BlockData.class.getMethod("getAsString");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            geyserLogger.error("*********************************************");
            geyserLogger.error("");
            geyserLogger.error(GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_server.header"));
            geyserLogger.error(GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_server.message", "1.13.2"));
            geyserLogger.error("");
            geyserLogger.error("*********************************************");
            return;
        }

        try {
            Class.forName("net.md_5.bungee.chat.ComponentSerializer");
        } catch (ClassNotFoundException e) {
            if (!PaperAdventure.canSendMessageUsingComponent()) { // Prepare for Paper eventually removing Bungee chat
                geyserLogger.error("*********************************************");
                geyserLogger.error("");
                geyserLogger.error(GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_server_type.header", plugin.getServer().getName()));
                geyserLogger.error(GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_server_type.message", "Paper"));
                geyserLogger.error("");
                geyserLogger.error("*********************************************");
                return;
            }
        }

        try {
            Class.forName("io.netty.util.internal.ObjectPool$ObjectCreator", false, plugin.getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            geyserLogger.error("*********************************************");
            geyserLogger.error("");
            geyserLogger.error("This version of Spigot is using an outdated version of netty. Please use Paper instead!");
            geyserLogger.error("");
            geyserLogger.error("*********************************************");
            return;
        }

        try {
            // Check spigot config for BungeeCord mode
            if (Bukkit.getServer().spigot().getConfig().getBoolean("settings.bungeecord")) {
                warnInvalidProxySetups("BungeeCord");
                return;
            }

            // Now: Check for velocity mode - deliberately after checking bungeecord because this is a paper only option
            if (Bukkit.getServer().spigot().getPaperConfig().getBoolean("proxies.velocity.enabled")) {
                warnInvalidProxySetups("Velocity");
                return;
            }
        } catch (NoSuchMethodError e) {
            // no-op
        }

        geyserConfig = loadConfig(GeyserPluginConfig.class);
        if (geyserConfig == null) {
            // We'll disable ourselves later
            return;
        }

        // Turn "(MC: 1.16.4)" into 1.16.4.
        this.minecraftVersion = Bukkit.getServer().getVersion().split("\\(MC: ")[1].split("\\)")[0];

        this.geyser = GeyserImpl.load(this);
    }

    public void onEnable() {
        // Disabling the plugin in onLoad() is not supported; we need to manually stop here and disable ourselves
        if (geyser == null) {
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }

        // Register commands after Geyser initialization, but before the server starts.
        var sourceConverter = new CommandSourceConverter<>(
                CommandSender.class,
                Bukkit::getPlayer,
                Bukkit::getConsoleSender,
                SpigotCommandSource::new
        );
        LegacyPaperCommandManager<GeyserCommandSource> cloud;
        try {
            // LegacyPaperCommandManager works for spigot too, see https://cloud.incendo.org/minecraft/paper
            cloud = new LegacyPaperCommandManager<>(
                    plugin,
                    ExecutionCoordinator.simpleCoordinator(),
                    sourceConverter
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            // Commodore brigadier on Spigot/Paper 1.13 - 1.18.2
            // Paper-only brigadier on 1.19+
            cloud.registerBrigadier();
        } catch (BukkitCommandManager.BrigadierInitializationException e) {
            geyserLogger.debug("Failed to initialize Brigadier support: " + e.getMessage());
        }

        this.commandRegistry = new SpigotCommandRegistry(geyser, cloud);
    }

    public void onServerLoaded(boolean reloading) {
        if (geyser == null) {
            return;
        }
        if (reloading) {
            geyser.setShuttingDown(false);
        }
        onGeyserEnable();
    }

    public void onGeyserEnable() {
        // Configs are loaded once early - so we can create the logger, then load extensions and finally register
        // extension commands in #onEnable. To ensure reloading geyser also reloads the geyser config, this exists
        if (GeyserImpl.getInstance().isReloading()) {
            geyserConfig = loadConfig(GeyserPluginConfig.class);
            if (geyserConfig == null) {
                Bukkit.getPluginManager().disablePlugin(plugin);
                return;
            }
        }

        GeyserImpl.start();

        if (!geyserConfig.motd().integratedPingPassthrough()) {
            this.geyserSpigotPingPassthrough = GeyserLegacyPingPassthrough.init(geyser);
        } else {
            if (ReflectedNames.checkPaperPingEvent()) {
                this.geyserSpigotPingPassthrough = new GeyserPaperPingPassthrough(geyserLogger);
            } else if (ReflectedNames.newSpigotPingConstructorExists()) {
                this.geyserSpigotPingPassthrough = new GeyserSpigotPingPassthrough(geyserLogger);
            } else {
                // Can't enable one of the other options
                this.geyserSpigotPingPassthrough = GeyserLegacyPingPassthrough.init(geyser);
            }
        }
        geyserLogger.debug("Spigot ping passthrough type: " + (this.geyserSpigotPingPassthrough == null ? null : this.geyserSpigotPingPassthrough.getClass()));

        // Don't need to re-create the world manager/reinject when reloading
        if (GeyserImpl.getInstance().isReloading()) {
            return;
        }

        boolean isViaVersion = Bukkit.getPluginManager().getPlugin("ViaVersion") != null;

        // Check to ensure the current setup can support the protocol version Geyser uses
        GeyserSpigotVersionChecker.checkForSupportedProtocol(geyserLogger, isViaVersion);

        // We want to do this late in the server startup process to allow plugins such as ViaVersion and ProtocolLib
        // To do their job injecting, then connect into *that*
        this.geyserInjector = new GeyserSpigotInjector(host);
        this.geyserInjector.initializeLocalChannel(this);

        if (Boolean.parseBoolean(System.getProperty("Geyser.UseDirectAdapters", "true"))) {
            try {
                host.initializeWorldAdapter();

                if (isViaVersion && isViaVersionNeeded()) {
                    this.geyserWorldManager = new GeyserSpigotLegacyNativeWorldManager(this, host);
                } else {
                    // No ViaVersion
                    this.geyserWorldManager = new GeyserSpigotNativeWorldManager(plugin, host);
                }
                geyserLogger.debug("Using world manager of type: " + this.geyserWorldManager.getClass().getSimpleName());
            } catch (Throwable e) {
                if (geyserConfig.debugMode()) {
                    geyserLogger.debug("Error while attempting to find NMS adapter. Most likely, this can be safely ignored. :)");
                    e.printStackTrace();
                }
            }
        } else {
            geyserLogger.debug("Not using NMS adapter as it is disabled via system property.");
        }

        if (this.geyserWorldManager == null) {
            // No NMS adapter
            this.geyserWorldManager = new GeyserSpigotWorldManager(plugin, host);
            geyserLogger.debug("Using default world manager.");
        }

        // Register permissions so they appear in, for example, LuckPerms' UI
        // Re-registering permissions without removing it throws an error
        PluginManager pluginManager = Bukkit.getPluginManager();
        geyser.eventBus().fire((GeyserRegisterPermissionsEvent) (permission, def) -> {
            Objects.requireNonNull(permission, "permission");
            Objects.requireNonNull(def, "permission default for " + permission);

            if (permission.isBlank()) {
                return;
            }
            PermissionDefault permissionDefault = switch (def) {
                case TRUE -> PermissionDefault.TRUE;
                case FALSE -> PermissionDefault.FALSE;
                case NOT_SET -> PermissionDefault.OP;
            };

            Permission existingPermission = pluginManager.getPermission(permission);
            if (existingPermission != null) {
                geyserLogger.debug("permission " + permission + " with default " +
                        existingPermission.getDefault() + " is being overridden by " + permissionDefault);

                pluginManager.removePermission(permission);
            }

            pluginManager.addPermission(new Permission(permission, permissionDefault));
        });

        // Events cannot be unregistered - re-registering results in duplicate firings
        GeyserSpigotBlockPlaceListener blockPlaceListener = new GeyserSpigotBlockPlaceListener(geyser, this.geyserWorldManager);
        pluginManager.registerEvents(blockPlaceListener, plugin);

        pluginManager.registerEvents(new GeyserPistonListener(geyser, this.geyserWorldManager), plugin);

        pluginManager.registerEvents(new GeyserSpigotUpdateListener(), plugin);
    }

    @Override
    public void onGeyserDisable() {
        if (geyser != null) {
            geyser.disable();
        }
    }

    @Override
    public void onGeyserShutdown() {
        if (geyser != null) {
            geyser.shutdown();
            geyser = null;
        }
        if (geyserInjector != null) {
            geyserInjector.shutdown();
        }
    }

    @Override
    public @NonNull PlatformType platformType() {
        return PlatformType.SPIGOT;
    }

    @Override
    public GeyserPluginConfig config() {
        return this.geyserConfig;
    }

    @Override
    public GeyserSpigotLogger getGeyserLogger() {
        return geyserLogger;
    }

    @Override
    public CommandRegistry getCommandRegistry() {
        return this.commandRegistry;
    }

    @Override
    public IGeyserPingPassthrough getGeyserPingPassthrough() {
        return geyserSpigotPingPassthrough;
    }

    @Override
    public WorldManager getWorldManager() {
        return this.geyserWorldManager;
    }

    @Override
    public Path getConfigFolder() {
        return plugin.getDataFolder().toPath();
    }

    @Override
    public BootstrapDumpInfo getDumpInfo() {
        return new GeyserSpigotDumpInfo();
    }

    @Override
    public String getMinecraftServerVersion() {
        return this.minecraftVersion;
    }

    @Override
    public @NonNull String getServerPlatform() {
        return Bukkit.getName();
    }

    @Override
    public SocketAddress getSocketAddress() {
        return this.geyserInjector == null ? null : this.geyserInjector.getServerSocketAddress();
    }

    /**
     * @return the server version before ViaVersion finishes initializing
     */
    public int getServerProtocolVersion() {
        return ProtocolVersion.getClosest(this.minecraftVersion).getVersion();
    }

    /**
     * This function should not run unless ViaVersion is installed on the server.
     *
     * @return true if there is any block mappings difference between the server and client.
     */
    private boolean isViaVersionNeeded() {
        int serverVersion = getServerProtocolVersion();
        List<ProtocolPathEntry> protocolList = Via.getManager().getProtocolManager().getProtocolPath(GameProtocol.getJavaProtocolVersion(),
                serverVersion);
        if (protocolList == null) {
            // No translation needed!
            return false;
        }
        for (int i = protocolList.size() - 1; i >= 0; i--) {
            MappingData mappingData = protocolList.get(i).protocol().getMappingData();
            if (mappingData != null) {
                return true;
            }
        }
        // All mapping data is null, which means client and server block states are the same
        return false;
    }

    @NonNull
    @Override
    public String getServerBindAddress() {
        return Bukkit.getIp();
    }

    @Override
    public int getServerPort() {
        return Bukkit.getPort();
    }

    @Override
    public boolean testFloodgatePluginPresent() {
        return Bukkit.getPluginManager().getPlugin("floodgate") != null;
    }

    @Override
    public Path getFloodgateKeyPath() {
        Plugin floodgate = Bukkit.getPluginManager().getPlugin("floodgate");
        Path geyserDataFolder = plugin.getDataFolder().toPath();
        Path floodgateDataFolder = floodgate != null ? floodgate.getDataFolder().toPath() : null;

        return FloodgateKeyLoader.getKeyPath(geyserConfig, floodgateDataFolder, geyserDataFolder, geyserLogger);
    }

    @Override
    public MetricsPlatform createMetricsPlatform() {
        return new SpigotMetrics(plugin);
    }

    private void warnInvalidProxySetups(String platform) {
        geyserLogger.error("*********************************************");
        geyserLogger.error("");
        geyserLogger.error(GeyserLocale.getLocaleStringLog("geyser.bootstrap.unsupported_proxy_backend", platform));
        geyserLogger.error(GeyserLocale.getLocaleStringLog("geyser.bootstrap.setup_guide", "https://geysermc.org/wiki/geyser/setup/"));
        geyserLogger.error("");
        geyserLogger.error("*********************************************");
    }
}

/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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
package org.geysermc.geyser.platform.viaproxy;

import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.protocoltranslator.viaproxy.ViaProxyConfig;
import org.apache.logging.log4j.LogManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.network.AuthType;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.command.CommandRegistry;
import org.geysermc.geyser.command.standalone.StandaloneCloudCommandManager;
import org.geysermc.geyser.configuration.ConfigLoader;
import org.geysermc.geyser.configuration.GeyserConfig;
import org.geysermc.geyser.configuration.GeyserPluginConfig;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.ping.GeyserLegacyPingPassthrough;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.geysermc.geyser.platform.viaproxy.listener.GeyserServerTransferListener;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class GeyserViaProxyPlugin implements GeyserBootstrap, EventRegistrar {

    private static final File ROOT_FOLDER = new File(PluginManager.PLUGINS_DIR, "Geyser");

    private final GeyserViaProxyLogger logger = new GeyserViaProxyLogger(LogManager.getLogger("Geyser"));
    private GeyserPluginConfig geyserConfig;
    private GeyserImpl geyser;
    private StandaloneCloudCommandManager cloud;
    private CommandRegistry commandRegistry;
    private IGeyserPingPassthrough pingPassthrough;

    public void onViaProxyLoaded() {
        ROOT_FOLDER.mkdirs();
        GeyserLocale.init(this);
        this.onGeyserInitialize();
    }

    public boolean runConsoleCommand(String command, String[] args) {
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        CommandRegistry registry = this.getCommandRegistry();
        if (registry != null && registry.rootCommands().contains(command)) {
            registry.runCommand(this.getGeyserLogger(), command + " " + String.join(" ", args));
            return true;
        }
        return false;
    }

    public boolean isBedrockPlayer(UUID uuid, String name) {
        return geyser != null && uuid != null && geyser.onlineConnections().stream()
            .anyMatch(session -> uuid.equals(session.javaUuid()) && name.equals(session.javaUsername()));
    }

    public SocketAddress realAddress(SocketAddress localAddress) {
        if (geyser == null) {
            return null;
        }
        return geyser.onlineConnections().stream()
            .filter(session -> session.getDownstream() != null)
            .filter(session -> session.getDownstream().getSession().channel() != null
                && localAddress.equals(session.getDownstream().getSession().channel().localAddress()))
            .map(GeyserSession::getSocketAddress)
            .findAny().orElse(null);
    }

    @Override
    public void onGeyserInitialize() {
        geyserConfig = loadConfig(GeyserPluginConfig.class);
        if (geyserConfig == null) {
            return;
        }

        this.geyser = GeyserImpl.load(this);
        this.geyser.eventBus().register(this, new GeyserServerTransferListener());
    }

    @Override
    public void onGeyserEnable() {
        // If e.g. the config failed to load, GeyserImpl was not loaded and we cannot start
        if (geyser == null) {
            return;
        }
        boolean reloading = geyser.isReloading();
        if (reloading) {
            geyserConfig = loadConfig(GeyserPluginConfig.class);
            if (geyserConfig == null) {
                return;
            }
        } else if (this.cloud == null) {
            // Only initialized once - documented in the Geyser-Standalone bootstrap
            this.cloud = new StandaloneCloudCommandManager(geyser);
            this.commandRegistry = new CommandRegistry(geyser, cloud);
        }

        GeyserImpl.start();

        if (!reloading) {
            // Event must be fired after CommandRegistry has subscribed its listener.
            // Also, the subscription for the Permissions class is created when Geyser is initialized (by GeyserImpl#start)
            this.cloud.fireRegisterPermissionsEvent();
        }

        if (ViaProxy.getConfig().getTargetVersion() != null && ViaProxy.getConfig().getTargetVersion().newerThanOrEqualTo(LegacyProtocolVersion.b1_8tob1_8_1)) {
            // Only initialize the ping passthrough if the protocol version is above beta 1.7.3, as that's when the status protocol was added
            this.pingPassthrough = GeyserLegacyPingPassthrough.init(this.geyser);
        }
        if (this.geyserConfig.java().authType() == AuthType.FLOODGATE) {
            ViaProxy.getConfig().setPassthroughBungeecordPlayerInfo(true);
        }
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
    }

    @Override
    public @NonNull PlatformType platformType() {
        return PlatformType.VIAPROXY;
    }

    @Override
    public GeyserPluginConfig config() {
        return this.geyserConfig;
    }

    @Override
    public GeyserLogger getGeyserLogger() {
        return this.logger;
    }

    @Override
    public CommandRegistry getCommandRegistry() {
        return this.commandRegistry;
    }

    @Override
    public IGeyserPingPassthrough getGeyserPingPassthrough() {
        return this.pingPassthrough;
    }

    @Override
    public Path getConfigFolder() {
        return ROOT_FOLDER.toPath();
    }

    @Override
    public BootstrapDumpInfo getDumpInfo() {
        return new GeyserViaProxyDumpInfo();
    }

    @Override
    public @NonNull String getServerPlatform() {
        return PlatformType.VIAPROXY.platformName();
    }

    @NonNull
    @Override
    public String getServerBindAddress() {
        if (ViaProxy.getConfig().getBindAddress() instanceof InetSocketAddress socketAddress) {
            return socketAddress.getHostString();
        } else {
            throw new IllegalStateException("Unsupported bind address type: " + ViaProxy.getConfig().getBindAddress().getClass().getName());
        }
    }

    @Override
    public int getServerPort() {
        if (ViaProxy.getConfig().getBindAddress() instanceof InetSocketAddress socketAddress) {
            return socketAddress.getPort();
        } else {
            throw new IllegalStateException("Unsupported bind address type: " + ViaProxy.getConfig().getBindAddress().getClass().getName());
        }
    }

    @Override
    public boolean testFloodgatePluginPresent() {
        return false;
    }

    @Override
    public Path getFloodgateKeyPath() {
        return new File(ROOT_FOLDER, geyserConfig.advanced().floodgateKeyFile()).toPath();
    }

    @Override
    public <T extends GeyserConfig> T loadConfig(Class<T> configClass) {
        T config = new ConfigLoader(this)
            .transformer(node -> {
                try {
                    if (!ViaProxy.getConfig().getWildcardDomainHandling().equals(ViaProxyConfig.WildcardDomainHandling.NONE)) {
                        node.node("java", "forward-host").set(true);
                    }

                    var pingPassthroughInterval = node.node("ping-passthrough-interval");
                    int interval = pingPassthroughInterval.getInt();
                    if (interval < 15 && ViaProxy.getConfig().getTargetVersion() != null && ViaProxy.getConfig().getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_6_4)) {
                        // <= 1.6.4 servers sometimes block incoming connections from an IP address if too many connections are made
                        pingPassthroughInterval.set(15);
                    }
                } catch (SerializationException e) {
                    throw new RuntimeException(e);
                }
            })
            .configFile(new File(ROOT_FOLDER, "config.yml"))
            .load(configClass);
        if (config != null) {
            this.geyserConfig = (GeyserPluginConfig) config;
            config.java().authType(Files.isRegularFile(getFloodgateKeyPath()) ? AuthType.FLOODGATE : AuthType.OFFLINE);
        }
        return config;
    }
}

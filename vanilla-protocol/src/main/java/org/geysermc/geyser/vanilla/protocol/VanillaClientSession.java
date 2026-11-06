/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.services.MinecraftServicesDiscoveryService;
import io.netty.channel.ChannelFuture;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.network.protocol.configuration.ClientboundSelectKnownPacks;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ServerboundSelectKnownPacks;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.util.Crypt;

import javax.crypto.Cipher;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/** A headless upstream client that uses vanilla's connection, encryption, compression, and packets. */
public final class VanillaClientSession {
    private final Connection connection;
    private final VanillaPacketCodec codec = new VanillaPacketCodec();
    private final VanillaRegistryContext registries = new VanillaRegistryContext();
    private final Executor executor;
    private final Consumer<Packet<?>> packets;
    private final Consumer<DisconnectionDetails> disconnected;
    private final Consumer<Throwable> errors;
    private final GameProtocols.Context gameContext;
    private GameProfile profile;
    private String accessToken;
    private boolean automaticKeepAlive = true;
    private boolean connecting;
    private boolean closed;
    private boolean disconnectNotified;
    private Channel channel;
    private boolean memoryConnection;

    public VanillaClientSession(GameProfile profile, String accessToken, Executor executor,
                                GameProtocols.Context gameContext, Consumer<Packet<?>> packets,
                                Consumer<DisconnectionDetails> disconnected, Consumer<Throwable> errors) {
        this.profile = Objects.requireNonNull(profile);
        this.accessToken = accessToken;
        this.executor = Objects.requireNonNull(executor);
        this.gameContext = Objects.requireNonNull(gameContext);
        this.packets = Objects.requireNonNull(packets);
        this.disconnected = Objects.requireNonNull(disconnected);
        this.errors = Objects.requireNonNull(errors);
        connection = new Connection(PacketFlow.CLIENTBOUND);
    }

    /** The handshake hostname can include Floodgate forwarding data independently of the TCP target. */
    public ChannelFuture connect(InetSocketAddress target, String hostname, int port, boolean transferring) {
        return connect(VanillaNetwork.tcpBootstrap(), target,
            hostname, port, transferring, ignored -> {}, ignored -> {});
    }

    /** Connects through a platform's local channel or a TCP bootstrap with forwarding handlers. */
    public ChannelFuture connect(Bootstrap bootstrap, SocketAddress target, String hostname, int port,
                                  boolean transferring, Consumer<Channel> initialize, Consumer<Channel> connected) {
        if (connecting || closed) {
            throw new IllegalStateException("A vanilla session can only connect once");
        }
        connecting = true;
        memoryConnection = !(target instanceof InetSocketAddress);
        var listener = listener(ConnectionProtocol.LOGIN);
        connection.initiateServerboundPlayConnection(hostname, port, LoginProtocols.SERVERBOUND,
            LoginProtocols.CLIENTBOUND, listener, transferring);
        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) {
                initialize.accept(channel);
                channel.pipeline().addLast("timeout", new ReadTimeoutHandler(30));
                Connection.configureSerialization(channel.pipeline(), PacketFlow.CLIENTBOUND, false, null);
                connection.configurePacketHandler(channel.pipeline());
            }
        });
        ChannelFuture future = bootstrap.connect(target);
        channel = future.channel();
        future.addListener(result -> future.channel().eventLoop().execute(() -> executor.execute(() -> {
            if (result.isSuccess()) {
                connected.accept(channel);
                connection.tick();
                send(new ServerboundHelloPacket(profile.name(), profile.id()));
            } else {
                fail(result.cause());
            }
        })));
        return future;
    }

    public void tick() {
        if (closed) {
            return;
        }
        if (connection.isConnected()) {
            connection.tick();
        } else if (connecting) {
            connection.handleDisconnection();
        }
    }

    public void send(Packet<?> packet) {
        if (!closed) {
            connection.send(packet);
        }
    }

    public void disconnect(Component reason) {
        if (disconnectNotified) {
            return;
        }
        closed = true;
        accessToken = null;
        connection.disconnect(reason);
        connection.handleDisconnection();
        if (!connection.isConnected()) {
            notifyDisconnect(new DisconnectionDetails(reason));
        }
    }

    public boolean isConnected() {
        return !closed && connection.isConnected();
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isMemoryConnection() {
        return memoryConnection;
    }

    public Channel channel() {
        return channel;
    }

    public ConnectionProtocol state() {
        return codec.state();
    }

    public GameProfile profile() {
        return profile;
    }

    public VanillaRegistryContext registryContext() {
        return registries;
    }

    public void automaticKeepAlive(boolean enabled) {
        automaticKeepAlive = enabled;
    }

    private VanillaPacketListener listener(ConnectionProtocol phase) {
        return new VanillaPacketListener(phase, this::receiveNetwork,
            this::notifyDisconnect, () -> !closed);
    }

    private void receiveNetwork(Packet<?> packet) {
        if (packet instanceof ClientboundLoginCompressionPacket compression) {
            // This packet is not terminal. Install decompression before Netty decodes the next frame.
            connection.setupCompression(compression.getCompressionThreshold(), true);
            return;
        }
        executor.execute(() -> receive(packet));
    }

    private void notifyDisconnect(DisconnectionDetails details) {
        if (!disconnectNotified) {
            disconnectNotified = true;
            closed = true;
            accessToken = null;
            disconnected.accept(details);
        }
    }

    private void receive(Packet<?> packet) {
        if (closed) {
            return;
        }
        try {
            switch (packet) {
                case ClientboundBundlePacket bundle -> {
                    bundle.subPackets().forEach(this::receive);
                    return;
                }
                case ClientboundHelloPacket hello -> encrypt(hello);
                case ClientboundLoginFinishedPacket finished -> {
                    profile = finished.gameProfile();
                    codec.configuration();
                    setupInbound(codec.clientbound(), ConnectionProtocol.CONFIGURATION);
                    send(ServerboundLoginAcknowledgedPacket.INSTANCE);
                    connection.setupOutboundProtocol(codec.serverbound());
                }
                case ClientboundRegistryDataPacket registry -> registries.accept(registry);
                case ClientboundUpdateTagsPacket tags -> registries.accept(tags);
                case ClientboundSelectKnownPacks ignored -> {
                    // Require complete registry entries so every connection can build its own holders.
                    send(new ServerboundSelectKnownPacks(List.of()));
                }
                case ClientboundFinishConfigurationPacket ignored -> {
                    codec.play(registries.finishConfiguration(), gameContext);
                    setupPlayInbound(codec.clientbound());
                    send(ServerboundFinishConfigurationPacket.INSTANCE);
                    connection.setupOutboundProtocol(codec.serverbound());
                }
                case ClientboundStartConfigurationPacket ignored -> {
                    codec.configuration();
                    setupInbound(codec.clientbound(), ConnectionProtocol.CONFIGURATION);
                    send(ServerboundConfigurationAcknowledgedPacket.INSTANCE);
                    connection.setupOutboundProtocol(codec.serverbound());
                }
                case ClientboundKeepAlivePacket keepAlive when automaticKeepAlive -> send(new ServerboundKeepAlivePacket(keepAlive.getId()));
                case ClientboundPingPacket ping -> send(new ServerboundPongPacket(ping.getId()));
                case ClientboundLoginDisconnectPacket disconnect -> disconnect(disconnect.reason());
                case ClientboundDisconnectPacket disconnect -> disconnect(disconnect.reason());
                default -> {
                }
            }
            packets.accept(packet);
        } catch (Throwable error) {
            fail(error);
        }
    }

    private void setupPlayInbound(ProtocolInfo<?> protocol) {
        setupInbound(protocol, ConnectionProtocol.PLAY);
    }

    private void setupInbound(ProtocolInfo<?> protocol, ConnectionProtocol phase) {
        // The protocol's listener type is erased by the common transport interface.
        @SuppressWarnings("unchecked")
        ProtocolInfo<VanillaPacketListener> configured = (ProtocolInfo<VanillaPacketListener>) protocol;
        connection.setupInboundProtocol(configured, listener(phase));
    }

    private void encrypt(ClientboundHelloPacket hello) throws Exception {
        if (hello.shouldAuthenticate() && accessToken == null) {
            throw new UnexpectedEncryptionException();
        }
        var key = Crypt.generateSecretKey();
        var publicKey = hello.getPublicKey();
        var response = new ServerboundKeyPacket(key, publicKey, hello.getChallenge());
        var decrypt = Crypt.getCipher(Cipher.DECRYPT_MODE, key);
        var encrypt = Crypt.getCipher(Cipher.ENCRYPT_MODE, key);
        String hash = new BigInteger(Crypt.digestData(hello.getServerId(), publicKey, key)).toString(16);
        String token = accessToken;
        Thread.ofVirtual().name("GeyserVanilla server authentication").start(() -> {
            try {
                if (hello.shouldAuthenticate()) {
                    Authentication.SERVICE.joinServer(profile.id(), token, hash);
                }
                executor.execute(() -> {
                    if (!closed) {
                        connection.send(response, future -> {
                            if (future.isSuccess()) {
                                connection.setEncryptionKey(decrypt, encrypt);
                            } else {
                                executor.execute(() -> fail(future.cause()));
                            }
                        });
                    }
                });
            } catch (Exception error) {
                executor.execute(() -> fail(error));
            }
        });
    }

    private void fail(Throwable error) {
        try {
            errors.accept(error);
        } finally {
            disconnect(Component.literal(error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage()));
        }
    }

    private static final class Authentication {
        private static final com.mojang.authlib.minecraft.SessionService SERVICE =
            MinecraftServicesDiscoveryService.create(Proxy.NO_PROXY).createMinecraftSessionService();
    }

    public static final class UnexpectedEncryptionException extends Exception {
        private static final long serialVersionUID = 1L;

        private UnexpectedEncryptionException() {
            super("The Java server requires an authenticated Java account");
        }
    }
}

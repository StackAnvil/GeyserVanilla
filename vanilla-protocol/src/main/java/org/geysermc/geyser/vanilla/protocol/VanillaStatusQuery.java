/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;

import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Makes a status request over the native handshake, status codecs and connection pipeline. */
public final class VanillaStatusQuery {
    private VanillaStatusQuery() {
    }

    public static CompletableFuture<ServerStatus> query(Bootstrap bootstrap, SocketAddress target, String hostname, int port,
                                                         Consumer<Channel> initialize) {
        VanillaBootstrap.initialize();
        var result = new CompletableFuture<ServerStatus>();
        var connection = new Connection(PacketFlow.CLIENTBOUND);
        var listener = new VanillaPacketListener(ConnectionProtocol.STATUS, packet -> {
            if (packet instanceof ClientboundStatusResponsePacket response) {
                result.complete(response.status());
            }
        }, details -> result.completeExceptionally(new IllegalStateException(details.reason().getString())), () -> !result.isDone());
        connection.initiateServerboundStatusConnection(hostname, port, listener);
        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) {
                initialize.accept(channel);
                channel.pipeline().addLast("timeout", new ReadTimeoutHandler(10));
                Connection.configureSerialization(channel.pipeline(), PacketFlow.CLIENTBOUND, false, null);
                connection.configurePacketHandler(channel.pipeline());
            }
        });
        var connect = bootstrap.connect(target);
        result.whenComplete((status, failure) -> connect.channel().close());
        connect.channel().closeFuture().addListener(ignored -> result.completeExceptionally(new ClosedChannelException()));
        connect.addListener(future -> {
            if (future.isSuccess()) {
                connect.channel().eventLoop().execute(() -> {
                    connection.tick();
                    connection.send(ServerboundStatusRequestPacket.INSTANCE);
                });
            } else {
                result.completeExceptionally(future.cause());
            }
        });
        return result;
    }
}

/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.host;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.local.LocalAddress;
import org.geysermc.geyser.vanilla.runtime.VanillaLocalTransport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(15)
class HostLocalConnectorTest {
    @Test
    void forwardsBytesAndTheClientIpThroughTheHostLocalChannel() throws Exception {
        var group = new DefaultEventLoopGroup(1);
        var remoteAddress = new CompletableFuture<java.net.SocketAddress>();
        var serverChannel = new ServerBootstrap().group(group).channel(LocalServerChannelWrapper.class)
            .childHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel channel) {
                    remoteAddress.complete(channel.remoteAddress());
                    channel.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>(ByteBuf.class) {
                        @Override
                        protected void channelRead0(ChannelHandlerContext context, ByteBuf bytes) {
                            context.writeAndFlush(bytes.retain());
                        }
                    });
                }
            }).bind(LocalAddress.ANY).sync().channel();
        try (var connector = new HostLocalConnector(serverChannel.localAddress())) {
            var bytes = new CompletableFuture<byte[]>();
            var closed = new CompletableFuture<Void>();
            var acknowledgement = new CompletableFuture<Void>();
            var client = new InetSocketAddress("192.0.2.43", 54321);
            var connection = connector.connect(client, new VanillaLocalTransport.Receiver() {
                @Override
                public CompletableFuture<Void> receive(byte[] message) {
                    bytes.complete(message);
                    return acknowledgement;
                }

                @Override
                public void closed(Throwable failure) {
                    if (failure == null) {
                        closed.complete(null);
                    } else {
                        closed.completeExceptionally(failure);
                    }
                }
            }).toCompletableFuture().get(5, TimeUnit.SECONDS);
            assertEquals(client, remoteAddress.get(5, TimeUnit.SECONDS));
            connection.write(new byte[] {1, 3, 5, 7}).toCompletableFuture().get(5, TimeUnit.SECONDS);
            assertArrayEquals(new byte[] {1, 3, 5, 7}, bytes.get(5, TimeUnit.SECONDS));
            acknowledgement.complete(null);
            connection.close();
            closed.get(5, TimeUnit.SECONDS);
            var failure = assertThrows(java.util.concurrent.ExecutionException.class,
                () -> connection.write(new byte[] {0}).toCompletableFuture().get(5, TimeUnit.SECONDS));
            assertInstanceOf(java.nio.channels.ClosedChannelException.class, failure.getCause());
        } finally {
            serverChannel.close().sync();
            group.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
        }
    }
}

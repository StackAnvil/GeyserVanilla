/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.host;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.geysermc.geyser.vanilla.runtime.VanillaLocalTransport;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** Host-side connector. This class must use the host's Netty classes, never the client's copies. */
public final class HostLocalConnector implements VanillaLocalTransport, AutoCloseable {
    private final SocketAddress target;
    private final EventLoopGroup group = new DefaultEventLoopGroup(0, new DefaultThreadFactory("Geyser host bridge", true));
    private final Set<Channel> channels = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    public HostLocalConnector(SocketAddress target) {
        this.target = target;
    }

    @Override
    public CompletionStage<Connection> connect(InetSocketAddress clientAddress, Receiver receiver) {
        if (closed) {
            return CompletableFuture.failedFuture(new ClosedChannelException());
        }
        var result = new CompletableFuture<Connection>();
        var connect = new Bootstrap().group(group).channel(LocalChannelWithRemoteAddress.class)
            .handler(new ChannelInitializer<LocalChannelWithRemoteAddress>() {
                @Override
                protected void initChannel(LocalChannelWithRemoteAddress channel) {
                    channel.spoofedRemoteAddress(clientAddress);
                    channel.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>(ByteBuf.class) {
                        private int pendingReads;

                        @Override
                        protected void channelRead0(ChannelHandlerContext context, ByteBuf message) {
                            pendingReads++;
                            channel.config().setAutoRead(false);
                            receiver.receive(ByteBufUtil.getBytes(message)).whenComplete((ignored, failure) ->
                                channel.eventLoop().execute(() -> {
                                    pendingReads--;
                                    if (failure != null) {
                                        receiver.closed(failure);
                                        channel.close();
                                    } else if (pendingReads == 0) {
                                        channel.config().setAutoRead(true);
                                    }
                                }));
                        }

                        @Override
                        public void channelInactive(ChannelHandlerContext context) {
                            receiver.closed(null);
                        }

                        @Override
                        public void exceptionCaught(ChannelHandlerContext context, Throwable failure) {
                            receiver.closed(failure);
                            context.close();
                        }
                    });
                }
            }).connect(target);
        Channel channel = connect.channel();
        channels.add(channel);
        channel.closeFuture().addListener(ignored -> channels.remove(channel));
        if (closed) {
            channel.close();
        }
        connect.addListener(future -> {
            if (!future.isSuccess()) {
                result.completeExceptionally(future.cause());
                channel.close();
                return;
            }
            result.complete(new Connection() {
                @Override
                public CompletionStage<Void> write(byte[] bytes) {
                    var written = new CompletableFuture<Void>();
                    channel.writeAndFlush(Unpooled.wrappedBuffer(bytes)).addListener(write -> {
                        if (write.isSuccess()) {
                            written.complete(null);
                        } else {
                            written.completeExceptionally(write.cause());
                        }
                    });
                    return written;
                }

                @Override
                public void close() {
                    channel.close();
                }
            });
        });
        return result;
    }

    @Override
    public void close() {
        closed = true;
        channels.forEach(Channel::close);
        group.shutdownGracefully(0, 5, TimeUnit.SECONDS).syncUninterruptibly();
    }
}

/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.host.fixture;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.local.LocalAddress;
import org.geysermc.geyser.vanilla.host.HostLocalConnector;
import org.geysermc.geyser.vanilla.host.LocalServerChannelWrapper;
import org.geysermc.geyser.vanilla.runtime.VanillaLocalTransport;

import java.io.ByteArrayOutputStream;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Test fixture that can run under a different Netty version from the native protocol tests. */
public final class HostStatusServer implements AutoCloseable {
    private final DefaultEventLoopGroup group = new DefaultEventLoopGroup(1);
    private final Channel server;
    private final HostLocalConnector connector;
    private final CompletableFuture<byte[]> request = new CompletableFuture<>();
    private final CompletableFuture<SocketAddress> clientAddress = new CompletableFuture<>();
    private final CompletableFuture<Void> disconnected = new CompletableFuture<>();

    public HostStatusServer(byte[] response) {
        server = new ServerBootstrap().group(group).channel(LocalServerChannelWrapper.class)
            .childHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel channel) {
                    clientAddress.complete(channel.remoteAddress());
                    channel.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>(ByteBuf.class) {
                        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

                        @Override
                        protected void channelRead0(ChannelHandlerContext context, ByteBuf message) {
                            bytes.writeBytes(ByteBufUtil.getBytes(message));
                            byte[] accumulated = bytes.toByteArray();
                            if (!request.isDone() && hasTwoFrames(accumulated)) {
                                request.complete(accumulated);
                                context.writeAndFlush(Unpooled.wrappedBuffer(response));
                            }
                        }

                        @Override
                        public void channelInactive(ChannelHandlerContext context) {
                            disconnected.complete(null);
                        }
                    });
                }
            }).bind(LocalAddress.ANY).syncUninterruptibly().channel();
        connector = new HostLocalConnector(server.localAddress());
    }

    private static boolean hasTwoFrames(byte[] bytes) {
        int offset = 0;
        for (int frame = 0; frame < 2; frame++) {
            int size = 0;
            int shift = 0;
            int next;
            do {
                if (offset >= bytes.length) {
                    return false;
                }
                next = Byte.toUnsignedInt(bytes[offset++]);
                size |= (next & 0x7f) << shift;
                shift += 7;
                if (shift > 35) {
                    throw new IllegalArgumentException("Invalid fixture frame length");
                }
            } while ((next & 0x80) != 0);
            if (size > bytes.length - offset) {
                return false;
            }
            offset += size;
        }
        return true;
    }

    public VanillaLocalTransport transport() {
        return connector;
    }

    public CompletableFuture<byte[]> request() {
        return request;
    }

    public CompletableFuture<SocketAddress> clientAddress() {
        return clientAddress;
    }

    public CompletableFuture<Void> disconnected() {
        return disconnected;
    }

    public String nettyLocation() {
        return Channel.class.getProtectionDomain().getCodeSource().getLocation().toString();
    }

    @Override
    public void close() {
        connector.close();
        server.close().syncUninterruptibly();
        group.shutdownGracefully(0, 5, TimeUnit.SECONDS).syncUninterruptibly();
    }
}

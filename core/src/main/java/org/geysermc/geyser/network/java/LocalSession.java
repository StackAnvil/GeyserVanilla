/*
 * Copyright (c) 2019-2026 GeyserMC and 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.network.java;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.local.LocalIoHandler;
import io.netty.channel.unix.PreferredDirectByteBufAllocator;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.geysermc.geyser.vanilla.protocol.VanillaClientSession;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/** Connects vanilla's serialized protocol to the platform's injected local channel. */
public final class LocalSession {
    private static EventLoopGroup group;
    private static PreferredDirectByteBufAllocator allocator;

    private LocalSession() {
    }

    public static void connect(VanillaClientSession session, SocketAddress target, String clientIp,
                               String hostname, int port, boolean transferring, Runnable connected) {
        Bootstrap bootstrap = bootstrap(target, clientIp);
        session.connect(bootstrap, target, hostname, port, transferring,
            channel -> {
                if (channel instanceof LocalChannelWithRemoteAddress local) {
                    local.spoofedRemoteAddress(new InetSocketAddress(clientIp, 0));
                }
            },
            channel -> connected.run());
    }

    public static java.util.concurrent.CompletableFuture<net.minecraft.network.protocol.status.ServerStatus> connectStatus(
        SocketAddress target, String clientIp, String hostname, int port) {
        return org.geysermc.geyser.vanilla.protocol.VanillaStatusQuery.query(bootstrap(target, clientIp), target, hostname, port,
            channel -> {
                if (channel instanceof LocalChannelWithRemoteAddress local) {
                    local.spoofedRemoteAddress(new InetSocketAddress(clientIp, 0));
                }
            });
    }

    private static Bootstrap bootstrap(SocketAddress target, String clientIp) {
        if (target instanceof org.geysermc.geyser.vanilla.runtime.VanillaLocalTransport.Address) {
            return org.geysermc.geyser.vanilla.protocol.VanillaNetwork.hostedBootstrap(new InetSocketAddress(clientIp, 0));
        }
        Bootstrap bootstrap = new Bootstrap().group(eventLoopGroup()).channel(LocalChannelWithRemoteAddress.class);
        if (allocator != null) {
            bootstrap.option(ChannelOption.ALLOCATOR, allocator);
        }
        return bootstrap;
    }

    private static synchronized EventLoopGroup eventLoopGroup() {
        if (group == null) {
            group = new MultiThreadIoEventLoopGroup(new DefaultThreadFactory("Geyser local client", true), LocalIoHandler.newFactory());
        }
        return group;
    }

    public static void createDirectByteBufAllocator() {
        if (allocator == null) {
            allocator = new PreferredDirectByteBufAllocator();
            allocator.updateAllocator(ByteBufAllocator.DEFAULT);
        }
    }

    public static synchronized void shutdown() {
        if (group != null) {
            group.shutdownGracefully(0, 5, TimeUnit.SECONDS).syncUninterruptibly();
            group = null;
        }
        allocator = null;
    }
}

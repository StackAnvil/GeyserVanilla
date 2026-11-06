/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** Owns upstream TCP workers so unloading Geyser never shuts down a host's native worker group. */
public final class VanillaNetwork {
    private static EventLoopGroup group;
    private static EventLoopGroup hostedGroup;
    private static final Set<Channel> HOSTED_CHANNELS = ConcurrentHashMap.newKeySet();

    private VanillaNetwork() {
    }

    public static synchronized Bootstrap tcpBootstrap() {
        if (group == null) {
            group = new MultiThreadIoEventLoopGroup(new DefaultThreadFactory("Geyser vanilla TCP", true), NioIoHandler.newFactory());
        }
        return new Bootstrap().group(group).channel(NioSocketChannel.class);
    }

    public static synchronized Bootstrap hostedBootstrap(InetSocketAddress clientAddress) {
        if (hostedGroup == null) {
            hostedGroup = new DefaultEventLoopGroup(0, new DefaultThreadFactory("Geyser hosted client", true));
        }
        EventLoopGroup owner = hostedGroup;
        return new Bootstrap().group(owner).channelFactory(() -> createHostedChannel(owner, clientAddress));
    }

    private static synchronized Channel createHostedChannel(EventLoopGroup owner, InetSocketAddress clientAddress) {
        if (owner != hostedGroup) {
            throw new IllegalStateException("The hosted connection workers have stopped");
        }
        var channel = new VanillaHostChannel(clientAddress);
        HOSTED_CHANNELS.add(channel);
        channel.closeFuture().addListener(ignored -> HOSTED_CHANNELS.remove(channel));
        return channel;
    }

    public static void shutdown() {
        EventLoopGroup closing;
        EventLoopGroup closingHosted;
        List<Channel> closingHostedChannels;
        synchronized (VanillaNetwork.class) {
            closing = group;
            group = null;
            closingHosted = hostedGroup;
            hostedGroup = null;
            closingHostedChannels = List.copyOf(HOSTED_CHANNELS);
        }
        if (closing != null) {
            closing.shutdownGracefully(0, 5, TimeUnit.SECONDS).syncUninterruptibly();
        }
        if (closingHosted != null) {
            closingHostedChannels.forEach(channel -> channel.close().syncUninterruptibly());
            closingHosted.shutdownGracefully(0, 5, TimeUnit.SECONDS).syncUninterruptibly();
        }
    }
}

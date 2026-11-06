/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.TimeUnit;

/** Owns upstream TCP workers so unloading Geyser never shuts down a host's native worker group. */
public final class VanillaNetwork {
    private static EventLoopGroup group;

    private VanillaNetwork() {
    }

    public static synchronized Bootstrap tcpBootstrap() {
        if (group == null) {
            group = new MultiThreadIoEventLoopGroup(new DefaultThreadFactory("Geyser vanilla TCP", true), NioIoHandler.newFactory());
        }
        return new Bootstrap().group(group).channel(NioSocketChannel.class);
    }

    public static void shutdown() {
        EventLoopGroup closing;
        synchronized (VanillaNetwork.class) {
            closing = group;
            group = null;
        }
        if (closing != null) {
            closing.shutdownGracefully(0, 5, TimeUnit.SECONDS).syncUninterruptibly();
        }
    }
}

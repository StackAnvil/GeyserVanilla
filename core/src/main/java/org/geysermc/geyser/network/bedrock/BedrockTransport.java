/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.network.bedrock;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringDatagramChannel;
import io.netty.channel.uring.IoUringIoHandler;

import java.util.concurrent.ThreadFactory;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/** UDP transport selection for Bedrock's RakNet and signaling listeners. */
public enum BedrockTransport {
    NIO(NioDatagramChannel.class, NioIoHandler::newFactory),
    EPOLL(EpollDatagramChannel.class, EpollIoHandler::newFactory),
    KQUEUE(KQueueDatagramChannel.class, KQueueIoHandler::newFactory),
    IO_URING(IoUringDatagramChannel.class, IoUringIoHandler::newFactory);

    public static final BedrockTransport PREFERRED = select();

    private final Class<? extends DatagramChannel> datagramChannelClass;
    private final Supplier<IoHandlerFactory> ioHandlerFactory;

    BedrockTransport(Class<? extends DatagramChannel> datagramChannelClass, Supplier<IoHandlerFactory> ioHandlerFactory) {
        this.datagramChannelClass = datagramChannelClass;
        this.ioHandlerFactory = ioHandlerFactory;
    }

    public Class<? extends DatagramChannel> datagramChannelClass() {
        return datagramChannelClass;
    }

    public BiFunction<Integer, ThreadFactory, EventLoopGroup> eventLoopGroupFactory() {
        return (threads, factory) -> new MultiThreadIoEventLoopGroup(threads, factory, ioHandlerFactory.get());
    }

    private static BedrockTransport select() {
        if (Boolean.getBoolean("disableNativeEventLoop")) {
            return NIO;
        }
        // Keep the previous JVM flag working for existing installations.
        if ((Boolean.getBoolean("Geyser.io_uring") || Boolean.getBoolean("Mcpl.io_uring")) && IoUring.isAvailable()) {
            return IO_URING;
        }
        if (Epoll.isAvailable()) {
            return EPOLL;
        }
        if (KQueue.isAvailable()) {
            return KQUEUE;
        }
        return NIO;
    }
}

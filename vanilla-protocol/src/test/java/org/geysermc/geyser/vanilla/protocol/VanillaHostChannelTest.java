/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.geysermc.geyser.vanilla.runtime.VanillaLocalTransport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(15)
class VanillaHostChannelTest {
    @AfterEach
    void stop() {
        VanillaNetwork.shutdown();
    }

    @Test
    void preservesBytesAndWaitsForWriteAcknowledgement() throws Exception {
        var writes = new LinkedBlockingQueue<byte[]>();
        var acknowledgements = new LinkedBlockingQueue<CompletableFuture<Void>>();
        var received = new LinkedBlockingQueue<byte[]>();
        var receiver = new AtomicReference<VanillaLocalTransport.Receiver>();
        var closed = new CompletableFuture<Void>();
        var address = new InetSocketAddress("192.0.2.42", 12345);
        var target = new VanillaLocalTransport.Address((clientAddress, sink) -> {
            assertEquals(address, clientAddress);
            receiver.set(sink);
            return CompletableFuture.completedFuture(new VanillaLocalTransport.Connection() {
                @Override
                public CompletableFuture<Void> write(byte[] bytes) {
                    writes.add(bytes);
                    var done = new CompletableFuture<Void>();
                    acknowledgements.add(done);
                    return done;
                }

                @Override
                public void close() {
                    closed.complete(null);
                }
            });
        });
        Channel channel = VanillaNetwork.hostedBootstrap(address).handler(new SimpleChannelInboundHandler<ByteBuf>(ByteBuf.class) {
            @Override
            protected void channelRead0(ChannelHandlerContext context, ByteBuf bytes) {
                received.add(ByteBufUtil.getBytes(bytes));
            }
        }).connect(target).sync().channel();
        try {
            ByteBuf firstBytes = Unpooled.wrappedBuffer(new byte[] {1, 2, 3});
            var first = channel.writeAndFlush(firstBytes);
            var second = channel.writeAndFlush(Unpooled.wrappedBuffer(new byte[] {4, 5}));
            assertArrayEquals(new byte[] {1, 2, 3}, writes.poll(5, TimeUnit.SECONDS));
            assertFalse(first.isDone());
            assertFalse(second.isDone());
            assertTrue(writes.isEmpty());
            acknowledgements.poll(5, TimeUnit.SECONDS).complete(null);
            first.sync();
            assertEquals(0, firstBytes.refCnt());
            assertArrayEquals(new byte[] {4, 5}, writes.poll(5, TimeUnit.SECONDS));
            acknowledgements.poll(5, TimeUnit.SECONDS).complete(null);
            second.sync();

            channel.eventLoop().submit(() -> channel.config().setAutoRead(false)).sync();
            var delivery = receiver.get().receive(new byte[] {8, 9}).toCompletableFuture();
            channel.eventLoop().submit(() -> {}).sync();
            assertFalse(delivery.isDone());
            assertTrue(received.isEmpty());
            channel.config().setAutoRead(true);
            delivery.get(5, TimeUnit.SECONDS);
            assertArrayEquals(new byte[] {8, 9}, received.poll(5, TimeUnit.SECONDS));
        } finally {
            channel.close().sync();
        }
        closed.get(5, TimeUnit.SECONDS);
        assertTrue(receiver.get().receive(new byte[] {0}).toCompletableFuture().isCompletedExceptionally());
        assertDoesNotThrow(() -> receiver.get().closed(null));
    }

    @Test
    void closesAConnectionThatFinishesAfterTheNativeChannelCloses() throws Exception {
        var connecting = new CompletableFuture<VanillaLocalTransport.Connection>();
        var started = new CompletableFuture<Void>();
        var closed = new CompletableFuture<Void>();
        var target = new VanillaLocalTransport.Address((address, receiver) -> {
            started.complete(null);
            return connecting;
        });
        var future = VanillaNetwork.hostedBootstrap(new InetSocketAddress(0))
            .handler(new io.netty.channel.ChannelInboundHandlerAdapter()).connect(target);
        started.get(5, TimeUnit.SECONDS);
        future.channel().close().sync();
        VanillaNetwork.shutdown();
        connecting.complete(new VanillaLocalTransport.Connection() {
            @Override
            public CompletableFuture<Void> write(byte[] bytes) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public void close() {
                closed.complete(null);
            }
        });
        closed.get(5, TimeUnit.SECONDS);
        assertFalse(future.isSuccess());
    }

    @Test
    void propagatesHostConnectionFailureAndClosesTheNativeChannel() throws Exception {
        var failure = new IOException("host rejected connection");
        var target = new VanillaLocalTransport.Address((address, receiver) -> CompletableFuture.failedFuture(failure));
        var future = VanillaNetwork.hostedBootstrap(new InetSocketAddress(0))
            .handler(new io.netty.channel.ChannelInboundHandlerAdapter()).connect(target);
        assertTrue(future.await(5, TimeUnit.SECONDS));
        assertSame(failure, future.cause());
        future.channel().closeFuture().sync();
        assertFalse(future.channel().isOpen());
    }
}

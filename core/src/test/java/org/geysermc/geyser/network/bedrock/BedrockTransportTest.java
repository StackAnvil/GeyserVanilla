/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.network.bedrock;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedrockTransportTest {
    @Test
    void receivesDatagramsWithNioAndTheSelectedNativeTransport() throws Exception {
        for (var transport : EnumSet.of(BedrockTransport.NIO, BedrockTransport.PREFERRED)) {
            var group = transport.eventLoopGroupFactory().apply(1, new DefaultThreadFactory("bedrock-transport-test", true));
            Channel channel = null;
            try {
                var received = new CompletableFuture<String>();
                channel = new Bootstrap().group(group).channel(transport.datagramChannelClass())
                    .handler(new SimpleChannelInboundHandler<DatagramPacket>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext context, DatagramPacket packet) {
                            received.complete(packet.content().toString(StandardCharsets.UTF_8));
                        }

                        @Override
                        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
                            received.completeExceptionally(cause);
                        }
                    }).bind(new InetSocketAddress("127.0.0.1", 0)).sync().channel();
                channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer("geyser-vanilla-udp", StandardCharsets.UTF_8),
                    (InetSocketAddress) channel.localAddress())).sync();
                assertEquals("geyser-vanilla-udp", received.get(5, TimeUnit.SECONDS));
            } finally {
                if (channel != null) channel.close().sync();
                group.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
            }
        }
    }
}

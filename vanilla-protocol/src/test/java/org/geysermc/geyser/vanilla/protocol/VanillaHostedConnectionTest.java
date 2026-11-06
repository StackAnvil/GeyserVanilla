/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.handshake.HandshakeProtocols;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.minecraft.network.protocol.status.StatusProtocols;
import org.geysermc.geyser.vanilla.runtime.VanillaClassLoader;
import org.geysermc.geyser.vanilla.runtime.VanillaLocalTransport;
import org.geysermc.geyser.vanilla.runtime.VanillaRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class VanillaHostedConnectionTest {
    @Test
    @Timeout(20)
    @SuppressWarnings("unchecked")
    void queriesAHostWithAnIndependentOlderNettyRuntime() throws Exception {
        VanillaBootstrap.initialize();
        var status = new ServerStatus(Component.literal("isolated host status"), Optional.empty(), Optional.empty(), Optional.empty(), false);
        var response = new FriendlyByteBuf(Unpooled.buffer());
        var frame = new FriendlyByteBuf(Unpooled.buffer());
        byte[] bytes;
        try {
            StatusProtocols.CLIENTBOUND.codec().encode(response, new ClientboundStatusResponsePacket(status));
            frame.writeVarInt(response.readableBytes());
            frame.writeBytes(response);
            bytes = io.netty.buffer.ByteBufUtil.getBytes(frame);
        } finally {
            response.release();
            frame.release();
        }
        var classpath = Arrays.stream(System.getProperty("geyser.test.host.classpath").split(File.pathSeparator))
            .map(Path::of).toList();
        try (var hostLoader = new VanillaClassLoader(VanillaRuntime.urls(classpath), getClass().getClassLoader(),
            List.of("org.geysermc.geyser.vanilla.runtime."))) {
            Class<?> fixture = hostLoader.loadClass("org.geysermc.geyser.vanilla.host.fixture.HostStatusServer");
            try (var server = (AutoCloseable) fixture.getConstructor(byte[].class).newInstance((Object) bytes)) {
                assertTrue(((String) fixture.getMethod("nettyLocation").invoke(server)).contains("netty-transport-4.1."));
                assertTrue(io.netty.util.Version.identify().get("netty-transport").artifactVersion().startsWith("4.2."));
                assertNotSame(io.netty.channel.Channel.class, hostLoader.loadClass("io.netty.channel.Channel"));
                var transport = (VanillaLocalTransport) fixture.getMethod("transport").invoke(server);
                var clientAddress = new InetSocketAddress("192.0.2.44", 34567);
                var query = VanillaStatusQuery.query(VanillaNetwork.hostedBootstrap(clientAddress),
                    new VanillaLocalTransport.Address(transport), "hosted.example", 25571, ignored -> {});
                assertEquals(status, query.get(5, TimeUnit.SECONDS));
                assertEquals(clientAddress, ((CompletableFuture<?>) fixture.getMethod("clientAddress").invoke(server)).get(5, TimeUnit.SECONDS));
                byte[] sent = ((CompletableFuture<byte[]>) fixture.getMethod("request").invoke(server)).get(5, TimeUnit.SECONDS);
                var request = new FriendlyByteBuf(Unpooled.wrappedBuffer(sent));
                try {
                    var handshake = assertInstanceOf(ClientIntentionPacket.class,
                        HandshakeProtocols.SERVERBOUND.codec().decode(request.readSlice(request.readVarInt())));
                    assertEquals("hosted.example", handshake.hostName());
                    assertEquals(25571, handshake.port());
                    assertSame(ServerboundStatusRequestPacket.INSTANCE,
                        StatusProtocols.SERVERBOUND.codec().decode(request.readSlice(request.readVarInt())));
                    assertEquals(0, request.readableBytes());
                } finally {
                    request.release();
                }
                ((CompletableFuture<?>) fixture.getMethod("disconnected").invoke(server)).get(5, TimeUnit.SECONDS);
            } finally {
                VanillaNetwork.shutdown();
            }
        }
    }
}

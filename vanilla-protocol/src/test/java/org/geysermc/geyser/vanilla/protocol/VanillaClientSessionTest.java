/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.CompressionDecoder;
import net.minecraft.network.CompressionEncoder;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.handshake.HandshakeProtocols;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.util.Crypt;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Timeout;

import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class VanillaClientSessionTest {
    private static final GameProtocols.Context CONTEXT = new GameProtocols.Context() {
        public boolean hasInfiniteMaterials() {
            return true;
        }

        public boolean canUseCommandBlocks() {
            return true;
        }
    };

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @Timeout(30)
    void connectsAndTransitionsThroughLoginConfigurationAndPlay(boolean encryptedAndCompressed) throws Exception {
        VanillaBootstrap.defaultRegistries();
        var profile = new GameProfile(UUID.randomUUID(), "BedrockPlayer");
        CountDownLatch disconnected = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicReference<Vec3> motion = new AtomicReference<>();
        AtomicReference<String> reason = new AtomicReference<>();
        var worker = java.util.concurrent.Executors.newSingleThreadExecutor();
        var client = new VanillaClientSession(profile, null, worker, CONTEXT, packet -> {
            if (packet instanceof ClientboundSetEntityMotionPacket move) {
                motion.set(move.movement());
            }
        }, details -> {
            reason.set(details.reason().getString());
            disconnected.countDown();
        }, error::set);

        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            CompletableFuture<Void> fixture = CompletableFuture.runAsync(() -> {
                try (var socket = server.accept()) {
                    socket.setSoTimeout(15000);
                    var transport = new FixtureTransport(socket.getInputStream(), socket.getOutputStream());
                    var handshake = assertInstanceOf(ClientIntentionPacket.class, transport.read(HandshakeProtocols.SERVERBOUND));
                    assertEquals("bedrock.example\0forwarded", handshake.hostName());
                    assertEquals(25565, handshake.port());
                    var hello = assertInstanceOf(ServerboundHelloPacket.class, transport.read(LoginProtocols.SERVERBOUND));
                    assertEquals(profile.id(), hello.profileId());
                    assertEquals(profile.name(), hello.name());

                    if (encryptedAndCompressed) {
                        var keys = Crypt.generateKeyPair();
                        byte[] challenge = {1, 2, 3, 4};
                        transport.write(LoginProtocols.CLIENTBOUND, new ClientboundHelloPacket("", keys.getPublic().getEncoded(), challenge, false));
                        var response = assertInstanceOf(ServerboundKeyPacket.class, transport.read(LoginProtocols.SERVERBOUND));
                        assertTrue(response.isChallengeValid(challenge, keys.getPrivate()));
                        var secret = response.getSecretKey(keys.getPrivate());
                        transport.input = new CipherInputStream(transport.input, Crypt.getCipher(Cipher.DECRYPT_MODE, secret));
                        transport.output = new CipherOutputStream(transport.output, Crypt.getCipher(Cipher.ENCRYPT_MODE, secret));
                        transport.write(LoginProtocols.CLIENTBOUND, new ClientboundLoginCompressionPacket(64));
                        transport.compression = 64;
                    }
                    transport.write(LoginProtocols.CLIENTBOUND, new ClientboundLoginFinishedPacket(profile, UUID.randomUUID()));
                    assertInstanceOf(ServerboundLoginAcknowledgedPacket.class, transport.read(LoginProtocols.SERVERBOUND));
                    for (var packet : VanillaTestRegistries.packets()) {
                        transport.write(ConfigurationProtocols.CLIENTBOUND, packet);
                    }
                    transport.write(ConfigurationProtocols.CLIENTBOUND, VanillaTestRegistries.tags());
                    transport.write(ConfigurationProtocols.CLIENTBOUND, ClientboundFinishConfigurationPacket.INSTANCE);
                    assertInstanceOf(ServerboundFinishConfigurationPacket.class, transport.read(ConfigurationProtocols.SERVERBOUND));

                    var codec = new VanillaPacketCodec();
                    codec.play(VanillaBootstrap.defaultRegistries(), CONTEXT);
                    transport.write(codec.clientbound(), new ClientboundSetEntityMotionPacket(42, new Vec3(0.5, 0.25, -0.125)));
                    transport.write(codec.clientbound(), new ClientboundKeepAlivePacket(123456789L));
                    var response = assertInstanceOf(ServerboundKeepAlivePacket.class, transport.read(codec.serverbound()));
                    assertEquals(123456789L, response.getId());
                    transport.write(codec.clientbound(), new ClientboundDisconnectPacket(Component.literal("Fixture complete")));
                    assertEquals(-1, transport.input.read());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            try {
                client.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort()),
                    "bedrock.example\0forwarded", 25565, false);
                fixture.get(20, TimeUnit.SECONDS);
                assertTrue(disconnected.await(5, TimeUnit.SECONDS));
                assertNull(error.get(), () -> String.valueOf(error.get()));
                assertEquals(ConnectionProtocol.PLAY, client.state());
                assertEquals("Fixture complete", reason.get());
                assertNotNull(motion.get());
                assertEquals(0.5, motion.get().x, 0.001);
                assertFalse(client.isConnected());
            } finally {
                client.disconnect(Component.literal("Test cleanup"));
            }
        } finally {
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static final class FixtureTransport {
        private InputStream input;
        private OutputStream output;
        private int compression = -1;

        private FixtureTransport(InputStream input, OutputStream output) {
            this.input = input;
            this.output = output;
        }

        private Packet<?> read(ProtocolInfo<?> protocol) throws Exception {
            int length = readVarInt(input);
            byte[] bytes = new byte[length];
            new java.io.DataInputStream(input).readFully(bytes);
            ByteBuf buffer = Unpooled.wrappedBuffer(bytes);
            if (compression >= 0) {
                var decoder = new EmbeddedChannel(new CompressionDecoder(compression, true));
                try {
                    assertTrue(decoder.writeInbound(buffer));
                    buffer = decoder.readInbound();
                } finally {
                    decoder.finishAndReleaseAll();
                }
            }
            try {
                var packet = protocol.codec().decode(buffer);
                assertEquals(0, buffer.readableBytes());
                return packet;
            } finally {
                buffer.release();
            }
        }

        private void write(ProtocolInfo<?> protocol, Packet<?> packet) throws Exception {
            ByteBuf buffer = Unpooled.buffer();
            try {
                VanillaPacketCodec.encode(protocol, buffer, packet);
                if (compression >= 0) {
                    var encoder = new EmbeddedChannel(new CompressionEncoder(compression));
                    try {
                        assertTrue(encoder.writeOutbound(buffer));
                        buffer = encoder.readOutbound();
                    } finally {
                        encoder.finishAndReleaseAll();
                    }
                }
                int length = buffer.readableBytes();
                int remaining = length;
                while ((remaining & ~0x7F) != 0) {
                    output.write((remaining & 0x7F) | 0x80);
                    remaining >>>= 7;
                }
                output.write(remaining);
                buffer.readBytes(output, length);
                output.flush();
            } finally {
                buffer.release();
            }
        }

    }

    private static int readVarInt(InputStream input) throws Exception {
        int result = 0;
        for (int shift = 0; shift < 35; shift += 7) {
            int value = input.read();
            if (value == -1) {
                throw new EOFException("Connection closed before Minecraft frame");
            }
            result |= (value & 0x7F) << shift;
            if ((value & 0x80) == 0) {
                return result;
            }
        }
        throw new IllegalArgumentException("Invalid frame length");
    }
}

/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VanillaPayloadTest {
    @BeforeAll
    static void initialize() {
        VanillaBootstrap.initialize();
    }

    @Test
    void preservesUnknownPluginMessagesInBothDirectionsDuringConfiguration() {
        var codec = new VanillaPacketCodec();
        codec.configuration();
        var payload = new VanillaPayload(Identifier.fromNamespaceAndPath("test", "channel"), new byte[] {0, 1, -1, 0, 42});
        var buffer = Unpooled.buffer();
        try {
            VanillaPacketCodec.encode(codec.clientbound(), buffer, new ClientboundCustomPayloadPacket(payload));
            var decoded = assertInstanceOf(ClientboundCustomPayloadPacket.class, codec.decode(buffer));
            assertEquals(payload.id(), decoded.payload().type().id());
            assertArrayEquals(payload.data(), VanillaPayload.data(decoded.payload()));
            codec.encode(buffer, new ServerboundCustomPayloadPacket(payload));
            var response = assertInstanceOf(ServerboundCustomPayloadPacket.class, codec.serverbound().codec().decode(buffer));
            assertArrayEquals(payload.data(), VanillaPayload.data(response.payload()));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void usesVanillaEncodingForBrandPayloads() {
        var codec = new VanillaPacketCodec();
        codec.configuration();
        var payload = new BrandPayload("GeyserVanilla");
        var expected = Unpooled.buffer();
        var actual = Unpooled.buffer();
        try {
            VanillaPacketCodec.encode(ConfigurationProtocols.SERVERBOUND, expected, new ServerboundCustomPayloadPacket(payload));
            codec.encode(actual, new ServerboundCustomPayloadPacket(payload));
            assertEquals(expected, actual);
            var decoded = assertInstanceOf(ServerboundCustomPayloadPacket.class, codec.serverbound().codec().decode(actual));
            assertEquals(payload, decoded.payload());
        } finally {
            expected.release();
            actual.release();
        }
    }

    @Test
    void preservesMessagesDuringPlay() {
        var codec = new VanillaPacketCodec();
        codec.play(RegistryAccess.EMPTY, new GameProtocols.Context() {
            public boolean hasInfiniteMaterials() {
                return true;
            }

            public boolean canUseCommandBlocks() {
                return true;
            }
        });
        var payload = new VanillaPayload(Identifier.fromNamespaceAndPath("floodgate", "skin"), new byte[] {1, 2, 3});
        var buffer = Unpooled.buffer();
        try {
            VanillaPacketCodec.encode(codec.clientbound(), buffer, new ClientboundCustomPayloadPacket(payload));
            var decoded = assertInstanceOf(ClientboundCustomPayloadPacket.class, codec.decode(buffer));
            assertArrayEquals(payload.data(), VanillaPayload.data(decoded.payload()));
            codec.encode(buffer, new ServerboundCustomPayloadPacket(payload));
            var response = assertInstanceOf(ServerboundCustomPayloadPacket.class, codec.serverbound().codec().decode(buffer));
            assertArrayEquals(payload.data(), VanillaPayload.data(response.payload()));
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsOversizedServerboundMessages() {
        var codec = new VanillaPacketCodec();
        codec.configuration();
        var payload = new VanillaPayload(Identifier.fromNamespaceAndPath("test", "channel"), new byte[32768]);
        var buffer = Unpooled.buffer();
        try {
            assertThrows(io.netty.handler.codec.EncoderException.class,
                () -> codec.encode(buffer, new ServerboundCustomPayloadPacket(payload)));
        } finally {
            buffer.release();
        }
    }
}

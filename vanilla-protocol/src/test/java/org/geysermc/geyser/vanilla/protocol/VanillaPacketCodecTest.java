/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.InstrumentComponent;
import com.mojang.serialization.Lifecycle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class VanillaPacketCodecTest {
    private static final GameProtocols.Context CONTEXT = new GameProtocols.Context() {
        @Override
        public boolean hasInfiniteMaterials() {
            return true;
        }

        @Override
        public boolean canUseCommandBlocks() {
            return true;
        }
    };

    @BeforeAll
    static void bootstrap() {
        var stdout = System.out;
        var stderr = System.err;
        VanillaBootstrap.initialize();
        assertSame(stdout, System.out);
        assertSame(stderr, System.err);
        assertEquals("26.3", SharedConstants.getCurrentVersion().name());
    }

    @Test
    void serializesLoginUsingVanillaProtocol() {
        VanillaPacketCodec codec = new VanillaPacketCodec();
        var packet = new ServerboundHelloPacket("BedrockPlayer", UUID.randomUUID());
        var buffer = Unpooled.buffer();
        try {
            codec.encode(buffer, packet);
            assertEquals(packet, codec.serverbound().codec().decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void switchesToConfigurationInBothDirections() {
        VanillaPacketCodec codec = new VanillaPacketCodec();
        codec.configuration();
        assertEquals(ConnectionProtocol.CONFIGURATION, codec.state());
        var buffer = Unpooled.buffer();
        try {
            VanillaPacketCodec.encode(codec.clientbound(), buffer, ClientboundFinishConfigurationPacket.INSTANCE);
            assertSame(ClientboundFinishConfigurationPacket.INSTANCE, codec.decode(buffer));
            codec.encode(buffer, ServerboundFinishConfigurationPacket.INSTANCE);
            assertSame(ServerboundFinishConfigurationPacket.INSTANCE, codec.serverbound().codec().decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void serializesPlayPacketsInBothDirections() {
        VanillaPacketCodec codec = new VanillaPacketCodec();
        codec.play(RegistryAccess.EMPTY, CONTEXT);
        assertEquals(ConnectionProtocol.PLAY, codec.state());
        var motion = new ClientboundSetEntityMotionPacket(42, new Vec3(0.5, -0.25, 0.125));
        var teleport = new ServerboundAcceptTeleportationPacket(123, 1, 64, 2, 90, 45);
        var buffer = Unpooled.buffer();
        try {
            VanillaPacketCodec.encode(codec.clientbound(), buffer, motion);
            var decoded = assertInstanceOf(ClientboundSetEntityMotionPacket.class, codec.decode(buffer));
            assertEquals(motion.id(), decoded.id());
            assertEquals(motion.movement().x, decoded.movement().x, 0.001);
            assertEquals(motion.movement().y, decoded.movement().y, 0.001);
            assertEquals(motion.movement().z, decoded.movement().z, 0.001);

            codec.encode(buffer, teleport);
            var decodedTeleport = assertInstanceOf(ServerboundAcceptTeleportationPacket.class,
                codec.serverbound().codec().decode(buffer));
            assertEquals(teleport, decodedTeleport);
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsPacketsFromWrongProtocolPhase() {
        VanillaPacketCodec codec = new VanillaPacketCodec();
        var buffer = Unpooled.buffer();
        try {
            assertThrows(io.netty.handler.codec.EncoderException.class,
                () -> codec.encode(buffer, new ServerboundAcceptTeleportationPacket(1, 0, 0, 0, 0, 0)));
        } finally {
            buffer.release();
        }
    }

    @Test
    void decodesItemComponentsAgainstEachConnectionsRegistry() {
        VanillaBootstrap.defaultRegistries();
        var key = ResourceKey.create(Registries.INSTRUMENT, Identifier.fromNamespaceAndPath("test", "instrument"));
        var firstInstrument = new Instrument(SoundEvents.AMBIENT_CAVE, 1, 16, 1, Component.literal("first"));
        var secondInstrument = new Instrument(SoundEvents.AMBIENT_CAVE, 2, 32, 2, Component.literal("second"));
        var firstRegistry = new MappedRegistry<Instrument>(Registries.INSTRUMENT, Lifecycle.stable());
        var firstHolder = firstRegistry.register(key, firstInstrument, RegistrationInfo.BUILT_IN);
        firstRegistry.freeze();
        var secondRegistry = new MappedRegistry<Instrument>(Registries.INSTRUMENT, Lifecycle.stable());
        secondRegistry.register(key, secondInstrument, RegistrationInfo.BUILT_IN);
        secondRegistry.freeze();

        var firstRegistries = new ArrayList<net.minecraft.core.Registry<?>>();
        BuiltInRegistries.REGISTRY.forEach(firstRegistries::add);
        firstRegistries.add(firstRegistry);
        var secondRegistries = new ArrayList<net.minecraft.core.Registry<?>>();
        BuiltInRegistries.REGISTRY.forEach(secondRegistries::add);
        secondRegistries.add(secondRegistry);
        VanillaPacketCodec first = new VanillaPacketCodec();
        first.play(new RegistryAccess.ImmutableRegistryAccess(firstRegistries).freeze(), CONTEXT);
        VanillaPacketCodec second = new VanillaPacketCodec();
        second.play(new RegistryAccess.ImmutableRegistryAccess(secondRegistries).freeze(), CONTEXT);

        ItemStack item = new ItemStack(Items.GOAT_HORN, 1);
        item.set(DataComponents.INSTRUMENT, new InstrumentComponent(firstHolder));
        item.set(DataComponents.CUSTOM_NAME, Component.literal("Bedrock horn"));
        var packet = new ClientboundContainerSetSlotPacket(2, 3, 4, item);
        var buffer = Unpooled.buffer();
        try {
            VanillaPacketCodec.encode(first.clientbound(), buffer, packet);
            var otherBuffer = buffer.retainedDuplicate();
            try {
                var firstItem = assertInstanceOf(ClientboundContainerSetSlotPacket.class, first.decode(buffer)).getItem();
                var secondItem = assertInstanceOf(ClientboundContainerSetSlotPacket.class, second.decode(otherBuffer)).getItem();
                assertSame(firstInstrument, firstItem.get(DataComponents.INSTRUMENT).instrument().value());
                assertSame(secondInstrument, secondItem.get(DataComponents.INSTRUMENT).instrument().value());
                assertEquals("Bedrock horn", firstItem.get(DataComponents.CUSTOM_NAME).getString());
                assertEquals(1, firstItem.getCount());
            } finally {
                otherBuffer.release();
            }
        } finally {
            buffer.release();
        }
    }
}

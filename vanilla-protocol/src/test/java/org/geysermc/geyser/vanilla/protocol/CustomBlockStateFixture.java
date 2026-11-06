/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.IdMapper;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Runs against the real client in the private loader, without sharing test-library classes. */
public final class CustomBlockStateFixture {
    private static int vanillaSize;
    private static int[] customIds;

    public static void initialize() {
        VanillaBootstrap.defaultRegistries();
        vanillaSize = Block.BLOCK_STATE_REGISTRY.size();
        customIds = new int[] {vanillaSize + 100, 1_000_000_007, Integer.MAX_VALUE};
    }

    public static int[] mappings() {
        check(Block.getId(Blocks.AIR.defaultBlockState()) == 0, "Vanilla air ID changed");
        check(Block.BLOCK_STATE_REGISTRY.size() == vanillaSize, "Custom IDs entered dense vanilla storage");
        int iterated = 0;
        for (var state : Block.BLOCK_STATE_REGISTRY) {
            check(state != null && Block.getId(state) == iterated, "Vanilla state identity changed");
            check(Block.BLOCK_STATE_REGISTRY.byId(iterated) == state, "Vanilla ID lookup changed");
            iterated++;
        }
        check(iterated == vanillaSize, "Vanilla iteration changed");
        var other = new IdMapper<String>();
        other.add("fixture");
        check(other.getId("fixture") == 0 && other.byId(0).equals("fixture"), "Unrelated mapper changed");
        check(other.byId(customIds[0]) == null && other.getId("unknown") == -1, "Custom lookup leaked into another mapper");
        for (int id : customIds) {
            var state = Block.BLOCK_STATE_REGISTRY.byId(id);
            check(state != null && Block.getId(state) == id, "Lost sparse ID " + id);
            check(Block.BLOCK_STATE_REGISTRY.contains(id), "Native contains rejected custom ID");
        }
        var otherStates = new IdMapper<net.minecraft.world.level.block.state.BlockState>();
        var customState = Block.BLOCK_STATE_REGISTRY.byId(customIds[0]);
        otherStates.add(customState);
        check(otherStates.byId(0) == customState && otherStates.getId(customState) == 0,
            "A foreign state mapper used the global custom wire ID");
        check(Block.BLOCK_STATE_REGISTRY.byId(-1) == null, "Negative ID became valid");
        check(Block.getId(Block.BLOCK_STATE_REGISTRY.byId(customIds[0] - 1)) == customIds[0] - 1,
            "Unconfigured state ID was lost before Geyser could apply its fallback");
        return customIds.clone();
    }

    public static void blockUpdates() {
        int[] ids = {0, Block.getId(Blocks.STONE.defaultBlockState()), customIds[0], customIds[1], customIds[2]};
        var position = new BlockPos(-17, 72, 36);
        var protocol = new VanillaPacketCodec();
        protocol.play(VanillaBootstrap.defaultRegistries(), new GameProtocols.Context() {
            public boolean hasInfiniteMaterials() { return false; }
            public boolean canUseCommandBlocks() { return false; }
        });
        int[] packetId = {-1};
        GameProtocols.CLIENTBOUND_TEMPLATE.details().listPackets((type, id) -> {
            if (type.equals(net.minecraft.network.protocol.game.GamePacketTypes.CLIENTBOUND_BLOCK_UPDATE)) packetId[0] = id;
        });
        for (int id : ids) {
            var input = buffer();
            var output = buffer();
            try {
                input.writeVarInt(packetId[0]);
                BlockPos.STREAM_CODEC.encode(input, position);
                input.writeVarInt(id);
                byte[] expected = bytes(input);
                var packet = (ClientboundBlockUpdatePacket) protocol.decode(input);
                check(packet.getClass() == ClientboundBlockUpdatePacket.class, "Packet implementation changed");
                check(packet.getPos().equals(position), "Block position changed");
                check(Block.getId(packet.getBlockState()) == id, "Block update lost ID " + id);
                VanillaPacketCodec.encode(protocol.clientbound(), output, packet);
                check(Arrays.equals(expected, bytes(output)), "Block update bytes changed for ID " + id);
            } finally {
                input.release();
                output.release();
            }
        }
    }

    public static void sectionUpdates() {
        var input = buffer();
        var output = buffer();
        var section = SectionPos.of(-3, 5, 2);
        int[] positions = {0, 0x123, 0xabc, 0xfff};
        int[] ids = {Block.getId(Blocks.STONE.defaultBlockState()), customIds[0], customIds[1], customIds[2]};
        try {
            SectionPos.STREAM_CODEC.encode(input, section);
            input.writeVarInt(ids.length);
            Map<BlockPos, Integer> expectedUpdates = new LinkedHashMap<>();
            for (int i = 0; i < ids.length; i++) {
                input.writeVarLong((long) ids[i] << 12 | positions[i]);
                expectedUpdates.put(new BlockPos(section.minBlockX() + (positions[i] >> 8 & 15),
                    section.minBlockY() + (positions[i] & 15), section.minBlockZ() + (positions[i] >> 4 & 15)), ids[i]);
            }
            byte[] expected = bytes(input);
            var packet = ClientboundSectionBlocksUpdatePacket.STREAM_CODEC.decode(input);
            Map<BlockPos, Integer> actual = new LinkedHashMap<>();
            packet.runUpdates((position, state) -> actual.put(position.immutable(), Block.getId(state)));
            check(expectedUpdates.equals(actual), "Section update positions or custom IDs changed: " + actual);
            ClientboundSectionBlocksUpdatePacket.STREAM_CODEC.encode(output, packet);
            check(Arrays.equals(expected, bytes(output)), "Section update bytes changed");
        } finally {
            input.release();
            output.release();
        }
    }

    public static void particlesAndMetadata() {
        for (int id : customIds) {
            var input = buffer();
            var output = buffer();
            try {
                input.writeVarInt(id);
                byte[] expected = bytes(input);
                var codec = BlockParticleOption.streamCodec(ParticleTypes.BLOCK);
                var particle = codec.decode(input);
                check(Block.getId(particle.getState()) == id, "Particle lost ID " + id);
                codec.encode(output, particle);
                check(Arrays.equals(expected, bytes(output)), "Particle bytes changed");
                input.clear().writeVarInt(id);
                output.clear();
                var state = EntityDataSerializers.BLOCK_STATE.codec().decode(input);
                check(Block.getId(state) == id, "Entity metadata lost ID " + id);
                EntityDataSerializers.BLOCK_STATE.codec().encode(output, state);
                check(Arrays.equals(expected, bytes(output)), "Metadata bytes changed");
                input.clear();
                output.clear();
                var value = Optional.of(state);
                EntityDataSerializers.OPTIONAL_BLOCK_STATE.codec().encode(input, value);
                var decoded = EntityDataSerializers.OPTIONAL_BLOCK_STATE.codec().decode(input);
                check(decoded.isPresent() && Block.getId(decoded.orElseThrow()) == id, "Optional metadata lost ID " + id);
            } finally {
                input.release();
                output.release();
            }
        }
    }

    public static void unconfiguredIds() {
        int id = customIds[0] - 1;
        var first = Block.BLOCK_STATE_REGISTRY.byId(id);
        check(first == Block.BLOCK_STATE_REGISTRY.byId(id), "Live state identity was not retained");
        check(Block.getId(first) == id, "Unconfigured wire ID was lost");
        check(Block.BLOCK_STATE_REGISTRY.size() == vanillaSize, "Lookup expanded dense storage");
    }

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), VanillaBootstrap.defaultRegistries());
    }

    private static byte[] bytes(RegistryFriendlyByteBuf buffer) {
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), bytes);
        return bytes;
    }

    private static void check(boolean condition, String error) {
        if (!condition) throw new AssertionError(error);
    }
}

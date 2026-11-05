/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.CommonPacketTypes;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Retains plugin message bytes that a graphical vanilla client would discard. */
public record VanillaPayload(Identifier id, byte[] data) implements CustomPacketPayload {
    public VanillaPayload {
        data = data.clone();
    }

    @Override
    public byte[] data() {
        return data.clone();
    }

    @Override
    public Type<VanillaPayload> type() {
        return new Type<>(id);
    }

    public static byte[] data(CustomPacketPayload payload) {
        if (payload instanceof VanillaPayload raw) {
            return raw.data();
        }
        if (payload instanceof BrandPayload brand) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                BrandPayload.STREAM_CODEC.encode(buffer, brand);
                byte[] bytes = new byte[buffer.readableBytes()];
                buffer.readBytes(bytes);
                return bytes;
            } finally {
                buffer.release();
            }
        }
        throw new IllegalArgumentException("Unsupported custom payload: " + payload.type().id());
    }

    private static StreamCodec<FriendlyByteBuf, CustomPacketPayload> codec(int maximumSize) {
        var original = CustomPacketPayload.<FriendlyByteBuf>codec(id -> new StreamCodec<>() {
            @Override
            public CustomPacketPayload decode(FriendlyByteBuf input) {
                int length = input.readableBytes();
                if (length > maximumSize) {
                    throw new io.netty.handler.codec.DecoderException("Plugin message exceeds " + maximumSize + " bytes");
                }
                byte[] bytes = new byte[length];
                input.readBytes(bytes);
                return new VanillaPayload(id, bytes);
            }

            @Override
            public void encode(FriendlyByteBuf output, CustomPacketPayload payload) {
                output.writeBytes(((VanillaPayload) payload).data);
            }
        }, List.of(new CustomPacketPayload.TypeAndCodec<>(BrandPayload.TYPE, BrandPayload.STREAM_CODEC)));
        return new StreamCodec<>() {
            @Override
            public CustomPacketPayload decode(FriendlyByteBuf input) {
                return original.decode(input);
            }

            @Override
            public void encode(FriendlyByteBuf output, CustomPacketPayload payload) {
                if (payload instanceof VanillaPayload raw) {
                    if (raw.data.length > maximumSize) {
                        throw new io.netty.handler.codec.EncoderException("Plugin message exceeds " + maximumSize + " bytes");
                    }
                    output.writeIdentifier(raw.id);
                    output.writeBytes(raw.data);
                } else {
                    original.encode(output, payload);
                }
            }
        };
    }

    public static <T extends PacketListener> ProtocolInfo<T> preserve(ProtocolInfo<T> protocol, ProtocolInfo.Details details) {
        var type = protocol.flow() == PacketFlow.CLIENTBOUND
            ? CommonPacketTypes.CLIENTBOUND_CUSTOM_PAYLOAD : CommonPacketTypes.SERVERBOUND_CUSTOM_PAYLOAD;
        int[] customId = {-1};
        details.listPackets((packetType, networkId) -> {
            if (packetType.equals(type)) {
                customId[0] = networkId;
            }
        });
        if (customId[0] == -1) {
            return protocol;
        }
        var payloadCodec = codec(protocol.flow() == PacketFlow.CLIENTBOUND ? 1048576 : 32767);
        StreamCodec<ByteBuf, Packet<? super T>> codec = new StreamCodec<>() {
            @Override
            @SuppressWarnings("unchecked")
            public Packet<? super T> decode(ByteBuf input) {
                int start = input.readerIndex();
                int packetId = VarInt.read(input);
                if (packetId == customId[0]) {
                    var payload = payloadCodec.decode(new FriendlyByteBuf(input));
                    return (Packet<? super T>) (protocol.flow() == PacketFlow.CLIENTBOUND
                        ? new ClientboundCustomPayloadPacket(payload) : new ServerboundCustomPayloadPacket(payload));
                }
                input.readerIndex(start);
                return protocol.codec().decode(input);
            }

            @Override
            public void encode(ByteBuf output, Packet<? super T> packet) {
                CustomPacketPayload payload;
                if (packet instanceof ClientboundCustomPayloadPacket custom && protocol.flow() == PacketFlow.CLIENTBOUND) {
                    payload = custom.payload();
                } else if (packet instanceof ServerboundCustomPayloadPacket custom && protocol.flow() == PacketFlow.SERVERBOUND) {
                    payload = custom.payload();
                } else {
                    protocol.codec().encode(output, packet);
                    return;
                }
                VarInt.write(output, customId[0]);
                payloadCodec.encode(new FriendlyByteBuf(output), payload);
            }
        };
        return new ProtocolInfo<>() {
            public net.minecraft.network.ConnectionProtocol id() {
                return protocol.id();
            }

            public PacketFlow flow() {
                return protocol.flow();
            }

            public StreamCodec<ByteBuf, Packet<? super T>> codec() {
                return codec;
            }

            public net.minecraft.network.protocol.BundlerInfo bundlerInfo() {
                return protocol.bundlerInfo();
            }
        };
    }
}

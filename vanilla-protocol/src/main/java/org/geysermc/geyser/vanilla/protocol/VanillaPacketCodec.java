/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.status.StatusProtocols;

/** A connection's Java protocol codecs, including its server-supplied play registries. */
public final class VanillaPacketCodec {
    private ProtocolInfo<?> clientbound = LoginProtocols.CLIENTBOUND;
    private ProtocolInfo<?> serverbound = LoginProtocols.SERVERBOUND;

    public void status() {
        clientbound = StatusProtocols.CLIENTBOUND;
        serverbound = StatusProtocols.SERVERBOUND;
    }

    public void configuration() {
        clientbound = ConfigurationProtocols.CLIENTBOUND;
        serverbound = ConfigurationProtocols.SERVERBOUND;
    }

    public void play(RegistryAccess registries, GameProtocols.Context context) {
        var buffers = RegistryFriendlyByteBuf.decorator(registries);
        clientbound = GameProtocols.CLIENTBOUND_TEMPLATE.bind(buffers);
        serverbound = GameProtocols.SERVERBOUND_TEMPLATE.bind(buffers, context);
    }

    public ConnectionProtocol state() {
        return serverbound.id();
    }

    public ProtocolInfo<?> clientbound() {
        return clientbound;
    }

    public ProtocolInfo<?> serverbound() {
        return serverbound;
    }

    public Packet<?> decode(ByteBuf input) {
        return clientbound.codec().decode(input);
    }

    public void encode(ByteBuf output, Packet<?> packet) {
        encode(serverbound, output, packet);
    }

    // A protocol owns a heterogeneous packet set. Vanilla validates packet membership at dispatch.
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void encode(ProtocolInfo protocol, ByteBuf output, Packet<?> packet) {
        protocol.codec().encode(output, packet);
    }
}

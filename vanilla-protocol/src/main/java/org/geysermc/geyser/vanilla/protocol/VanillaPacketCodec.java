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
        clientbound = VanillaPayload.preserve(ConfigurationProtocols.CLIENTBOUND, ConfigurationProtocols.CLIENTBOUND_TEMPLATE.details());
        serverbound = VanillaPayload.preserve(ConfigurationProtocols.SERVERBOUND, ConfigurationProtocols.SERVERBOUND_TEMPLATE.details());
    }

    public void play(RegistryAccess registries, GameProtocols.Context context) {
        var buffers = RegistryFriendlyByteBuf.decorator(registries);
        clientbound = VanillaPayload.preserve(GameProtocols.CLIENTBOUND_TEMPLATE.bind(buffers), GameProtocols.CLIENTBOUND_TEMPLATE.details());
        serverbound = VanillaPayload.preserve(GameProtocols.SERVERBOUND_TEMPLATE.bind(buffers, context), GameProtocols.SERVERBOUND_TEMPLATE.details());
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

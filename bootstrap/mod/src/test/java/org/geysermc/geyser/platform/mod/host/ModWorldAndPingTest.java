/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.mod.host;

import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import org.geysermc.geyser.network.bedrock.GameProtocol;
import org.geysermc.geyser.platform.mod.ModPingPassthrough;
import org.geysermc.geyser.platform.mod.world.GeyserModWorldManager;
import org.geysermc.geyser.vanilla.protocol.VanillaBootstrap;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModWorldAndPingTest {
    @Test
    void usesServerWorldStorageOnlyForMatchingProtocolsAndConvertsGameModeById() {
        var world = mock(ModWorldAccess.class);
        var manager = new GeyserModWorldManager(world);
        when(world.protocolVersion()).thenReturn(GameProtocol.getJavaProtocolVersion());
        when(world.defaultGameMode()).thenReturn(1);
        assertTrue(manager.hasOwnChunkCache());
        assertEquals(GameType.CREATIVE, manager.getDefaultGameMode(null));
        when(world.protocolVersion()).thenReturn(GameProtocol.getJavaProtocolVersion() - 1);
        assertFalse(manager.hasOwnChunkCache());
    }

    @Test
    void interceptsHostStatusWithoutAChannelAndConvertsItsData() {
        var registries = VanillaBootstrap.defaultRegistries();
        var server = mock(MinecraftServer.class);
        var failure = new AtomicReference<Throwable>();
        var status = new ServerStatus(Component.literal("host-modified motd").withStyle(ChatFormatting.GREEN),
            Optional.of(new ServerStatus.Players(50, 7, List.of())), Optional.empty(), Optional.empty(), false);
        when(server.getStatus()).thenReturn(status);
        when(server.registryAccess()).thenReturn(registries);
        var result = new ModPingPassthrough(new ServerPing(server, failure::set))
            .getPingInformation(new InetSocketAddress("127.0.0.1", 25565));
        assertNotNull(result);
        assertNull(failure.get());
        var json = JsonParser.parseString(result.getDescription()).getAsJsonObject();
        assertEquals("host-modified motd", json.get("text").getAsString());
        assertEquals("green", json.get("color").getAsString());
        assertEquals(50, result.getPlayers().getMax());
        assertEquals(7, result.getPlayers().getOnline());
    }

    @Test
    void returnsNoPingWhenTheServerHasNoStatusYet() {
        var server = mock(MinecraftServer.class);
        var ping = new ModPingPassthrough(new ServerPing(server, failure -> fail(failure)));
        assertNull(ping.getPingInformation(new InetSocketAddress("127.0.0.1", 25565)));
    }
}

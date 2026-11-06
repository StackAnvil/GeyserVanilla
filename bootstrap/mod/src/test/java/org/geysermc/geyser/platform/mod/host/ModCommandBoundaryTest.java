/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.mod.host;

import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.commands.CommandSourceStack;
import org.geysermc.geyser.platform.mod.command.ModCommandSource;
import org.geysermc.geyser.vanilla.protocol.VanillaBootstrap;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModCommandBoundaryTest {
    @Test
    void keepsTheOriginalNonPlayerCommandContextWhenReversingTheMapper() {
        VanillaBootstrap.initialize();
        var stack = mock(CommandSourceStack.class);
        when(stack.getTextName()).thenReturn("command block");
        var mapper = ServerCommandSender.mapper(ModCommandSource::new, source -> (ModCommandSender) source.handle());
        ModCommandSource mapped = mapper.map(stack);
        assertTrue(mapped.isConsole());
        assertEquals("command block", mapped.name());
        assertSame(stack, mapper.reverse(mapped));
    }

    @Test
    void passesPlayerTextAndIdentityThroughTheOpaqueHandle() {
        var sender = new CapturingSender();
        var source = new ModCommandSource(sender);
        source.sendMessage("A plain message with §aformatting");
        assertEquals("A plain message with §aformatting", sender.plain);
        assertEquals(sender.uuid, source.playerUuid());
        assertEquals("Bedrock player", source.name());
        assertFalse(source.isConsole());
        assertSame(sender, source.handle());
    }

    @Test
    void sendsStyledMessagesAsJsonWithoutPassingNativeOrAdventureObjects() {
        var sender = new CapturingSender();
        new ModCommandSource(sender).sendMessage(Component.text("Styled message", NamedTextColor.GREEN));
        var json = JsonParser.parseString(sender.json).getAsJsonObject();
        assertEquals("Styled message", json.get("text").getAsString());
        assertEquals("green", json.get("color").getAsString());
        assertNull(sender.plain);
    }

    private static final class CapturingSender implements ModCommandSender {
        private final UUID uuid = UUID.randomUUID();
        private String plain;
        private String json;

        public String name() { return "Bedrock player"; }
        public boolean isConsole() { return false; }
        public UUID playerUuid() { return uuid; }
        public void sendPlain(String message) { plain = message; }
        public void sendJson(String message) { json = message; }
    }
}

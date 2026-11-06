/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VanillaCommandsTest {
    @Test
    void reconstructsNativeArgumentsRedirectsExecutionAndRestrictionFlags() {
        var registries = VanillaBootstrap.defaultRegistries();
        var root = new RootCommandNode<Object>();
        var context = CommandBuildContext.simple(registries, FeatureFlags.DEFAULT_FLAGS);
        var command = LiteralArgumentBuilder.<Object>literal("give")
            .then(RequiredArgumentBuilder.<Object, Integer>argument("count", IntegerArgumentType.integer(1, 64)).executes(ignored -> 1))
            .then(RequiredArgumentBuilder.argument("effect", ResourceArgument.resource(context, Registries.MOB_EFFECT)))
            .build();
        root.addChild(command);
        root.addChild(LiteralArgumentBuilder.<Object>literal("alias").redirect(command).build());
        var packet = new ClientboundCommandsPacket(root, new ClientboundCommandsPacket.NodeInspector<Object>() {
            @Override
            public Identifier suggestionId(ArgumentCommandNode<Object, ?> node) {
                return null;
            }

            @Override
            public boolean isExecutable(CommandNode<Object> node) {
                return node.getCommand() != null;
            }

            @Override
            public boolean isRestricted(CommandNode<Object> node) {
                return node == command;
            }
        });
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ClientboundCommandsPacket.STREAM_CODEC.encode(buffer, packet);
            var decoded = ClientboundCommandsPacket.STREAM_CODEC.decode(buffer);
            var tree = VanillaCommands.root(decoded, registries, FeatureFlags.DEFAULT_FLAGS);
            var give = tree.getChild("give");
            assertTrue(VanillaCommands.restricted(give));
            assertFalse(VanillaCommands.restricted(tree.getChild("alias")));
            assertSame(give, tree.getChild("alias").getRedirect());
            assertNull(give.getCommand());
            var count = (ArgumentCommandNode<?, ?>) give.getChild("count");
            assertNotNull(count.getCommand());
            var type = assertInstanceOf(IntegerArgumentType.class, count.getType());
            assertEquals(1, type.getMinimum());
            assertEquals(64, type.getMaximum());
            var description = VanillaCommands.describe(type);
            assertEquals(Identifier.parse("brigadier:integer"), description.type());
            assertEquals(1, description.properties().get("min").getAsInt());
            assertEquals(64, description.properties().get("max").getAsInt());
            var effect = (ArgumentCommandNode<?, ?>) give.getChild("effect");
            assertEquals(Registries.MOB_EFFECT.identifier(), VanillaCommands.describe(effect.getType()).registry().orElseThrow());
            assertFalse(buffer.isReadable());
        } finally {
            buffer.release();
        }
    }

    @Test
    void preservesStringArgumentModesInNativeDescriptions() {
        var word = VanillaCommands.describe(StringArgumentType.word());
        var greedy = VanillaCommands.describe(StringArgumentType.greedyString());
        assertEquals(Identifier.parse("brigadier:string"), word.type());
        assertEquals("word", word.properties().get("type").getAsString());
        assertEquals("greedy", greedy.properties().get("type").getAsString());
        assertNotEquals(word, greedy);
        assertTrue(word.registry().isEmpty());
    }
}

/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlagSet;

import java.util.Optional;
import java.util.function.Predicate;

/** Reconstructs server command descriptions with vanilla's native argument serializers. */
public final class VanillaCommands {
    private VanillaCommands() {
    }

    public static RootCommandNode<Object> root(ClientboundCommandsPacket packet, RegistryAccess registries, FeatureFlagSet features) {
        return packet.getRoot(CommandBuildContext.simple(registries, features), new ClientboundCommandsPacket.NodeBuilder<>() {
            @Override
            public ArgumentBuilder<Object, ?> createLiteral(String name) {
                return LiteralArgumentBuilder.literal(name);
            }

            @Override
            public ArgumentBuilder<Object, ?> createArgument(String name, ArgumentType<?> type, Identifier suggestions) {
                return RequiredArgumentBuilder.argument(name, type);
            }

            @Override
            public ArgumentBuilder<Object, ?> configure(ArgumentBuilder<Object, ?> builder, boolean executable, boolean restricted) {
                builder.requires(new Restriction(restricted));
                // As in vanilla's client tree, executable nodes describe commands that run on the server.
                return executable ? builder.executes(context -> 0) : builder;
            }
        });
    }

    public static boolean restricted(CommandNode<?> node) {
        return node.getRequirement() instanceof Restriction restriction && restriction.restricted();
    }

    public static <A extends ArgumentType<?>> Argument describe(A argument) {
        VanillaBootstrap.initialize();
        return describeTemplate(ArgumentTypeInfos.unpack(argument));
    }

    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> Argument describeTemplate(T template) {
        @SuppressWarnings("unchecked")
        ArgumentTypeInfo<A, T> type = (ArgumentTypeInfo<A, T>) template.type();
        JsonObject properties = new JsonObject();
        type.serializeToJson(template, properties);
        return new Argument(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(type), properties);
    }

    public record Argument(Identifier type, JsonObject properties) {
        public Optional<Identifier> registry() {
            return properties.has("registry") ? Optional.of(Identifier.parse(properties.get("registry").getAsString())) : Optional.empty();
        }
    }

    private record Restriction(boolean restricted) implements Predicate<Object> {
        @Override
        public boolean test(Object source) {
            return !restricted;
        }
    }
}

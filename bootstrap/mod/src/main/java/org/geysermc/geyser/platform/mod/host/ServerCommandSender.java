/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.mod.host;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import org.incendo.cloud.SenderMapper;

import java.util.UUID;
import java.util.function.Function;

/** Retains the original host command context, including its position and permission level. */
public record ServerCommandSender(CommandSourceStack source) implements ModCommandSender {
    public static <S> SenderMapper<CommandSourceStack, S> mapper(Function<ModCommandSender, S> map,
                                                                Function<S, ModCommandSender> reverse) {
        return SenderMapper.create(source -> map.apply(new ServerCommandSender(source)), mapped -> {
            ModCommandSender sender = reverse.apply(mapped);
            if (!(sender instanceof ServerCommandSender original)) {
                throw new IllegalArgumentException("The command sender is no longer available on this server");
            }
            return original.source();
        });
    }

    @Override
    public String name() {
        return source.getTextName();
    }

    @Override
    public boolean isConsole() {
        return !(source.getEntity() instanceof ServerPlayer);
    }

    @Override
    public UUID playerUuid() {
        return source.getEntity() instanceof ServerPlayer player ? player.getUUID() : null;
    }

    @Override
    public void sendPlain(String message) {
        if (source.getEntity() instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.literal(message), false);
        }
    }

    @Override
    public void sendJson(String message) {
        if (source.getEntity() instanceof ServerPlayer player) {
            var component = ComponentSerialization.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, player.registryAccess()),
                JsonParser.parseString(message)).getOrThrow();
            player.sendSystemMessage(component, false);
        }
    }
}

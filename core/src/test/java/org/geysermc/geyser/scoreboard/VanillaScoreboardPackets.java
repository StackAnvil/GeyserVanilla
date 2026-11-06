/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.scoreboard;

import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team.CollisionRule;
import net.minecraft.world.scores.Team.Visibility;
import net.minecraft.world.scores.TeamColor;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType;
import org.geysermc.geyser.vanilla.protocol.VanillaBootstrap;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Native packet fixtures for the scoreboard behaviour tests. */
public final class VanillaScoreboardPackets {
    public static net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket playerInfo(
        java.util.EnumSet<net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action> actions,
        List<net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry> entries) {
        var packet = new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(actions, List.of());
        try {
            var field = packet.getClass().getDeclaredField("entries");
            field.setAccessible(true);
            field.set(packet, List.copyOf(entries));
            return packet;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    public static net.minecraft.network.protocol.game.ClientboundSetPassengersPacket passengers(int vehicle, int[] passengers) {
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            buffer.writeVarInt(vehicle);
            buffer.writeVarIntArray(passengers);
            return net.minecraft.network.protocol.game.ClientboundSetPassengersPacket.STREAM_CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    private VanillaScoreboardPackets() {
    }

    public static ClientboundSetObjectivePacket objective(String name, int method, Component display,
                                                         RenderType renderType, NumberFormat format) {
        return new ClientboundSetObjectivePacket(objective(name, display, renderType, format), method);
    }

    public static ClientboundSetDisplayObjectivePacket display(DisplaySlot slot, String name) {
        return new ClientboundSetDisplayObjectivePacket(slot,
            name == null ? null : objective(name, Component.empty(), RenderType.INTEGER, null));
    }

    public static ClientboundSetScorePacket score(String owner, String objective, int value) {
        return score(owner, objective, value, null, null);
    }

    public static ClientboundSetScorePacket score(String owner, String objective, int value,
                                                 Component display, NumberFormat format) {
        return new ClientboundSetScorePacket(owner, objective, value,
            Optional.ofNullable(display).map(VanillaScoreboardPackets::component), Optional.ofNullable(format));
    }

    public static FixedFormat fixed(Component value) {
        return new FixedFormat(component(value));
    }

    public static ClientboundSetPlayerTeamPacket createTeam(String name, Component display, Component prefix,
                                                           Component suffix, boolean friendlyFire, boolean seeInvisible,
                                                           Visibility visibility, CollisionRule collision, TeamColor color,
                                                           String[] players) {
        PlayerTeam team = team(name, display, prefix, suffix, friendlyFire, seeInvisible, visibility, collision, color);
        team.getPlayers().addAll(Arrays.asList(players));
        return ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true);
    }

    public static ClientboundSetPlayerTeamPacket updateTeam(String name, Component display, Component prefix,
                                                           Component suffix, boolean friendlyFire, boolean seeInvisible,
                                                           Visibility visibility, CollisionRule collision, TeamColor color) {
        return ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(
            team(name, display, prefix, suffix, friendlyFire, seeInvisible, visibility, collision, color), false);
    }

    public static ClientboundSetPlayerTeamPacket addPlayers(String name, String[] players) {
        // Vanilla's public factory accepts one player. Retain the bulk packets used by these regression tests.
        try {
            var lookup = MethodHandles.privateLookupIn(ClientboundSetPlayerTeamPacket.class, MethodHandles.lookup());
            int method = (int) lookup.findStaticGetter(ClientboundSetPlayerTeamPacket.class, "METHOD_JOIN", int.class).invokeExact();
            var constructor = lookup.findConstructor(ClientboundSetPlayerTeamPacket.class,
                MethodType.methodType(void.class, String.class, int.class, Optional.class, Collection.class));
            return (ClientboundSetPlayerTeamPacket) constructor.invoke(name, method, Optional.empty(), List.of(players));
        } catch (Throwable e) {
            throw new AssertionError("Cannot create native team membership fixture", e);
        }
    }

    private static PlayerTeam team(String name, Component display, Component prefix, Component suffix,
                                   boolean friendlyFire, boolean seeInvisible, Visibility visibility,
                                   CollisionRule collision, TeamColor color) {
        var team = new PlayerTeam(new net.minecraft.world.scores.Scoreboard(), name);
        team.setDisplayName(component(display));
        team.setPlayerPrefix(component(prefix));
        team.setPlayerSuffix(component(suffix));
        team.setAllowFriendlyFire(friendlyFire);
        team.setSeeFriendlyInvisibles(seeInvisible);
        team.setNameTagVisibility(visibility);
        team.setCollisionRule(collision);
        team.setColor(Optional.ofNullable(color));
        return team;
    }

    private static net.minecraft.world.scores.Objective objective(String name, Component display,
                                                                 RenderType renderType, NumberFormat format) {
        return new net.minecraft.world.scores.Objective(new net.minecraft.world.scores.Scoreboard(), name,
            ObjectiveCriteria.DUMMY, component(display), renderType, false, format);
    }

    private static net.minecraft.network.chat.Component component(Component value) {
        var operations = VanillaBootstrap.defaultRegistries().createSerializationContext(JsonOps.INSTANCE);
        return ComponentSerialization.CODEC.parse(operations, GsonComponentSerializer.gson().serializeToTree(value)).getOrThrow();
    }
}

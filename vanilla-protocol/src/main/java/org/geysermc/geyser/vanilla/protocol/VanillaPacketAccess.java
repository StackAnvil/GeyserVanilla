/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import io.netty.buffer.Unpooled;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.Vec3;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Optional;

/** Access to packet data that vanilla exposes only through a running client world. */
public final class VanillaPacketAccess {
    private static final VarHandle MOVEMENT_ENTITY_ID = field(ClientboundMoveEntityPacket.class, "entityId", int.class);

    private static final VarHandle EVENT_ENTITY_ID = field(ClientboundEntityEventPacket.class, "entityId", int.class);
    private static final VarHandle HEAD_ENTITY_ID = field(ClientboundRotateHeadPacket.class, "entityId", int.class);
    private static final VarHandle CAMERA_ENTITY_ID = field(ClientboundSetCameraPacket.class, "cameraId", int.class);
    private static final VarHandle LOOK_X = field(ClientboundPlayerLookAtPacket.class, "x", double.class);
    private static final VarHandle LOOK_Y = field(ClientboundPlayerLookAtPacket.class, "y", double.class);
    private static final VarHandle LOOK_Z = field(ClientboundPlayerLookAtPacket.class, "z", double.class);
    private static final VarHandle LOOK_ENTITY = field(ClientboundPlayerLookAtPacket.class, "entity", int.class);
    private static final VarHandle LOOK_ANCHOR = field(ClientboundPlayerLookAtPacket.class, "toAnchor", Anchor.class);
    private static final VarHandle LOOK_AT_ENTITY = field(ClientboundPlayerLookAtPacket.class, "atEntity", boolean.class);

    private VanillaPacketAccess() {
    }

    public static int entityId(ClientboundMoveEntityPacket packet) {
        return (int) MOVEMENT_ENTITY_ID.get(packet);
    }

    public static int entityId(ClientboundEntityEventPacket packet) {
        return (int) EVENT_ENTITY_ID.get(packet);
    }

    public static int entityId(ClientboundRotateHeadPacket packet) {
        return (int) HEAD_ENTITY_ID.get(packet);
    }

    public static int entityId(ClientboundSetCameraPacket packet) {
        return (int) CAMERA_ENTITY_ID.get(packet);
    }

    public static LookAtTarget lookAtTarget(ClientboundPlayerLookAtPacket packet) {
        Vec3 position = new Vec3((double) LOOK_X.get(packet), (double) LOOK_Y.get(packet), (double) LOOK_Z.get(packet));
        Optional<EntityTarget> entity = (boolean) LOOK_AT_ENTITY.get(packet)
            ? Optional.of(new EntityTarget((int) LOOK_ENTITY.get(packet), (Anchor) LOOK_ANCHOR.get(packet)))
            : Optional.empty();
        return new LookAtTarget(position, entity);
    }

    public record EntityTarget(int id, Anchor anchor) {
    }

    public record LookAtTarget(Vec3 position, Optional<EntityTarget> entity) {
    }

    public static ServerboundPlayerAbilitiesPacket playerAbilities(boolean flying) {
        Abilities abilities = new Abilities();
        abilities.flying = flying;
        return new ServerboundPlayerAbilitiesPacket(abilities);
    }

    /** Creates a packet through vanilla's codec when its public constructors require a client entity. */
    public static ServerboundPlayerCommandPacket playerCommand(int entityId, ServerboundPlayerCommandPacket.Action action, int data) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeVarInt(entityId);
            buffer.writeEnum(action);
            buffer.writeVarInt(data);
            return ServerboundPlayerCommandPacket.STREAM_CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    public static ServerboundPlayerCommandPacket playerCommand(int entityId, ServerboundPlayerCommandPacket.Action action) {
        return playerCommand(entityId, action, 0);
    }

    public static Vec3 movementDelta(ClientboundMoveEntityPacket packet) {
        VecDeltaCodec codec = new VecDeltaCodec();
        codec.setBase(Vec3.ZERO);
        return packet.getPositionDelta().decode(codec).endPosition();
    }

    private static VarHandle field(Class<?> owner, String name, Class<?> type) {
        try {
            return MethodHandles.privateLookupIn(owner, MethodHandles.lookup()).findVarHandle(owner, name, type);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}

/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.world.phys.Vec3;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/** Access to packet data that vanilla exposes only through a running client world. */
public final class VanillaPacketAccess {
    private static final VarHandle MOVEMENT_ENTITY_ID = field(ClientboundMoveEntityPacket.class, "entityId", int.class);

    private VanillaPacketAccess() {
    }

    public static int entityId(ClientboundMoveEntityPacket packet) {
        return (int) MOVEMENT_ENTITY_ID.get(packet);
    }

    public static ServerboundPlayerAbilitiesPacket playerAbilities(boolean flying) {
        Abilities abilities = new Abilities();
        abilities.flying = flying;
        return new ServerboundPlayerAbilitiesPacket(abilities);
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

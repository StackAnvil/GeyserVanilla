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
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.VecDelta;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VanillaPacketAccessTest {
    @Test
    void createsPlayerCommandsWithoutAClientEntityAndEncodesThemWithVanilla() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            for (var action : ServerboundPlayerCommandPacket.Action.values()) {
                for (int data : new int[]{0, 100, 300}) {
                    var packet = VanillaPacketAccess.playerCommand(12345, action, data);
                    ServerboundPlayerCommandPacket.STREAM_CODEC.encode(buffer, packet);
                    var decoded = ServerboundPlayerCommandPacket.STREAM_CODEC.decode(buffer);
                    assertEquals(12345, decoded.getId());
                    assertSame(action, decoded.getAction());
                    assertEquals(data, decoded.getData());
                    assertFalse(buffer.isReadable());
                    buffer.clear();
                }
            }
            assertEquals(0, VanillaPacketAccess.playerCommand(1, ServerboundPlayerCommandPacket.Action.START_SPRINTING).getData());
        } finally {
            buffer.release();
        }
    }

    @Test
    void readsEntityIdsFromNativeWirePacketsWithoutAClientWorld() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeInt(12345).writeByte(EntityEvent.DEATH);
            var event = ClientboundEntityEventPacket.STREAM_CODEC.decode(buffer);
            assertEquals(12345, VanillaPacketAccess.entityId(event));
            assertEquals(EntityEvent.DEATH, event.getEventId());

            buffer.clear();
            buffer.writeVarInt(54321).writeByte(64);
            var rotation = ClientboundRotateHeadPacket.STREAM_CODEC.decode(buffer);
            assertEquals(54321, VanillaPacketAccess.entityId(rotation));
            assertEquals(90, rotation.getYHeadRot());

            buffer.clear();
            buffer.writeVarInt(321);
            var camera = ClientboundSetCameraPacket.STREAM_CODEC.decode(buffer);
            assertEquals(321, VanillaPacketAccess.entityId(camera));
            assertFalse(buffer.isReadable());
        } finally {
            buffer.release();
        }
    }

    @Test
    void preservesLookAtFallbackPositionAndEntityAnchor() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var positionOnly = new ClientboundPlayerLookAtPacket(Anchor.EYES, 1.25, -2.5, 100.75);
            ClientboundPlayerLookAtPacket.STREAM_CODEC.encode(buffer, positionOnly);
            var decoded = ClientboundPlayerLookAtPacket.STREAM_CODEC.decode(buffer);
            var target = VanillaPacketAccess.lookAtTarget(decoded);
            assertEquals(Anchor.EYES, decoded.getFromAnchor());
            assertEquals(new Vec3(1.25, -2.5, 100.75), target.position());
            assertTrue(target.entity().isEmpty());

            for (Anchor anchor : Anchor.values()) {
                buffer.clear();
                buffer.writeEnum(Anchor.FEET);
                buffer.writeDouble(4).writeDouble(5).writeDouble(6);
                buffer.writeBoolean(true);
                buffer.writeVarInt(999);
                buffer.writeEnum(anchor);
                decoded = ClientboundPlayerLookAtPacket.STREAM_CODEC.decode(buffer);
                target = VanillaPacketAccess.lookAtTarget(decoded);
                assertEquals(new Vec3(4, 5, 6), target.position());
                assertEquals(new VanillaPacketAccess.EntityTarget(999, anchor), target.entity().orElseThrow());
                assertFalse(buffer.isReadable());
            }
        } finally {
            buffer.release();
        }
    }

    @Test
    void readsMovementEntityIdWithoutCreatingAClientWorld() {
        var packet = new ClientboundMoveEntityPacket.Rot(12345, (byte) 10, (byte) 20, true);
        assertEquals(12345, VanillaPacketAccess.entityId(packet));
    }

    @Test
    void resolvesLinearAndSteppedMovementUsingVanillaDeltaCodec() {
        var linear = new ClientboundMoveEntityPacket.Pos(1, new VecDelta.Linear((short) 2048, (short) -1024, (short) 4096), true);
        var move = VanillaPacketAccess.movementDelta(linear);
        assertEquals(0.5, move.x);
        assertEquals(-0.25, move.y);
        assertEquals(1, move.z);

        var stepped = new ClientboundMoveEntityPacket.Pos(1, new VecDelta.Stepped(List.of(
            new VecDelta.Stepped.DeltaStep((short) 2048, (short) -1024, (short) 4096, 0),
            new VecDelta.Stepped.DeltaStep((short) -1024, (short) 2048, (short) -2048, 1))), true);
        move = VanillaPacketAccess.movementDelta(stepped);
        assertEquals(0.25, move.x);
        assertEquals(0.25, move.y);
        assertEquals(0.5, move.z);
    }
}

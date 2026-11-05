/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.VecDelta;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VanillaPacketAccessTest {
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

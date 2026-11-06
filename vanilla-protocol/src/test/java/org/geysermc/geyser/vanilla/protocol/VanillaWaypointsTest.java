/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.Waypoint;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VanillaWaypointsTest {
    @Test
    void readsEveryNativeLocationAfterAWireRoundTrip() {
        UUID id = UUID.fromString("d5fe5a3f-d044-4f1b-bafd-b5cae048f442");
        var icon = new Waypoint.Icon();
        icon.color = Optional.of(0xFF123456);
        var waypoints = List.of(
            TrackedWaypoint.setPosition(id, icon, new Vec3i(123, -64, -456)),
            TrackedWaypoint.setChunk(id, icon, new ChunkPos(-12, 34)),
            TrackedWaypoint.setAzimuth(id, icon, -1.25f),
            TrackedWaypoint.empty(id));
        var locations = List.of(
            new VanillaWaypoints.Coordinates(new Vec3i(123, -64, -456)),
            new VanillaWaypoints.Chunk(new ChunkPos(-12, 34)),
            new VanillaWaypoints.Azimuth(-1.25f),
            new VanillaWaypoints.Empty());
        var buffer = Unpooled.buffer();
        try {
            for (int i = 0; i < waypoints.size(); i++) {
                var waypoint = waypoints.get(i);
                TrackedWaypoint.STREAM_CODEC.encode(buffer, waypoint);
                var decoded = TrackedWaypoint.STREAM_CODEC.decode(buffer);
                assertEquals(locations.get(i), VanillaWaypoints.location(decoded));
                assertEquals(waypoint.id(), decoded.id());
                assertEquals(waypoint.icon().style, decoded.icon().style);
                assertEquals(waypoint.icon().color, decoded.icon().color);
                assertFalse(buffer.isReadable());
                buffer.clear();
            }
        } finally {
            buffer.release();
        }
    }

    @Test
    void readsNewValuesAfterNativeUpdatesAndCopiesMutablePositions() {
        UUID id = UUID.randomUUID();
        var icon = new Waypoint.Icon();
        var vector = new BlockPos.MutableBlockPos(1, 2, 3);
        var waypoint = TrackedWaypoint.setPosition(id, icon, vector);
        var snapshot = VanillaWaypoints.location(waypoint);
        vector.set(4, 5, 6);
        assertEquals(new VanillaWaypoints.Coordinates(new Vec3i(1, 2, 3)), snapshot);
        assertEquals(new VanillaWaypoints.Coordinates(new Vec3i(4, 5, 6)), VanillaWaypoints.location(waypoint));
        waypoint.update(TrackedWaypoint.setPosition(id, icon, new Vec3i(7, 8, 9)));
        assertEquals(new VanillaWaypoints.Coordinates(new Vec3i(7, 8, 9)), VanillaWaypoints.location(waypoint));

        waypoint = TrackedWaypoint.setChunk(id, icon, new ChunkPos(0, 0));
        waypoint.update(TrackedWaypoint.setChunk(id, icon, new ChunkPos(-1, 2)));
        assertEquals(new VanillaWaypoints.Chunk(new ChunkPos(-1, 2)), VanillaWaypoints.location(waypoint));

        waypoint = TrackedWaypoint.setAzimuth(id, icon, 0);
        waypoint.update(TrackedWaypoint.setAzimuth(id, icon, 2.5f));
        assertEquals(new VanillaWaypoints.Azimuth(2.5f), VanillaWaypoints.location(waypoint));
    }
}

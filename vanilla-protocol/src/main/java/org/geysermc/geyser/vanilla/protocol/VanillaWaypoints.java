/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.waypoints.TrackedWaypoint;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/** Reads native waypoint locations without constructing a client world or camera. */
public final class VanillaWaypoints {
    private static final Field COORDINATES = field("Vec3iWaypoint", "vector", Vec3i.class);
    private static final Field CHUNK = field("ChunkWaypoint", "chunkPos", ChunkPos.class);
    private static final Field AZIMUTH = field("AzimuthWaypoint", "angle", float.class);
    private static final Class<?> EMPTY = waypointClass("EmptyWaypoint");

    private VanillaWaypoints() {
    }

    public static Location location(TrackedWaypoint waypoint) {
        if (COORDINATES.owner().isInstance(waypoint)) {
            Vec3i position = (Vec3i) COORDINATES.handle().get(waypoint);
            return new Coordinates(new Vec3i(position.getX(), position.getY(), position.getZ()));
        }
        if (CHUNK.owner().isInstance(waypoint)) {
            return new Chunk((ChunkPos) CHUNK.handle().get(waypoint));
        }
        if (AZIMUTH.owner().isInstance(waypoint)) {
            return new Azimuth((float) AZIMUTH.handle().get(waypoint));
        }
        if (EMPTY.isInstance(waypoint)) {
            return new Empty();
        }
        throw new IllegalArgumentException("Unknown vanilla waypoint: " + waypoint.getClass().getName());
    }

    public sealed interface Location permits Coordinates, Chunk, Azimuth, Empty {
    }

    public record Coordinates(Vec3i position) implements Location {
    }

    public record Chunk(ChunkPos position) implements Location {
    }

    public record Azimuth(float angle) implements Location {
    }

    public record Empty() implements Location {
    }

    private record Field(Class<?> owner, VarHandle handle) {
    }

    private static Field field(String name, String field, Class<?> type) {
        Class<?> owner = waypointClass(name);
        try {
            return new Field(owner, MethodHandles.privateLookupIn(owner, MethodHandles.lookup()).findVarHandle(owner, field, type));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Class<?> waypointClass(String name) {
        try {
            return Class.forName(TrackedWaypoint.class.getName() + "$" + name, false, TrackedWaypoint.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}

/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import net.minecraft.util.BitStorage;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/** Exposes native palette indices for converting Java chunk storage to Bedrock storage. */
public final class VanillaPalettes {
    private static final VarHandle DATA;
    private static final VarHandle STORAGE;
    private static final VarHandle PALETTE;

    static {
        try {
            Class<?> data = Class.forName(PalettedContainer.class.getName() + "$Data", false, PalettedContainer.class.getClassLoader());
            var lookup = MethodHandles.lookup();
            DATA = MethodHandles.privateLookupIn(PalettedContainer.class, lookup).findVarHandle(PalettedContainer.class, "data", data);
            var dataLookup = MethodHandles.privateLookupIn(data, lookup);
            STORAGE = dataLookup.findVarHandle(data, "storage", BitStorage.class);
            PALETTE = dataLookup.findVarHandle(data, "palette", Palette.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private VanillaPalettes() {
    }

    /** The view remains valid until the container changes its palette or storage. */
    @SuppressWarnings("unchecked")
    public static <T> View<T> view(PalettedContainerRO<T> container) {
        if (!(container instanceof PalettedContainer<?>)) {
            throw new IllegalArgumentException("Unknown vanilla palette container: " + container.getClass().getName());
        }
        Object data = DATA.getVolatile(container);
        return new View<>((Palette<T>) PALETTE.get(data), (BitStorage) STORAGE.get(data));
    }

    public record View<T>(Palette<T> palette, BitStorage storage) {
    }
}

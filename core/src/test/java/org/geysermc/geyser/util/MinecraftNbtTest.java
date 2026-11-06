/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MinecraftNbtTest {
    @Test
    void roundTripsNativePrimitivesArraysAndHeterogeneousLists() {
        var original = new CompoundTag();
        original.putByte("byte", (byte) -1);
        original.putShort("short", (short) 1234);
        original.putInt("int", Integer.MIN_VALUE);
        original.putLong("long", Long.MAX_VALUE);
        original.putFloat("float", 1.25f);
        original.putDouble("double", -2.5);
        original.putString("string", "hello");
        original.putByteArray("bytes", new byte[]{1, -2, 3});
        original.putIntArray("ints", new int[]{Integer.MIN_VALUE, 0, Integer.MAX_VALUE});
        original.putLongArray("longs", new long[]{Long.MIN_VALUE, Long.MAX_VALUE});
        var mixed = new ListTag();
        mixed.add(IntTag.valueOf(42));
        mixed.add(StringTag.valueOf("mixed"));
        var nested = new CompoundTag();
        nested.putString("text", "component");
        mixed.add(nested);
        original.put("mixed", mixed);
        original.put("empty", new ListTag());

        NbtMap cloudburst = MinecraftNbt.toCloudburst(original);
        assertEquals(original, MinecraftNbt.fromCloudburst(cloudburst));
        var list = cloudburst.getList("mixed", NbtType.COMPOUND);
        assertEquals(List.of(42, "mixed", NbtMap.builder().putString("text", "component").build()), MinecraftNbt.unwrapList(list));
        assertEquals(List.of(), MinecraftNbt.unwrapList(cloudburst.getList("empty", NbtType.END)));
    }

    @Test
    void keepsMutableArraysIndependentInBothDirections() {
        byte[] bytes = {1, 2, 3};
        var original = NbtMap.builder().putByteArray("bytes", bytes).build();
        var nativeTag = MinecraftNbt.fromCloudburst(original);
        bytes[0] = 99;
        assertArrayEquals(new byte[]{1, 2, 3}, nativeTag.getByteArray("bytes").orElseThrow());
        var copy = MinecraftNbt.toCloudburst(nativeTag);
        nativeTag.getByteArray("bytes").orElseThrow()[1] = 88;
        assertArrayEquals(new byte[]{1, 2, 3}, copy.getByteArray("bytes"));
    }

    @Test
    void unwrapsNativeListWrappersWithoutUnwrappingOtherCompounds() {
        var wrapper = NbtMap.builder().putString("", "text").build();
        var compound = NbtMap.builder().putString("", "kept").putInt("other", 1).build();
        assertEquals(List.of("text", compound), MinecraftNbt.unwrapList(List.of(wrapper, compound)));
        assertThrows(IllegalArgumentException.class, () -> MinecraftNbt.fromCloudburst(new Object()));
    }
}

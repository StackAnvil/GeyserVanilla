/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.util;

import net.minecraft.nbt.*;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;

import java.util.ArrayList;
import java.util.List;

/** Converts vanilla Java NBT values to the Cloudburst values used for Bedrock translation. */
public final class MinecraftNbt {
    private MinecraftNbt() {
    }

    public static NbtMap toCloudburst(CompoundTag tag) {
        var builder = NbtMap.builder();
        tag.forEach((name, value) -> builder.put(name, toCloudburst(value)));
        return builder.build();
    }

    public static Object toCloudburst(Tag tag) {
        return switch (tag) {
            case CompoundTag compound -> toCloudburst(compound);
            case NumericTag number -> number.box();
            case StringTag string -> string.value();
            case ByteArrayTag array -> array.getAsByteArray().clone();
            case IntArrayTag array -> array.getAsIntArray().clone();
            case LongArrayTag array -> array.getAsLongArray().clone();
            case ListTag list -> toCloudburstList(list);
            case EndTag ignored -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private static NbtList<?> toCloudburstList(ListTag list) {
        if (list.isEmpty()) {
            return new NbtList<>(NbtType.END, List.of());
        }
        byte type = list.getFirst().getId();
        boolean mixed = list.stream().anyMatch(value -> value.getId() != type);
        List<Object> values = new ArrayList<>(list.size());
        for (Tag element : list) {
            Object value = toCloudburst(element);
            // This is vanilla's NBT representation for heterogeneous lists.
            if (mixed && !(value instanceof NbtMap)) {
                var wrapper = NbtMap.builder();
                wrapper.put("", value);
                value = wrapper.build();
            }
            values.add(value);
        }
        return new NbtList<>((NbtType<Object>) NbtType.byId(mixed ? Tag.TAG_COMPOUND : type), values);
    }

    public static List<Object> unwrapList(List<?> list) {
        ListTag nativeList = (ListTag) fromCloudburst(list);
        return nativeList.stream().map(MinecraftNbt::toCloudburst).toList();
    }

    public static CompoundTag fromCloudburst(NbtMap map) {
        CompoundTag tag = new CompoundTag();
        map.forEach((name, value) -> tag.put(name, fromCloudburst(value)));
        return tag;
    }

    public static Tag fromCloudburst(Object value) {
        return switch (value) {
            case NbtMap map -> fromCloudburst(map);
            case Byte number -> ByteTag.valueOf(number);
            case Short number -> ShortTag.valueOf(number);
            case Integer number -> IntTag.valueOf(number);
            case Long number -> LongTag.valueOf(number);
            case Float number -> FloatTag.valueOf(number);
            case Double number -> DoubleTag.valueOf(number);
            case Boolean bool -> ByteTag.valueOf(bool);
            case String string -> StringTag.valueOf(string);
            case byte[] array -> new ByteArrayTag(array.clone());
            case int[] array -> new IntArrayTag(array.clone());
            case long[] array -> new LongArrayTag(array.clone());
            case List<?> list -> {
                ListTag tag = new ListTag();
                list.forEach(element -> tag.addAndUnwrap(fromCloudburst(element)));
                yield tag;
            }
            case null -> EndTag.INSTANCE;
            default -> throw new IllegalArgumentException("Unsupported NBT value: " + value.getClass().getName());
        };
    }
}

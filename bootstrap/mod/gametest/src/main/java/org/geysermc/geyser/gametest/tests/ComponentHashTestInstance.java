/*
 * Copyright (c) 2025-2026 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.gametest.tests;

import com.google.common.hash.HashCode;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.Unpooled;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.HashOps;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.registry.java.JavaRegistryProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComponentHashTestInstance extends GeyserTestInstance {
    private static final MapCodec<List<TypedDataComponent<?>>> TYPED_COMPONENT_LIST_CODEC = DataComponentType.PERSISTENT_CODEC
        .dispatchMap("component", list -> list.getFirst().type(), ComponentHashTestInstance::typedComponentListCodec);
    private static final MapCodec<List<TypedDataComponent<?>>> MERGED_TYPED_COMPONENT_LIST_CODEC = TYPED_COMPONENT_LIST_CODEC.codec().listOf()
        .xmap(listOfLists -> listOfLists.stream().flatMap(List::stream).toList(), list -> {
            Map<DataComponentType<?>, List<TypedDataComponent<?>>> split = new HashMap<>();
            list.forEach(component -> {
                split.compute(component.type(), (_, typeList) -> {
                    if (typeList == null) {
                        typeList = new ArrayList<>();
                    }
                    typeList.add(component);
                    return typeList;
                });
            });
            return split.values().stream().toList();
        }).fieldOf("components");
    private static final MapCodec<List<TypedDataComponent<?>>> MERGED_AND_SINGLE_COMPONENT_MAP_CODEC = Codec.mapEither(MERGED_TYPED_COMPONENT_LIST_CODEC, TYPED_COMPONENT_LIST_CODEC)
        .xmap(Either::unwrap, Either::left);
    public static final MapCodec<ComponentHashTestInstance> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
        commonFields(instance)
            .and(MERGED_AND_SINGLE_COMPONENT_MAP_CODEC.forGetter(testInstance -> testInstance.testCases))
            .apply(instance, ComponentHashTestInstance::new)
    );

    private final List<TypedDataComponent<?>> testCases;

    private ComponentHashTestInstance(HolderGetter<TestEnvironmentDefinition<?>> testEnvironments, boolean required, List<TypedDataComponent<?>> testCases) {
        super(testEnvironments, required);
        this.testCases = testCases;
    }

    public List<TypedDataComponent<?>> testCases() {
        return testCases;
    }

    @Override
    public void run(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var hashOps = registries.createSerializationContext(HashOps.CRC32C_INSTANCE);
        var hasher = org.geysermc.geyser.vanilla.protocol.VanillaItemHashing.hasher(registries);
        for (TypedDataComponent<?> testCase : testCases) {
            var patch = net.minecraft.core.component.DataComponentPatch.builder().set(testCase).build();
            var edits = org.geysermc.geyser.vanilla.protocol.GeyserDataComponents.from(patch);
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
            try {
                net.minecraft.core.component.DataComponentPatch.STREAM_CODEC.encode(buffer, edits.toVanilla());
                var decoded = org.geysermc.geyser.vanilla.protocol.GeyserDataComponents.from(
                    net.minecraft.core.component.DataComponentPatch.STREAM_CODEC.decode(buffer));
                int expected = testCase.encodeValue(hashOps).getOrThrow().asInt();
                int actual = hasher.apply(decoded.getTyped(testCase.type()));
                helper.assertValueEqual(expected, actual, "hash for component " + testCase.type());
                helper.assertValueEqual(0, buffer.readableBytes(), "unread component bytes");
            } finally {
                buffer.release();
            }
        }

        // Succeed if nothing was thrown
        helper.succeed();
    }

    @Override
    public MapCodec<ComponentHashTestInstance> codec() {
        return MAP_CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        // TODO more descriptive?
        return Component.literal("Geyser Data Component Hash Test");
    }

    // Generics are NOT friendly!!!
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static MapCodec<List<TypedDataComponent<?>>> typedComponentListCodec(DataComponentType component) {
        return ExtraCodecs.compactListCodec(component.codecOrThrow(), ExtraCodecs.nonEmptyList(component.codecOrThrow().listOf()))
            .fieldOf("value")
            .xmap(
                values -> ((List<?>) values).stream()
                    .map(testCase -> new TypedDataComponent(component, testCase))
                    .map(testCase -> (TypedDataComponent<?>) testCase)
                    .toList(),
                typedComponents -> ((List<?>) typedComponents).stream()
                    .map(testCase -> ((TypedDataComponent<?>) testCase).value())
                    .toList());
    }
}

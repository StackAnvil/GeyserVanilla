/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket.AttributeSnapshot;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VanillaAttributesTest {
    @BeforeAll
    static void bootstrap() {
        VanillaBootstrap.defaultRegistries();
    }

    @Test
    void appliesModifierPhasesRegardlessOfPacketOrderWithoutChangingTheSnapshot() {
        var attribute = Holder.<net.minecraft.world.entity.ai.attributes.Attribute>direct(new RangedAttribute("test.attribute", 10, 0, 1000));
        var modifiers = new ArrayList<>(List.of(
            modifier("total_first", .2, Operation.ADD_MULTIPLIED_TOTAL),
            modifier("base_first", .5, Operation.ADD_MULTIPLIED_BASE),
            modifier("addition_first", 2, Operation.ADD_VALUE),
            modifier("total_second", -.5, Operation.ADD_MULTIPLIED_TOTAL),
            modifier("addition_second", 3, Operation.ADD_VALUE),
            modifier("base_second", .25, Operation.ADD_MULTIPLIED_BASE)));
        var original = List.copyOf(modifiers);
        var snapshot = new AttributeSnapshot(attribute, 10, modifiers);
        assertEquals(15.75, VanillaAttributes.calculateValue(snapshot), 1e-10);
        assertEquals(original, modifiers);
        Collections.reverse(modifiers);
        assertEquals(15.75, VanillaAttributes.calculateValue(snapshot), 1e-10);
        assertEquals(10, snapshot.base());
    }

    @Test
    void appliesNativeRangeAndNanHandling() {
        var attribute = Holder.<net.minecraft.world.entity.ai.attributes.Attribute>direct(new RangedAttribute("test.attribute", 10, 0, 20));
        assertEquals(20, VanillaAttributes.calculateValue(new AttributeSnapshot(attribute, 30, List.of())));
        assertEquals(0, VanillaAttributes.calculateValue(new AttributeSnapshot(attribute, -10, List.of())));
        assertEquals(0, VanillaAttributes.calculateValue(new AttributeSnapshot(attribute, Double.NaN, List.of())));
        assertEquals(20, VanillaAttributes.calculateValue(new AttributeSnapshot(attribute, 10,
            List.of(modifier("increase", 50, Operation.ADD_VALUE)))));
    }

    @Test
    void rejectsDuplicateModifierIdentifiersAsTheClientDoes() {
        var attribute = Holder.<net.minecraft.world.entity.ai.attributes.Attribute>direct(new RangedAttribute("test.attribute", 10, 0, 20));
        var snapshot = new AttributeSnapshot(attribute, 10, List.of(
            modifier("same", 1, Operation.ADD_VALUE), modifier("same", 2, Operation.ADD_VALUE)));
        assertThrows(IllegalArgumentException.class, () -> VanillaAttributes.calculateValue(snapshot));
    }

    @Test
    void decodesNativeSnapshotsWithRegistryKeysAndModifierValues() {
        var registries = VanillaBootstrap.defaultRegistries();
        var holder = registries.lookupOrThrow(Registries.ATTRIBUTE)
            .get(Identifier.withDefaultNamespace("movement_speed")).orElseThrow();
        var snapshot = new AttributeSnapshot(holder, .1,
            List.of(modifier("speed", .2, Operation.ADD_MULTIPLIED_TOTAL)));
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            AttributeSnapshot.STREAM_CODEC.encode(buffer, snapshot);
            var decoded = AttributeSnapshot.STREAM_CODEC.decode(buffer);
            assertEquals(Identifier.withDefaultNamespace("movement_speed"), VanillaAttributes.key(decoded));
            assertEquals(.12, VanillaAttributes.calculateValue(decoded), 1e-10);
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    private static AttributeModifier modifier(String name, double amount, Operation operation) {
        return new AttributeModifier(Identifier.parse("geyser:" + name), amount, operation);
    }
}

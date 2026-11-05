/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket.AttributeSnapshot;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import java.util.Objects;

/** Applies network attribute snapshots with vanilla's modifier and range rules. */
public final class VanillaAttributes {
    private VanillaAttributes() {
    }

    public static double calculateValue(AttributeSnapshot snapshot) {
        var instance = new AttributeInstance(snapshot.attribute(), ignored -> {});
        instance.setBaseValue(snapshot.base());
        snapshot.modifiers().forEach(instance::addTransientModifier);
        return instance.getValue();
    }

    public static Identifier key(AttributeSnapshot snapshot) {
        return snapshot.attribute().unwrapKey().map(key -> key.identifier())
            .orElseGet(() -> Objects.requireNonNull(BuiltInRegistries.ATTRIBUTE.getKey(snapshot.attribute().value())));
    }
}

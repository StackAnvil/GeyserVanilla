/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity;

import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.Constants;
import org.geysermc.geyser.api.entity.definition.GeyserEntityDefinition;
import org.geysermc.geyser.api.entity.definition.JavaEntityType;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.registry.java.BuiltInJavaRegistries;
import org.geysermc.geyser.util.MinecraftKey;
import net.minecraft.world.entity.EntityType;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

public record GeyserEntityType(Identifier identifier, EntityType<?> nativeType) implements JavaEntityType {
    private static final Identifier UNREGISTERED = Identifier.of(Constants.GEYSER_CUSTOM_NAMESPACE, "unregistered_sadface");

    private static final Map<EntityType<?>, GeyserEntityType> VANILLA = new IdentityHashMap<>();

    private GeyserEntityType(EntityType<?> builtin) {
        this(Identifier.of(EntityType.getKey(builtin).toString()), builtin);
    }

    public boolean isUnregistered() {
        return identifier.equals(UNREGISTERED);
    }

    @Override
    public boolean vanilla() {
        return true;
    }

    @Override
    public float width() {
        return definitionOrThrow().width();
    }

    @Override
    public float height() {
        return definitionOrThrow().height();
    }

    @Override
    public @Nullable GeyserEntityDefinition defaultBedrockDefinition() {
        return definitionOrThrow().defaultBedrockDefinition();
    }

    public Optional<EntityTypeDefinition<?>> definition() {
        return BuiltInJavaRegistries.ENTITY_TYPE.get(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getId(nativeType));
    }

    private EntityTypeDefinition<?> definitionOrThrow() {
        return BuiltInJavaRegistries.ENTITY_TYPE.getOrThrow(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getId(nativeType));
    }

    public boolean is(EntityType<?> type) {
        return this.nativeType == type;
    }

    public static GeyserEntityType ofVanilla(EntityType<?> builtin) {
        return VANILLA.computeIfAbsent(builtin, GeyserEntityType::new);
    }

    public static GeyserEntityType ofVanilla(Identifier javaIdentifier) {
        return ofVanilla(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath(javaIdentifier.namespace(), javaIdentifier.path())));
    }

    public static GeyserEntityType of(int javaId) {
        return ofVanilla(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.byId(javaId));
    }

    @Nullable
    public static GeyserEntityType of(Key javaKey) {
        if (javaKey.namespace().equals(Key.MINECRAFT_NAMESPACE)) {
            try {
                return ofVanilla(MinecraftKey.keyToIdentifier(javaKey));
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }
        throw new IllegalArgumentException("Unsupported Java entity key: " + javaKey.getClass().getName());
    }

    public static GeyserEntityType of(EntityType<?> type) {
        return ofVanilla(type);
    }
}

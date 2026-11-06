/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.translator.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.geysermc.geyser.vanilla.runtime.VanillaClassLoader;
import org.geysermc.geyser.vanilla.runtime.VanillaRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HostedMessageTranslatorTest {
    @Test
    @Timeout(30)
    void translatesStyledChatWhileAdventureAndVanillaUseDifferentGsonClasses() throws Exception {
        var classpath = Arrays.stream(System.getProperty("geyser.test.engine.classpath").split(File.pathSeparator))
            .map(Path::of).toList();
        try (var loader = new VanillaClassLoader(VanillaRuntime.urls(classpath), getClass().getClassLoader(),
            List.of("net.kyori.", "org.slf4j.", "org.geysermc.geyser.vanilla.runtime."))) {
            assertNotSame(com.google.gson.JsonElement.class, loader.loadClass("com.google.gson.JsonElement"));
            assertSame(Component.class, loader.loadClass(Component.class.getName()));
            Class<?> nativeComponent = loader.loadClass("net.minecraft.network.chat.Component");
            Class<?> nativeAccess = loader.loadClass("net.minecraft.core.RegistryAccess");
            Object registries = loader.loadClass("org.geysermc.geyser.vanilla.protocol.VanillaBootstrap")
                .getMethod("defaultRegistries").invoke(null);
            Class<?> translator = loader.loadClass(MessageTranslator.class.getName());
            var fromVanilla = translator.getMethod("fromVanilla", nativeComponent, nativeAccess);
            var toVanilla = translator.getMethod("toVanilla", Component.class, nativeAccess);

            Object literal = nativeComponent.getMethod("literal", String.class).invoke(null, "Native chat");
            assertEquals(Component.text("Native chat"), fromVanilla.invoke(null, literal, registries));

            Component styled = Component.text("Adventure chat", TextColor.color(0x1f4c7d)).decorate(TextDecoration.BOLD);
            Object nativeStyled = toVanilla.invoke(null, styled, registries);
            assertEquals("Adventure chat", nativeComponent.getMethod("getString").invoke(nativeStyled));
            assertEquals(styled, fromVanilla.invoke(null, nativeStyled, registries));
        }
    }
}

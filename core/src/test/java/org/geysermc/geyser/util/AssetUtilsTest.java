/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.util;

import com.google.gson.JsonParser;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.vanilla.runtime.VanillaRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssetUtilsTest {
    @TempDir
    Path folder;

    @Test
    void extractsTheEnglishLocaleFromTheLocalClientWithoutNetworkRequests() throws Exception {
        try (var singleton = mockStatic(GeyserImpl.class)) {
            setup(singleton);
            try (var network = mockStatic(WebUtils.class)) {
                Path file = folder.resolve("locales/en_us.json");
                var completed = new AtomicInteger();
                AssetUtils.addTask(true, new AssetUtils.ClientJarTask("assets/minecraft/lang/en_us.json",
                    input -> AssetUtils.saveFile(file, input), completed::incrementAndGet));
                AssetUtils.runClientJarTasks();
                var locale = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                assertEquals("Singleplayer", locale.get("menu.singleplayer").getAsString());
                assertEquals(1, completed.get());
                assertEquals(VanillaRuntime.manifest().artifacts().getFirst().sha1(), Files.readString(folder.resolve("cache/client_jar.hash")));
                assertFalse(Files.exists(folder.resolve("tmp_locale.jar")));
                network.verifyNoInteractions();
            }
        }
    }

    @Test
    void usesTheExistingCacheButRepairsMissingAssets() throws Exception {
        try (var singleton = mockStatic(GeyserImpl.class)) {
            setup(singleton);
            Path file = folder.resolve("asset.json");
            var completed = new AtomicInteger();
            AssetUtils.addTask(true, new AssetUtils.ClientJarTask("assets/minecraft/lang/en_us.json",
                input -> AssetUtils.saveFile(file, input), completed::incrementAndGet));
            AssetUtils.runClientJarTasks();
            Files.writeString(file, "existing cached asset");
            AssetUtils.addTask(false, new AssetUtils.ClientJarTask("assets/minecraft/lang/en_us.json",
                input -> fail("The unchanged client must keep the cached file"), completed::incrementAndGet));
            AssetUtils.runClientJarTasks();
            assertEquals("existing cached asset", Files.readString(file));
            assertEquals(2, completed.get());
            Files.delete(file);
            AssetUtils.addTask(true, new AssetUtils.ClientJarTask("assets/minecraft/lang/en_us.json",
                input -> AssetUtils.saveFile(file, input), completed::incrementAndGet));
            AssetUtils.runClientJarTasks();
            assertTrue(JsonParser.parseString(Files.readString(file)).getAsJsonObject().has("menu.singleplayer"));
            assertEquals(3, completed.get());
        }
    }

    @Test
    void doesNotMarkExtractionCompleteWhenAnAssetIsMissingFromTheClient() {
        try (var singleton = mockStatic(GeyserImpl.class)) {
            GeyserLogger logger = setup(singleton);
            var completed = new AtomicInteger();
            AssetUtils.addTask(true, new AssetUtils.ClientJarTask("assets/minecraft/missing-fixture.json",
                input -> fail("A missing client asset cannot be extracted"), completed::incrementAndGet));
            AssetUtils.runClientJarTasks();
            assertEquals(0, completed.get());
            assertFalse(Files.exists(folder.resolve("cache/client_jar.hash")));
            verify(logger).error(anyString(), isA(java.io.IOException.class));
        }
    }

    private GeyserLogger setup(org.mockito.MockedStatic<GeyserImpl> singleton) {
        var geyser = mock(GeyserImpl.class);
        var bootstrap = mock(GeyserBootstrap.class);
        var logger = mock(GeyserLogger.class);
        singleton.when(GeyserImpl::getInstance).thenReturn(geyser);
        when(geyser.getBootstrap()).thenReturn(bootstrap);
        when(geyser.getLogger()).thenReturn(logger);
        when(bootstrap.getConfigFolder()).thenReturn(folder);
        when(bootstrap.getGeyserLogger()).thenReturn(logger);
        when(bootstrap.getResourceOrNull(anyString())).thenAnswer(invocation ->
            getClass().getClassLoader().getResourceAsStream(invocation.getArgument(0)));
        GeyserLocale.init(bootstrap);
        return logger;
    }
}

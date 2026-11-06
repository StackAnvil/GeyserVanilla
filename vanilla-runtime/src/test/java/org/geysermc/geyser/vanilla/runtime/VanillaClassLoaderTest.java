/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VanillaClassLoaderTest {
    @TempDir
    Path temporary;

    @Test
    void prefersClientLibrariesAndResourcesWhileSharingSelectedHostApis() throws Exception {
        Path host = compile("host", fixtures("host"));
        Path client = compile("client", fixtures("client"));
        Files.writeString(host.resolve("version.json"), "host version");
        Files.writeString(client.resolve("version.json"), "client version");
        try (var parent = new URLClassLoader(new URL[] {host.toUri().toURL()}, getClass().getClassLoader());
             var loader = new VanillaClassLoader(new URL[] {client.toUri().toURL()}, parent, List.of("fixture.api."))) {
            Class<?> engine = loader.loadClass("fixture.Engine");
            assertEquals("host:client", engine.getMethod("libraries").invoke(null));
            assertSame(parent.loadClass("fixture.api.HostApi"), loader.loadClass("fixture.api.HostApi"));
            assertNotSame(parent.loadClass("fixture.ClientLibrary"), loader.loadClass("fixture.ClientLibrary"));
            assertSame(loader, engine.getClassLoader());
            assertSame(String.class, loader.loadClass("java.lang.String"));
            assertSame(VanillaRuntime.class, loader.loadClass(VanillaRuntime.class.getName()));
            try (var resource = loader.getResourceAsStream("version.json")) {
                assertEquals("client version", new String(resource.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            }
            assertEquals(List.of(client.resolve("version.json").toUri().toURL(), host.resolve("version.json").toUri().toURL()),
                Collections.list(loader.getResources("version.json")));
        }
    }

    @Test
    void cleansUpLoadedRuntimeOnceBeforeClosing() throws Exception {
        String bootstrapName = "org.geysermc.geyser.vanilla.protocol.VanillaBootstrap";
        Path classes = compile("lifecycle", Map.of(bootstrapName, """
            package org.geysermc.geyser.vanilla.protocol;
            public class VanillaBootstrap {
                public static int shutdowns;
                public static void shutdown() { shutdowns++; }
            }
            """));
        var loader = new VanillaClassLoader(new URL[] {classes.toUri().toURL()}, getClass().getClassLoader(), List.of());
        Class<?> bootstrap = loader.loadClass(bootstrapName);
        loader.close();
        loader.close();
        assertEquals(1, bootstrap.getField("shutdowns").get(null));
    }

    @Test
    void rejectsHostGameClassesMissingFromTheClientRuntime() throws Exception {
        String hostOnly = "net.minecraft.runtime_fixture.HostOnly";
        Path host = compile("game-host", Map.of(hostOnly,
            "package net.minecraft.runtime_fixture; public class HostOnly {}"));
        try (var parent = new URLClassLoader(new URL[] {host.toUri().toURL()}, getClass().getClassLoader());
             var loader = new VanillaClassLoader(new URL[0], parent, List.of("net.minecraft."))) {
            assertNotNull(parent.loadClass(hostOnly));
            assertThrows(ClassNotFoundException.class, () -> loader.loadClass(hostOnly));
        }
    }

    @Test
    void doesNotLoadRuntimeOnlyToCloseIt() throws Exception {
        Path classes = compile("early-failure", fixtures("client"));
        var loader = new VanillaClassLoader(new URL[] {classes.toUri().toURL()}, getClass().getClassLoader(), List.of());
        assertDoesNotThrow(loader::close);
        assertThrows(ClassNotFoundException.class, () -> loader.loadClass("fixture.Engine"));
    }

    private static Map<String, String> fixtures(String source) {
        return Map.of(
            "fixture.api.HostApi", "package fixture.api; public class HostApi { public static String name() { return \"" + source + "\"; } }",
            "fixture.ClientLibrary", "package fixture; public class ClientLibrary { public static String name() { return \"" + source + "\"; } }",
            "fixture.Engine", """
                package fixture;
                public class Engine {
                    public static String libraries() { return fixture.api.HostApi.name() + ":" + ClientLibrary.name(); }
                }
                """);
    }

    private Path compile(String name, Map<String, String> sources) throws Exception {
        Path classes = temporary.resolve(name);
        Files.createDirectories(classes);
        var arguments = new ArrayList<>(List.of("-d", classes.toString()));
        for (var source : sources.entrySet()) {
            Path path = classes.resolve(source.getKey().replace('.', '/') + ".java");
            Files.createDirectories(path.getParent());
            Files.writeString(path, source.getValue());
            arguments.add(path.toString());
        }
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, arguments.toArray(String[]::new)));
        return classes;
    }
}

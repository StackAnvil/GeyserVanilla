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
    void resolvesPluginCodeSourcesWithLoaderFragmentsAndEscapedPaths() throws Exception {
        Path plugin = temporary.resolve("plugin + name.jar");
        var location = plugin.toUri().toURL();
        assertEquals(plugin, VanillaHostedRuntime.pluginLocation(location));
        assertEquals(plugin, VanillaHostedRuntime.pluginLocation(new URL(location + "#loader-123")));
        assertThrows(java.io.IOException.class,
            () -> VanillaHostedRuntime.pluginLocation(new URL(location + "?unsupported=query#loader-123")));
    }

    @Test
    void retainsHostLoggingConfigurationWhenTheLoggingFrameworkIsShared() throws Exception {
        Path host = Files.createDirectory(temporary.resolve("host-logging"));
        Path client = Files.createDirectory(temporary.resolve("client-logging"));
        Files.writeString(host.resolve("log4j2.xml"), "host appenders");
        Files.writeString(client.resolve("log4j2.xml"), "graphical client appenders");
        try (var parent = new URLClassLoader(new URL[] {host.toUri().toURL()}, null);
             var loader = new VanillaClassLoader(new URL[] {client.toUri().toURL()}, parent,
                 List.of("org.apache.logging.log4j."))) {
            assertEquals(host.resolve("log4j2.xml").toUri().toURL(), loader.getResource("log4j2.xml"));
            assertEquals(List.of(host.resolve("log4j2.xml").toUri().toURL()),
                Collections.list(loader.getResources("log4j2.xml")));
            Files.delete(host.resolve("log4j2.xml"));
            assertNull(loader.getResource("log4j2.xml"));
            assertFalse(loader.getResources("log4j2.xml").hasMoreElements());
        }
    }

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

    @Test
    void callsHostedDelegatesWithTheirContextAndPreservesTheirFailures() throws Exception {
        Path classes = compile("delegate", Map.of("fixture.Delegate", """
            package fixture;
            public class Delegate {
                public Delegate(String value) throws java.io.IOException {
                    if (Thread.currentThread().getContextClassLoader() != getClass().getClassLoader()) {
                        throw new AssertionError("wrong constructor context");
                    }
                    if (value.equals("fail")) { throw new java.io.IOException("constructor failed"); }
                }
                public ClassLoader context() { return Thread.currentThread().getContextClassLoader(); }
                public void fail() throws java.io.IOException { throw new java.io.IOException("delegate failed"); }
                public void fatal() { throw new AssertionError("delegate error"); }
            }
            """));
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        var loader = new VanillaClassLoader(new URL[] {classes.toUri().toURL()}, getClass().getClassLoader(), List.of());
        try (var runtime = new VanillaHostedRuntime(loader)) {
            Object delegate = runtime.create("fixture.Delegate", new Class<?>[] {String.class}, "ok");
            assertSame(previous, Thread.currentThread().getContextClassLoader());
            assertSame(loader, runtime.invoke(delegate, "context", new Class<?>[0]));
            var failure = assertThrows(java.io.IOException.class,
                () -> runtime.invoke(delegate, "fail", new Class<?>[0]));
            assertEquals("delegate failed", failure.getMessage());
            assertSame(previous, Thread.currentThread().getContextClassLoader());
            var fatal = assertThrows(AssertionError.class,
                () -> runtime.invoke(delegate, "fatal", new Class<?>[0]));
            assertEquals("delegate error", fatal.getMessage());
            assertSame(previous, Thread.currentThread().getContextClassLoader());
            var constructor = assertThrows(java.io.IOException.class,
                () -> runtime.create("fixture.Delegate", new Class<?>[] {String.class}, "fail"));
            assertEquals("constructor failed", constructor.getMessage());
        }
        assertSame(previous, Thread.currentThread().getContextClassLoader());
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

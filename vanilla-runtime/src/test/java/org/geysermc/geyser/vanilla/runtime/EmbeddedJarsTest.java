/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddedJarsTest {
    @TempDir
    Path temporary;

    @Test
    void loadsRecursiveFabricAndNeoForgeDependenciesAndDeduplicatesThem() throws Exception {
        Path source = temporary.resolve("EmbeddedLibrary.java");
        Files.writeString(source, "public class EmbeddedLibrary { public static String message() { return \"loaded embedded class\"; } }");
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", temporary.toString(), source.toString()));
        byte[] dependency = jar(Map.of("nested-resource.txt", "nested dependency".getBytes(StandardCharsets.UTF_8),
            "EmbeddedLibrary.class", Files.readAllBytes(temporary.resolve("EmbeddedLibrary.class"))));
        byte[] bridge = jar(Map.of("META-INF/jars/library.jar", dependency));
        Path plugin = temporary.resolve("mod.jar");
        Files.write(plugin, jar(Map.of("META-INF/jars/bridge.jar", bridge,
            "META-INF/jarjar/duplicate.jar", dependency)));
        List<Path> paths = EmbeddedJars.classpath(plugin, temporary.resolve("cache"));
        assertEquals(plugin, paths.getFirst());
        assertEquals(3, paths.size());
        try (var loader = new VanillaClassLoader(VanillaRuntime.urls(paths), getClass().getClassLoader(), List.of());
             var input = loader.getResourceAsStream("nested-resource.txt")) {
            assertNotNull(input);
            assertEquals("nested dependency", new String(input.readAllBytes(), StandardCharsets.UTF_8));
            Class<?> library = loader.loadClass("EmbeddedLibrary");
            assertSame(loader, library.getClassLoader());
            assertEquals("loaded embedded class", library.getMethod("message").invoke(null));
        }
    }

    @Test
    void repairsChangedCacheEntriesAndDoesNotUseEmbeddedNamesAsDiskPaths() throws Exception {
        byte[] dependency = jar(Map.of("value", new byte[] {1, 2, 3}));
        Path plugin = temporary.resolve("mod.jar");
        Files.write(plugin, jar(Map.of("META-INF/jars/../../outside.jar", dependency)));
        Path cache = temporary.resolve("cache");
        List<Path> paths = EmbeddedJars.classpath(plugin, cache);
        Path extracted = paths.get(1);
        assertEquals(cache, extracted.getParent());
        assertArrayEquals(dependency, Files.readAllBytes(extracted));
        Files.writeString(extracted, "corrupted cache");
        assertEquals(paths, EmbeddedJars.classpath(plugin, cache));
        assertArrayEquals(dependency, Files.readAllBytes(extracted));
        assertFalse(Files.exists(temporary.resolve("outside.jar")));
    }

    @Test
    void allowsDevelopmentClassDirectories() throws Exception {
        Path classes = Files.createDirectory(temporary.resolve("classes"));
        Path cache = temporary.resolve("cache");
        assertEquals(List.of(classes), EmbeddedJars.classpath(classes, cache));
        assertFalse(Files.exists(cache));
    }

    private static byte[] jar(Map<String, byte[]> entries) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var jar = new JarOutputStream(bytes)) {
            for (var entry : entries.entrySet()) {
                jar.putNextEntry(new JarEntry(entry.getKey()));
                jar.write(entry.getValue());
                jar.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}

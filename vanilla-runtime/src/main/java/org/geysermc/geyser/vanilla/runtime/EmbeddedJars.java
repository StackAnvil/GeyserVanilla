/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.jar.JarFile;

/** Makes Fabric and NeoForge's embedded dependencies available to the isolated engine. */
final class EmbeddedJars {
    private EmbeddedJars() {
    }

    static List<Path> classpath(Path plugin, Path cache) throws IOException {
        var paths = new LinkedHashSet<Path>();
        var pending = new ArrayDeque<Path>();
        paths.add(plugin);
        pending.add(plugin);
        while (!pending.isEmpty()) {
            Path archive = pending.removeFirst();
            // Development launches may use a classes directory rather than a packaged mod.
            if (Files.isDirectory(archive)) {
                continue;
            }
            try (var jar = new JarFile(archive.toFile())) {
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    String name = entry.getName();
                    if (entry.isDirectory() || !name.endsWith(".jar")
                        || !(name.startsWith("META-INF/jars/") || name.startsWith("META-INF/jarjar/"))) {
                        continue;
                    }
                    byte[] content;
                    try (var input = jar.getInputStream(entry)) {
                        content = input.readAllBytes();
                    }
                    Path extracted = cache.resolve(hash(content) + ".jar");
                    if (!Files.isRegularFile(extracted) || Files.size(extracted) != content.length
                        || !Arrays.equals(content, Files.readAllBytes(extracted))) {
                        Files.createDirectories(cache);
                        Path temporary = Files.createTempFile(cache, "embedded-", ".part");
                        try {
                            Files.write(temporary, content);
                            Files.move(temporary, extracted, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        } finally {
                            Files.deleteIfExists(temporary);
                        }
                    }
                    if (paths.add(extracted)) {
                        pending.addLast(extracted);
                    }
                }
            }
        }
        return List.copyOf(paths);
    }

    private static String hash(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}

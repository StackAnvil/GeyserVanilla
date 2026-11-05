/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class VanillaRuntimeTest {
    @TempDir
    Path cache;

    @Test
    void downloadsOnceAndRepairsCorruptCachedArtifacts() throws Exception {
        byte[] content = "verified client bytes".getBytes(StandardCharsets.UTF_8);
        var artifact = artifact(content, "");
        var manifest = manifest(artifact);
        AtomicInteger downloads = new AtomicInteger();
        VanillaRuntime.Downloader downloader = (uri, destination) -> {
            downloads.incrementAndGet();
            Files.write(destination, content);
        };

        List<Path> first = VanillaRuntime.prepare(manifest, cache, downloader, message -> {});
        assertEquals(1, downloads.get());
        assertArrayEquals(content, Files.readAllBytes(first.getFirst()));
        assertEquals(first, VanillaRuntime.prepare(manifest, cache, downloader, message -> {}));
        assertEquals(1, downloads.get());

        // Same-length corruption must still be detected by the checksum.
        Files.write(first.getFirst(), new byte[content.length]);
        VanillaRuntime.prepare(manifest, cache, downloader, message -> {});
        assertEquals(2, downloads.get());
        assertArrayEquals(content, Files.readAllBytes(first.getFirst()));
    }

    @Test
    void rejectsWrongDownloadAndRemovesTemporaryFile() throws Exception {
        var artifact = artifact(new byte[] {1, 2, 3}, "");
        IOException error = assertThrows(IOException.class, () -> VanillaRuntime.prepare(manifest(artifact), cache,
            (uri, destination) -> Files.write(destination, new byte[] {3, 2, 1}), message -> {}));
        assertTrue(error.getMessage().contains("SHA-1 mismatch"));
        try (var files = Files.list(cache.resolve("artifacts"))) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void removesPartialDownloadAfterNetworkFailure() throws Exception {
        var artifact = artifact(new byte[] {1, 2, 3}, "");
        assertThrows(IOException.class, () -> VanillaRuntime.prepare(manifest(artifact), cache,
            (uri, destination) -> {
                Files.write(destination, new byte[] {1});
                throw new IOException("Connection reset");
            }, message -> {}));
        try (var files = Files.list(cache.resolve("artifacts"))) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void skipsLibrariesForOtherOperatingSystems() throws Exception {
        String current = System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT);
        String different = current.contains("win") ? "osx" : "windows";
        var artifact = artifact(new byte[] {1}, different);
        List<Path> classpath = VanillaRuntime.prepare(manifest(artifact), cache, (uri, destination) -> {
            fail("A library for another OS must not be downloaded");
        }, message -> {});
        assertTrue(classpath.isEmpty());
    }

    @Test
    void readsPinnedClientAsFirstClasspathEntry() throws Exception {
        var manifest = VanillaRuntime.manifest();
        assertEquals("minecraft-client:" + manifest.version(), manifest.artifacts().getFirst().name());
        assertEquals(25, manifest.javaVersion());
        assertTrue(manifest.artifacts().stream().anyMatch(a -> a.name().startsWith("com.mojang:authlib:")));
        assertTrue(manifest.artifacts().stream().anyMatch(a -> a.name().startsWith("io.netty:netty-buffer:")));
    }

    @Test
    void rejectsMalformedManifest() {
        var input = new ByteArrayInputStream("artifact.count=1\n".getBytes(StandardCharsets.ISO_8859_1));
        IOException error = assertThrows(IOException.class, () -> VanillaRuntime.RuntimeManifest.read(input));
        assertEquals("Invalid vanilla runtime manifest", error.getMessage());
    }

    private static VanillaRuntime.Artifact artifact(byte[] bytes, String os) throws Exception {
        String sha1 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
        return new VanillaRuntime.Artifact("test-artifact", URI.create("https://example.invalid/library.jar"),
            sha1, bytes.length, os);
    }

    private static VanillaRuntime.RuntimeManifest manifest(VanillaRuntime.Artifact artifact) {
        return new VanillaRuntime.RuntimeManifest("test", 25, List.of(artifact));
    }
}

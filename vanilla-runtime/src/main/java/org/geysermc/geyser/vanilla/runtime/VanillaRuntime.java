/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Consumer;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Downloads the exact vanilla client and libraries selected when this Geyser build was compiled. */
public final class VanillaRuntime {
    private static final String LOCK_RESOURCE = "/vanilla-runtime.properties";
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private VanillaRuntime() {
    }

    public static RuntimeManifest manifest() throws IOException {
        try (InputStream input = VanillaRuntime.class.getResourceAsStream(LOCK_RESOURCE)) {
            if (input == null) {
                throw new IOException("Missing " + LOCK_RESOURCE);
            }
            return RuntimeManifest.read(input);
        }
    }

    public static List<Path> prepare(Path cache, Consumer<String> logger) throws IOException {
        return prepare(manifest(), cache, VanillaRuntime::download, logger);
    }

    // Serialize access in this JVM as well as between processes sharing a cache.
    static synchronized List<Path> prepare(RuntimeManifest manifest, Path cache, Downloader downloader,
                                           Consumer<String> logger) throws IOException {
        Files.createDirectories(cache);
        Path artifacts = cache.resolve("artifacts");
        Files.createDirectories(artifacts);
        try (FileChannel channel = FileChannel.open(cache.resolve("runtime.lock"),
            StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.lock()) {
            List<Path> classpath = new ArrayList<>();
            String os = operatingSystem();
            for (Artifact artifact : manifest.artifacts()) {
                if (!artifact.os().isEmpty() && !artifact.os().equals(os)) {
                    continue;
                }
                Path destination = artifacts.resolve(artifact.sha1() + ".jar");
                if (!matches(destination, artifact)) {
                    logger.accept("Downloading " + artifact.name());
                    Path temporary = Files.createTempFile(artifacts, "download-", ".tmp");
                    try {
                        downloader.download(artifact.uri(), temporary);
                        if (!matches(temporary, artifact)) {
                            throw new IOException("Size or SHA-1 mismatch for " + artifact.name());
                        }
                        try {
                            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.ATOMIC_MOVE);
                        } catch (AtomicMoveNotSupportedException ignored) {
                            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } finally {
                        Files.deleteIfExists(temporary);
                    }
                }
                classpath.add(destination.toAbsolutePath());
            }
            return List.copyOf(classpath);
        }
    }

    private static void download(URI uri, Path destination) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(5)).GET().build();
        try {
            HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() != 200) {
                    throw new IOException("HTTP " + response.statusCode() + " downloading " + uri);
                }
                Files.copy(body, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted downloading " + uri, e);
        }
    }

    private static boolean matches(Path path, Artifact artifact) throws IOException {
        if (!Files.isRegularFile(path) || Files.size(path) != artifact.size()) {
            return false;
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[65536];
            int length;
            while ((length = input.read(buffer)) != -1) {
                digest.update(buffer, 0, length);
            }
        }
        return HexFormat.of().formatHex(digest.digest()).equals(artifact.sha1());
    }

    private static String operatingSystem() {
        String name = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (name.contains("mac") || name.contains("darwin")) {
            return "osx";
        }
        if (name.contains("win")) {
            return "windows";
        }
        return "linux";
    }

    public static URL[] urls(List<Path> classpath) throws IOException {
        List<URL> urls = new ArrayList<>();
        for (Path entry : classpath) {
            urls.add(entry.toUri().toURL());
        }
        return urls.toArray(URL[]::new);
    }

    /** Used by Gradle to prepare compile-only game dependencies without packaging Mojang's code. */
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: VanillaRuntime <cache-directory>");
        }
        prepare(Path.of(args[0]), System.out::println);
    }

    @FunctionalInterface
    interface Downloader {
        void download(URI uri, Path destination) throws IOException;
    }

    public record Artifact(String name, URI uri, String sha1, long size, String os) {
        public Artifact {
            if (name.isBlank() || !"https".equals(uri.getScheme()) || !sha1.matches("[0-9a-f]{40}") || size <= 0
                || !List.of("", "linux", "windows", "osx").contains(os)) {
                throw new IllegalArgumentException("Invalid vanilla artifact: " + name);
            }
        }
    }

    public record RuntimeManifest(String version, int javaVersion, List<Artifact> artifacts) {
        public RuntimeManifest {
            artifacts = List.copyOf(artifacts);
            if (version.isBlank() || javaVersion < 21 || artifacts.isEmpty()) {
                throw new IllegalArgumentException("Invalid vanilla runtime manifest");
            }
        }

        public static RuntimeManifest read(InputStream input) throws IOException {
            Properties properties = new Properties();
            properties.load(input);
            try {
                int count = Integer.parseInt(properties.getProperty("artifact.count"));
                List<Artifact> artifacts = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String prefix = "artifact." + i + ".";
                    artifacts.add(new Artifact(properties.getProperty(prefix + "name"),
                        URI.create(properties.getProperty(prefix + "url")),
                        properties.getProperty(prefix + "sha1"),
                        Long.parseLong(properties.getProperty(prefix + "size")),
                        properties.getProperty(prefix + "os", "")));
                }
                return new RuntimeManifest(properties.getProperty("version"),
                    Integer.parseInt(properties.getProperty("java.version")), artifacts);
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new IOException("Invalid vanilla runtime manifest", e);
            }
        }
    }
}

/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.util;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Owns the optional remote language index request until completion or shutdown. */
public final class AssetIndexLoader implements AutoCloseable {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL).build();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(
        Thread.ofPlatform().daemon(true).name("Geyser asset index").factory());
    private final Future<?> task;
    private volatile boolean closed;

    AssetIndexLoader(URI manifest, String version, String userAgent,
                     Consumer<Map<String, AssetUtils.Asset>> loaded, Consumer<Throwable> failed) {
        task = executor.submit(() -> {
            try {
                var versions = JsonParser.parseString(get(manifest, userAgent)).getAsJsonObject().getAsJsonArray("versions");
                URI metadata = null;
                for (var entry : versions) {
                    var candidate = entry.getAsJsonObject();
                    if (version.equals(candidate.get("id").getAsString())) {
                        metadata = URI.create(candidate.get("url").getAsString());
                        break;
                    }
                }
                if (metadata == null) {
                    throw new IOException("Minecraft version is missing from the asset manifest: " + version);
                }
                var info = JsonParser.parseString(get(metadata, userAgent)).getAsJsonObject();
                URI index = URI.create(info.getAsJsonObject("assetIndex").get("url").getAsString());
                var objects = JsonParser.parseString(get(index, userAgent)).getAsJsonObject().getAsJsonObject("objects");
                var gson = new Gson();
                Map<String, AssetUtils.Asset> assets = new HashMap<>();
                objects.entrySet().stream().filter(entry -> entry.getKey().startsWith("minecraft/lang/"))
                    .forEach(entry -> assets.put(entry.getKey(), gson.fromJson(entry.getValue(), AssetUtils.Asset.class)));
                if (!closed) {
                    loaded.accept(Map.copyOf(assets));
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } catch (Exception failure) {
                if (!closed) {
                    failed.accept(failure);
                }
            }
        });
        executor.shutdown();
    }

    private String get(URI uri, String userAgent) throws IOException, InterruptedException {
        if (closed) {
            throw new InterruptedException("Asset index loader is closed");
        }
        var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10))
            .header("User-Agent", userAgent).GET().build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " fetching Minecraft assets: " + uri);
        }
        return response.body();
    }

    @Override
    public void close() {
        closed = true;
        task.cancel(true);
        http.shutdownNow();
        executor.shutdownNow();
        boolean interrupted = false;
        try {
            while (!executor.isTerminated()) {
                try {
                    executor.awaitTermination(1, TimeUnit.SECONDS);
                } catch (InterruptedException interruption) {
                    interrupted = true;
                }
            }
            // Wait for the HTTP client's selector and request tasks before its loader closes.
            http.close();
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

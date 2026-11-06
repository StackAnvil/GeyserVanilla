/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.util;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.geysermc.geyser.GeyserImpl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(15)
class AssetIndexLoaderTest {
    @Test
    void loadsLanguageAssetsForThePinnedVersion() throws Exception {
        var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext("/manifest", exchange -> reply(exchange,
            "{\"versions\":[{\"id\":\"fixture\",\"url\":\"" + base + "/version\"}]}"));
        server.createContext("/version", exchange -> reply(exchange,
            "{\"assetIndex\":{\"url\":\"" + base + "/assets\"}}"));
        server.createContext("/assets", exchange -> reply(exchange,
            "{\"objects\":{\"minecraft/lang/de_de.json\":{\"hash\":\"abc123\",\"size\":42},"
                + "\"minecraft/sounds/example.ogg\":{\"hash\":\"def456\",\"size\":100}}}"));
        server.start();
        var result = new CompletableFuture<Map<String, AssetUtils.Asset>>();
        try (var loader = new AssetIndexLoader(URI.create(base + "/manifest"), "fixture", "GeyserTest",
            result::complete, result::completeExceptionally)) {
            var assets = result.get(5, TimeUnit.SECONDS);
            assertEquals(1, assets.size());
            assertEquals("abc123", assets.get("minecraft/lang/de_de.json").getHash());
            assertEquals(42, assets.get("minecraft/lang/de_de.json").getSize());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void reportsHttpFailureWithoutPublishingAnIncompleteIndex() throws Exception {
        var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> { exchange.sendResponseHeaders(503, -1); exchange.close(); });
        server.start();
        URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/manifest");
        var failure = new CompletableFuture<Throwable>();
        var published = new AtomicInteger();
        try (var loader = new AssetIndexLoader(uri, "fixture", "GeyserTest",
            assets -> published.incrementAndGet(), failure::complete)) {
            assertEquals("HTTP 503 fetching Minecraft assets: " + uri, failure.get(5, TimeUnit.SECONDS).getMessage());
            assertEquals(0, published.get());
        } finally {
            server.stop(0);
        }
    }

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "true,true"})
    void cancelsStalledRequestsOnCloseAndStartupFailure(boolean sentHeaders, boolean startupFailure) throws Exception {
        try (var server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            var requested = new CompletableFuture<Void>();
            var disconnected = new CompletableFuture<Integer>();
            Thread peer = Thread.ofVirtual().start(() -> {
                try (var socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    var input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                    while (!input.readLine().isEmpty()) { }
                    if (sentHeaders) {
                        socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 4096\r\n\r\n{".getBytes(StandardCharsets.US_ASCII));
                        socket.getOutputStream().flush();
                    }
                    requested.complete(null);
                    try {
                        disconnected.complete(input.read());
                    } catch (java.net.SocketException reset) {
                        // Cancelling a partially read response can reset the TCP connection.
                        disconnected.complete(-1);
                    }
                } catch (Throwable failure) {
                    requested.completeExceptionally(failure);
                    disconnected.completeExceptionally(failure);
                }
            });
            var callbacks = new AtomicInteger();
            try (var loader = new AssetIndexLoader(URI.create("http://127.0.0.1:" + server.getLocalPort()),
                "fixture", "GeyserTest", assets -> callbacks.incrementAndGet(), failure -> callbacks.incrementAndGet())) {
                requested.get(5, TimeUnit.SECONDS);
                boolean interruptCaller = sentHeaders && !startupFailure;
                if (interruptCaller) Thread.currentThread().interrupt();
                try {
                    if (startupFailure) failStartupWith(loader);
                    else loader.close();
                    assertEquals(interruptCaller, Thread.currentThread().isInterrupted());
                } finally {
                    Thread.interrupted();
                }
                assertEquals(-1, disconnected.get(5, TimeUnit.SECONDS));
                peer.join();
                assertEquals(0, callbacks.get());
            }
        }
    }

    @Test
    void waitsForAnActiveCompletionCallbackBeforeClosing() throws Exception {
        var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext("/manifest", exchange -> reply(exchange,
            "{\"versions\":[{\"id\":\"fixture\",\"url\":\"" + base + "/version\"}]}"));
        server.createContext("/version", exchange -> reply(exchange,
            "{\"assetIndex\":{\"url\":\"" + base + "/assets\"}}"));
        server.createContext("/assets", exchange -> reply(exchange, "{\"objects\":{}}"));
        server.start();
        var entered = new CompletableFuture<Void>();
        var interrupted = new CompletableFuture<Void>();
        var release = new CompletableFuture<Void>();
        var completed = new AtomicInteger();
        try (var loader = new AssetIndexLoader(URI.create(base + "/manifest"), "fixture", "GeyserTest", assets -> {
            entered.complete(null);
            while (true) {
                try {
                    release.get();
                    break;
                } catch (InterruptedException cancellation) {
                    interrupted.complete(null);
                } catch (java.util.concurrent.ExecutionException failure) {
                    throw new AssertionError(failure);
                }
            }
            completed.incrementAndGet();
        }, entered::completeExceptionally)) {
            try {
                entered.get(5, TimeUnit.SECONDS);
                var closing = CompletableFuture.runAsync(loader::close);
                try {
                    interrupted.get(5, TimeUnit.SECONDS);
                    assertFalse(closing.isDone());
                } finally {
                    release.complete(null);
                }
                closing.get(5, TimeUnit.SECONDS);
                assertEquals(1, completed.get());
            } finally {
                release.complete(null);
            }
        } finally {
            release.complete(null);
            server.stop(0);
        }
    }

    private static void failStartupWith(AssetIndexLoader loader) throws Exception {
        var singleton = GeyserImpl.class.getDeclaredField("instance");
        singleton.setAccessible(true);
        Object previous = singleton.get(null);
        var field = GeyserImpl.class.getDeclaredField("assetIndexLoader");
        field.setAccessible(true);
        var engine = mock(GeyserImpl.class);
        var failure = new IllegalStateException("Fixture startup failed after requesting the language index");
        doThrow(failure).when(engine).initialize();
        field.set(engine, loader);
        singleton.set(null, engine);
        try {
            assertSame(failure, assertThrows(IllegalStateException.class, GeyserImpl::start));
            verify(engine).disable();
            assertNull(field.get(engine));
        } finally {
            singleton.set(null, previous);
        }
    }

    private static void reply(com.sun.net.httpserver.HttpExchange exchange, String json) throws java.io.IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}

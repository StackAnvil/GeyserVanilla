/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.Environment;
import com.mojang.authlib.services.MinecraftServicesDiscoveryService;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.world.item.component.ResolvableProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class VanillaProfilesTest {
    private static HttpServer server;
    private static VanillaProfiles profiles;

    @BeforeAll
    static void startDiscoveryFixture() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] document = """
            {"environment":"test","product":"minecraft","discovery":{"product":"minecraft",
              "profiles":{"endpoints":{"getTexture":{"uri":"https://textures.minecraft.net/texture/{textureId}",
                "validUris":["https://textures.minecraft.net/texture/{textureId}"]}}}}}
            """.getBytes(StandardCharsets.UTF_8);
        server.createContext("/discovery", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, document.length);
            try (var output = exchange.getResponseBody()) {
                output.write(document);
            }
        });
        server.start();
        var environment = new Environment("http://127.0.0.1:" + server.getAddress().getPort() + "/discovery", "test");
        profiles = new VanillaProfiles(MinecraftServicesDiscoveryService.create(Proxy.NO_PROXY, false, environment));
    }

    @AfterAll
    static void stopDiscoveryFixture() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void decodesNativeTexturePropertiesAndUsesTheBundledDomainPolicy() {
        var profile = profileWithTextures("""
            {"textures":{"SKIN":{"url":"https://textures.minecraft.net/texture/abc","metadata":{"model":"slim"}},
                         "CAPE":{"url":"https://textures.minecraft.net/texture/def"}}}
            """);
        var textures = profiles.decode(profile);
        var skin = textures.get(MinecraftProfileTexture.Type.SKIN);
        assertEquals("https://textures.minecraft.net/texture/abc", skin.getUrl());
        assertEquals("abc", skin.getHash());
        assertEquals("slim", skin.getMetadata("model"));
        assertEquals("def", textures.get(MinecraftProfileTexture.Type.CAPE).getHash());
        assertTrue(profiles.allowed(skin.getUrl()));
        assertFalse(profiles.allowed("https://textures.minecraft.net.example.com/texture/abc"));
        assertFalse(profiles.allowed("https://localhost/texture/abc"));
    }

    @Test
    void ignoresMissingMalformedAndDisallowedTexturePayloads() {
        assertTrue(profiles.decode(new GameProfile(UUID.randomUUID(), "NoSkin")).isEmpty());
        assertTrue(profiles.decode(profileWithTextures("not json")).isEmpty());
        assertTrue(profiles.decode(profileWithTextures("""
            {"textures":{"SKIN":{"url":"https://localhost/private"}}}
            """)).isEmpty());
    }

    @Test
    void leavesTexturesAbsentWhenServicesAreUnavailable() {
        var offline = new VanillaProfiles(MinecraftServicesDiscoveryService.createOffline(Proxy.NO_PROXY));
        var profile = profileWithTextures("""
            {"textures":{"SKIN":{"url":"https://textures.minecraft.net/texture/abc"}}}
            """);
        assertTrue(offline.decode(profile).isEmpty());
        assertFalse(offline.allowed("https://textures.minecraft.net/texture/abc"));
    }

    @Test
    void resolvesNativeProfileVariantsThroughTheProvidedResolver() {
        var calls = new AtomicInteger();
        var expected = profileWithTextures("""
            {"textures":{"SKIN":{"url":"https://textures.minecraft.net/texture/abc"}}}
            """);
        var resolver = new ProfileResolver() {
            @Override
            public Optional<GameProfile> fetchByName(String name) {
                assertEquals(expected.name(), name);
                calls.incrementAndGet();
                return Optional.of(expected);
            }

            @Override
            public Optional<GameProfile> fetchById(UUID id) {
                assertEquals(expected.id(), id);
                calls.incrementAndGet();
                return Optional.of(expected);
            }
        };
        assertEquals(expected, ResolvableProfile.createResolved(expected).resolveProfile(resolver).join());
        assertEquals(0, calls.get());
        assertEquals(expected, ResolvableProfile.createUnresolved(expected.name()).resolveProfile(resolver).join());
        assertEquals(expected, ResolvableProfile.createUnresolved(expected.id()).resolveProfile(resolver).join());
        assertEquals(2, calls.get());

        var nbt = new CompoundTag();
        nbt.putString("name", expected.name());
        var decoded = ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow();
        assertEquals(expected, decoded.resolveProfile(resolver).join());
        assertEquals(3, calls.get());
    }

    private static GameProfile profileWithTextures(String json) {
        String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        return new GameProfile(UUID.fromString("b221483b-e5b4-42e7-9e30-4db9e7012a34"), "NativeProfile",
            new PropertyMap(ImmutableMultimap.of("textures", new Property("textures", encoded))));
    }
}

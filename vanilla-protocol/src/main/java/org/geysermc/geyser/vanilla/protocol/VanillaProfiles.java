/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.SessionService;
import com.mojang.authlib.services.MinecraftServicesDiscoveryService;

import java.net.Proxy;
import java.util.EnumMap;
import java.util.Map;

/** Decodes profile textures and validates their URLs using the client's authlib. */
public final class VanillaProfiles {
    private final MinecraftServicesDiscoveryService discovery;
    private final SessionService sessionService;

    VanillaProfiles(MinecraftServicesDiscoveryService discovery) {
        this.discovery = discovery;
        this.sessionService = discovery.createMinecraftSessionService();
    }

    public static boolean isAllowedTextureDomain(String url) {
        return DefaultServices.INSTANCE.allowed(url);
    }

    public static Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures(GameProfile profile) {
        return DefaultServices.INSTANCE.decode(profile);
    }

    boolean allowed(String url) {
        try {
            return discovery.isAllowedTextureDomain(url);
        } catch (MinecraftClientException e) {
            return false;
        }
    }

    Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> decode(GameProfile profile) {
        MinecraftProfileTextures textures;
        try {
            textures = sessionService.getTextures(profile);
        } catch (MinecraftClientException e) {
            return Map.of();
        }
        var result = new EnumMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>(MinecraftProfileTexture.Type.class);
        if (textures.skin() != null) {
            result.put(MinecraftProfileTexture.Type.SKIN, textures.skin());
        }
        if (textures.cape() != null) {
            result.put(MinecraftProfileTexture.Type.CAPE, textures.cape());
        }
        if (textures.elytra() != null) {
            result.put(MinecraftProfileTexture.Type.ELYTRA, textures.elytra());
        }
        return result;
    }

    private static final class DefaultServices {
        private static final VanillaProfiles INSTANCE = new VanillaProfiles(MinecraftServicesDiscoveryService.create(Proxy.NO_PROXY));
    }
}

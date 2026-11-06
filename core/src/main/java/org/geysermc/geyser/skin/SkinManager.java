/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.skin;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.skin.ImageData;
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin;
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.skin.Cape;
import org.geysermc.geyser.api.skin.Skin;
import org.geysermc.geyser.api.skin.SkinData;
import org.geysermc.geyser.api.skin.SkinGeometry;
import org.geysermc.geyser.entity.type.player.AvatarEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.auth.BedrockClientData;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.FileUtils;
import org.geysermc.geyser.util.PlayerListUtils;
import org.geysermc.geyser.util.WebUtils;
import com.mojang.authlib.GameProfile;
import net.minecraft.world.item.component.ResolvableProfile;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SkinManager {
    private static final int MAX_IMAGE_SIZE = 128 * 128 * 4;
    /**
     * The library's default limit for geometry read from a skin packet
     */
    private static final int MAX_GEOMETRY_SIZE = 512 * 1024;

    private static final Map<ResolvableProfile, CompletableFuture<GameProfile>> requestedProfiles = new ConcurrentHashMap<>();
    private static final Cache<ResolvableProfile, GameProfile> RESOLVED_PROFILES_CACHE = CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build();
    private static final UUID EMPTY_UUID = new UUID(0L, 0L);
    static final String GEOMETRY = new String(FileUtils.readAllBytes("bedrock/geometries/geo.json"), StandardCharsets.UTF_8);

    /**
     * Builds a Bedrock player list entry from our existing, cached Bedrock skin information
     */
    public static PlayerListPacket.Entry buildEntryFromCachedSkin(GeyserSession session, AvatarEntity playerEntity) {
        // First: see if we have the cached skin texture ID.
        GameProfileData data = GameProfileData.from(playerEntity);
        Skin skin = null;
        Cape cape = null;
        SkinGeometry geometry = SkinGeometry.WIDE;
        if (data != null) {
            // GameProfileData is not null = server provided us with textures data to work with.
            skin = SkinProvider.getCachedSkin(data.skinUrl());
            cape = SkinProvider.getCachedCape(data.capeUrl());
            geometry = data.isSlim() ? SkinGeometry.SLIM : SkinGeometry.WIDE;
        }

        if (skin == null || cape == null) {
            // The server either didn't have a texture to send, or we didn't have the texture ID cached.
            // Let's see if this player is a Bedrock player, and if so, let's pull their skin.
            // Otherwise, grab the default player skin
            SkinData fallbackSkinData = SkinProvider.determineFallbackSkinData(playerEntity.uuid());
            if (skin == null) {
                skin = fallbackSkinData.skin();
                geometry = fallbackSkinData.geometry();
            }
            if (cape == null) {
                cape = fallbackSkinData.cape();
            }
        }

        return PlayerListUtils.buildEntryManually(
                session,
                playerEntity.uuid(),
                playerEntity.getUsername(),
                playerEntity.geyserId(),
                getSkin(session, skin.textureUrl(), skin, cape, geometry)
        );
    }

    public static void sendSkinPacket(GeyserSession session, AvatarEntity entity, SkinData skinData) {
        Skin skin = skinData.skin();
        Cape cape = skinData.cape();
        SkinGeometry geometry = skinData.geometry();

        // Since 1.21.130 the client takes a player's skin from their player list entry, which means a
        // PlayerSkinPacket no longer applies to listed players (still broken as of 26.51). Always go through
        // the player list instead, as that works for both listed and unlisted entities.
        PlayerListPacket.Entry entry = PlayerListUtils.buildEntryManually(
            session,
            entity.uuid(),
            entity.getUsername(),
            entity.geyserId(),
            getSkin(session, skin.textureUrl(), skin, cape, geometry)
        );

        // Slight delay ensures skins are actually shown
        session.scheduleInEventLoop(() -> {
            PlayerListUtils.sendSkinUsingPlayerList(session, entry, entity, entity.isListed());
        }, 100, TimeUnit.MILLISECONDS);
    }

    private static SerializedSkin getSkin(GeyserSession session, String skinId, Skin skin, Cape cape, SkinGeometry geometry) {
        return SerializedSkin.builder()
            .skinId(skinId)
            .skinResourcePatch(geometry.geometryName())
            .skinData(ImageData.of(skin.skinData()))
            .capeData(ImageData.of(cape.capeData()))
            .geometryData(geometry.geometryData().isBlank() ? GEOMETRY : geometry.geometryData())
            .premium(true)
            .capeId(cape.capeId())
            .fullSkinId(skinId)
            .geometryDataEngineVersion(session.getClientData().getGameVersion())
            .overridingPlayerAppearance(true)
            .color(new Color(0, true))
            .trusted(true)
            .profileHash("") // TODO Look into this, the session sends it, so it's probably important for skins to work correctly
            .build();
    }

    private static final net.minecraft.server.players.ProfileResolver PROFILE_RESOLVER = new net.minecraft.server.players.ProfileResolver() {
        @Override
        public java.util.Optional<GameProfile> fetchByName(String name) {
            UUID id = SkinProvider.requestUUIDFromUsername(name).join();
            return id == null ? java.util.Optional.empty() : fetch(id, name);
        }

        @Override
        public java.util.Optional<GameProfile> fetchById(UUID id) {
            String name = SkinProvider.requestUsernameFromUUID(id).join();
            return name == null ? java.util.Optional.empty() : fetch(id, name);
        }

        private java.util.Optional<GameProfile> fetch(UUID id, String name) {
            String textures = SkinProvider.requestTexturesFromUUID(id).join();
            if (textures == null) {
                return java.util.Optional.of(new GameProfile(id, name));
            }
            var properties = new com.mojang.authlib.properties.PropertyMap(com.google.common.collect.ImmutableMultimap.of(
                "textures", new com.mojang.authlib.properties.Property("textures", textures)));
            return java.util.Optional.of(new GameProfile(id, name, properties));
        }
    };

    public static CompletableFuture<GameProfile> resolveProfile(ResolvableProfile profile) {
        if (profile instanceof ResolvableProfile.Static) {
            return profile.resolveProfile(PROFILE_RESOLVER);
        }
        GameProfile cached = RESOLVED_PROFILES_CACHE.getIfPresent(profile);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        CompletableFuture<GameProfile> future = requestedProfiles.computeIfAbsent(profile, key ->
            key.resolveProfile(PROFILE_RESOLVER).thenApply(resolved -> {
                RESOLVED_PROFILES_CACHE.put(key, resolved);
                return resolved;
            }));
        future.whenComplete((result, error) -> requestedProfiles.remove(profile, future));
        return future;
    }

    public static @Nullable MinecraftProfileTexture getTextureDataFromProfile(GameProfile profile, MinecraftProfileTexture.Type type) {
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures;
        try {
            textures = org.geysermc.geyser.vanilla.protocol.VanillaProfiles.textures(profile);
        } catch (IllegalStateException e) {
            GeyserImpl.getInstance().getLogger().debug("Could not decode textures from game profile (%s)! Got: %s", profile, e);
            return null;
        }

        if (textures == null) {
            return null;
        }
        return textures.get(type);
    }

    public static void requestAndHandleSkinAndCape(AvatarEntity entity, GeyserSession session, Consumer<SkinProvider.SkinAndCape> skinAndCapeConsumer) {
        SkinProvider.requestSkinData(entity, session).whenCompleteAsync((skinData, throwable) -> {
            if (skinData != null && skinData.geometry() != null) {
                sendSkinPacket(session, entity, skinData);
            }

            if (skinAndCapeConsumer != null) {
                skinAndCapeConsumer.accept(skinData == null ? null : new SkinProvider.SkinAndCape(skinData.skin(), skinData.cape()));
            }
        });
    }

    public static void handleBedrockSkin(AvatarEntity playerEntity, BedrockClientData clientData) {
        GeyserImpl geyser = GeyserImpl.getInstance();
        if (geyser.config().debugMode()) {
            geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.skin.bedrock.register", playerEntity.getUsername(), playerEntity.uuid()));
        }

        try {
            byte[] skinBytes = clientData.getSkinData();
            byte[] capeBytes = clientData.getCapeData();

            byte[] geometryNameBytes = clientData.getGeometryName();
            byte[] geometryBytes = clientData.getGeometryData();

            if (isImage(skinBytes) && !clientData.isPersonaSkin()) {
                SkinProvider.storeBedrockSkin(playerEntity.uuid(), clientData.getSkinId(), skinBytes);
                if (geometryNameBytes.length + geometryBytes.length <= MAX_GEOMETRY_SIZE) {
                    SkinProvider.storeBedrockGeometry(playerEntity.uuid(), geometryNameBytes, geometryBytes);
                }
            } else if (geyser.config().debugMode()) {
                geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.skin.bedrock.fail", playerEntity.getUsername()));
                geyser.getLogger().debug("The size of '" + playerEntity.getUsername() + "' skin is: " + clientData.getSkinImageWidth() + "x" + clientData.getSkinImageHeight());
            }

            if (!clientData.getCapeId().isEmpty() && isImage(capeBytes)) {
                SkinProvider.storeBedrockCape(playerEntity.uuid(), clientData.getCapeId(), capeBytes);
            }
        } catch (Exception e) {
            throw new AssertionError("Failed to cache skin for bedrock user (" + playerEntity.getUsername() + "): ", e);
        }
    }

    /**
     * Whether the data has a size Bedrock images can have.
     */
    private static boolean isImage(byte[] data) {
        if (data.length > MAX_IMAGE_SIZE) {
            return false;
        }
        try {
            ImageData.of(data);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static UUID createOfflinePlayerUUID(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }

    public record GameProfileData(String skinUrl, String capeUrl, boolean isSlim) {
        /**
         * Generate the GameProfileData from the given player entity
         *
         * @param entity entity to build the GameProfileData from
         * @return The built GameProfileData
         */
        public static @Nullable GameProfileData from(AvatarEntity entity) {
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = entity.getTextures();
            if (textures == null) {
                // Likely offline mode or failed to load
                // We'll fall back to default skins
                return null;
            }

            MinecraftProfileTexture skin = textures.get(MinecraftProfileTexture.Type.SKIN);
            if (skin == null) {
                return null;
            }

            String skinUrl = WebUtils.toHttps(skin.getUrl());
            if (Objects.equals(DEFAULT_FLOODGATE_STEVE, skinUrl)) {
                // https://github.com/GeyserMC/Floodgate/commit/00b8b1b6364116ff4bc9b00e2015ce35bae8abb1 ensures that
                // Bedrock players on online-mode servers will always have a textures property. However, this skin is
                // also sent our way, and isn't overwritten. It's very likely that this skin is *only* a placeholder,
                // and no one should ever be using it outside of Floodgate, and therefore no one wants to see this
                // specific Steve skin.
                return null;
            }

            MinecraftProfileTexture cape = textures.get(MinecraftProfileTexture.Type.CAPE);
            String capeUrl = cape == null ? null : WebUtils.toHttps(cape.getUrl());
            return new GameProfileData(skinUrl, capeUrl, "slim".equals(skin.getMetadata("model")));
        }

        private static final String DEFAULT_FLOODGATE_STEVE = "https://textures.minecraft.net/texture/31f477eb1a7beee631c2ca64d06f8f68fa93a3386d04452ab27f43acdf1b60cb";
    }
}

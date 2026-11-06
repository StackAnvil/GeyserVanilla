/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.util;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.network.bedrock.GameProtocol;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.vanilla.protocol.VanillaBootstrap;
import org.geysermc.geyser.vanilla.runtime.VanillaRuntime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;

/** Loads local assets from the verified client and indexes additional language downloads. */
public final class AssetUtils {
    private static final String CLIENT_JAR_HASH_FILE = "client_jar.hash";
    private static volatile Map<String, Asset> assetMap = Map.of();
    private static final Queue<ClientJarTask> CLIENT_JAR_TASKS = new ArrayDeque<>();
    private static boolean extractClientAssets;

    public static Asset getAsset(String name) {
        return assetMap.get(name);
    }

    public static boolean isAssetKnown(String name) {
        return assetMap.containsKey(name);
    }

    /** Queues extraction from the local client when an asset is missing or the client changed. */
    public static void addTask(boolean required, ClientJarTask task) {
        CLIENT_JAR_TASKS.add(task);
        extractClientAssets |= required;
    }

    public static AssetIndexLoader generateAssetCache() {
        return new AssetIndexLoader(URI.create("https://launchermeta.mojang.com/mc/game/version_manifest.json"),
            GameProtocol.getJavaMinecraftVersion(), WebUtils.getUserAgent(), assets -> assetMap = assets,
            failure -> GeyserImpl.getInstance().getLogger().error(
                GeyserLocale.getLocaleStringLog("geyser.locale.fail.asset_cache", failure.toString()), failure));
    }

    public static void runClientJarTasks() {
        Path cache = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("cache");
        try {
            var manifest = VanillaRuntime.manifest();
            String hash = manifest.artifacts().stream()
                .filter(artifact -> artifact.name().equals("minecraft-client:" + manifest.version()))
                .findFirst().orElseThrow().sha1();
            Path marker = cache.resolve(CLIENT_JAR_HASH_FILE);
            boolean extract = extractClientAssets || !Files.isRegularFile(marker)
                || !Files.readString(marker).trim().equals(hash);
            ClientJarTask task;
            while ((task = CLIENT_JAR_TASKS.poll()) != null) {
                if (extract) {
                    // The private engine loader resolves the pinned client before host resources.
                    try (InputStream input = VanillaBootstrap.class.getClassLoader().getResourceAsStream(task.asset())) {
                        if (input == null) {
                            throw new IOException("Missing asset in the pinned Minecraft client: " + task.asset());
                        }
                        task.ifNewDownload().accept(input);
                    }
                }
                task.whenDone().run();
            }
            Files.createDirectories(cache);
            Files.writeString(marker, hash);
            extractClientAssets = false;
        } catch (Exception | AssertionError failure) {
            try {
                Files.deleteIfExists(cache.resolve(CLIENT_JAR_HASH_FILE));
            } catch (IOException cleanup) {
                failure.addSuppressed(cleanup);
            }
            GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.locale.fail.en_us"), failure);
        }
    }

    public static void saveFile(Path location, InputStream fileStream) throws IOException {
        Files.createDirectories(location.toAbsolutePath().getParent());
        try (OutputStream output = Files.newOutputStream(location)) {
            fileStream.transferTo(output);
        }
    }

    public record ClientJarTask(String asset, InputStreamConsumer ifNewDownload, Runnable whenDone) {
    }

    @FunctionalInterface
    public interface InputStreamConsumer {
        void accept(InputStream stream) throws IOException;
    }

    @Getter
    public static class Asset {
        @SerializedName("hash")
        private String hash;

        @SerializedName("size")
        private int size;
    }

    private AssetUtils() {
    }
}

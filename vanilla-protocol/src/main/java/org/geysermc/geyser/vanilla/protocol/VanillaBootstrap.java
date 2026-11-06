/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.Util;
import org.geysermc.geyser.vanilla.runtime.VanillaClassLoader;

import java.util.List;
import java.util.stream.Stream;

import java.io.PrintStream;

/** Initializes shared game data without starting the graphical client or a game server. */
public final class VanillaBootstrap {
    private static boolean initialized;
    private static RegistryAccess.Frozen defaultRegistries;
    private static boolean stopped;

    private VanillaBootstrap() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        SharedConstants.tryDetectVersion();
        PrintStream stdout = System.out;
        PrintStream stderr = System.err;
        try {
            Bootstrap.bootStrap();
        } finally {
            // Vanilla redirects the process streams. Geyser's host owns its logging setup.
            System.setOut(stdout);
            System.setErr(stderr);
        }
        initialized = true;
    }

    public static synchronized RegistryAccess.Frozen defaultRegistries() {
        initialize();
        if (defaultRegistries == null) {
            var builtIn = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            var resources = new MultiPackResourceManager(PackType.SERVER_DATA,
                List.of(ServerPacksSource.createVanillaPackSource().fullResources()));
            var tags = TagLoader.loadTagsForExistingRegistries(resources, builtIn);
            var context = TagLoader.buildUpdatedLookups(builtIn, tags);
            var dynamic = RegistryDataLoader.load(resources, context,
                RegistryDataLoader.WORLD_REGISTRIES, Runnable::run).join();
            tags.forEach(pending -> pending.apply());
            var loaded = new RegistryAccess.ImmutableRegistryAccess(
                Stream.concat(builtIn.registries(), dynamic.registries())).freeze();
            BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(loaded)
                .forEach(pending -> pending.apply());
            defaultRegistries = loaded;
        }
        return defaultRegistries;
    }

    public static synchronized void shutdown() {
        VanillaNetwork.shutdown();
        if (stopped) {
            return;
        }
        stopped = true;
        // Util executors belong to the game classes. Only stop them when those classes
        // came from our isolated runtime, never when a mod loader supplied the host's copy.
        ClassLoader loader = VanillaBootstrap.class.getClassLoader();
        if (initialized && loader instanceof VanillaClassLoader && Util.class.getClassLoader() == loader) {
            Util.shutdownExecutors();
            Util.shutdownTimeSource();
        }
    }
}

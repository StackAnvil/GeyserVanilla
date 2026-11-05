/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.runtime;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;

/** Starts Standalone with the pinned client libraries before any Geyser core classes are loaded. */
public final class VanillaLauncher {
    private VanillaLauncher() {
    }

    public static void main(String[] args) throws Throwable {
        var manifest = VanillaRuntime.manifest();
        if (Runtime.version().feature() < manifest.javaVersion()) {
            throw new IllegalStateException("GeyserVanilla requires Java " + manifest.javaVersion() + " or newer");
        }
        Path cache = Path.of(System.getProperty("geyser.vanilla.cache", "cache/vanilla"));
        var classpath = new ArrayList<>(VanillaRuntime.prepare(cache, System.out::println));
        // This supports both the packaged jar and Gradle's development runtime classpath.
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            classpath.add(Path.of(entry));
        }
        var loader = new URLClassLoader("GeyserVanilla", VanillaRuntime.urls(classpath),
            ClassLoader.getPlatformClassLoader());
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(loader);
        try {
            Class<?> bootstrap = Class.forName("org.geysermc.geyser.platform.standalone.GeyserStandaloneBootstrap", true, loader);
            bootstrap.getMethod("main", String[].class).invoke(null, (Object) args);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    loader.close();
                } catch (java.io.IOException e) {
                    System.err.println("Could not close the vanilla runtime: " + e.getMessage());
                }
            }, "GeyserVanilla runtime shutdown"));
        } catch (InvocationTargetException e) {
            loader.close();
            throw e.getCause();
        } catch (Throwable e) {
            loader.close();
            throw e;
        } finally {
            thread.setContextClassLoader(previous);
        }
    }
}

/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.runtime;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Loads the pinned client and engine before host libraries, with explicit shared API packages. */
public final class VanillaClassLoader extends URLClassLoader {
    private static final List<String> PRIVATE_PACKAGES = List.of("net.minecraft.", "com.mojang.", "io.netty.",
        "it.unimi.dsi.fastutil.", "com.google.common.", "com.google.gson.", "org.joml.");
    static {
        registerAsParallelCapable();
    }

    private final List<String> sharedPackages;
    private boolean closed;

    public VanillaClassLoader(URL[] classpath, ClassLoader parent, List<String> sharedPackages) {
        super("GeyserVanilla", classpath, parent);
        this.sharedPackages = List.copyOf(sharedPackages);
        if (sharedPackages.stream().anyMatch(prefix -> prefix.isEmpty() || !prefix.endsWith("."))) {
            throw new IllegalArgumentException("Shared API packages must have a nonempty name ending in a dot");
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                boolean privateClass = PRIVATE_PACKAGES.stream().anyMatch(name::startsWith);
                if (name.startsWith("java.") || !privateClass && sharedPackages.stream().anyMatch(name::startsWith)) {
                    loaded = super.loadClass(name, false);
                } else {
                    try {
                        loaded = findClass(name);
                    } catch (ClassNotFoundException missing) {
                        if (privateClass) {
                            throw missing;
                        }
                        loaded = getParent().loadClass(name);
                    }
                }
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }
    }

    @Override
    public URL getResource(String name) {
        URL own = findResource(name);
        return own == null ? getParent().getResource(name) : own;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Set<URL> resources = new LinkedHashSet<>(Collections.list(findResources(name)));
        resources.addAll(Collections.list(getParent().getResources(name)));
        return Collections.enumeration(resources);
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        IOException failure = null;
        // Do not initialize game classes solely to close a loader whose startup failed early.
        Class<?> bootstrap = findLoadedClass("org.geysermc.geyser.vanilla.protocol.VanillaBootstrap");
        if (bootstrap != null) {
            try {
                bootstrap.getMethod("shutdown").invoke(null);
            } catch (ReflectiveOperationException | LinkageError e) {
                Throwable cause = e instanceof InvocationTargetException invocation ? invocation.getCause() : e;
                failure = new IOException("Could not stop the vanilla runtime", cause);
            }
        }
        try {
            super.close();
        } catch (IOException e) {
            if (failure == null) {
                failure = e;
            } else {
                failure.addSuppressed(e);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}

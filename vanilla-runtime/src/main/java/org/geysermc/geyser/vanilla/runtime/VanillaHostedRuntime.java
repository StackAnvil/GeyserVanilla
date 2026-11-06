/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.runtime;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Starts a platform delegate without resolving its native classes in the host loader. */
public final class VanillaHostedRuntime implements AutoCloseable {
    private static final List<String> SHARED_APIS = List.of(
        "org.geysermc.geyser.vanilla.runtime.", "org.geysermc.geyser.vanilla.host.",
        "org.geysermc.geyser.api.", "org.geysermc.api.", "org.geysermc.event.", "org.cloudburstmc.math.");

    private final VanillaClassLoader loader;

    VanillaHostedRuntime(VanillaClassLoader loader) {
        this.loader = loader;
    }

    public static VanillaHostedRuntime open(Class<?> entrypoint, Path cache, Consumer<String> logger,
                                             List<String> hostApis) throws IOException {
        var manifest = VanillaRuntime.manifest();
        if (Runtime.version().feature() < manifest.javaVersion()) {
            throw new IllegalStateException("GeyserVanilla requires Java " + manifest.javaVersion() + " or newer");
        }
        var classpath = new ArrayList<>(VanillaRuntime.prepare(cache, logger));
        classpath.add(pluginLocation(entrypoint.getProtectionDomain().getCodeSource().getLocation()));
        var shared = new ArrayList<>(SHARED_APIS);
        shared.addAll(hostApis);
        return new VanillaHostedRuntime(new VanillaClassLoader(VanillaRuntime.urls(classpath), entrypoint.getClassLoader(), shared));
    }

    static Path pluginLocation(URL location) throws IOException {
        try {
            URI uri = location.toURI();
            // ViaProxy marks each plugin's code source with a class-loader fragment.
            if (uri.getFragment() != null) {
                uri = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), uri.getQuery(), null);
            }
            return Path.of(uri);
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw new IOException("Invalid GeyserVanilla plugin location: " + location, e);
        }
    }

    public Object create(String name, Class<?>[] parameters, Object... arguments) throws Exception {
        return inContext(() -> Class.forName(name, true, loader).getConstructor(parameters).newInstance(arguments));
    }

    public Object invoke(Object target, String name, Class<?>[] parameters, Object... arguments) throws Exception {
        return inContext(() -> target.getClass().getMethod(name, parameters).invoke(target, arguments));
    }

    private Object inContext(Invocation invocation) throws Exception {
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(loader);
        try {
            return invocation.call();
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception exception) {
                throw exception;
            }
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Platform delegate failed", e.getCause());
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    @Override
    public void close() throws IOException {
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(loader);
        try {
            loader.close();
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    @FunctionalInterface
    private interface Invocation {
        Object call() throws Exception;
    }
}

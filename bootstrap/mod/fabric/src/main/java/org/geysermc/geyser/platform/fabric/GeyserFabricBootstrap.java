/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.platform.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.Person;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.platform.mod.GeyserModUpdateListener;
import org.geysermc.geyser.platform.mod.host.ModCommandSender;
import org.geysermc.geyser.platform.mod.host.ModRuntime;
import org.geysermc.geyser.platform.mod.host.ServerCommandSender;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.fabric.FabricServerCommandManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

public class GeyserFabricBootstrap extends ModRuntime implements ModInitializer {
    @Override
    public void onInitialize() {
        if (dedicated()) {
            ServerLifecycleEvents.SERVER_STARTED.register(this::serverStarted);
        } else {
            ClientLifecycleEvents.CLIENT_STOPPING.register(client -> close());
        }
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> serverStopped());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> GeyserModUpdateListener.onPlayReady(handler.getPlayer()));
        initialize();
    }

    public PlatformType platformType() { return PlatformType.FABRIC; }
    public boolean dedicated() { return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER; }
    public Path dataFolder() { return FabricLoader.getInstance().getConfigDir().resolve("Geyser-Fabric"); }
    public Path floodgateFolder() {
        return FabricLoader.getInstance().isModLoaded("floodgate")
            ? FabricLoader.getInstance().getConfigDir().resolve("floodgate") : null;
    }

    public InputStream resource(String resource) {
        var path = FabricLoader.getInstance().getModContainer("geyser-fabric").orElseThrow().findPath(resource);
        try {
            return path.isPresent() ? Files.newInputStream(path.get()) : null;
        } catch (IOException failure) {
            return null;
        }
    }

    public Object commandManager(Function<ModCommandSender, Object> map, Function<Object, ModCommandSender> reverse) {
        return new FabricServerCommandManager<>(ExecutionCoordinator.simpleCoordinator(), ServerCommandSender.mapper(map, reverse));
    }

    public String commandRegistryType() { return ""; }

    @Override
    public Map<String, Object> metadata() {
        var info = super.metadata();
        var loader = FabricLoader.getInstance();
        loader.getModContainer("fabricloader").ifPresent(mod ->
            info.put("platformVersion", mod.getMetadata().getVersion().getFriendlyString()));
        info.put("environmentType", loader.getEnvironmentType().name());
        info.put("mods", loader.getAllMods().stream().map(mod -> {
            var meta = mod.getMetadata();
            return Map.of("enabled", loader.isModLoaded(meta.getId()), "name", meta.getId(),
                "version", meta.getVersion().getFriendlyString(),
                "authors", meta.getAuthors().stream().map(Person::getName).toList());
        }).toList());
        return info;
    }
}

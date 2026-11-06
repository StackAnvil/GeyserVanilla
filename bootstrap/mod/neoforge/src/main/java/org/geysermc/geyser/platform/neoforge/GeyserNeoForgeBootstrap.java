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

package org.geysermc.geyser.platform.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.platform.mod.GeyserModUpdateListener;
import org.geysermc.geyser.platform.mod.host.ModCommandSender;
import org.geysermc.geyser.platform.mod.host.ModRuntime;
import org.geysermc.geyser.platform.mod.host.ServerCommandSender;
import org.geysermc.geyser.platform.neoforge.host.PermissionUtils;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.neoforge.NeoForgeServerCommandManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Mod(ModConstants.MOD_ID)
public class GeyserNeoForgeBootstrap extends ModRuntime {
    private final ModContainer container;

    public GeyserNeoForgeBootstrap(ModContainer container) {
        this.container = container;
        if (dedicated()) {
            NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        } else {
            NeoForge.EVENT_BUS.addListener(this::onClientStopping);
        }
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::onPermissionGather);
        initialize();
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::onUndefinedPermissionGather);
    }

    private void onServerStarted(ServerStartedEvent event) { serverStarted(event.getServer()); }
    private void onServerStopping(ServerStoppingEvent event) { serverStopped(); }
    private void onClientStopping(GameShuttingDownEvent event) { close(); }
    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GeyserModUpdateListener.onPlayReady(player);
        }
    }

    public PlatformType platformType() { return PlatformType.NEOFORGE; }
    public boolean dedicated() { return FMLLoader.getCurrent().getDist().isDedicatedServer(); }
    public Path dataFolder() { return FMLPaths.CONFIGDIR.get().resolve("Geyser-NeoForge"); }
    public Path floodgateFolder() {
        return ModList.get().isLoaded("floodgate") ? FMLPaths.CONFIGDIR.get().resolve("floodgate") : null;
    }
    public InputStream resource(String resource) {
        try {
            return container.getModInfo().getOwningFile().getFile().getContents().openFile(resource);
        } catch (IOException failure) {
            return null;
        }
    }
    public Object commandManager(Function<ModCommandSender, Object> map, Function<Object, ModCommandSender> reverse) {
        return new NeoForgeServerCommandManager<>(ExecutionCoordinator.simpleCoordinator(), ServerCommandSender.mapper(map, reverse));
    }
    public String commandRegistryType() { return "org.geysermc.geyser.platform.neoforge.GeyserNeoForgeCommandRegistry"; }

    private void onPermissionGather(PermissionGatherEvent.Nodes event) {
        gatherPermissions((permission, defaultValue) -> {
            Objects.requireNonNull(permission, "permission");
            Objects.requireNonNull(defaultValue, "permission default for " + permission);
            if (!permission.isBlank()) {
                PermissionUtils.register(permission, defaultValue, event);
            }
        });
    }
    private void onUndefinedPermissionGather(PermissionGatherEvent.Nodes event) {
        gatherUndefinedPermissions((permission, defaultValue) -> PermissionUtils.register(permission, defaultValue, event));
    }

    @Override
    public Map<String, Object> metadata() {
        var info = super.metadata();
        var loader = FMLLoader.getCurrent();
        info.put("platformVersion", loader.getVersionInfo().neoForgeVersion());
        info.put("dist", loader.getDist().name());
        info.put("mods", ModList.get().getMods().stream().map(mod -> Map.of(
            "enabled", ModList.get().isLoaded(mod.getModId()), "name", mod.getModId(),
            "version", mod.getVersion().toString(), "url", mod.getModURL().map(URL::toString).orElse(""))).toList());
        return info;
    }
}

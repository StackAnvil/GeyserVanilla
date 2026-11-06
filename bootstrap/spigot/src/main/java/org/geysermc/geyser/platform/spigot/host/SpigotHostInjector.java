/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.platform.spigot.host;

import com.viaversion.viaversion.bukkit.handlers.BukkitChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.adapters.WorldAdapter;
import org.geysermc.geyser.adapters.paper.PaperAdapters;
import org.geysermc.geyser.adapters.spigot.SpigotAdapters;
import org.geysermc.geyser.vanilla.host.HostLocalConnector;
import org.geysermc.geyser.vanilla.host.LocalServerChannelWrapper;
import org.geysermc.geyser.vanilla.runtime.VanillaLocalTransport;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class SpigotHostInjector implements SpigotHost {
    private final Plugin plugin;
    private List<ChannelFuture> allServerChannels;
    private ChannelFuture localChannel;
    private EventLoopGroup group;
    private HostLocalConnector connector;
    private WorldAdapter<World> adapter;
    private boolean closed;

    public SpigotHostInjector(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public synchronized SocketAddress initialize(boolean disableCompression, boolean debug) throws Exception {
        if (closed || localChannel != null) {
            throw new IllegalStateException("The Spigot injector is already initialized or closed");
        }
        try {
            initializeLocalChannel(disableCompression, debug);
            connector = new HostLocalConnector(localChannel.channel().localAddress(), true);
            return new VanillaLocalTransport.Address(connector);
        } catch (Exception | Error failure) {
            try {
                close();
            } catch (RuntimeException | Error cleanup) {
                if (cleanup != failure) {
                    failure.addSuppressed(cleanup);
                }
            }
            throw failure;
        }
    }

    @SuppressWarnings("unchecked")
    private void initializeLocalChannel(boolean disableCompression, boolean debug) throws Exception {
        Class<?> serverClazz;
        try {
            serverClazz = Class.forName("net.minecraft.server.MinecraftServer");
            // We're using 1.17+
        } catch (ClassNotFoundException e) {
            // We're using pre-1.17
            String prefix = Bukkit.getServer().getClass().getPackage().getName().replace("org.bukkit.craftbukkit", "net.minecraft.server");
            serverClazz = Class.forName(prefix + ".MinecraftServer");
        }
        Method getServer = serverClazz.getDeclaredMethod("getServer");
        Object server = getServer.invoke(null);
        Object connection = null;
        // Find the class that manages network IO
        for (Method m : serverClazz.getDeclaredMethods()) {
            // First is Spigot-mapped name, second is Mojang-mapped name which is implemented as future-proofing
            if (m.getReturnType().getSimpleName().equals("ServerConnection") || m.getReturnType().getSimpleName().equals("ServerConnectionListener")) {
                if (m.getParameterTypes().length == 0) {
                    connection = m.invoke(server);
                }
            }
        }
        if (connection == null) {
            throw new RuntimeException("Unable to find ServerConnection class!");
        }

        // Find the channel that Minecraft uses to listen to connections
        ChannelFuture listeningChannel = null;
        for (Field field : connection.getClass().getDeclaredFields()) {
            if (field.getType() != List.class) {
                continue;
            }
            field.setAccessible(true);
            boolean rightList = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0] == ChannelFuture.class;
            if (!rightList) continue;

            allServerChannels = (List<ChannelFuture>) field.get(connection);
            for (ChannelFuture o : allServerChannels) {
                listeningChannel = o;
                break;
            }
        }
        if (listeningChannel == null) {
            throw new RuntimeException("Unable to find listening channel!");
        }

        // Making this a function prevents childHandler from being treated as a non-final variable
        ChannelInitializer<Channel> childHandler = getChildHandler(debug, listeningChannel);
        // This method is what initializes the connection in Java Edition, after Netty is all set.
        Method initChannel = childHandler.getClass().getDeclaredMethod("initChannel", Channel.class);
        initChannel.setAccessible(true);

        group = new DefaultEventLoopGroup(0, new DefaultThreadFactory("Geyser Spigot connection thread", Thread.MAX_PRIORITY));
        ChannelFuture channelFuture = (new ServerBootstrap()
                .channel(LocalServerChannelWrapper.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(@NonNull Channel ch) throws Exception {
                        initChannel.invoke(childHandler, ch);

                        int index = ch.pipeline().names().indexOf("encoder");
                        String baseName = index != -1 ? "encoder" : "outbound_config";

                        if (disableCompression && GeyserSpigotCompressionDisabler.ENABLED) {
                            ch.pipeline().addAfter(baseName, "geyser-compression-disabler", new GeyserSpigotCompressionDisabler());
                        }
                    }
                })
                // Set to MAX_PRIORITY as MultithreadEventLoopGroup#newDefaultThreadFactory which DefaultEventLoopGroup implements does by default
                .group(group)
                .localAddress(LocalAddress.ANY))
                .bind()
                .syncUninterruptibly();
        // We don't need to add to the list, but plugins like ProtocolSupport and ProtocolLib that add to the main pipeline
        // will work when we add to the list.
        allServerChannels.add(channelFuture);
        this.localChannel = channelFuture;
    }

    @SuppressWarnings("unchecked")
    private ChannelInitializer<Channel> getChildHandler(boolean debug, ChannelFuture listeningChannel) {
        List<String> names = listeningChannel.channel().pipeline().names();
        ChannelInitializer<Channel> childHandler = null;
        for (String name : names) {
            ChannelHandler handler = listeningChannel.channel().pipeline().get(name);
            try {
                Field childHandlerField = handler.getClass().getDeclaredField("childHandler");
                childHandlerField.setAccessible(true);
                childHandler = (ChannelInitializer<Channel>) childHandlerField.get(handler);
                // ViaVersion non-Paper-injector workaround so we aren't double-injecting
                if (Bukkit.getPluginManager().getPlugin("ViaVersion") != null && childHandler instanceof BukkitChannelInitializer) {
                    childHandler = ((BukkitChannelInitializer) childHandler).original();
                }
                break;
            } catch (Exception e) {
                if (debug) {
                    plugin.getLogger().fine("The handler " + name + " isn't a ChannelInitializer. THIS ERROR IS SAFE TO IGNORE!");
                    e.printStackTrace();
                }
            }
        }
        if (childHandler == null) {
            throw new RuntimeException();
        }
        return childHandler;
    }

    @Override
    public void initializeWorldAdapter() throws Exception {
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(plugin.getClass().getClassLoader());
        try {
            try {
                String name = Bukkit.getServer().getClass().getPackage().getName();
                String nmsVersion = name.substring(name.lastIndexOf('.') + 1);
                SpigotAdapters.registerWorldAdapter(nmsVersion);
                adapter = SpigotAdapters.getWorldAdapter();
                plugin.getLogger().fine("Using Spigot world adapter for " + nmsVersion);
            } catch (Exception failure) {
                // Paper 1.20.5+ supplies Mojang-mapped classes rather than a versioned CraftBukkit package.
                int protocolVersion = Bukkit.getUnsafe().getProtocolVersion();
                PaperAdapters.registerClosestWorldAdapter(protocolVersion);
                adapter = PaperAdapters.getWorldAdapter();
                plugin.getLogger().fine("Using Paper world adapter for protocol " + protocolVersion);
            }
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    @Override
    public int blockAt(World world, int x, int y, int z) {
        return adapter.getBlockAt(world, x, y, z);
    }

    @Override
    public String[] biomeIdentifiers(boolean withTags) {
        return adapter.getBiomeSuggestions(withTags);
    }

    @Override
    public int[] allBlockStates() {
        return adapter.getAllBlockStates().toIntArray();
    }

    @Override
    public Map<String, String> decoratedPotAssets(Block block) {
        return SpigotDecoratedPots.assets(block);
    }

    @Override
    public synchronized void close() {
        closed = true;
        if (allServerChannels != null && localChannel != null) {
            allServerChannels.remove(localChannel);
        }
        allServerChannels = null;
        if (connector != null) {
            connector.close();
            connector = null;
        }
        if (localChannel != null) {
            localChannel.channel().close().syncUninterruptibly();
            localChannel = null;
        }
        if (group != null) {
            group.shutdownGracefully(0, 5, TimeUnit.SECONDS).syncUninterruptibly();
            group = null;
        }
    }
}

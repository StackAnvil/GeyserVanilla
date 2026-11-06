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

package org.geysermc.geyser.platform.bungeecord.host;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.IoEventLoop;
import io.netty.channel.IoEventLoopGroup;
import io.netty.channel.IoHandler;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SingleThreadIoEventLoop;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalIoHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.ThreadAwareExecutor;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.event.ProxyReloadEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.netty.PipelineUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.vanilla.host.HostLocalConnector;
import org.geysermc.geyser.vanilla.host.IoHandlerWrapper;
import org.geysermc.geyser.vanilla.host.LocalServerChannelWrapper;
import org.geysermc.geyser.vanilla.runtime.VanillaLocalTransport;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class BungeeHostInjector implements BungeeHost, Listener {
    private final Plugin plugin;
    private final ProxyServer proxy;
    /**
     * Set as a variable so it is only set after the proxy has finished initializing
     */
    private ChannelInitializer<Channel> channelInitializer = null;
    private Set<Channel> bungeeChannels = null;
    private boolean eventRegistered = false;

    private ChannelFuture localChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup wrapperGroup;
    private HostLocalConnector connector;
    private Runnable reloadHandler;
    private boolean closed;
    private final Set<RetiredListener> retired = ConcurrentHashMap.newKeySet();

    public BungeeHostInjector(Plugin plugin) {
        this.plugin = plugin;
        this.proxy = plugin.getProxy();
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized SocketAddress initialize(boolean disableCompression, String listenerBindAddress, boolean proxyProtocol) throws Exception {
        if (closed) {
            throw new IllegalStateException("The BungeeCord injector is closed");
        }
        if (localChannel != null) {
            throw new IllegalStateException("The BungeeCord listener is already injected");
        }
        Class.forName("io.netty.channel.MultiThreadIoEventLoopGroup");
        if (proxy.getConfig().getListeners().size() != 1 && listenerBindAddress.isBlank()) {
            throw new UnsupportedOperationException("You have multiple listeners defined in your proxy config! " +
                "Please define a listener for Geyser to listen to in your Geyser config (advanced.java.bungee-listener)");
        }

        SocketAddress bungeeAddress = listenerBindAddress.isBlank() ? null : Util.getAddr(listenerBindAddress);
        var stream = proxy.getConfig().getListeners().stream();
        if (bungeeAddress != null) {
            stream = stream.filter(info -> info.getSocketAddress().equals(bungeeAddress));
        }
        ListenerInfo listenerInfo = stream.findFirst().orElseThrow(IllegalStateException::new);

        Class<? extends ProxyServer> proxyClass = proxy.getClass();
        // Using the specified EventLoop is required, or else an error will be thrown
        MultiThreadIoEventLoopGroup workerGroup;
        try {
            workerGroup = (MultiThreadIoEventLoopGroup) proxyClass.getField("eventLoops").get(proxy);
            plugin.getLogger().fine("BungeeCord event loop style detected.");
        } catch (NoSuchFieldException e) {
            // Waterfall uses two separate event loops
            // https://github.com/PaperMC/Waterfall/blob/fea7ec356dba6c6ac28819ff11be604af6eb484e/BungeeCord-Patches/0022-Use-a-worker-and-a-boss-event-loop-group.patch
            workerGroup = (MultiThreadIoEventLoopGroup) proxyClass.getField("workerEventLoopGroup").get(proxy);
            plugin.getLogger().fine("Waterfall event loop style detected.");
        }

        final IoEventLoopGroup finalWorkerGroup = workerGroup;
        var factory = LocalIoHandler.newFactory();
        var nativeFactory = getNativeHandlerFactory();
        var wrapperFactory = new IoHandlerFactory() {
            @Override
            public IoHandler newHandler(ThreadAwareExecutor ioExecutor) {
                return new IoHandlerWrapper(factory.newHandler(ioExecutor), nativeFactory.newHandler(ioExecutor));
            }
        };

        wrapperGroup = new MultiThreadIoEventLoopGroup(factory) {
            @Override
            protected ThreadFactory newDefaultThreadFactory() {
                return new DefaultThreadFactory("Geyser Backend Worker Group", Thread.MAX_PRIORITY);
            }

            @Override
            protected IoEventLoop newChild(Executor executor, IoHandlerFactory ioHandlerFactory, Object... args) {
                return new SingleThreadIoEventLoop(finalWorkerGroup, executor, wrapperFactory);
            }
        };

        // Is currently just AttributeKey.valueOf("ListerInfo") but we might as well copy the value itself.
        AttributeKey<ListenerInfo> listener = PipelineUtils.LISTENER;
        listenerInfo = new ListenerInfo(
                listenerInfo.getSocketAddress(),
                listenerInfo.getMotd(),
                listenerInfo.getMaxPlayers(),
                listenerInfo.getTabListSize(),
                listenerInfo.getServerPriority(),
                listenerInfo.isForceDefault(),
                listenerInfo.getForcedHosts(),
                listenerInfo.getTabListType(),
                listenerInfo.isSetLocalAddress(),
                listenerInfo.isPingPassthrough(),
                listenerInfo.getQueryPort(),
                listenerInfo.isQueryEnabled(),
                proxyProtocol // If Geyser is expecting HAProxy, so should the Bungee end
        );

        // The field that stores all listeners in BungeeCord
        // As of https://github.com/ViaVersion/ViaVersion/pull/2698 ViaVersion adds a wrapper to this field to
        // add its connections
        Field listenerField = proxyClass.getDeclaredField("listeners");
        listenerField.setAccessible(true);
        bungeeChannels = (Set<Channel>) listenerField.get(proxy);

        // This method is what initializes the connection in Java Edition, after Netty is all set.
        Method initChannel = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
        initChannel.setAccessible(true);

        bossGroup = new MultiThreadIoEventLoopGroup(LocalIoHandler.newFactory());
        ChannelFuture channelFuture = (new ServerBootstrap()
                .channel(LocalServerChannelWrapper.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(@NonNull Channel ch) throws Exception {
                        if (proxy.getConfig().getServers() == null) {
                            // Proxy hasn't finished loading all plugins - it loads the config after all plugins
                            // Probably doesn't need to be translatable?
                            plugin.getLogger().info("Disconnecting player as Bungee has not finished loading");
                            ch.close();
                            return;
                        }

                        if (channelInitializer == null) {
                            // Proxy has finished initializing; we can safely grab this variable without fear of plugins modifying it
                            // (Older versions of ViaVersion replace this to inject)
                            channelInitializer = proxy.unsafe().getFrontendChannelInitializer().getChannelInitializer();
                        }
                        initChannel.invoke(channelInitializer, ch);

                        if (disableCompression) {
                            ch.pipeline().addAfter(PipelineUtils.PACKET_ENCODER, "geyser-compression-disabler",
                                    new GeyserBungeeCompressionDisabler());
                        }
                    }
                })
                .childAttr(listener, listenerInfo)
                .group(bossGroup, wrapperGroup)
                .localAddress(LocalAddress.ANY))
                .bind()
                .syncUninterruptibly();

        this.localChannel = channelFuture;
        this.bungeeChannels.add(this.localChannel.channel());
        connector = new HostLocalConnector(channelFuture.channel().localAddress(), true);

        if (!this.eventRegistered) {
            // Register reload listener
            this.proxy.getPluginManager().registerListener(this.plugin, this);
            this.eventRegistered = true;
        }

        return new VanillaLocalTransport.Address(connector);
    }

    @Override
    public synchronized void reloadHandler(Runnable handler) {
        reloadHandler = handler;
    }

    @Override
    public synchronized void close() {
        closed = true;
        if (eventRegistered) {
            proxy.getPluginManager().unregisterListener(this);
            eventRegistered = false;
        }
        reloadHandler = null;
        stopCurrent(false);
        retired.forEach(RetiredListener::close);
        retired.clear();
    }

    @Override
    public synchronized SocketAddress rebind(boolean disableCompression, String listener, boolean proxyProtocol) throws Exception {
        if (closed) {
            return null;
        }
        stopCurrent(true);
        try {
            return initialize(disableCompression, listener, proxyProtocol);
        } catch (Exception | Error failure) {
            try {
                stopCurrent(false);
            } catch (RuntimeException | Error cleanup) {
                if (cleanup != failure) {
                    failure.addSuppressed(cleanup);
                }
            }
            throw failure;
        }
    }

    private void stopCurrent(boolean preserveSessions) {
        if (localChannel != null) {
            if (bungeeChannels != null) {
                bungeeChannels.remove(localChannel.channel());
            }
            localChannel.channel().close().syncUninterruptibly();
            localChannel = null;
        }
        bungeeChannels = null;
        channelInitializer = null;
        var listener = new RetiredListener(connector, bossGroup, wrapperGroup);
        connector = null;
        bossGroup = null;
        wrapperGroup = null;
        if (preserveSessions && listener.connector() != null) {
            // Existing channels retain their IO handlers until their clients disconnect.
            retired.add(listener);
            listener.boss().shutdownGracefully(0, 5, TimeUnit.SECONDS);
            listener.connector().retire().whenComplete((ignored, failure) ->
                listener.workers().shutdownGracefully(0, 5, TimeUnit.SECONDS).addListener(stopped ->
                    listener.boss().terminationFuture().addListener(done -> retired.remove(listener))));
        } else {
            listener.close();
        }
    }

    private record RetiredListener(HostLocalConnector connector, EventLoopGroup boss, EventLoopGroup workers) {
        void close() {
            if (connector != null) {
                connector.close();
            }
            if (workers != null) {
                workers.shutdownGracefully(0, 5, TimeUnit.SECONDS).syncUninterruptibly();
            }
            if (boss != null) {
                boss.shutdownGracefully(0, 5, TimeUnit.SECONDS).syncUninterruptibly();
            }
        }
    }

    @EventHandler
    public void onProxyReload(ProxyReloadEvent event) {
        Runnable handler;
        synchronized (this) {
            // BungeeCord replaces its listener set during a reload.
            bungeeChannels = null;
            handler = reloadHandler;
        }
        if (handler != null) {
            handler.run();
        }
    }

    private static IoHandlerFactory getNativeHandlerFactory() {
        // Match whatever settings the Bungee proxy is using
        // https://github.com/SpigotMC/BungeeCord/blob/617c2728a25347487eee4e8649d52fe57f1ff6e2/proxy/src/main/java/net/md_5/bungee/netty/PipelineUtils.java#L139-L162
        if (Boolean.parseBoolean(System.getProperty("bungee.io_uring", "false")) && IoUring.isAvailable()) {
            return IoUringIoHandler.newFactory();
        }
        if (Boolean.parseBoolean(System.getProperty("bungee.epoll", "true")) && Epoll.isAvailable()) {
            return EpollIoHandler.newFactory();
        }
        return NioIoHandler.newFactory();
    }
}

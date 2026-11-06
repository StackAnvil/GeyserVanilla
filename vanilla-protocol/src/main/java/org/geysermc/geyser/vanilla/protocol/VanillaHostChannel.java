/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.AbstractChannel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import org.geysermc.geyser.vanilla.runtime.VanillaLocalTransport;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Native Netty endpoint whose peer uses the host's independently loaded Netty classes. */
final class VanillaHostChannel extends AbstractChannel {
    private static final ChannelMetadata METADATA = new ChannelMetadata(false);
    private final ChannelConfig config = new DefaultChannelConfig(this) {
        @Override
        protected void autoReadCleared() {
            if (isRegistered() && !eventLoop().inEventLoop()) {
                eventLoop().execute(() -> reading = false);
            } else {
                reading = false;
            }
        }
    };
    private final Deque<Read> pendingReads = new ArrayDeque<>();
    private InetSocketAddress clientAddress;
    private VanillaLocalTransport.Address target;
    private VanillaLocalTransport.Connection connection;
    private ChannelPromise connectPromise;
    private volatile boolean open = true;
    private volatile boolean active;
    private boolean reading;
    private boolean writing;

    VanillaHostChannel(InetSocketAddress clientAddress) {
        super(null);
        this.clientAddress = clientAddress;
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new AbstractUnsafe() {
            @Override
            public void connect(SocketAddress remote, SocketAddress local, ChannelPromise promise) {
                if (!promise.setUncancellable() || !ensureOpen(promise)) {
                    return;
                }
                if (connectPromise != null) {
                    promise.setFailure(new IllegalStateException("A hosted channel can only connect once"));
                    return;
                }
                if (!(remote instanceof VanillaLocalTransport.Address address)) {
                    promise.setFailure(new IllegalArgumentException("Expected a hosted connection address"));
                    return;
                }
                target = address;
                connectPromise = promise;
                try {
                    address.transport().connect(clientAddress, new VanillaLocalTransport.Receiver() {
                        @Override
                        public CompletionStage<Void> receive(byte[] bytes) {
                            if (!open) {
                                return CompletableFuture.failedFuture(new ClosedChannelException());
                            }
                            var delivered = new CompletableFuture<Void>();
                            eventLoop().execute(() -> {
                                if (!open) {
                                    delivered.completeExceptionally(new ClosedChannelException());
                                    return;
                                }
                                pendingReads.add(new Read(bytes, delivered));
                                drainReads();
                            });
                            return delivered;
                        }

                        @Override
                        public void closed(Throwable cause) {
                            if (!open) {
                                return;
                            }
                            eventLoop().execute(() -> {
                                if (cause != null) {
                                    pipeline().fireExceptionCaught(cause);
                                }
                                VanillaHostChannel.this.close();
                            });
                        }
                    }).whenComplete((peer, failure) -> {
                        if (!open) {
                            if (peer != null) {
                                peer.close();
                            }
                            promise.tryFailure(new ClosedChannelException());
                            return;
                        }
                        eventLoop().execute(() -> {
                            if (failure != null) {
                                promise.tryFailure(failure);
                                VanillaHostChannel.this.close();
                            } else if (!open) {
                                peer.close();
                                promise.tryFailure(new ClosedChannelException());
                            } else {
                                connection = peer;
                                active = true;
                                promise.trySuccess();
                                pipeline().fireChannelActive();
                                drainReads();
                            }
                        });
                    });
                } catch (Throwable failure) {
                    promise.tryFailure(failure);
                    VanillaHostChannel.this.close();
                }
            }
        };
    }

    @Override
    public ChannelConfig config() {
        return config;
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof DefaultEventLoop;
    }

    @Override
    protected SocketAddress localAddress0() {
        return clientAddress;
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return target;
    }

    @Override
    protected void doBind(SocketAddress local) {
        clientAddress = (InetSocketAddress) local;
    }

    @Override
    protected void doDisconnect() {
        doClose();
    }

    @Override
    protected void doClose() {
        if (!open) {
            return;
        }
        open = false;
        active = false;
        if (connectPromise != null) {
            connectPromise.tryFailure(new ClosedChannelException());
        }
        while (!pendingReads.isEmpty()) {
            pendingReads.remove().delivered().completeExceptionally(new ClosedChannelException());
        }
        if (connection != null) {
            connection.close();
        }
    }

    @Override
    protected void doBeginRead() {
        reading = true;
        drainReads();
    }

    private void drainReads() {
        boolean delivered = false;
        while (active && (reading || config.isAutoRead()) && !pendingReads.isEmpty()) {
            reading = false;
            Read read = pendingReads.remove();
            pipeline().fireChannelRead(alloc().buffer(read.bytes().length).writeBytes(read.bytes()));
            read.delivered().complete(null);
            delivered = true;
        }
        if (delivered) {
            pipeline().fireChannelReadComplete();
        }
    }

    @Override
    protected Object filterOutboundMessage(Object message) {
        if (!(message instanceof ByteBuf)) {
            throw new UnsupportedOperationException("Hosted connections accept serialized bytes only");
        }
        return message;
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer outbound) {
        if (writing || outbound.current() == null) {
            return;
        }
        ByteBuf message = (ByteBuf) outbound.current();
        writing = true;
        try {
            connection.write(ByteBufUtil.getBytes(message)).whenComplete((ignored, failure) -> eventLoop().execute(() -> {
                writing = false;
                if (!open || outbound.current() != message) {
                    return;
                }
                if (failure != null) {
                    outbound.remove(failure);
                    pipeline().fireExceptionCaught(failure);
                    VanillaHostChannel.this.close();
                } else {
                    outbound.remove();
                    doWrite(outbound);
                }
            }));
        } catch (Throwable failure) {
            writing = false;
            outbound.remove(failure);
            pipeline().fireExceptionCaught(failure);
            close();
        }
    }

    private record Read(byte[] bytes, CompletableFuture<Void> delivered) {
    }
}

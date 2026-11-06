/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.runtime;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/** Passes serialized Java protocol bytes across the host/runtime classloader boundary. */
public interface VanillaLocalTransport {
    CompletionStage<Connection> connect(InetSocketAddress clientAddress, Receiver receiver);

    interface Connection {
        /** Transfers an owned byte array. Completion acknowledges the write and provides backpressure. */
        CompletionStage<Void> write(byte[] bytes);

        void close();
    }

    interface Receiver {
        /** Transfers an owned byte array. Completion acknowledges delivery to the native pipeline. */
        CompletionStage<Void> receive(byte[] bytes);

        void closed(Throwable cause);
    }

    final class Address extends SocketAddress {
        private static final long serialVersionUID = 1L;
        private final transient VanillaLocalTransport transport;

        public Address(VanillaLocalTransport transport) {
            this.transport = Objects.requireNonNull(transport);
        }

        public VanillaLocalTransport transport() {
            return transport;
        }

        @Override
        public String toString() {
            return "Geyser hosted connection";
        }
    }
}

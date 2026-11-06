/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeyserStartupTest {
    private Field instance;
    private GeyserImpl previous;
    private GeyserImpl engine;

    @BeforeEach
    void setup() throws Exception {
        instance = GeyserImpl.class.getDeclaredField("instance");
        instance.setAccessible(true);
        previous = (GeyserImpl) instance.get(null);
        engine = mock(GeyserImpl.class);
        instance.set(null, engine);
    }

    @AfterEach
    void restore() throws Exception {
        instance.set(null, previous);
    }

    static Stream<Throwable> startupFailures() {
        return Stream.of(new IllegalStateException("listener startup failed"), new LinkageError("host API mismatch"));
    }

    @ParameterizedTest
    @MethodSource("startupFailures")
    void cleansUpPartialStartupAndPreservesTheOriginalFailure(Throwable failure) {
        doThrow(failure).when(engine).initialize();
        assertSame(failure, assertThrows(Throwable.class, GeyserImpl::start));
        verify(engine).disable();
        verify(engine, never()).setEnabled(true);
    }

    @Test
    void retainsCleanupFailureAsSuppressed() {
        var failure = new IllegalStateException("listener startup failed");
        var cleanup = new IllegalStateException("cleanup failed");
        doThrow(failure).when(engine).initialize();
        doThrow(cleanup).when(engine).disable();
        assertSame(failure, assertThrows(IllegalStateException.class, GeyserImpl::start));
        assertArrayEquals(new Throwable[] {cleanup}, failure.getSuppressed());
    }

    @Test
    void enablesTheEngineAfterSuccessfulStartup() {
        GeyserImpl.start();
        verify(engine).initialize();
        verify(engine).setEnabled(true);
        verify(engine, never()).disable();
    }
}

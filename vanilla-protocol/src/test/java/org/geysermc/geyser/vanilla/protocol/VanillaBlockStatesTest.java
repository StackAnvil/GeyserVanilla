/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import net.minecraft.core.IdMapper;
import net.minecraft.world.level.block.Block;
import org.geysermc.geyser.vanilla.runtime.VanillaClassLoader;
import org.geysermc.geyser.vanilla.runtime.VanillaRuntime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.File;
import java.lang.classfile.ClassFile;
import java.lang.constant.ClassDesc;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
class VanillaBlockStatesTest {
    private static VanillaClassLoader loader;
    private static Class<?> fixture;

    @BeforeAll
    static void open() throws Exception {
        VanillaBootstrap.defaultRegistries();
        var classpath = Arrays.stream(System.getProperty("geyser.test.runtime.classpath").split(File.pathSeparator))
            .map(Path::of).toList();
        loader = new VanillaClassLoader(VanillaRuntime.urls(classpath), VanillaBlockStatesTest.class.getClassLoader(),
            List.of("org.geysermc.geyser.vanilla.runtime.", "org.slf4j."));
        fixture = loader.loadClass(CustomBlockStateFixture.class.getName());
        run("initialize");
    }

    @AfterAll
    static void close() throws Exception {
        if (loader != null) loader.close();
    }

    @Test
    void retainsSparseIdsWithoutGrowingDenseStorageOrChangingOtherMappers() throws Exception {
        int[] ids = (int[]) run("mappings");
        assertArrayEquals(new int[] {Block.BLOCK_STATE_REGISTRY.size() + 100, 1_000_000_007, Integer.MAX_VALUE}, ids);
        assertNotSame(IdMapper.class, loader.loadClass(IdMapper.class.getName()));
        for (int id : ids) assertNull(Block.BLOCK_STATE_REGISTRY.byId(id), "The host's mapper was changed");
        var gameSource = loader.loadClass(Block.class.getName()).getProtectionDomain().getCodeSource();
        var mapperSource = loader.loadClass(IdMapper.class.getName()).getProtectionDomain().getCodeSource();
        assertEquals(gameSource.getLocation(), mapperSource.getLocation());
        assertArrayEquals(gameSource.getCertificates(), mapperSource.getCertificates());
    }

    @Test
    void roundTripsSingleBlockUpdatesThroughTheOriginalNativeProtocol() throws Exception { run("blockUpdates"); }

    @Test
    void roundTripsSectionUpdatesThroughTheOriginalNativeCodec() throws Exception { run("sectionUpdates"); }

    @Test
    void roundTripsBlockParticlesAndEntityMetadataThroughNativeCodecs() throws Exception { run("particlesAndMetadata"); }

    @Test
    void retainsUnconfiguredIdsForTheExistingCustomBlockFallback() throws Exception { run("unconfiguredIds"); }

    @Test
    void rejectsUnexpectedClientClassesAndChangedLookupSignatures() {
        assertThrows(IllegalArgumentException.class, () -> VanillaClassTransforms.idMapper(
            ClassFile.of().build(ClassDesc.of("fixture.NotIdMapper"), builder -> {})));
        assertThrows(IllegalStateException.class, () -> VanillaClassTransforms.idMapper(
            ClassFile.of().build(ClassDesc.of("net.minecraft.core.IdMapper"), builder -> {})));
    }

    @Test
    void refusesToChangeTheHostRegistryOutsideThePrivateRuntime() {
        int size = Block.BLOCK_STATE_REGISTRY.size();
        assertNull(VanillaBlockStates.byId(Block.BLOCK_STATE_REGISTRY, size + 100));
        assertEquals(-1, VanillaBlockStates.getId(Block.BLOCK_STATE_REGISTRY, new Object()));
        assertEquals(size, Block.BLOCK_STATE_REGISTRY.size());
        assertNull(Block.BLOCK_STATE_REGISTRY.byId(size + 100));
    }

    private static Object run(String method) throws Exception {
        try {
            return fixture.getMethod(method).invoke(null);
        } catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof Error error) throw error;
            if (failure.getCause() instanceof Exception exception) throw exception;
            throw failure;
        }
    }
}

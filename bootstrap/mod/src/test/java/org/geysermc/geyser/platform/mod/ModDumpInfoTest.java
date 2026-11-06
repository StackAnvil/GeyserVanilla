/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.mod;

import com.google.gson.Gson;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.text.AsteriskSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ModDumpInfoTest {
    @Test
    void retainsHostMetadataAndMasksPublicIpUnlessFullDumpWasRequested() {
        boolean previous = AsteriskSerializer.showSensitive;
        try (var singleton = mockStatic(GeyserImpl.class)) {
            var engine = mock(GeyserImpl.class);
            when(engine.platformType()).thenReturn(PlatformType.FABRIC);
            singleton.when(GeyserImpl::getInstance).thenReturn(engine);
            var dump = new ModDumpInfo(Map.of("serverIP", "203.0.113.96", "serverPort", 25565,
                "mods", List.of(Map.of("name", "example", "enabled", true))));
            var gson = new Gson();
            AsteriskSerializer.showSensitive = false;
            var masked = gson.toJsonTree(dump).getAsJsonObject();
            assertEquals("***", masked.get("serverIP").getAsString());
            assertEquals(25565, masked.get("serverPort").getAsInt());
            assertEquals("Fabric", masked.getAsJsonObject("platform").get("platformName").getAsString());
            assertEquals("example", masked.getAsJsonArray("mods").get(0).getAsJsonObject().get("name").getAsString());
            AsteriskSerializer.showSensitive = true;
            assertEquals("203.0.113.96", gson.toJsonTree(dump).getAsJsonObject().get("serverIP").getAsString());
        } finally {
            AsteriskSerializer.showSensitive = previous;
        }
    }
}

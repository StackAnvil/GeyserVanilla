/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.mod;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.text.AsteriskSerializer;

import java.lang.reflect.Type;
import java.util.Map;

/** Serializes plain host metadata with the engine's normal IP masking policy. */
@JsonAdapter(ModDumpInfo.Serializer.class)
public final class ModDumpInfo extends BootstrapDumpInfo {
    private final Map<String, Object> metadata;

    public ModDumpInfo(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public static final class Serializer implements JsonSerializer<ModDumpInfo> {
        @Override
        public JsonElement serialize(ModDumpInfo value, Type type, JsonSerializationContext context) {
            var result = context.serialize(value.metadata).getAsJsonObject();
            result.add("platform", context.serialize(value.getPlatform()));
            Object address = value.metadata.get("serverIP");
            if (address instanceof String ip) {
                result.add("serverIP", new AsteriskSerializer().serialize(ip, String.class, context));
            }
            return result;
        }
    }
}

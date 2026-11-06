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

package org.geysermc.geyser.translator.protocol.java;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.SetPlayerGameTypePacket;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.level.JavaDimension;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.ChunkUtils;
import org.geysermc.geyser.util.DimensionUtils;
import org.geysermc.geyser.util.EntityUtils;
import net.minecraft.network.protocol.game.CommonPlayerSpawnInfo;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;

@Translator(packet = ClientboundRespawnPacket.class)
public class JavaRespawnTranslator extends PacketTranslator<ClientboundRespawnPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundRespawnPacket packet) {
        SessionPlayerEntity entity = session.getPlayerEntity();
        CommonPlayerSpawnInfo spawnInfo = packet.commonPlayerSpawnInfo();

        if (!packet.shouldKeep(ClientboundRespawnPacket.KEEP_ENTITY_DATA)) {
            entity.resetMetadata();
        }

        if (!packet.shouldKeep(ClientboundRespawnPacket.KEEP_ATTRIBUTE_MODIFIERS)) {
            entity.resetAttributes();
        }

        session.setSpawned(false);
        entity.setMotion(Vector3f.ZERO);
        entity.setLastTickEndVelocity(Vector3f.ZERO);

        entity.setHealth(entity.getMaxHealth());
        entity.getAttributes().put(GeyserAttributeType.HEALTH, entity.createHealthAttribute());

        session.setInventoryHolder(null);
        session.setClosingInventory(false);

        entity.setLastDeathPosition(spawnInfo.lastDeathLocation().orElse(null));
        entity.updateBedrockMetadata();

        SetPlayerGameTypePacket playerGameTypePacket = new SetPlayerGameTypePacket();
        playerGameTypePacket.setGamemode(EntityUtils.toBedrockGamemode(spawnInfo.gameType()).ordinal());
        session.sendUpstreamPacket(playerGameTypePacket);
        session.setGameMode(spawnInfo.gameType());

        if (session.isRaining()) {
            session.updateRain(0);
        }

        if (session.isThunder()) {
            session.updateThunder(0);
        }

        JavaDimension newDimension = JavaRegistries.DIMENSION_TYPE.getOrThrow(session.javaRegistries(), org.geysermc.geyser.util.MinecraftKey.key(spawnInfo.dimensionType().unwrapKey().orElseThrow().identifier().toString()));
        if (session.getDimensionType() != newDimension || !org.geysermc.geyser.util.MinecraftKey.key(spawnInfo.dimension().identifier().toString()).equals(session.getWorldName())) {
            // Switching to a new world (based off the world name change or new dimension); send a fake dimension change
            if (session.getDimensionType().bedrockId() == newDimension.bedrockId()) {
                int fakeDim = DimensionUtils.getTemporaryDimension(session.getDimensionType().bedrockId(), newDimension.bedrockId());
                DimensionUtils.fastSwitchDimension(session, fakeDim);
            }
            session.setWorldName(org.geysermc.geyser.util.MinecraftKey.key(spawnInfo.dimension().identifier().toString()));
            session.resetTimeParameters();
            DimensionUtils.switchDimension(session, newDimension);

            ChunkUtils.loadDimension(session);
        }

        session.sendDownstreamGamePacket(new ServerboundPlayerLoadedPacket());
    }
}

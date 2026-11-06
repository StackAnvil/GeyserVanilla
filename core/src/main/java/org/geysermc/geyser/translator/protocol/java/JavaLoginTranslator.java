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

import net.kyori.adventure.key.Key;
import org.cloudburstmc.protocol.bedrock.data.GameRuleData;
import org.cloudburstmc.protocol.bedrock.packet.GameRulesChangedPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetPlayerGameTypePacket;
import org.geysermc.erosion.Constants;
import org.geysermc.floodgate.pluginmessage.PluginMessageChannels;
import org.geysermc.geyser.api.network.AuthType;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.level.BedrockDimension;
import org.geysermc.geyser.level.JavaDimension;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.ChunkUtils;
import org.geysermc.geyser.util.DimensionUtils;
import org.geysermc.geyser.util.EntityUtils;
import org.geysermc.geyser.util.MinecraftKey;
import net.minecraft.network.protocol.game.CommonPlayerSpawnInfo;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;

import java.nio.charset.StandardCharsets;

@Translator(packet = ClientboundLoginPacket.class)
public class JavaLoginTranslator extends PacketTranslator<ClientboundLoginPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundLoginPacket packet) {
        SessionPlayerEntity entity = session.getPlayerEntity();
        entity.setEntityId(packet.playerId());

        CommonPlayerSpawnInfo spawnInfo = packet.commonPlayerSpawnInfo();
        JavaDimension newDimension = JavaRegistries.DIMENSION_TYPE.getOrThrow(session.javaRegistries(), org.geysermc.geyser.util.MinecraftKey.key(spawnInfo.dimensionType().unwrapKey().orElseThrow().identifier().toString()));

        // If the player is already initialized and a join game packet is sent, they
        // are swapping servers
        if (session.isSpawned()) {
            int fakeDim = DimensionUtils.getTemporaryDimension(session.getBedrockDimension().bedrockId(), newDimension.bedrockId());
            if (fakeDim != newDimension.bedrockId()) {
                // The player's current dimension and new dimension are the same
                // We want a dimension switch to clear old chunks out, so switch to a dimension that isn't the one we're currently in.
                // Another dimension switch will be required to switch back
                DimensionUtils.fastSwitchDimension(session, fakeDim);
            }

            // Remove all bossbars
            session.getEntityCache().removeAllBossBars();
            // Remove extra hearts, hunger, etc.
            entity.resetAttributes();
            entity.resetMetadata();

            // Reset inventories; just in case. Might resolve some issues where inventories get stuck?
            session.setInventoryHolder(null);
            session.setPendingOrCurrentBedrockInventoryId(-1);
            session.setClosingInventory(false);

            // Clear waypoints
            session.getWaypointCache().clear();
        }

        session.setDimensionType(newDimension);
        session.setWorldName(org.geysermc.geyser.util.MinecraftKey.key(spawnInfo.dimension().identifier().toString()));
        session.setLevels(packet.levels().stream().map(key -> key.identifier().toString()).toArray(String[]::new));
        session.setGameMode(spawnInfo.gameType());

        boolean needsSpawnPacket = !session.isSentSpawnPacket();
        if (needsSpawnPacket) {
            // The player has yet to spawn so let's do that using some of the information in this Java packet
            DimensionUtils.setBedrockDimension(session, newDimension.bedrockId());
            session.connect();

            // It is now safe to send these packets
            session.getUpstream().sendPostStartGamePackets();
        } else {
            SetPlayerGameTypePacket playerGameTypePacket = new SetPlayerGameTypePacket();
            playerGameTypePacket.setGamemode(EntityUtils.toBedrockGamemode(spawnInfo.gameType()).ordinal());
            session.sendUpstreamPacket(playerGameTypePacket);
        }

        entity.setLastDeathPosition(spawnInfo.lastDeathLocation().orElse(null));

        entity.updateBedrockMetadata();

        // Send if client should show respawn screen
        GameRulesChangedPacket gamerulePacket = new GameRulesChangedPacket();
        gamerulePacket.getGameRules().add(new GameRuleData<>("doimmediaterespawn", !packet.showDeathScreen()));
        session.sendUpstreamPacket(gamerulePacket);

        session.setReducedDebugInfo(packet.reducedDebugInfo());

        session.setServerRenderDistance(packet.chunkRadius());

        // send this again now that we know the server render distance
        // as the bedrock client isn't required to send a render distance
        session.sendJavaClientSettings();

        Key register = MinecraftKey.key("register");
        if (session.remoteServer().authType() == AuthType.FLOODGATE) {
            session.sendDownstreamPacket(new ServerboundCustomPayloadPacket(new org.geysermc.geyser.vanilla.protocol.VanillaPayload(org.geysermc.geyser.util.MinecraftKey.keyToVanilla(register), PluginMessageChannels.getFloodgateRegisterData())));
        }
        session.sendDownstreamPacket(new ServerboundCustomPayloadPacket(new org.geysermc.geyser.vanilla.protocol.VanillaPayload(org.geysermc.geyser.util.MinecraftKey.keyToVanilla(register), Constants.PLUGIN_MESSAGE.getBytes(StandardCharsets.UTF_8))));

        if (session.getBedrockDimension().bedrockId() != newDimension.bedrockId()) {
            DimensionUtils.switchDimension(session, newDimension);
        } else if (BedrockDimension.isCustomBedrockNetherId() && newDimension.isNetherLike()) {
            // If the player is spawning into the "fake" nether, send them some fog
            session.camera().sendFog(DimensionUtils.BEDROCK_FOG_HELL);
        }

        ChunkUtils.loadDimension(session);

        if (!needsSpawnPacket) {
            session.sendDownstreamGamePacket(new ServerboundPlayerLoadedPacket());
        }
    }
}

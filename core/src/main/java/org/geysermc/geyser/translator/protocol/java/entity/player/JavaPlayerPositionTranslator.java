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

package org.geysermc.geyser.translator.protocol.java.entity.player;

import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.packet.ChunkRadiusUpdatedPacket;
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.RespawnPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.TeleportCache;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.ChunkUtils;
import org.geysermc.geyser.util.MathUtils;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;

@Translator(packet = ClientboundPlayerPositionPacket.class)
public class JavaPlayerPositionTranslator extends PacketTranslator<ClientboundPlayerPositionPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundPlayerPositionPacket packet) {
        if (!session.isLoggedIn()) {
            return;
        }

        final SessionPlayerEntity entity = session.getPlayerEntity();
        if (entity.getVehicle() != null) { // Vanilla behaviour, the player ignores teleport if they're on a vehicle.
            acceptTeleport(session, entity.position().toDouble(), entity.getJavaYaw(), entity.getPitch(), packet.id());
            entity.setPendingBedInteraction(null);
            return;
        }

        PositionMoveRotation absolute = PositionMoveRotation.calculateAbsolute(
            new PositionMoveRotation(MathUtils.toVec3(entity.position()), MathUtils.toVec3(entity.getMotion()), entity.getYaw(), entity.getPitch()),
            packet.change(), packet.relatives());
        Vector3d position = MathUtils.toVector3d(absolute.position());
        float newPitch = MathUtils.clamp(absolute.xRot(), -90, 90);
        float newYaw = absolute.yRot();

        final int teleportId = packet.id();

        acceptTeleport(session, position, newYaw, newPitch, teleportId);

        if (!session.isSpawned()) {
            entity.setPosition(position.toFloat());
            entity.setYaw(newYaw);
            entity.setPitch(newPitch);
            entity.setHeadYaw(newYaw);

            RespawnPacket respawnPacket = new RespawnPacket();
            respawnPacket.setRuntimeEntityId(0); // Bedrock server behavior
            respawnPacket.setPosition(entity.bedrockPosition());
            respawnPacket.setState(RespawnPacket.State.SERVER_READY);
            session.sendUpstreamPacket(respawnPacket);

            entity.updateBedrockMetadata();

            MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
            movePlayerPacket.setRuntimeEntityId(entity.geyserId());
            movePlayerPacket.setPosition(entity.bedrockPosition());
            movePlayerPacket.setRotation(entity.bedrockRotation());
            movePlayerPacket.setMode(MovePlayerPacket.Mode.RESPAWN);
            session.sendUpstreamPacket(movePlayerPacket);

            // Fixes incorrect rotation upon login
            // Yes, even that's not respected by Bedrock. Try it out in singleplayer!
            // Log out and back in - and you're looking elsewhere :)
            entity.updateOwnRotation(entity.getYaw(), entity.getPitch(), entity.getHeadYaw());
            session.setSpawned(true);

            // Make sure the player moves away from (0, 32767, 0) before accepting movement packets
            Vector3f entityPosition = entity.position();
            session.setUnconfirmedTeleport(new TeleportCache(session, entityPosition, newPitch, newYaw, packet.id()));

            if (session.getServerRenderDistance() > 32 && !session.isEmulatePost1_13Logic()) {
                // See DimensionUtils for an explanation
                ChunkRadiusUpdatedPacket chunkRadiusUpdatedPacket = new ChunkRadiusUpdatedPacket();
                chunkRadiusUpdatedPacket.setRadius(session.getServerRenderDistance());
                session.sendUpstreamPacket(chunkRadiusUpdatedPacket);

                session.setLastChunkPosition(null);
            }

            ChunkUtils.updateChunkPosition(session, entityPosition.toInt());

            if (session.getGeyser().config().debugMode()) {
                session.getGeyser().getLogger().debug("Spawned player at " + position);
            }
            entity.setPendingBedInteraction(null);
            return;
        }

        Vector3f lastPlayerPosition = entity.position();
        float lastPlayerPitch = entity.getPitch();
        float lastPlayerYaw = entity.getYaw();
        session.getGeyser().getLogger().debug("Teleport (" + teleportId + ") from " + lastPlayerPosition);

        Vector3f teleportDestination = position.toFloat();

        Vector3f deltaMovement = MathUtils.toVector3f(absolute.deltaMovement());

        SessionPlayerEntity.PendingBedInteraction pendingBedInteraction = entity.getPendingBedInteraction();
        if (pendingBedInteraction != null) {
            // See the Javadocs over at SessionPlayerEntity#pendingBedInteraction - we need to silently ignore a teleport if it just teleports us to the bed

            entity.setPendingBedInteraction(null);
            if (looksLikeBedTeleport(pendingBedInteraction, position, deltaMovement)) {
                GeyserImpl.getInstance().getLogger().debug("Not translating teleport because it is to a pending bed interaction");
                return;
            }
        }

        TeleportCache.TeleportType type = (deltaMovement.distanceSquared(Vector3f.ZERO) > 1.0E-8F) ?
            TeleportCache.TeleportType.KEEP_VELOCITY : TeleportCache.TeleportType.NORMAL;

        // Set the unconfirmed teleport to ensure we send the adjusted Bedrock position in moveAbsolute (see CollisionManager#adjustPositionForBedrock)
        session.setUnconfirmedTeleport(new TeleportCache(session, teleportDestination, deltaMovement, newPitch, newYaw, teleportId, type));
        entity.moveAbsolute(teleportDestination, newYaw, newPitch, false, true);

        // Bedrock ignores teleports that are extremely close to the player's original position and orientation, so check if we need to cache the teleport
        if (lastPlayerPosition.distanceSquared(teleportDestination) < 0.001 && Math.abs(newPitch - lastPlayerPitch) < 5 && Math.abs(newYaw - lastPlayerYaw) < 5) {
            session.setUnconfirmedTeleport(null);
        }

        if (type == TeleportCache.TeleportType.KEEP_VELOCITY) {
            entity.setMotion(deltaMovement);
            // Our motion got reset by the teleport but the deltaMovement is not 0 so send a motion packet to fix that.
            SetEntityMotionPacket entityMotionPacket = new SetEntityMotionPacket();
            entityMotionPacket.setRuntimeEntityId(entity.geyserId());
            entityMotionPacket.setMotion(entity.getMotion());
            session.sendUpstreamPacket(entityMotionPacket);
        }

        session.getGeyser().getLogger().debug("to " + entity.position());
    }

    private void acceptTeleport(GeyserSession session, Vector3d position, float yaw, float pitch, int id) {
        // Confirm the teleport when we receive it to match Java edition
        ServerboundAcceptTeleportationPacket teleportConfirmPacket = new ServerboundAcceptTeleportationPacket(id, position.getX(), position.getY(), position.getZ(), yaw, pitch);
        session.sendDownstreamGamePacket(teleportConfirmPacket);
    }

    private static boolean looksLikeBedTeleport(SessionPlayerEntity.PendingBedInteraction pendingInteraction, Vector3d packetPosition, Vector3f deltaMovement) {
        // Teleports to a bed after a bed interaction always teleports to the centre of the bed (plus some Y offset), with a delta movement of 0 (as of Java 26.3)
        return pendingInteraction.expectedTeleportPosition().equals(packetPosition) && deltaMovement.length() == 0.0;
    }
}

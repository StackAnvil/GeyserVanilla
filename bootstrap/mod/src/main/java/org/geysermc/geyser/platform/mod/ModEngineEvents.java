/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.platform.mod;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.Permissions;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.level.physics.PistonAction;
import org.geysermc.geyser.platform.mod.command.ModCommandSource;
import org.geysermc.geyser.platform.mod.host.ModCommandSender;
import org.geysermc.geyser.platform.mod.host.ModEvents;
import org.geysermc.geyser.translator.level.block.entity.PistonBlockEntity;
import org.geysermc.geyser.util.VersionCheckUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Engine-owned translation of host gameplay notifications. */
public final class ModEngineEvents implements ModEvents {
    private final GeyserImpl geyser;

    public ModEngineEvents(GeyserImpl geyser) {
        this.geyser = geyser;
    }

    @Override
    public Map<UUID, Integer> playerRenderDistances() {
        Map<UUID, Integer> distances = new HashMap<>();
        geyser.getSessionManager().getSessions().forEach((uuid, session) -> distances.put(uuid, session.getServerRenderDistance()));
        return distances;
    }

    @Override
    public void blockPlaced(UUID player, int x, int y, int z, int blockState) {
        var session = geyser.connectionByUuid(player);
        if (session == null) {
            return;
        }
        var sound = new LevelSoundEventPacket();
        sound.setSound(SoundEvent.PLACE);
        sound.setPosition(Vector3f.from(x, y, z));
        sound.setBabySound(false);
        sound.setExtraData(session.getBlockMappings().getBedrockBlockId(blockState));
        sound.setIdentifier(":");
        session.sendUpstreamPacket(sound);
        session.setLastBlockPlacePosition(null);
        session.setLastBlockPlaced(null);
    }

    @Override
    public void pistonMoved(UUID[] players, int x, int y, int z, int direction, boolean sticky, boolean extending, int[] blocks) {
        var attached = new Object2ObjectArrayMap<Vector3i, BlockState>();
        for (int i = 0; i < blocks.length; i += 4) {
            attached.put(Vector3i.from(blocks[i], blocks[i + 1], blocks[i + 2]), BlockState.of(blocks[i + 3]));
        }
        var position = Vector3i.from(x, y, z);
        var orientation = Direction.VALUES[direction];
        var action = extending ? PistonAction.EXTEND : PistonAction.RETRACT;
        for (UUID player : players) {
            var session = geyser.connectionByUuid(player);
            if (session != null) {
                session.executeInEventLoop(() -> {
                    var piston = session.getPistonCache().getPistons().computeIfAbsent(position,
                        pos -> new PistonBlockEntity(session, pos, orientation, sticky, !extending));
                    piston.setAction(action, attached);
                });
            }
        }
    }

    @Override
    public void playerJoined(ModCommandSender player) {
        if (geyser.config().notifyOnNewBedrockUpdate()) {
            var source = new ModCommandSource(player);
            if (source.hasPermission(Permissions.CHECK_UPDATE)) {
                VersionCheckUtils.checkForGeyserUpdate(() -> source);
            }
        }
    }
}

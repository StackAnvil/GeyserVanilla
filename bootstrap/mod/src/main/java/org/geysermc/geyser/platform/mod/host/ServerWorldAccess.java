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

package org.geysermc.geyser.platform.mod.host;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class ServerWorldAccess implements ModWorldAccess {

    private final MinecraftServer server;

    public ServerWorldAccess(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public int blockAt(UUID playerId, int x, int y, int z) {
        ServerPlayer player = this.getPlayer(playerId);
        if (player == null) {
            return 0;
        }

        Level level = player.level();
        if (y < level.getMinY()) {
            return 0;
        }

        // Only loads active chunks, and doesn't delegate to main thread
        ChunkAccess chunk = ((ServerChunkCache) level.getChunkSource()).chunkMap.getChunkToSend(ChunkPos.pack(x >> 4, z >> 4));
        if (chunk == null) {
            return 0;
        }

        int worldOffset = level.getMinY() >> 4;
        int chunkOffset = (y >> 4) - worldOffset;
        if (chunkOffset < chunk.getSections().length) {
            LevelChunkSection section = chunk.getSections()[chunkOffset];
            if (section != null && !section.hasOnlyAir()) {
                return Block.getId(section.getBlockState(x & 15, y & 15, z & 15));
            }
        }

        return 0;
    }

    @Override
    public int protocolVersion() {
        return SharedConstants.getCurrentVersion().protocolVersion();
    }

    @Override
    public int defaultGameMode() {
        return server.getDefaultGameType().getId();
    }

    @Override
    public void decoratedPotAssets(UUID playerId, int x, int y, int z, Consumer<Map<String, String>> apply) {
        server.execute(() -> {
            ServerPlayer player = getPlayer(playerId);
            if (player == null) {
                return;
            }

            BlockPos blockPos = new BlockPos(x, y, z);
            // Don't create a new block entity if invalid
            //noinspection resource - level() is just a getter
            BlockEntity blockEntity = player.level().getChunkAt(blockPos).getBlockEntity(blockPos);
            if (blockEntity instanceof DecoratedPotBlockEntity pot) {
                PotDecorations decorations = pot.getDecorations();
                Map<String, String> toApply = new HashMap<>();
                decorations.back().flatMap(ServerWorldAccess::extractPotteryPattern).ifPresent(pattern -> toApply.put("back", pattern));
                decorations.left().flatMap(ServerWorldAccess::extractPotteryPattern).ifPresent(pattern -> toApply.put("left", pattern));
                decorations.right().flatMap(ServerWorldAccess::extractPotteryPattern).ifPresent(pattern -> toApply.put("right", pattern));
                decorations.front().flatMap(ServerWorldAccess::extractPotteryPattern).ifPresent(pattern -> toApply.put("front", pattern));
                apply.accept(toApply);
            }
        });
    }

    private static Optional<String> extractPotteryPattern(ItemStackTemplate template) {
        return Optional.ofNullable(template.get(DataComponents.PROVIDES_POTTERY_PATTERN))
            .map(holder -> holder.value().assetId().toString());
    }

    private ServerPlayer getPlayer(UUID playerId) {
        return server.getPlayerList().getPlayer(playerId);
    }
}

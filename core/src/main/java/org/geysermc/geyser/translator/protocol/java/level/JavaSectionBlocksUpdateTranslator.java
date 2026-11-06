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

package org.geysermc.geyser.translator.protocol.java.level;

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.BlockChangeEntry.MessageType;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateSubChunkBlocksPacket;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.registry.BlockRegistries;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

import java.util.BitSet;
import java.util.Objects;

@Translator(packet = ClientboundSectionBlocksUpdatePacket.class)
public class JavaSectionBlocksUpdateTranslator extends PacketTranslator<ClientboundSectionBlocksUpdatePacket> {

    private static final int FLAG_ALL = 1 << UpdateBlockPacket.Flag.NEIGHBORS.ordinal() | 1 << UpdateBlockPacket.Flag.NETWORK.ordinal();

    @Override
    public void translate(GeyserSession session, ClientboundSectionBlocksUpdatePacket packet) {
        Vector3i clientBreakPos = session.getBlockBreakHandler().getCurrentBlockPos();
        BitSet waterlogged = BlockRegistries.WATERLOGGED.get();

        UpdateSubChunkBlocksPacket updateSubChunkBlocksPacket = new UpdateSubChunkBlocksPacket();
        packet.runUpdates((javaPosition, javaState) -> {
            Vector3i position = org.geysermc.geyser.util.MathUtils.toVector3i(javaPosition);
            int blockId = net.minecraft.world.level.block.Block.getId(javaState);
            PalettedContainer<net.minecraft.world.level.block.state.BlockState> palette = null;
            if (session.getChunkCache().isCache()) {
                palette = session.getChunkCache().getChunkSection(position.getX() >> 4, position.getY() >> 4, position.getZ() >> 4, true);
                if (palette == null) {
                    return;
                }
            }
            updateSubChunkBlocksPacket.setPosition(Vector3i.from(position.getX() & ~15, position.getY() & ~15, position.getZ() & ~15));
            session.getWorldCache().removePrediction(position);

            // Hack to avoid looking up blockstates for the currently broken position each tick
            if (clientBreakPos != null && Objects.equals(clientBreakPos, position)) {
                session.getBlockBreakHandler().setUpdatedServerBlockStateId(blockId);
            }

            int oldBlock;
            if (palette != null) {
                oldBlock = net.minecraft.world.level.block.Block.getId(palette.get(position.getX() & 0xF, position.getY() & 0xF, position.getZ() & 0xF));
                if (blockId == oldBlock) {
                    // Skip unchanged blocks which may occur with older versions of Minecraft
                    return;
                }

                palette.set(position.getX() & 0xF, position.getY() & 0xF, position.getZ() & 0xF, javaState);
            } else {
                oldBlock = -1;
            }

            BlockState blockState = BlockState.of(blockId);
            if (blockState.is(Blocks.AIR)) {
                ItemFrameEntity itemFrameEntity = ItemFrameEntity.getItemFrameEntity(session, position);
                if (itemFrameEntity != null) { // Item frame is still present and no block overrides that; refresh it
                    itemFrameEntity.updateBlock(true);
                    return;
                }
            }

            // Some block may have special handling, keep it that way
            if (!(blockState.block().getClass().equals(Block.class))) {
                blockState.block().updateBlock(session, blockState, position);
                return;
            }

            // Skull is gone
            session.getSkullCache().removeSkull(position);

            updateSubChunkBlocksPacket.getStandardBlocks().add(new org.cloudburstmc.protocol.bedrock.data.BlockChangeEntry(
                position,
                session.getBlockMappings().getBedrockBlock(blockState),
                FLAG_ALL,
                -1,
                MessageType.NONE
            ));

            boolean isWaterlogged = waterlogged.get(blockId);
            if (palette == null || waterlogged.get(oldBlock) != isWaterlogged) {
                updateSubChunkBlocksPacket.getExtraBlocks().add(new org.cloudburstmc.protocol.bedrock.data.BlockChangeEntry(
                    position,
                    isWaterlogged ? session.getBlockMappings().getBedrockWater() : session.getBlockMappings().getBedrockAir(),
                    0,
                    -1,
                    MessageType.NONE
                ));
            }
        });

        if (!updateSubChunkBlocksPacket.getStandardBlocks().isEmpty()) {
            session.sendUpstreamPacket(updateSubChunkBlocksPacket);
        }
    }
}

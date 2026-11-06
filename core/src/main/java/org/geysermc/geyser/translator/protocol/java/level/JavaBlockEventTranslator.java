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

import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.BlockEventPacket;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.PistonCache;
import org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator;
import org.geysermc.geyser.translator.level.block.entity.DecoratedPotBlockEntityTranslator;
import org.geysermc.geyser.translator.level.block.entity.PistonBlockEntity;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.geyser.level.physics.PistonAction;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;

import java.util.List;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.EndGatewayBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import java.util.Map;

@Translator(packet = ClientboundBlockEventPacket.class)
public class JavaBlockEventTranslator extends PacketTranslator<ClientboundBlockEventPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundBlockEventPacket packet) {
        Vector3i position = org.geysermc.geyser.util.MathUtils.toVector3i(packet.getPos());
        var block = packet.getBlock();

        BlockEventPacket blockEventPacket = new BlockEventPacket();
        blockEventPacket.setBlockPosition(position);

        if (block instanceof ChestBlock || block instanceof EnderChestBlock || block instanceof ShulkerBoxBlock) {
            blockEventPacket.setEventType(1);
            blockEventPacket.setEventData(packet.getB1() > 0 ? 1 : 0);
            session.sendUpstreamPacket(blockEventPacket);
        } else if (block instanceof EndGatewayBlock) {
            blockEventPacket.setEventType(1);
            session.sendUpstreamPacket(blockEventPacket);
        } else if (block instanceof NoteBlock) {
            session.getGeyser().getWorldManager().getBlockAtAsync(session, position).thenAccept(blockState -> {
                blockEventPacket.setEventData(BlockState.of(blockState).getValue(Properties.NOTE));
                session.sendUpstreamPacket(blockEventPacket);
            });
        } else if (block instanceof PistonBaseBlock) {
            PistonAction action = switch (packet.getB0()) {
                case PistonBaseBlock.TRIGGER_EXTEND -> PistonAction.EXTEND;
                case PistonBaseBlock.TRIGGER_CONTRACT -> PistonAction.RETRACT;
                case PistonBaseBlock.TRIGGER_DROP -> PistonAction.DROP;
                default -> null;
            };
            if (action == null) {
                return;
            }
            Direction direction = Direction.fromJava(net.minecraft.core.Direction.from3DDataValue(packet.getB1() & 7));
            PistonCache pistonCache = session.getPistonCache();

            if (session.getGeyser().getWorldManager().hasOwnChunkCache() || session.getErosionHandler().isActive()) {
                // Mostly handled in the GeyserPistonEvents class (Spigot) / the PistonBlockBaseMixin (Mod platforms)
                // However, the retracting event is not fully covered. (Spigot)
                // Mod platforms only handle pistons moving blocks; not the retracting of pistons.
                if (action == PistonAction.RETRACT || action == PistonAction.DROP) {
                    BlockState pistonBlock = session.getGeyser().getWorldManager().blockAt(session, position);

                    // Retracting sticky pistons is an exception, since the event is not called on Spigot from 1.13.2 - 1.17.1
                    // See https://github.com/PaperMC/Paper/blob/6fa1983e9ce177a4a412d5b950fd978620174777/patches/server/0304-Fire-BlockPistonRetractEvent-for-all-empty-pistons.patch
                    boolean isSticky = isSticky(pistonBlock);
                    if (session.getGeyser().platformType() == PlatformType.SPIGOT && !isSticky) {
                        return;
                    }

                    // Only sticky pistons that don't pull any blocks are affected
                    if (action != PistonAction.DROP && isSticky) {
                        Vector3i blockInFrontPos = position.add(direction.getUnitVector());
                        int blockInFront = session.getGeyser().getWorldManager().getBlockAt(session, blockInFrontPos);
                        if (blockInFront != Block.JAVA_AIR_ID) {
                            // Piston pulled something
                            return;
                        }
                    }
                    PistonBlockEntity blockEntity = pistonCache.getPistons().computeIfAbsent(position, pos -> new PistonBlockEntity(session, pos, direction, isSticky, true));
                    if (blockEntity.getAction() != action) {
                        blockEntity.setAction(action, Object2ObjectMaps.emptyMap());
                    }
                }
            } else {
                PistonBlockEntity blockEntity = pistonCache.getPistons().computeIfAbsent(position, pos -> {
                    BlockState state = session.getGeyser().getWorldManager().blockAt(session, position);
                    boolean sticky = isSticky(state);
                    boolean extended = action != PistonAction.EXTEND;
                    return new PistonBlockEntity(session, pos, direction, sticky, extended);
                });
                blockEntity.setAction(action);
            }
        } else if (block instanceof SpawnerBlock) {
            blockEventPacket.setEventType(1);
            session.sendUpstreamPacket(blockEventPacket);
        } else if (block instanceof BellBlock) {
            // Bells - needed to show ring from other players
            BlockEntityDataPacket blockEntityPacket = new BlockEntityDataPacket();
            blockEntityPacket.setBlockPosition(position);

            NbtMapBuilder builder = BlockEntityTranslator.getConstantBedrockTag("Bell", position.getX(), position.getY(), position.getZ());
            int bedrockRingDirection = switch (net.minecraft.core.Direction.from3DDataValue(packet.getB1())) {
                case SOUTH -> 0;
                case WEST -> 1;
                case NORTH -> 2;
                case EAST -> 3;
                default -> throw new IllegalStateException("Unexpected BellValue Direction: " + net.minecraft.core.Direction.from3DDataValue(packet.getB1()));
            };
            builder.putInt("Direction", bedrockRingDirection);
            builder.putByte("Ringing", (byte) 1);
            builder.putInt("Ticks", 0);
            
            blockEntityPacket.setData(builder.build());
            session.sendUpstreamPacket(blockEntityPacket);
        } else if (block instanceof DecoratedPotBlock) {
            // Decorated pots - wobble wobble
            // We need to send the sherd data with the client, but we don't really care about latency here so we
            // can safely get this from the server
            session.getGeyser().getWorldManager().getDecoratedPotData(session, position, sherds -> {
                BlockEntityDataPacket blockEntityPacket = new BlockEntityDataPacket();
                blockEntityPacket.setBlockPosition(position);

                NbtMapBuilder builder = BlockEntityTranslator.getConstantBedrockTag("DecoratedPot", position);
                builder.putList("sherds", NbtType.STRING, javaSherdsToBedrock(sherds));
                builder.putByte("animation", (byte) (packet.getB1() == net.minecraft.world.level.block.entity.DecoratedPotBlockEntity.WobbleStyle.POSITIVE.ordinal() ? 2 : 1));

                blockEntityPacket.setData(builder.build());
                session.sendUpstreamPacket(blockEntityPacket);
            });
        } else if (session.getGeyser().getLogger().isDebug()) {
            session.getGeyser().getLogger().debug("Unhandled block event packet: " + packet);
        }
    }

    private static List<String> javaSherdsToBedrock(Map<String, String> sherds) {
        String back = javaSherdToBedrock(sherds.get("back"));
        String left = javaSherdToBedrock(sherds.get("left"));
        String right = javaSherdToBedrock(sherds.get("right"));
        String front = javaSherdToBedrock(sherds.get("front"));
        return List.of(back, left, right, front);
    }

    private static String javaSherdToBedrock(@Nullable String sherd) {
        if (sherd == null) {
            return DecoratedPotBlockEntityTranslator.DEFAULT_BEDROCK_ITEM;
        }
        return Registries.DECORATED_POT_ASSETS.getOrDefault(MinecraftKey.key(sherd), DecoratedPotBlockEntityTranslator.DEFAULT_PATTERN).toString();
    }

    private static boolean isSticky(BlockState state) {
        return state.is(Blocks.STICKY_PISTON) || (state.is(Blocks.MOVING_PISTON) && "sticky".equals(state.getValue(Properties.PISTON_TYPE)));
    }
}

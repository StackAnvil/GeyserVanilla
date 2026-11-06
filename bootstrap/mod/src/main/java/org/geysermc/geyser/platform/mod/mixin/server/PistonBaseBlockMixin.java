/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.platform.mod.mixin.server;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.geysermc.geyser.platform.mod.host.ModEventBridge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(PistonBaseBlock.class)
public class PistonBaseBlockMixin {

    @Shadow
    @Final
    private boolean isSticky;

    @ModifyExpressionValue(method = "moveBlocks",
        at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;")
    )
    private HashMap<BlockPos, BlockState> geyser$onMapCreate(HashMap<BlockPos, BlockState> original, @Share("pushBlocks") LocalRef<Map<BlockPos, BlockState>> localRef) {
        localRef.set(original);
        return original;
    }

    @Inject(method = "moveBlocks",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;getToDestroy()Ljava/util/List;")
    )
    private void geyser$onBlocksMove(Level level, BlockPos blockPos, Direction direction, boolean isExtending, CallbackInfoReturnable<Boolean> cir, @Share("pushBlocks") LocalRef<Map<BlockPos, BlockState>> localRef) {
        var events = ModEventBridge.events();
        if (events == null) {
            return;
        }
        var recipients = new ArrayList<UUID>();
        for (var entry : events.playerRenderDistances().entrySet()) {
            Player player = level.getPlayerByUUID(entry.getKey());
            if (player == null || !player.level().equals(level)) {
                continue;
            }
            int dX = Math.abs(blockPos.getX() - player.getBlockX()) >> 4;
            int dZ = Math.abs(blockPos.getZ() - player.getBlockZ()) >> 4;
            if (dX * dX + dZ * dZ <= entry.getValue() * entry.getValue()) {
                recipients.add(entry.getKey());
            }
        }
        if (recipients.isEmpty()) {
            return;
        }
        Map<BlockPos, BlockState> pushed = localRef.get();
        int[] blocks = new int[pushed.size() * 4];
        int index = 0;
        for (var entry : pushed.entrySet()) {
            blocks[index++] = entry.getKey().getX();
            blocks[index++] = entry.getKey().getY();
            blocks[index++] = entry.getKey().getZ();
            blocks[index++] = Block.getId(entry.getValue());
        }
        events.pistonMoved(recipients.toArray(UUID[]::new), blockPos.getX(), blockPos.getY(), blockPos.getZ(),
            direction.ordinal(), isSticky, isExtending, blocks);
    }
}

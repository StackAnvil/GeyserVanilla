/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.type;

import net.minecraft.network.syncher.SynchedEntityData.DataValue;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.InteractiveTag;
import net.minecraft.world.InteractionHand;

public class CushionEntity extends Entity {

    public CushionEntity(EntitySpawnContext context) {
        super(context);
        // The default colour is white, or 15 on bedrock, which Java doesn't send
        metadata.put(EntityDataTypes.VARIANT, 15);
    }

    public void setDyeColor(DataValue<net.minecraft.world.item.DyeColor> dyeColor) {
        int color = dyeColor.value().getId();
        // Every color is offset 15 in bedrock edition
        metadata.put(EntityDataTypes.VARIANT, Math.abs(color - 15));
    }

    @Override
    protected InteractiveTag testInteraction(InteractionHand hand) {
        if (session.isSneaking() || !passengers.isEmpty()) {
            return InteractiveTag.NONE;
        }
        return InteractiveTag.SIT;
    }

    @Override
    public InteractionResult interact(InteractionHand hand) {
        if (session.isSneaking() || !passengers.isEmpty()) {
            return InteractionResult.PASS;
        }
        return InteractionResult.CONSUME;
    }
}

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

package org.geysermc.geyser.entity.type.living;

import net.minecraft.network.syncher.SynchedEntityData.DataValue;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.session.cache.tags.ItemTag;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.InteractiveTag;
import net.minecraft.world.InteractionHand;

public class AllayEntity extends MobEntity {
    private boolean canDuplicate;

    public AllayEntity(EntitySpawnContext context) {
        super(context);
    }

    public void setDancing(DataValue<Boolean> entityMetadata) {
        setFlag(EntityFlag.DANCING, entityMetadata.value());
    }

    public void setCanDuplicate(DataValue<Boolean> entityMetadata) {
        this.canDuplicate = entityMetadata.value();
    }

    @NonNull
    @Override
    protected InteractiveTag testMobInteraction(@NonNull InteractionHand hand, @NonNull GeyserItemStack itemInHand) {
        if (this.canDuplicate && getFlag(EntityFlag.DANCING) && isDuplicationItem(itemInHand)) {
            // Maybe better as another tag?
            return InteractiveTag.GIVE_ITEM_TO_ALLAY;
        } else if (getMainHandItem().isEmpty() && !itemInHand.isEmpty()) {
            return InteractiveTag.GIVE_ITEM_TO_ALLAY;
        } else if (!getMainHandItem().isEmpty() && hand == InteractionHand.MAIN_HAND && itemInHand.isEmpty()) {
            // Seems like there isn't a good tag for this yet
            return InteractiveTag.GIVE_ITEM_TO_ALLAY;
        } else {
            return super.testMobInteraction(hand, itemInHand);
        }
    }

    @NonNull
    @Override
    protected InteractionResult mobInteract(@NonNull InteractionHand hand, @NonNull GeyserItemStack itemInHand) {
        if (this.canDuplicate && getFlag(EntityFlag.DANCING) && isDuplicationItem(itemInHand)) {
            //TOCHECK sound
            return InteractionResult.SUCCESS;
        } else if (getMainHandItem().isEmpty() && !itemInHand.isEmpty()) {
            //TODO play sound?
            return InteractionResult.SUCCESS;
        } else if (!getMainHandItem().isEmpty() && hand == InteractionHand.MAIN_HAND && itemInHand.isEmpty()) {
            //TOCHECK also play sound here?
            return InteractionResult.SUCCESS;
        } else {
            return super.mobInteract(hand, itemInHand);
        }
    }

    private boolean isDuplicationItem(GeyserItemStack itemStack) {
        return itemStack.is(session, ItemTag.DUPLICATES_ALLAYS);
    }
}

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

package org.geysermc.geyser.entity.type.living.monster;

import net.minecraft.network.syncher.SynchedEntityData.DataValue;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.living.merchant.VillagerEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.InteractiveTag;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.InteractionHand;

public class ZombieVillagerEntity extends ZombieEntity {

    public ZombieVillagerEntity(EntitySpawnContext context) {
        super(context);
    }

    public void setTransforming(DataValue<Boolean> entityMetadata) {
        setFlag(EntityFlag.IS_TRANSFORMING, entityMetadata.value());
        setFlag(EntityFlag.SHAKING, isShaking());
    }

    public void setZombieVillagerData(DataValue<VillagerData> entityMetadata) {
        VillagerData villagerData = entityMetadata.value();
        metadata.put(EntityDataTypes.VARIANT, VillagerEntity.getBedrockProfession(net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION.getId(villagerData.profession().value()))); // Actually works properly with the OptionalPack
        metadata.put(EntityDataTypes.MARK_VARIANT, VillagerEntity.getBedrockRegion(net.minecraft.core.registries.BuiltInRegistries.VILLAGER_TYPE.getId(villagerData.type().value())));
        // Used with the OptionalPack
        metadata.put(EntityDataTypes.TRADE_TIER, villagerData.level() - 1);
    }

    @Override
    protected boolean isShaking() {
        return getFlag(EntityFlag.IS_TRANSFORMING) || super.isShaking();
    }

    @NonNull
    @Override
    protected InteractiveTag testMobInteraction(@NonNull InteractionHand hand, @NonNull GeyserItemStack itemInHand) {
        if (itemInHand.is(Items.GOLDEN_APPLE)) {
            return InteractiveTag.CURE;
        } else {
            return super.testMobInteraction(hand, itemInHand);
        }
    }

    @NonNull
    @Override
    protected InteractionResult mobInteract(@NonNull InteractionHand hand, @NonNull GeyserItemStack itemInHand) {
        if (itemInHand.is(Items.GOLDEN_APPLE)) {
            // The client doesn't know if the entity has weakness as that's not usually sent over the network
            return InteractionResult.CONSUME;
        } else {
            return super.mobInteract(hand, itemInHand);
        }
    }
}

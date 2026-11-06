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

package org.geysermc.geyser.entity.type;

import net.minecraft.network.syncher.SynchedEntityData.DataValue;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.inventory.item.Potion;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import org.geysermc.geyser.vanilla.protocol.GeyserDataComponents;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.EnumSet;

public class ThrownPotionEntity extends ThrowableItemEntity {
    private static final EnumSet<Potion> NON_ENCHANTED_POTIONS = EnumSet.of(Potion.WATER, Potion.MUNDANE, Potion.THICK, Potion.AWKWARD);

    public ThrownPotionEntity(EntitySpawnContext context) {
        super(context);
    }

    @Override
    public void setItem(DataValue<ItemStack> entityMetadata) {
        ItemStack itemStack = entityMetadata.value();
        if (itemStack == null) {
            metadata.put(EntityDataTypes.AUX_VALUE_DATA, (short) 0);
            setFlag(EntityFlag.ENCHANTED, false);
            setFlag(EntityFlag.LINGERING, false);
        } else {
            // As of Java 1.19.3, the server/client doesn't seem to care of the item is actually a potion?
            GeyserDataComponents components = org.geysermc.geyser.vanilla.protocol.GeyserDataComponents.from(itemStack.getComponentsPatch());
            if (components != null) {
                PotionContents potionContents = components.get(DataComponents.POTION_CONTENTS);
                if (potionContents != null) {
                    Potion potion = Potion.fromComponent(potionContents);
                    if (potion != null) {
                        metadata.put(EntityDataTypes.AUX_VALUE_DATA, potion.getBedrockId());
                        setFlag(EntityFlag.ENCHANTED, !NON_ENCHANTED_POTIONS.contains(potion));
                    } else {
                        metadata.put(EntityDataTypes.AUX_VALUE_DATA, (short) 0);
                        GeyserImpl.getInstance().getLogger().debug("Unknown java potion: " + potionContents.potion());
                    }
                }

                boolean isLingering = javaDefinition.type().is(EntityTypes.LINGERING_POTION);
                setFlag(EntityFlag.LINGERING, isLingering);
            }
        }
    }
}

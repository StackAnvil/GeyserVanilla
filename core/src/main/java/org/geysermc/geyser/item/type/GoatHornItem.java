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

package org.geysermc.geyser.item.type;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.item.GeyserInstrument;
import org.geysermc.geyser.item.TooltipOptions;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.BedrockItemBuilder;
import org.geysermc.geyser.translator.text.MessageTranslator;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import org.geysermc.geyser.vanilla.protocol.GeyserDataComponents;
import net.minecraft.world.item.Instrument;

public class GoatHornItem extends Item {
    public GoatHornItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public ItemData.Builder translateToBedrock(GeyserSession session, int count, GeyserDataComponents components, ItemMapping mapping, ItemMappings mappings) {
        ItemData.Builder builder = super.translateToBedrock(session, count, components, mapping, mappings);
        if (components == null) {
            return builder;
        }

        net.minecraft.world.item.component.InstrumentComponent instrumentComponent = components.get(DataComponents.INSTRUMENT);
        if (instrumentComponent != null) {
            GeyserInstrument instrument = GeyserInstrument.fromComponent(session, instrumentComponent);
            int bedrockId = instrument.bedrockId();
            if (bedrockId >= 0) {
                builder.damage(bedrockId);
            }
        }

        return builder;
    }

    @Override
    public void translateComponentsToBedrock(@NonNull GeyserSession session, @NonNull GeyserDataComponents components, @NonNull TooltipOptions tooltip, @NonNull BedrockItemBuilder builder) {
        super.translateComponentsToBedrock(session, components, tooltip, builder);

        net.minecraft.world.item.component.InstrumentComponent component = components.get(DataComponents.INSTRUMENT);
        if (component != null && tooltip.showInTooltip(DataComponents.INSTRUMENT)) {
            GeyserInstrument instrument = GeyserInstrument.fromComponent(session, component);
            if (instrument.bedrockInstrument() == null) {
                builder.getOrCreateLore().add(MessageTranslator.convertMessageForTooltip(instrument.description(), session.locale()));
            }
        }
    }

    @Override
    public @NonNull GeyserItemStack translateToJava(GeyserSession session, @NonNull ItemData itemData, @NonNull ItemMapping mapping, @NonNull ItemMappings mappings) {
        GeyserItemStack itemStack = super.translateToJava(session, itemData, mapping, mappings);

        int damage = itemData.getDamage();
        // This could cause an issue since -1 is returned for non-vanilla goat horns
        int javaId = GeyserInstrument.bedrockIdToJava(session.javaRegistries(), damage);
        session.vanillaRegistries().lookupOrThrow(net.minecraft.core.registries.Registries.INSTRUMENT).get(javaId)
            .ifPresent(instrument -> itemStack.getOrCreateComponents().put(DataComponents.INSTRUMENT, new net.minecraft.world.item.component.InstrumentComponent(instrument)));

        return itemStack;
    }

    @Override
    public boolean ignoreDamage() {
        return true;
    }
}

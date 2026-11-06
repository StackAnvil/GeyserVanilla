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

package org.geysermc.geyser.translator.protocol.java.entity;

import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.skin.FakeHeadProvider;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;

@Translator(packet = ClientboundSetEquipmentPacket.class)
public class JavaSetEquipmentTranslator extends PacketTranslator<ClientboundSetEquipmentPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundSetEquipmentPacket packet) {
        Entity entity = session.getEntityCache().getEntityByJavaId(packet.getEntity());
        if (entity == null)
            return;

        if (!(entity instanceof LivingEntity livingEntity)) {
            session.getGeyser().getLogger().debug("Attempted to add armor to a non-living entity (" +
                    entity.definition().identifier() + ").");
            return;
        }

        boolean armorUpdated = false;
        boolean mainHandUpdated = false;
        boolean offHandUpdated = false;
        for (var equipment : packet.getSlots()) {
            GeyserItemStack stack = GeyserItemStack.from(session, equipment.getSecond());
            switch (equipment.getFirst()) {
                case HEAD -> {
                    ResolvableProfile profile = stack.getComponent(DataComponents.PROFILE);
                    if (livingEntity instanceof PlayerEntity && stack.is(Items.PLAYER_HEAD) && profile != null) {
                        FakeHeadProvider.setHead(session, (PlayerEntity) livingEntity, profile);
                    } else {
                        FakeHeadProvider.restoreOriginalSkin(session, livingEntity);
                    }

                    livingEntity.setHelmet(stack);
                    armorUpdated = true;
                }
                case CHEST -> {
                    livingEntity.setChestplate(stack);
                    armorUpdated = true;
                }
                case BODY -> {
                    // BODY is sent for llamas with a carpet equipped, as of 1.20.5
                    // and for wolves
                    livingEntity.setBody(stack);
                    armorUpdated = true;
                }
                case LEGS -> {
                    livingEntity.setLeggings(stack);
                    armorUpdated = true;
                }
                case FEET -> {
                    livingEntity.setBoots(stack);
                    armorUpdated = true;
                }
                case SADDLE -> {
                    livingEntity.setSaddle(stack);
                }
                case MAINHAND -> {
                    livingEntity.setHand(stack);
                    mainHandUpdated = true;
                }
                case OFFHAND -> {
                    livingEntity.setOffhand(stack);
                    offHandUpdated = true;
                }
            }
        }

        if (armorUpdated) {
            livingEntity.updateArmor();
        }
        if (mainHandUpdated) {
            livingEntity.updateMainHand();
        }
        if (offHandUpdated) {
            livingEntity.updateOffHand();
        }
    }
}

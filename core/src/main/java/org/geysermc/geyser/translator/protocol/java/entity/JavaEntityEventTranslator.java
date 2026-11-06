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

import org.cloudburstmc.protocol.bedrock.data.ParticleType;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventoryContentPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.VanillaEntities;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.EvokerFangsEntity;
import org.geysermc.geyser.entity.type.FishingHookEntity;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.entity.type.ThrowableEggEntity;
import org.geysermc.geyser.entity.type.living.animal.ArmadilloEntity;
import org.geysermc.geyser.entity.type.living.monster.CreakingEntity;
import org.geysermc.geyser.entity.type.living.monster.WardenEntity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.InventoryUtils;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;

import java.util.Collections;

import static net.minecraft.world.entity.EntityEvent.*;
import java.util.concurrent.ThreadLocalRandom;

@Translator(packet = ClientboundEntityEventPacket.class)
public class JavaEntityEventTranslator extends PacketTranslator<ClientboundEntityEventPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundEntityEventPacket packet) {
        Entity entity = session.getEntityCache().getEntityByJavaId(org.geysermc.geyser.vanilla.protocol.VanillaPacketAccess.entityId(packet));
        if (entity == null)
            return;

        EntityEventPacket entityEventPacket = new EntityEventPacket();
        entityEventPacket.setRuntimeEntityId(entity.geyserId());
        switch (packet.getEventId()) {
            case REDUCED_DEBUG_INFO:
                session.setReducedDebugInfo(true);
                return;
            case FULL_DEBUG_INFO:
                session.setReducedDebugInfo(false);
                return;
            case PERMISSION_LEVEL_ALL:
                session.setOpPermissionLevel(0);
                session.sendAdventureSettings();
                return;
            case PERMISSION_LEVEL_MODERATORS:
                session.setOpPermissionLevel(1);
                session.sendAdventureSettings();
                return;
            case PERMISSION_LEVEL_GAMEMASTERS:
                session.setOpPermissionLevel(2);
                session.sendAdventureSettings();
                return;
            case PERMISSION_LEVEL_ADMINS:
                session.setOpPermissionLevel(3);
                session.sendAdventureSettings();
                return;
            case PERMISSION_LEVEL_OWNERS:
                session.setOpPermissionLevel(4);
                session.sendAdventureSettings();
                return;

            case DEATH:
                entityEventPacket.setType(EntityEventType.DEATH);
                if (entity instanceof ThrowableEggEntity egg) {
                    LevelEventPacket particlePacket = new LevelEventPacket();
                    particlePacket.setType(ParticleType.ICON_CRACK);
                    particlePacket.setData(ItemTranslator.getBedrockItemDefinition(session, egg.getItemStack()).getRuntimeId() << 16);
                    particlePacket.setPosition(entity.bedrockPosition());
                    for (int i = 0; i < 6; i++) {
                        session.sendUpstreamPacket(particlePacket);
                    }
                } else if (entity.getJavaDefinition() == VanillaEntities.SNOWBALL) {
                    LevelEventPacket particlePacket = new LevelEventPacket();
                    particlePacket.setType(ParticleType.SNOWBALL_POOF);
                    particlePacket.setPosition(entity.bedrockPosition());
                    for (int i = 0; i < 8; i++) {
                        session.sendUpstreamPacket(particlePacket);
                    }
                }
                break;
            case SHAKE_WETNESS:
                entityEventPacket.setType(EntityEventType.SHAKE_WETNESS);
                break;
            case CANCEL_SHAKE_WETNESS:
                entityEventPacket.setType(EntityEventType.SHAKE_WETNESS_STOP);
                break;
            case USE_ITEM_COMPLETE:
                if (entity instanceof SessionPlayerEntity) {
                    entity.setFlag(EntityFlag.USING_ITEM, false);
                }

                entityEventPacket.setType(EntityEventType.USE_ITEM);
                break;
            case FISHING_ROD_REEL_IN:
                // Player is pulled from a fishing rod
                // The physics of this are clientside on Java
                FishingHookEntity fishingHook = (FishingHookEntity) entity;
                if (fishingHook.getBedrockTargetId() == session.getPlayerEntity().geyserId()) {
                    Entity hookOwner = session.getEntityCache().getEntityByGeyserId(fishingHook.getBedrockOwnerId());
                    if (hookOwner != null) {
                        // https://minecraft.wiki/w/Fishing_Rod#Hooking_mobs_and_other_entities
                        SetEntityMotionPacket motionPacket = new SetEntityMotionPacket();
                        motionPacket.setRuntimeEntityId(session.getPlayerEntity().geyserId());
                        motionPacket.setMotion(hookOwner.position().sub(session.getPlayerEntity().position()).mul(0.1f));
                        session.sendUpstreamPacket(motionPacket);
                    }
                }
                return;
            case TAMING_FAILED:
                entityEventPacket.setType(EntityEventType.TAME_FAILED);
                break;
            case TAMING_SUCCEEDED:
                entityEventPacket.setType(EntityEventType.TAME_SUCCEEDED);
                break;
            case ZOMBIE_CONVERTING: // Played when a zombie bites the golden apple
                LevelSoundEventPacket soundPacket = new LevelSoundEventPacket();
                soundPacket.setSound(SoundEvent.REMEDY);
                soundPacket.setPosition(entity.bedrockPosition());
                soundPacket.setExtraData(-1);
                soundPacket.setIdentifier("");
                soundPacket.setRelativeVolumeDisabled(false);
                session.sendUpstreamPacket(soundPacket);
                return;
            case LOVE_HEARTS:
            case IN_LOVE_HEARTS:
                entityEventPacket.setType(EntityEventType.LOVE_PARTICLES);
                break;
            case FIREWORKS_EXPLODE:
                entityEventPacket.setType(EntityEventType.FIREWORK_EXPLODE);
                break;
            case WITCH_HAT_MAGIC:
                entityEventPacket.setType(EntityEventType.WITCH_HAT_MAGIC); //TODO: CHECK
                break;
            case PROTECTED_FROM_DEATH:
                // Bedrock will not play the spinning animation without the item in the hand o.o
                // Fixes https://github.com/GeyserMC/Geyser/issues/2446
                boolean totemItemWorkaround = !session.getPlayerInventory().isHolding(Items.TOTEM_OF_UNDYING);
                if (totemItemWorkaround) {
                    InventoryContentPacket offhandPacket = new InventoryContentPacket();
                    offhandPacket.setContainerId(ContainerId.OFFHAND);
                    offhandPacket.setContents(Collections.singletonList(InventoryUtils.getTotemOfUndying().apply(session.getUpstream().getProtocolVersion())));
                    session.sendUpstreamPacket(offhandPacket);
                }

                entityEventPacket.setType(EntityEventType.CONSUME_TOTEM);

                PlaySoundPacket playSoundPacket = new PlaySoundPacket();
                playSoundPacket.setSound("random.totem");
                playSoundPacket.setPosition(entity.bedrockPosition());
                playSoundPacket.setVolume(1.0F);
                playSoundPacket.setPitch(1.0F + (ThreadLocalRandom.current().nextFloat() * 0.1F) - 0.05F);
                session.sendUpstreamPacket(playSoundPacket);

                // Sent here early to ensure we have the totem in our hand
                session.sendUpstreamPacket(entityEventPacket);

                if (totemItemWorkaround) {
                    // Reset the item again
                    session.getPlayerInventoryHolder().updateSlot(45);
                }

                return;
            case EAT_GRASS:
                if (entity.getJavaDefinition() == VanillaEntities.SHEEP) {
                    entityEventPacket.setType(EntityEventType.EAT_GRASS);
                } else {
                    entityEventPacket.setType(EntityEventType.PRIME_TNT_MINECART);
                }
                break;
            case OFFER_FLOWER:
                entityEventPacket.setType(EntityEventType.GOLEM_FLOWER_OFFER);
                break;
            case VILLAGER_ANGRY:
                entityEventPacket.setType(EntityEventType.VILLAGER_ANGRY);
                break;
            case VILLAGER_HAPPY:
                entityEventPacket.setType(EntityEventType.VILLAGER_HAPPY);
                break;
            case VILLAGER_SWEAT:
                LevelEventPacket levelEventPacket = new LevelEventPacket();
                levelEventPacket.setType(ParticleType.WATER_SPLASH);
                levelEventPacket.setPosition(entity.position().up(entity.getBoundingBoxHeight()));
                session.sendUpstreamPacket(levelEventPacket);
                return;
            case STOP_OFFER_FLOWER:
                entityEventPacket.setType(EntityEventType.GOLEM_FLOWER_WITHDRAW);
                break;
            case START_ATTACKING:
                if (entity.getJavaDefinition().is(EntityTypes.IRON_GOLEM) || entity.getJavaDefinition().is(EntityTypes.EVOKER_FANGS)
                        || entity.getJavaDefinition().is(EntityTypes.WARDEN)) {
                    entityEventPacket.setType(EntityEventType.ATTACK_START);
                    if (entity.getJavaDefinition().is(EntityTypes.EVOKER_FANGS)) {
                        ((EvokerFangsEntity) entity).setAttackStarted();
                    }
                }
                break;
            case JUMP:
                if (entity.getJavaDefinition() == VanillaEntities.RABBIT) {
                    // This doesn't match vanilla Bedrock behavior but I'm unsure how to make it better
                    // I assume part of the problem is that Bedrock uses a duration and Java just says the rabbit is jumping
                    SetEntityDataPacket dataPacket = new SetEntityDataPacket();
                    dataPacket.getMetadata().put(EntityDataTypes.JUMP_DURATION, (byte) 3);
                    dataPacket.setRuntimeEntityId(entity.geyserId());
                    session.sendUpstreamPacket(dataPacket);
                    return;
                }
                break;
            case HEAD_BREAK:
            case CHEST_BREAK:
            case LEGS_BREAK:
            case FEET_BREAK:
            case MAINHAND_BREAK:
            case OFFHAND_BREAK:
            case SADDLE_BREAK:
            case BODY_BREAK:
                LevelSoundEventPacket equipmentBreakPacket = new LevelSoundEventPacket();
                equipmentBreakPacket.setSound(SoundEvent.BREAK);
                equipmentBreakPacket.setPosition(entity.bedrockPosition());
                equipmentBreakPacket.setExtraData(-1);
                equipmentBreakPacket.setIdentifier("");
                session.sendUpstreamPacket(equipmentBreakPacket);
                return;
            case SWAP_HANDS: // Not just used for players
                if (entity instanceof LivingEntity livingEntity) {
                    livingEntity.switchHands();

                    livingEntity.updateMainHand();
                    livingEntity.updateOffHand();
                } else {
                    session.getGeyser().getLogger().debug("Got status message to swap hands for a non-living entity.");
                }
                return;
            case START_RAM:
                if (entity.getJavaDefinition() == VanillaEntities.GOAT) {
                    entityEventPacket.setType(EntityEventType.ATTACK_START);
                }
                break;
            case END_RAM:
                if (entity.getJavaDefinition() == VanillaEntities.GOAT) {
                    entityEventPacket.setType(EntityEventType.ATTACK_STOP);
                }
                break;
            case POOF:
                if (entity instanceof LivingEntity) {
                    // Note that this event usually makes noise, but because we set all entities as silent on the
                    // client end this isn't an issue.
                    entityEventPacket.setType(EntityEventType.DEATH_SMOKE_CLOUD);
                }
                break;
            case TENDRILS_SHIVER:
                if (entity.getJavaDefinition() == VanillaEntities.WARDEN) {
                    entityEventPacket.setType(EntityEventType.VIBRATION_DETECTED);
                }
                break;
            case SONIC_CHARGE:
                if (entity instanceof WardenEntity wardenEntity) {
                    wardenEntity.onSonicBoom();
                }
                break;
            case ARMADILLO_PEEK:
                if (entity instanceof ArmadilloEntity armadilloEntity) {
                    armadilloEntity.onPeeking();
                }
                break;
            case SHAKE:
                if (entity instanceof CreakingEntity creakingEntity) {
                    creakingEntity.createParticleBeam();
                }
                break;
            case SQUID_ANIM_SYNCH:
                // unused, but spams a bit
                break;
            default:
                GeyserImpl.getInstance().getLogger().debug("unhandled entity event: " + packet);
        }

        if (entityEventPacket.getType() != null) {
            session.sendUpstreamPacket(entityEventPacket);
        }
    }
}

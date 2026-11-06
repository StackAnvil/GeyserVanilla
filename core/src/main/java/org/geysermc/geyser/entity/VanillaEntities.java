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

package org.geysermc.geyser.entity;

import net.minecraft.network.syncher.SynchedEntityData.DataValue;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.factory.EntityFactory;
import org.geysermc.geyser.entity.type.AbstractArrowEntity;
import org.geysermc.geyser.entity.type.AbstractWindChargeEntity;
import org.geysermc.geyser.entity.type.AreaEffectCloudEntity;
import org.geysermc.geyser.entity.type.ArrowEntity;
import org.geysermc.geyser.entity.type.BoatEntity;
import org.geysermc.geyser.entity.type.ChestBoatEntity;
import org.geysermc.geyser.entity.type.CommandBlockMinecartEntity;
import org.geysermc.geyser.entity.type.CushionEntity;
import org.geysermc.geyser.entity.type.DisplayBaseEntity;
import org.geysermc.geyser.entity.type.EnderCrystalEntity;
import org.geysermc.geyser.entity.type.EnderEyeEntity;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.EvokerFangsEntity;
import org.geysermc.geyser.entity.type.ExpOrbEntity;
import org.geysermc.geyser.entity.type.FallingBlockEntity;
import org.geysermc.geyser.entity.type.FireballEntity;
import org.geysermc.geyser.entity.type.FireworkEntity;
import org.geysermc.geyser.entity.type.FishingHookEntity;
import org.geysermc.geyser.entity.type.FurnaceMinecartEntity;
import org.geysermc.geyser.entity.type.HangingEntity;
import org.geysermc.geyser.entity.type.InteractionEntity;
import org.geysermc.geyser.entity.type.ItemEntity;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.entity.type.LeashKnotEntity;
import org.geysermc.geyser.entity.type.LightningEntity;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.entity.type.MinecartEntity;
import org.geysermc.geyser.entity.type.OminousItemSpawnerEntity;
import org.geysermc.geyser.entity.type.PaintingEntity;
import org.geysermc.geyser.entity.type.ProjectileEntity;
import org.geysermc.geyser.entity.type.SpawnerMinecartEntity;
import org.geysermc.geyser.entity.type.TNTEntity;
import org.geysermc.geyser.entity.type.TextDisplayEntity;
import org.geysermc.geyser.entity.type.ThrowableEggEntity;
import org.geysermc.geyser.entity.type.ThrowableItemEntity;
import org.geysermc.geyser.entity.type.ThrownPotionEntity;
import org.geysermc.geyser.entity.type.TridentEntity;
import org.geysermc.geyser.entity.type.WitherSkullEntity;
import org.geysermc.geyser.entity.type.living.AbstractFishEntity;
import org.geysermc.geyser.entity.type.living.AgeableEntity;
import org.geysermc.geyser.entity.type.living.AllayEntity;
import org.geysermc.geyser.entity.type.living.ArmorStandEntity;
import org.geysermc.geyser.entity.type.living.BatEntity;
import org.geysermc.geyser.entity.type.living.CopperGolemEntity;
import org.geysermc.geyser.entity.type.living.DolphinEntity;
import org.geysermc.geyser.entity.type.living.GlowSquidEntity;
import org.geysermc.geyser.entity.type.living.IronGolemEntity;
import org.geysermc.geyser.entity.type.living.MobEntity;
import org.geysermc.geyser.entity.type.living.SnowGolemEntity;
import org.geysermc.geyser.entity.type.living.SquidEntity;
import org.geysermc.geyser.entity.type.living.TadpoleEntity;
import org.geysermc.geyser.entity.type.living.animal.ArmadilloEntity;
import org.geysermc.geyser.entity.type.living.animal.AxolotlEntity;
import org.geysermc.geyser.entity.type.living.animal.BeeEntity;
import org.geysermc.geyser.entity.type.living.animal.FoxEntity;
import org.geysermc.geyser.entity.type.living.animal.FrogEntity;
import org.geysermc.geyser.entity.type.living.animal.GoatEntity;
import org.geysermc.geyser.entity.type.living.animal.HappyGhastEntity;
import org.geysermc.geyser.entity.type.living.animal.HoglinEntity;
import org.geysermc.geyser.entity.type.living.animal.MooshroomEntity;
import org.geysermc.geyser.entity.type.living.animal.OcelotEntity;
import org.geysermc.geyser.entity.type.living.animal.PandaEntity;
import org.geysermc.geyser.entity.type.living.animal.PolarBearEntity;
import org.geysermc.geyser.entity.type.living.animal.PufferFishEntity;
import org.geysermc.geyser.entity.type.living.animal.RabbitEntity;
import org.geysermc.geyser.entity.type.living.animal.SheepEntity;
import org.geysermc.geyser.entity.type.living.animal.SnifferEntity;
import org.geysermc.geyser.entity.type.living.animal.StriderEntity;
import org.geysermc.geyser.entity.type.living.animal.TemperatureVariantAnimal;
import org.geysermc.geyser.entity.type.living.animal.TropicalFishEntity;
import org.geysermc.geyser.entity.type.living.animal.TurtleEntity;
import org.geysermc.geyser.entity.type.living.animal.farm.ChickenEntity;
import org.geysermc.geyser.entity.type.living.animal.farm.CowEntity;
import org.geysermc.geyser.entity.type.living.animal.farm.PigEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.AbstractHorseEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.CamelEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.CamelHuskEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.ChestedHorseEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.HorseEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.LlamaEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.SkeletonHorseEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.TraderLlamaEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.ZombieHorseEntity;
import org.geysermc.geyser.entity.type.living.animal.nautilus.AbstractNautilusEntity;
import org.geysermc.geyser.entity.type.living.animal.nautilus.NautilusEntity;
import org.geysermc.geyser.entity.type.living.animal.nautilus.ZombieNautilusEntity;
import org.geysermc.geyser.entity.type.living.animal.tameable.CatEntity;
import org.geysermc.geyser.entity.type.living.animal.tameable.ParrotEntity;
import org.geysermc.geyser.entity.type.living.animal.tameable.TameableEntity;
import org.geysermc.geyser.entity.type.living.animal.tameable.WolfEntity;
import org.geysermc.geyser.entity.type.living.merchant.AbstractMerchantEntity;
import org.geysermc.geyser.entity.type.living.merchant.VillagerEntity;
import org.geysermc.geyser.entity.type.living.monster.AbstractSkeletonEntity;
import org.geysermc.geyser.entity.type.living.monster.BasePiglinEntity;
import org.geysermc.geyser.entity.type.living.monster.BlazeEntity;
import org.geysermc.geyser.entity.type.living.monster.BoggedEntity;
import org.geysermc.geyser.entity.type.living.monster.BreezeEntity;
import org.geysermc.geyser.entity.type.living.monster.CreakingEntity;
import org.geysermc.geyser.entity.type.living.monster.CreeperEntity;
import org.geysermc.geyser.entity.type.living.monster.ElderGuardianEntity;
import org.geysermc.geyser.entity.type.living.monster.EnderDragonEntity;
import org.geysermc.geyser.entity.type.living.monster.EnderDragonPartEntity;
import org.geysermc.geyser.entity.type.living.monster.EndermanEntity;
import org.geysermc.geyser.entity.type.living.monster.GhastEntity;
import org.geysermc.geyser.entity.type.living.monster.GiantEntity;
import org.geysermc.geyser.entity.type.living.monster.GuardianEntity;
import org.geysermc.geyser.entity.type.living.monster.MonsterEntity;
import org.geysermc.geyser.entity.type.living.monster.PhantomEntity;
import org.geysermc.geyser.entity.type.living.monster.PiglinEntity;
import org.geysermc.geyser.entity.type.living.monster.ShulkerEntity;
import org.geysermc.geyser.entity.type.living.monster.SkeletonEntity;
import org.geysermc.geyser.entity.type.living.monster.SpiderEntity;
import org.geysermc.geyser.entity.type.living.monster.VexEntity;
import org.geysermc.geyser.entity.type.living.monster.WardenEntity;
import org.geysermc.geyser.entity.type.living.monster.WitherEntity;
import org.geysermc.geyser.entity.type.living.monster.ZoglinEntity;
import org.geysermc.geyser.entity.type.living.monster.ZombieEntity;
import org.geysermc.geyser.entity.type.living.monster.ZombieVillagerEntity;
import org.geysermc.geyser.entity.type.living.monster.ZombifiedPiglinEntity;
import org.geysermc.geyser.entity.type.living.monster.cubemob.AbstractCubeEntity;
import org.geysermc.geyser.entity.type.living.monster.cubemob.MagmaCubeEntity;
import org.geysermc.geyser.entity.type.living.monster.cubemob.SlimeEntity;
import org.geysermc.geyser.entity.type.living.monster.cubemob.SulfurCubeEntity;
import org.geysermc.geyser.entity.type.living.monster.raid.PillagerEntity;
import org.geysermc.geyser.entity.type.living.monster.raid.RaidParticipantEntity;
import org.geysermc.geyser.entity.type.living.monster.raid.RavagerEntity;
import org.geysermc.geyser.entity.type.living.monster.raid.SpellcasterIllagerEntity;
import org.geysermc.geyser.entity.type.living.monster.raid.VindicatorEntity;
import org.geysermc.geyser.entity.type.player.AvatarEntity;
import org.geysermc.geyser.entity.type.player.MannequinEntity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.translator.text.MessageTranslator;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

public final class VanillaEntities {
    public static final VanillaEntityType<BoatEntity> ACACIA_BOAT;
    public static final VanillaEntityType<ChestBoatEntity> ACACIA_CHEST_BOAT;
    public static final VanillaEntityType<AllayEntity> ALLAY;
    public static final VanillaEntityType<AreaEffectCloudEntity> AREA_EFFECT_CLOUD;
    public static final VanillaEntityType<ArmadilloEntity> ARMADILLO;
    public static final VanillaEntityType<ArmorStandEntity> ARMOR_STAND;
    public static final VanillaEntityType<ArrowEntity> ARROW;
    public static final VanillaEntityType<AxolotlEntity> AXOLOTL;
    public static final VanillaEntityType<BoatEntity> BAMBOO_RAFT;
    public static final VanillaEntityType<ChestBoatEntity> BAMBOO_CHEST_RAFT;
    public static final VanillaEntityType<BatEntity> BAT;
    public static final VanillaEntityType<BeeEntity> BEE;
    public static final VanillaEntityType<BoatEntity> BIRCH_BOAT;
    public static final VanillaEntityType<ChestBoatEntity> BIRCH_CHEST_BOAT;
    public static final VanillaEntityType<BlazeEntity> BLAZE;
    public static final VanillaEntityType<BoggedEntity> BOGGED;
    public static final VanillaEntityType<BreezeEntity> BREEZE;
    public static final VanillaEntityType<AbstractWindChargeEntity> BREEZE_WIND_CHARGE;
    public static final VanillaEntityType<CamelEntity> CAMEL;
    public static final VanillaEntityType<CamelHuskEntity> CAMEL_HUSK;
    public static final VanillaEntityType<CatEntity> CAT;
    public static final VanillaEntityType<SpiderEntity> CAVE_SPIDER;
    public static final VanillaEntityType<BoatEntity> CHERRY_BOAT;
    public static final VanillaEntityType<ChestBoatEntity> CHERRY_CHEST_BOAT;
    public static final VanillaEntityType<MinecartEntity> CHEST_MINECART;
    public static final VanillaEntityType<ChickenEntity> CHICKEN;
    public static final VanillaEntityType<AbstractFishEntity> COD;
    public static final VanillaEntityType<CopperGolemEntity> COPPER_GOLEM;
    public static final VanillaEntityType<CommandBlockMinecartEntity> COMMAND_BLOCK_MINECART;
    public static final VanillaEntityType<CowEntity> COW;
    public static final VanillaEntityType<CreakingEntity> CREAKING;
    public static final VanillaEntityType<CreeperEntity> CREEPER;
    public static final VanillaEntityType<CushionEntity> CUSHION;
    public static final VanillaEntityType<BoatEntity> DARK_OAK_BOAT;
    public static final VanillaEntityType<ChestBoatEntity> DARK_OAK_CHEST_BOAT;
    public static final VanillaEntityType<DolphinEntity> DOLPHIN;
    public static final VanillaEntityType<ChestedHorseEntity> DONKEY;
    public static final VanillaEntityType<FireballEntity> DRAGON_FIREBALL;
    public static final VanillaEntityType<ZombieEntity> DROWNED;
    public static final VanillaEntityType<ThrowableEggEntity> EGG;
    public static final VanillaEntityType<ElderGuardianEntity> ELDER_GUARDIAN;
    public static final VanillaEntityType<EndermanEntity> ENDERMAN;
    public static final VanillaEntityType<MonsterEntity> ENDERMITE;
    public static final VanillaEntityType<EnderDragonEntity> ENDER_DRAGON;
    public static final VanillaEntityType<ThrowableItemEntity> ENDER_PEARL;
    public static final VanillaEntityType<EnderCrystalEntity> END_CRYSTAL;
    public static final VanillaEntityType<SpellcasterIllagerEntity> EVOKER;
    public static final VanillaEntityType<EvokerFangsEntity> EVOKER_FANGS;
    public static final VanillaEntityType<ThrowableItemEntity> EXPERIENCE_BOTTLE;
    public static final VanillaEntityType<ExpOrbEntity> EXPERIENCE_ORB;
    public static final VanillaEntityType<EnderEyeEntity> EYE_OF_ENDER;
    public static final VanillaEntityType<FallingBlockEntity> FALLING_BLOCK;
    public static final VanillaEntityType<FireballEntity> FIREBALL;
    public static final VanillaEntityType<FireworkEntity> FIREWORK_ROCKET;
    public static final VanillaEntityType<FishingHookEntity> FISHING_BOBBER;
    public static final VanillaEntityType<FoxEntity> FOX;
    public static final VanillaEntityType<FrogEntity> FROG;
    public static final VanillaEntityType<FurnaceMinecartEntity> FURNACE_MINECART; // Not present on Bedrock
    public static final VanillaEntityType<GhastEntity> GHAST;
    public static final VanillaEntityType<GiantEntity> GIANT;
    public static final VanillaEntityType<ItemFrameEntity> GLOW_ITEM_FRAME;
    public static final VanillaEntityType<GlowSquidEntity> GLOW_SQUID;
    public static final VanillaEntityType<GoatEntity> GOAT;
    public static final VanillaEntityType<GuardianEntity> GUARDIAN;
    public static final VanillaEntityType<HappyGhastEntity> HAPPY_GHAST;
    public static final VanillaEntityType<HoglinEntity> HOGLIN;
    public static final VanillaEntityType<MinecartEntity> HOPPER_MINECART;
    public static final VanillaEntityType<HorseEntity> HORSE;
    public static final VanillaEntityType<ZombieEntity> HUSK;
    public static final VanillaEntityType<SpellcasterIllagerEntity> ILLUSIONER; // Not present on Bedrock
    public static final VanillaEntityType<InteractionEntity> INTERACTION;
    public static final VanillaEntityType<IronGolemEntity> IRON_GOLEM;
    public static final VanillaEntityType<ItemEntity> ITEM;
    public static final VanillaEntityType<ItemFrameEntity> ITEM_FRAME;
    public static final VanillaEntityType<BoatEntity> JUNGLE_BOAT;
    public static final VanillaEntityType<ChestBoatEntity> JUNGLE_CHEST_BOAT;
    public static final VanillaEntityType<LeashKnotEntity> LEASH_KNOT;
    public static final VanillaEntityType<LightningEntity> LIGHTNING_BOLT;
    public static final VanillaEntityType<LlamaEntity> LLAMA;
    public static final VanillaEntityType<ProjectileEntity> LLAMA_SPIT;
    public static final VanillaEntityType<MagmaCubeEntity> MAGMA_CUBE;
    public static final VanillaEntityType<BoatEntity> MANGROVE_BOAT;
    public static final VanillaEntityType<ChestBoatEntity> MANGROVE_CHEST_BOAT;
    public static final VanillaEntityType<MannequinEntity> MANNEQUIN;
    /**
     * Is only ever sent over the network within (non-vanilla) entity type tags.
     */
    public static final VanillaEntityType<Entity> MARKER;
    public static final VanillaEntityType<MinecartEntity> MINECART;
    public static final VanillaEntityType<MooshroomEntity> MOOSHROOM;
    public static final VanillaEntityType<ChestedHorseEntity> MULE;
    public static final VanillaEntityType<NautilusEntity> NAUTILUS;
    public static final VanillaEntityType<BoatEntity> OAK_BOAT;
    public static final VanillaEntityType<ChestBoatEntity> OAK_CHEST_BOAT;
    public static final VanillaEntityType<OcelotEntity> OCELOT;
    public static final VanillaEntityType<OminousItemSpawnerEntity> OMINOUS_ITEM_SPAWNER;
    public static final VanillaEntityType<PaintingEntity> PAINTING;
    public static final VanillaEntityType<BoatEntity> PALE_OAK_BOAT;
    public static final VanillaEntityType<ChestBoatEntity> PALE_OAK_CHEST_BOAT;
    public static final VanillaEntityType<PandaEntity> PANDA;
    public static final VanillaEntityType<AbstractSkeletonEntity> PARCHED;
    public static final VanillaEntityType<ParrotEntity> PARROT;
    public static final VanillaEntityType<PhantomEntity> PHANTOM;
    public static final VanillaEntityType<PigEntity> PIG;
    public static final VanillaEntityType<PiglinEntity> PIGLIN;
    public static final VanillaEntityType<BasePiglinEntity> PIGLIN_BRUTE;
    public static final VanillaEntityType<PillagerEntity> PILLAGER;
    public static final VanillaEntityType<PlayerEntity> PLAYER;
    public static final VanillaEntityType<PolarBearEntity> POLAR_BEAR;
    public static final VanillaEntityType<BoatEntity> POPLAR_BOAT;
    public static final VanillaEntityType<ChestBoatEntity> POPLAR_CHEST_BOAT;
    public static final VanillaEntityType<ThrownPotionEntity> SPLASH_POTION;
    public static final VanillaEntityType<ThrownPotionEntity> LINGERING_POTION;
    public static final VanillaEntityType<PufferFishEntity> PUFFERFISH;
    public static final VanillaEntityType<RabbitEntity> RABBIT;
    public static final VanillaEntityType<RavagerEntity> RAVAGER;
    public static final VanillaEntityType<AbstractFishEntity> SALMON;
    public static final VanillaEntityType<SheepEntity> SHEEP;
    public static final VanillaEntityType<ShulkerEntity> SHULKER;
    public static final VanillaEntityType<SnifferEntity> SNIFFER;
    public static final VanillaEntityType<ProjectileEntity> SHULKER_BULLET;
    public static final VanillaEntityType<MonsterEntity> SILVERFISH;
    public static final VanillaEntityType<SkeletonEntity> SKELETON;
    public static final VanillaEntityType<SkeletonHorseEntity> SKELETON_HORSE;
    public static final VanillaEntityType<SlimeEntity> SLIME;
    public static final VanillaEntityType<FireballEntity> SMALL_FIREBALL;
    public static final VanillaEntityType<ThrowableItemEntity> SNOWBALL;
    public static final VanillaEntityType<SnowGolemEntity> SNOW_GOLEM;
    public static final VanillaEntityType<SpawnerMinecartEntity> SPAWNER_MINECART; // Not present on Bedrock
    public static final VanillaEntityType<AbstractArrowEntity> SPECTRAL_ARROW;
    public static final VanillaEntityType<SpiderEntity> SPIDER;
    public static final VanillaEntityType<BoatEntity> SPRUCE_BOAT;
    public static final VanillaEntityType<ChestBoatEntity> SPRUCE_CHEST_BOAT;
    public static final VanillaEntityType<SquidEntity> SQUID;
    public static final VanillaEntityType<AbstractSkeletonEntity> STRAY;
    public static final VanillaEntityType<StriderEntity> STRIDER;
    public static final VanillaEntityType<SulfurCubeEntity> SULFUR_CUBE;
    public static final VanillaEntityType<TadpoleEntity> TADPOLE;
    public static final VanillaEntityType<TextDisplayEntity> TEXT_DISPLAY;
    public static final VanillaEntityType<TNTEntity> TNT;
    public static final VanillaEntityType<MinecartEntity> TNT_MINECART;
    public static final VanillaEntityType<TraderLlamaEntity> TRADER_LLAMA;
    public static final VanillaEntityType<TridentEntity> TRIDENT;
    public static final VanillaEntityType<TropicalFishEntity> TROPICAL_FISH;
    public static final VanillaEntityType<TurtleEntity> TURTLE;
    public static final VanillaEntityType<VexEntity> VEX;
    public static final VanillaEntityType<VillagerEntity> VILLAGER;
    public static final VanillaEntityType<VindicatorEntity> VINDICATOR;
    public static final VanillaEntityType<AbstractMerchantEntity> WANDERING_TRADER;
    public static final VanillaEntityType<WardenEntity> WARDEN;
    public static final VanillaEntityType<AbstractWindChargeEntity> WIND_CHARGE;
    public static final VanillaEntityType<RaidParticipantEntity> WITCH;
    public static final VanillaEntityType<WitherEntity> WITHER;
    public static final VanillaEntityType<AbstractSkeletonEntity> WITHER_SKELETON;
    public static final VanillaEntityType<WitherSkullEntity> WITHER_SKULL;
    public static final VanillaEntityType<WolfEntity> WOLF;
    public static final VanillaEntityType<ZoglinEntity> ZOGLIN;
    public static final VanillaEntityType<ZombieEntity> ZOMBIE;
    public static final VanillaEntityType<ZombieHorseEntity> ZOMBIE_HORSE;
    public static final VanillaEntityType<ZombieNautilusEntity> ZOMBIE_NAUTILUS;
    public static final VanillaEntityType<ZombieVillagerEntity> ZOMBIE_VILLAGER;
    public static final VanillaEntityType<ZombifiedPiglinEntity> ZOMBIFIED_PIGLIN;

    /**
     * Is not sent over the network
     */
    public static final VanillaEntityType<EnderDragonPartEntity> ENDER_DRAGON_PART;

    public static final float PLAYER_ENTITY_OFFSET;

    static {
        EntityTypeBase<Entity> entityBase = EntityTypeDefinition.baseBuilder(Entity.class)
                .addTranslator(EntityDataSerializers.BYTE, Entity::setFlags)
                .addTranslator(EntityDataSerializers.INT, Entity::setAir) // Air/bubbles
                .addTranslator(EntityDataSerializers.OPTIONAL_COMPONENT, Entity::setCustomName)
                .addTranslator(EntityDataSerializers.BOOLEAN, Entity::setCustomNameVisible)
                .addTranslator(EntityDataSerializers.BOOLEAN, Entity::setSilent)
                .addTranslator(EntityDataSerializers.BOOLEAN, Entity::setGravity)
                .addTranslator(EntityDataSerializers.POSE, (entity, entityMetadata) -> entity.setPose(entityMetadata.value()))
                .addTranslator(EntityDataSerializers.INT, Entity::setFreezing)
                .build();

        // Extends entity
        {
            AREA_EFFECT_CLOUD = VanillaEntityType.inherited(AreaEffectCloudEntity::new, entityBase)
                    .type(EntityTypes.AREA_EFFECT_CLOUD)
                    .height(0.5f).width(1.0f)
                    .addTranslator(EntityDataSerializers.FLOAT, AreaEffectCloudEntity::setRadius)
                    .addTranslator(null) // Waiting
                    .addTranslator(EntityDataSerializers.PARTICLE, AreaEffectCloudEntity::setParticle)
                    .build();
            CUSHION = VanillaEntityType.inherited(CushionEntity::new, entityBase)
                    .type(EntityTypes.CUSHION)
                    .width(1.0F).height(0.25F)
                    .addTranslator(EntityDataSerializers.DYE_COLOR, CushionEntity::setDyeColor)
                    .build();
            DRAGON_FIREBALL = VanillaEntityType.inherited(FireballEntity::new, entityBase)
                    .type(EntityTypes.DRAGON_FIREBALL)
                    .heightAndWidth(1.0f)
                    .build();
            END_CRYSTAL = VanillaEntityType.inherited(EnderCrystalEntity::new, entityBase)
                    .type(EntityTypes.END_CRYSTAL)
                    .heightAndWidth(2.0f)
                    .bedrockIdentifier("minecraft:ender_crystal")
                    .addTranslator(EntityDataSerializers.OPTIONAL_BLOCK_POS, EnderCrystalEntity::setBlockTarget)
                    .addTranslator(EntityDataSerializers.BOOLEAN,
                            (enderCrystalEntity, entityMetadata) -> enderCrystalEntity.setFlag(EntityFlag.SHOW_BOTTOM, ((DataValue<Boolean>) entityMetadata).value())) // There is a base located on the ender crystal
                    .build();
            EXPERIENCE_ORB = VanillaEntityType.inherited(ExpOrbEntity::new, entityBase)
                    .type(EntityTypes.EXPERIENCE_ORB)
                    .addTranslator(null) // int determining xb orb texture
                    .bedrockIdentifier("minecraft:xp_orb")
                    .build();
            EVOKER_FANGS = VanillaEntityType.inherited(EvokerFangsEntity::new, entityBase)
                    .type(EntityTypes.EVOKER_FANGS)
                    .height(0.8f).width(0.5f)
                    .bedrockIdentifier("minecraft:evocation_fang")
                    .build();
            EYE_OF_ENDER = VanillaEntityType.inherited(EnderEyeEntity::new, entityBase)
                    .type(EntityTypes.EYE_OF_ENDER)
                    .heightAndWidth(0.25f)
                    .bedrockIdentifier("minecraft:eye_of_ender_signal")
                    .addTranslator(null)  // Item
                    .build();
            FALLING_BLOCK = VanillaEntityType.<FallingBlockEntity>inherited(null, entityBase)
                    .type(EntityTypes.FALLING_BLOCK)
                    .heightAndWidth(0.98f)
                    .addTranslator(null) // "start block position"
                    .build();
            FIREWORK_ROCKET = VanillaEntityType.inherited(FireworkEntity::new, entityBase)
                    .type(EntityTypes.FIREWORK_ROCKET)
                    .heightAndWidth(0.25f)
                    .bedrockIdentifier("minecraft:fireworks_rocket")
                    .addTranslator(EntityDataSerializers.ITEM_STACK, FireworkEntity::setFireworkItem)
                    .addTranslator(EntityDataSerializers.OPTIONAL_UNSIGNED_INT, FireworkEntity::setPlayerGliding)
                    .addTranslator(EntityDataSerializers.BOOLEAN, FireworkEntity::setShotAtAngle)
                    .build();
            FISHING_BOBBER = VanillaEntityType.<FishingHookEntity>inherited(null, entityBase)
                    .type(EntityTypes.FISHING_BOBBER)
                    .bedrockIdentifier("minecraft:fishing_hook")
                    .addTranslator(EntityDataSerializers.INT, FishingHookEntity::setHookedEntity)
                    .addTranslator(null) // Biting TODO check
                    .build();
            ITEM = VanillaEntityType.inherited(ItemEntity::new, entityBase)
                    .type(EntityTypes.ITEM)
                    .heightAndWidth(0.25f)
                    .offset(0.125f)
                    .addTranslator(EntityDataSerializers.ITEM_STACK, ItemEntity::setItem)
                    .build();
            LEASH_KNOT = VanillaEntityType.inherited(LeashKnotEntity::new, entityBase)
                    .type(EntityTypes.LEASH_KNOT)
                    .height(0.5f).width(0.375f)
                    .build();
            LIGHTNING_BOLT = VanillaEntityType.inherited(LightningEntity::new, entityBase)
                    .type(EntityTypes.LIGHTNING_BOLT)
                    .build();
            LLAMA_SPIT = VanillaEntityType.inherited(ProjectileEntity::new, entityBase)
                    .type(EntityTypes.LLAMA_SPIT)
                    .heightAndWidth(0.25f)
                    .build();
            OMINOUS_ITEM_SPAWNER = VanillaEntityType.inherited(OminousItemSpawnerEntity::new, entityBase)
                    .type(EntityTypes.OMINOUS_ITEM_SPAWNER)
                    .heightAndWidth(0.25f)
                    .addTranslator(EntityDataSerializers.ITEM_STACK, OminousItemSpawnerEntity::setItem)
                    .build();
            SHULKER_BULLET = VanillaEntityType.inherited(ProjectileEntity::new, entityBase)
                    .type(EntityTypes.SHULKER_BULLET)
                    .heightAndWidth(0.3125f)
                    .build();
            TNT = VanillaEntityType.inherited(TNTEntity::new, entityBase)
                    .type(EntityTypes.TNT)
                    .heightAndWidth(0.98f)
                    .offset(0.49f)
                    .addTranslator(EntityDataSerializers.INT, TNTEntity::setFuseLength)
                    .addTranslator(null) // Block state id
                    .build();

            EntityTypeBase<DisplayBaseEntity> displayBase = EntityTypeBase.baseInherited(DisplayBaseEntity.class, entityBase)
                    .addTranslator(null) // Interpolation delay
                    .addTranslator(null) // Transformation interpolation duration
                    .addTranslator(null) // Position/Rotation interpolation duration
                    .addTranslator(EntityDataSerializers.VECTOR3, DisplayBaseEntity::setTranslation) // Translation
                    .addTranslator(null) // Scale
                    .addTranslator(null) // Left rotation
                    .addTranslator(null) // Right rotation
                    .addTranslator(null) // Billboard render constraints
                    .addTranslator(null) // Brightness override
                    .addTranslator(null) // View range
                    .addTranslator(null) // Shadow radius
                    .addTranslator(null) // Shadow strength
                    .addTranslator(null) // Width
                    .addTranslator(null) // Height
                    .addTranslator(null) // Glow color override
                    .build();
            TEXT_DISPLAY = VanillaEntityType.inherited(TextDisplayEntity::new, displayBase)
                    .type(EntityTypes.TEXT_DISPLAY)
                    .bedrockDefinition(BedrockEntityDefinitions.ARMOR_STAND)
                    .addTranslator(EntityDataSerializers.COMPONENT, TextDisplayEntity::setText)
                    .addTranslator(null) // Line width
                    .addTranslator(null) // Background color
                    .addTranslator(null) // Text opacity
                    .addTranslator(null) // Bit mask
                    .build();

            INTERACTION = VanillaEntityType.inherited(InteractionEntity::new, entityBase)
                    .type(EntityTypes.INTERACTION)
                    .heightAndWidth(1.0f) // default size until server specifies otherwise
                    .bedrockDefinition(BedrockEntityDefinitions.ARMOR_STAND)
                    .addTranslator(EntityDataSerializers.FLOAT, InteractionEntity::setWidth)
                    .addTranslator(EntityDataSerializers.FLOAT, InteractionEntity::setHeight)
                    .addTranslator(EntityDataSerializers.BOOLEAN, InteractionEntity::setResponse)
                    .build();

            EntityTypeBase<FireballEntity> fireballBase = EntityTypeBase.baseInherited(FireballEntity.class, entityBase)
                    .addTranslator(null) // Item
                    .build();
            FIREBALL = VanillaEntityType.inherited(FireballEntity::new, fireballBase)
                    .type(EntityTypes.FIREBALL)
                    .heightAndWidth(1.0f)
                    .build();
            SMALL_FIREBALL = VanillaEntityType.inherited(FireballEntity::new, fireballBase)
                    .type(EntityTypes.SMALL_FIREBALL)
                    .heightAndWidth(0.3125f)
                    .build();

            EntityTypeBase<ThrowableItemEntity> throwableItemBase = EntityTypeBase.baseInherited(ThrowableItemEntity.class, entityBase)
                    .addTranslator(EntityDataSerializers.ITEM_STACK, ThrowableItemEntity::setItem)
                    .build();
            EGG = VanillaEntityType.inherited(ThrowableEggEntity::new, throwableItemBase)
                    .type(EntityTypes.EGG)
                    .heightAndWidth(0.25f)
                    .property(TemperatureVariantAnimal.TEMPERATE_VARIANT_PROPERTY)
                    .build();
            ENDER_PEARL = VanillaEntityType.inherited(ThrowableItemEntity::new, throwableItemBase)
                    .type(EntityTypes.ENDER_PEARL)
                    .heightAndWidth(0.25f)
                    .build();
            EXPERIENCE_BOTTLE = VanillaEntityType.inherited(ThrowableItemEntity::new, throwableItemBase)
                    .type(EntityTypes.EXPERIENCE_BOTTLE)
                    .heightAndWidth(0.25f)
                    .bedrockIdentifier("minecraft:xp_bottle")
                    .build();
            SPLASH_POTION = VanillaEntityType.inherited(ThrownPotionEntity::new, throwableItemBase)
                    .type(EntityTypes.SPLASH_POTION)
                    .heightAndWidth(0.25f)
                    .bedrockDefinition(BedrockEntityDefinitions.SPLASH_POTION)
                    .build();
            LINGERING_POTION = VanillaEntityType.inherited(ThrownPotionEntity::new, throwableItemBase)
                .type(EntityTypes.LINGERING_POTION)
                .heightAndWidth(0.25f)
                .bedrockDefinition(BedrockEntityDefinitions.SPLASH_POTION)
                .build();
            SNOWBALL = VanillaEntityType.inherited(ThrowableItemEntity::new, throwableItemBase)
                    .type(EntityTypes.SNOWBALL)
                    .heightAndWidth(0.25f)
                    .build();

            EntityFactory<AbstractWindChargeEntity> windChargeSupplier = AbstractWindChargeEntity::new;
            BREEZE_WIND_CHARGE = VanillaEntityType.inherited(windChargeSupplier, entityBase)
                    .type(EntityTypes.BREEZE_WIND_CHARGE)
                    .bedrockIdentifier("minecraft:breeze_wind_charge_projectile")
                    .heightAndWidth(0.3125f)
                    .build();
            WIND_CHARGE = VanillaEntityType.inherited(windChargeSupplier, entityBase)
                    .type(EntityTypes.WIND_CHARGE)
                    .bedrockIdentifier("minecraft:wind_charge_projectile")
                    .heightAndWidth(0.3125f)
                    .build();

            EntityTypeBase<AbstractArrowEntity> abstractArrowBase = EntityTypeBase.baseInherited(AbstractArrowEntity.class, entityBase)
                    .addTranslator(EntityDataSerializers.BYTE, AbstractArrowEntity::setArrowFlags)
                    .addTranslator(null) // "Piercing level"
                    .addTranslator(EntityDataSerializers.BOOLEAN, AbstractArrowEntity::setInGround) // If the arrow is in the ground
                    .build();
            ARROW = VanillaEntityType.inherited(ArrowEntity::new, abstractArrowBase)
                    .type(EntityTypes.ARROW)
                    .heightAndWidth(0.25f)
                    .bedrockDefinition(BedrockEntityDefinitions.ARROW)
                    .addTranslator(EntityDataSerializers.INT, ArrowEntity::setPotionEffectColor)
                    .build();
            SPECTRAL_ARROW = VanillaEntityType.inherited(AbstractArrowEntity::new, abstractArrowBase)
                    .type(EntityTypes.SPECTRAL_ARROW)
                    .heightAndWidth(0.25f)
                    .bedrockDefinition(BedrockEntityDefinitions.ARROW)
                    .build();
            TRIDENT = VanillaEntityType.inherited(TridentEntity::new, abstractArrowBase) // TODO remove class
                    .type(EntityTypes.TRIDENT)
                    .bedrockIdentifier("minecraft:thrown_trident")
                    .addTranslator(null) // Loyalty
                    .addTranslator(EntityDataSerializers.BOOLEAN, (tridentEntity, entityMetadata) -> tridentEntity.setFlag(EntityFlag.ENCHANTED, ((DataValue<Boolean>) entityMetadata).value()))
                    .build();

            EntityTypeBase<HangingEntity> hangingEntityBase = EntityTypeBase.baseInherited(HangingEntity.class, entityBase)
                .addTranslator(EntityDataSerializers.DIRECTION, HangingEntity::setDirectionMetadata)
                .build();

            PAINTING = VanillaEntityType.inherited(PaintingEntity::new, hangingEntityBase)
                .type(EntityTypes.PAINTING)
                .addTranslator(EntityDataSerializers.PAINTING_VARIANT, PaintingEntity::setPaintingType)
                .build();

            // Item frames are handled differently as they are blocks, not items, in Bedrock
            ITEM_FRAME = VanillaEntityType.inherited(ItemFrameEntity::new, hangingEntityBase)
                    .type(EntityTypes.ITEM_FRAME)
                    .addTranslator(EntityDataSerializers.ITEM_STACK, ItemFrameEntity::setItemInFrame)
                    .addTranslator(EntityDataSerializers.INT, ItemFrameEntity::setItemRotation)
                    .build();
            GLOW_ITEM_FRAME = VanillaEntityType.inherited(ITEM_FRAME.factory(), ITEM_FRAME)
                    .type(EntityTypes.GLOW_ITEM_FRAME)
                    .build();

            MINECART = VanillaEntityType.inherited(MinecartEntity::new, entityBase)
                    .type(EntityTypes.MINECART)
                    .height(0.7f).width(0.98f)
                    .offset(0.35f)
                    .bedrockDefinition(BedrockEntityDefinitions.MINECART)
                    .addTranslator(EntityDataSerializers.INT, (minecartEntity, entityMetadata) -> minecartEntity.getMetadata().put(EntityDataTypes.STRUCTURAL_INTEGRITY, entityMetadata.value()))
                    .addTranslator(EntityDataSerializers.INT, (minecartEntity, entityMetadata) -> minecartEntity.getMetadata().put(EntityDataTypes.HURT_DIRECTION, entityMetadata.value())) // Direction in which the minecart is shaking
                    .addTranslator(EntityDataSerializers.FLOAT, (minecartEntity, entityMetadata) ->
                            // Power in Java, hurt ticks in Bedrock
                            minecartEntity.getMetadata().put(EntityDataTypes.HURT_TICKS, Math.min(((DataValue<Float>) entityMetadata).value().intValue(), 15)))
                    .addTranslator(EntityDataSerializers.OPTIONAL_BLOCK_STATE, MinecartEntity::setCustomBlock)
                    .addTranslator(EntityDataSerializers.INT, MinecartEntity::setCustomBlockOffset)
                    .build();
            CHEST_MINECART = VanillaEntityType.inherited(MINECART.factory(), MINECART)
                    .type(EntityTypes.CHEST_MINECART)
                    .build();
            COMMAND_BLOCK_MINECART = VanillaEntityType.inherited(CommandBlockMinecartEntity::new, MINECART)
                    .type(EntityTypes.COMMAND_BLOCK_MINECART)
                    .addTranslator(EntityDataSerializers.STRING, (entity, entityMetadata) -> entity.getMetadata().put(EntityDataTypes.COMMAND_BLOCK_NAME, entityMetadata.value()))
                    .addTranslator(EntityDataSerializers.COMPONENT, (entity, entityMetadata) -> entity.getMetadata().put(EntityDataTypes.COMMAND_BLOCK_LAST_OUTPUT, MessageTranslator.convertMessage(entityMetadata.value())))
                    .build();
            FURNACE_MINECART = VanillaEntityType.inherited(FurnaceMinecartEntity::new, MINECART)
                    .type(EntityTypes.FURNACE_MINECART)
                    .bedrockDefinition(BedrockEntityDefinitions.MINECART)
                    .addTranslator(EntityDataSerializers.BOOLEAN, FurnaceMinecartEntity::setHasFuel)
                    .build();
            HOPPER_MINECART = VanillaEntityType.inherited(MINECART.factory(), MINECART)
                    .type(EntityTypes.HOPPER_MINECART)
                    .build();
            SPAWNER_MINECART = VanillaEntityType.inherited(SpawnerMinecartEntity::new, MINECART)
                    .type(EntityTypes.SPAWNER_MINECART)
                    .bedrockDefinition(BedrockEntityDefinitions.MINECART)
                    .build();
            TNT_MINECART = VanillaEntityType.inherited(MINECART.factory(), MINECART)
                    .type(EntityTypes.TNT_MINECART)
                    .build();

            WITHER_SKULL = VanillaEntityType.inherited(WitherSkullEntity::new, entityBase)
                    .type(EntityTypes.WITHER_SKULL)
                    .heightAndWidth(0.3125f)
                    .addTranslator(EntityDataSerializers.BOOLEAN, WitherSkullEntity::setDangerous)
                    .build();

            // Bedrock exclusive entity
            Identifier dangerousSkull = Identifier.of("wither_skull_dangerous");
            BedrockEntityDefinition bedrockDefinition = BedrockEntityDefinition.builder()
                .identifier(dangerousSkull)
                .build();
            Registries.BEDROCK_ENTITY_DEFINITIONS.get().put(dangerousSkull, bedrockDefinition);
        }

        // Boats
        {
            EntityTypeBase<BoatEntity> boatBase = EntityTypeBase.baseInherited(BoatEntity.class, entityBase)
                .height(0.6f).width(1.6f)
                .offset(0.375f)
                .addTranslator(EntityDataSerializers.INT, (boatEntity, entityMetadata) -> boatEntity.getMetadata().put(EntityDataTypes.HURT_TICKS, entityMetadata.value())) // Time since last hit
                .addTranslator(EntityDataSerializers.INT, (boatEntity, entityMetadata) -> boatEntity.getMetadata().put(EntityDataTypes.HURT_DIRECTION, entityMetadata.value())) // Rocking direction
                .addTranslator(EntityDataSerializers.FLOAT, (boatEntity, entityMetadata) ->
                    // 'Health' in Bedrock, damage taken in Java - it makes motion in Bedrock
                    boatEntity.getMetadata().put(EntityDataTypes.STRUCTURAL_INTEGRITY, 40 - (((DataValue<Float>) entityMetadata).value().intValue())))
                .addTranslator(EntityDataSerializers.BOOLEAN, BoatEntity::setPaddlingLeft)
                .addTranslator(EntityDataSerializers.BOOLEAN, BoatEntity::setPaddlingRight)
                .addTranslator(EntityDataSerializers.INT, (boatEntity, entityMetadata) -> boatEntity.getMetadata().put(EntityDataTypes.BOAT_BUBBLE_TIME, entityMetadata.value())) // May not actually do anything
                .build();

            ACACIA_BOAT = buildBoat(boatBase, EntityTypes.ACACIA_BOAT, BoatEntity.BoatVariant.ACACIA);
            BAMBOO_RAFT = buildBoat(boatBase, EntityTypes.BAMBOO_RAFT, BoatEntity.BoatVariant.BAMBOO);
            BIRCH_BOAT = buildBoat(boatBase, EntityTypes.BIRCH_BOAT, BoatEntity.BoatVariant.BIRCH);
            CHERRY_BOAT = buildBoat(boatBase, EntityTypes.CHERRY_BOAT, BoatEntity.BoatVariant.CHERRY);
            DARK_OAK_BOAT = buildBoat(boatBase, EntityTypes.DARK_OAK_BOAT, BoatEntity.BoatVariant.DARK_OAK);
            JUNGLE_BOAT = buildBoat(boatBase, EntityTypes.JUNGLE_BOAT, BoatEntity.BoatVariant.JUNGLE);
            MANGROVE_BOAT = buildBoat(boatBase, EntityTypes.MANGROVE_BOAT, BoatEntity.BoatVariant.MANGROVE);
            OAK_BOAT = buildBoat(boatBase, EntityTypes.OAK_BOAT, BoatEntity.BoatVariant.OAK);
            SPRUCE_BOAT = buildBoat(boatBase, EntityTypes.SPRUCE_BOAT, BoatEntity.BoatVariant.SPRUCE);
            PALE_OAK_BOAT = buildBoat(boatBase, EntityTypes.PALE_OAK_BOAT, BoatEntity.BoatVariant.PALE_OAK);
            POPLAR_BOAT = buildBoat(boatBase, EntityTypes.POPLAR_BOAT, BoatEntity.BoatVariant.POPLAR);

            EntityTypeBase<ChestBoatEntity> chestBoatBase = EntityTypeBase.baseInherited(ChestBoatEntity.class, boatBase)
                .build();

            ACACIA_CHEST_BOAT = buildChestBoat(chestBoatBase, EntityTypes.ACACIA_CHEST_BOAT, BoatEntity.BoatVariant.ACACIA);
            BAMBOO_CHEST_RAFT = buildChestBoat(chestBoatBase, EntityTypes.BAMBOO_CHEST_RAFT, BoatEntity.BoatVariant.BAMBOO);
            BIRCH_CHEST_BOAT = buildChestBoat(chestBoatBase, EntityTypes.BIRCH_CHEST_BOAT, BoatEntity.BoatVariant.BIRCH);
            CHERRY_CHEST_BOAT = buildChestBoat(chestBoatBase, EntityTypes.CHERRY_CHEST_BOAT, BoatEntity.BoatVariant.CHERRY);
            DARK_OAK_CHEST_BOAT = buildChestBoat(chestBoatBase, EntityTypes.DARK_OAK_CHEST_BOAT, BoatEntity.BoatVariant.DARK_OAK);
            JUNGLE_CHEST_BOAT = buildChestBoat(chestBoatBase, EntityTypes.JUNGLE_CHEST_BOAT, BoatEntity.BoatVariant.JUNGLE);
            MANGROVE_CHEST_BOAT = buildChestBoat(chestBoatBase, EntityTypes.MANGROVE_CHEST_BOAT, BoatEntity.BoatVariant.MANGROVE);
            OAK_CHEST_BOAT = buildChestBoat(chestBoatBase, EntityTypes.OAK_CHEST_BOAT, BoatEntity.BoatVariant.OAK);
            SPRUCE_CHEST_BOAT = buildChestBoat(chestBoatBase, EntityTypes.SPRUCE_CHEST_BOAT, BoatEntity.BoatVariant.SPRUCE);
            PALE_OAK_CHEST_BOAT = buildChestBoat(chestBoatBase, EntityTypes.PALE_OAK_CHEST_BOAT, BoatEntity.BoatVariant.PALE_OAK);
            POPLAR_CHEST_BOAT = buildChestBoat(chestBoatBase, EntityTypes.POPLAR_CHEST_BOAT, BoatEntity.BoatVariant.POPLAR);
        }

        EntityTypeBase<LivingEntity> livingEntityBase = EntityTypeBase.baseInherited(LivingEntity.class, entityBase)
                .addTranslator(EntityDataSerializers.BYTE, LivingEntity::setLivingEntityFlags)
                .addTranslator(EntityDataSerializers.FLOAT, LivingEntity::setHealth)
                .addTranslator(EntityDataSerializers.PARTICLES, LivingEntity::setParticles)
                .addTranslator(EntityDataSerializers.BOOLEAN,
                        (livingEntity, entityMetadata) -> livingEntity.getMetadata().put(EntityDataTypes.EFFECT_AMBIENCE, (byte) (((DataValue<Boolean>) entityMetadata).value() ? 1 : 0)))
                .addTranslator(null) // Arrow count
                .addTranslator(null) // Stinger count
                .addTranslator(EntityDataSerializers.OPTIONAL_BLOCK_POS, LivingEntity::setBedPosition)
                .build();

        ARMOR_STAND = VanillaEntityType.inherited(ArmorStandEntity::new, livingEntityBase)
                .type(EntityTypes.ARMOR_STAND)
                .bedrockDefinition(BedrockEntityDefinitions.ARMOR_STAND)
                .height(1.975f).width(0.5f)
                .addTranslator(EntityDataSerializers.BYTE, ArmorStandEntity::setArmorStandFlags)
                .addTranslator(EntityDataSerializers.ROTATIONS, ArmorStandEntity::setHeadRotation)
                .addTranslator(EntityDataSerializers.ROTATIONS, ArmorStandEntity::setBodyRotation)
                .addTranslator(EntityDataSerializers.ROTATIONS, ArmorStandEntity::setLeftArmRotation)
                .addTranslator(EntityDataSerializers.ROTATIONS, ArmorStandEntity::setRightArmRotation)
                .addTranslator(EntityDataSerializers.ROTATIONS, ArmorStandEntity::setLeftLegRotation)
                .addTranslator(EntityDataSerializers.ROTATIONS, ArmorStandEntity::setRightLegRotation)
                .build();

        EntityTypeBase<AvatarEntity> avatarEntityBase = EntityTypeBase.baseInherited(AvatarEntity.class, livingEntityBase)
            .height(1.8f).width(0.6f)
            // This is the offset sent by Bedrock in its player position. Verified on Bedrock 26.23.
            .offset(1.62001f)
            .addTranslator(null) // Player main hand
            .addTranslator(EntityDataSerializers.BYTE, AvatarEntity::setSkinVisibility)
            .build();

        MANNEQUIN = VanillaEntityType.inherited(MannequinEntity::new, avatarEntityBase)
            .type(EntityTypes.MANNEQUIN)
            .addTranslator(EntityDataSerializers.RESOLVABLE_PROFILE, MannequinEntity::setProfile)
            .addTranslator(null) // Immovable
            .addTranslator(EntityDataSerializers.OPTIONAL_COMPONENT, MannequinEntity::setDescription)
            .build();

        MARKER = VanillaEntityType.inherited(null, entityBase)
            .type(EntityTypes.MARKER)
            .build();

        PLAYER = VanillaEntityType.<PlayerEntity>inherited(null, avatarEntityBase)
                .type(EntityTypes.PLAYER)
                .addTranslator(EntityDataSerializers.FLOAT, PlayerEntity::setAbsorptionHearts)
                .addTranslator(null) // Player score
                .addTranslator(EntityDataSerializers.OPTIONAL_UNSIGNED_INT, PlayerEntity::setLeftParrot)
                .addTranslator(EntityDataSerializers.OPTIONAL_UNSIGNED_INT, PlayerEntity::setRightParrot)
                .build();

        EntityTypeBase<MobEntity> mobEntityBase = EntityTypeBase.baseInherited(MobEntity.class, livingEntityBase)
                .addTranslator(EntityDataSerializers.BYTE, MobEntity::setMobFlags)
                .build();

        // Extends mob
        {
            ALLAY = VanillaEntityType.inherited(AllayEntity::new, mobEntityBase)
                    .type(EntityTypes.ALLAY)
                    .height(0.6f).width(0.35f)
                    .addTranslator(EntityDataSerializers.BOOLEAN, AllayEntity::setDancing)
                    .addTranslator(EntityDataSerializers.BOOLEAN, AllayEntity::setCanDuplicate)
                    .build();
            BAT = VanillaEntityType.inherited(BatEntity::new, mobEntityBase)
                    .type(EntityTypes.BAT)
                    .height(0.9f).width(0.5f)
                    .addTranslator(EntityDataSerializers.BYTE, BatEntity::setBatFlags)
                    .build();
            BOGGED = VanillaEntityType.inherited(BoggedEntity::new, mobEntityBase)
                    .type(EntityTypes.BOGGED)
                    .height(1.99f).width(0.6f)
                    .addTranslator(EntityDataSerializers.BOOLEAN, BoggedEntity::setSheared)
                    .build();
            BLAZE = VanillaEntityType.inherited(BlazeEntity::new, mobEntityBase)
                    .type(EntityTypes.BLAZE)
                    .height(1.8f).width(0.6f)
                    .addTranslator(EntityDataSerializers.BYTE, BlazeEntity::setBlazeFlags)
                    .build();
            BREEZE = VanillaEntityType.inherited(BreezeEntity::new, mobEntityBase)
                    .type(EntityTypes.BREEZE)
                    .height(1.77f).width(0.6f)
                    .build();
            COPPER_GOLEM = VanillaEntityType.inherited(CopperGolemEntity::new, mobEntityBase)
                    .type(EntityTypes.COPPER_GOLEM)
                    .height(0.49f).width(0.98f)
                    .addTranslator(EntityDataSerializers.WEATHERING_COPPER_STATE, CopperGolemEntity::setWeatheringState)
                    .addTranslator(EntityDataSerializers.COPPER_GOLEM_STATE, CopperGolemEntity::setGolemState)
                    .property(CopperGolemEntity.CHEST_INTERACTION_PROPERTY)
                    .property(CopperGolemEntity.HAS_FLOWER_PROPERTY)
                    .property(CopperGolemEntity.OXIDATION_LEVEL_STATE_ENUM_PROPERTY)
                    .build();
            CREAKING = VanillaEntityType.inherited(CreakingEntity::new, mobEntityBase)
                    .type(EntityTypes.CREAKING)
                    .height(2.7f).width(0.9f)
                    .addTranslator(EntityDataSerializers.BOOLEAN, CreakingEntity::setCanMove)
                    .addTranslator(EntityDataSerializers.BOOLEAN, CreakingEntity::setActive)
                    .addTranslator(EntityDataSerializers.BOOLEAN, CreakingEntity::setIsTearingDown)
                    .addTranslator(EntityDataSerializers.OPTIONAL_BLOCK_POS, CreakingEntity::setHomePos)
                    .property(CreakingEntity.STATE_PROPERTY)
                    .property(CreakingEntity.SWAYING_TICKS_PROPERTY)
                    .build();
            CREEPER = VanillaEntityType.inherited(CreeperEntity::new, mobEntityBase)
                    .type(EntityTypes.CREEPER)
                    .height(1.7f).width(0.6f)
                    .addTranslator(EntityDataSerializers.INT, CreeperEntity::setSwelling)
                    .addTranslator(EntityDataSerializers.BOOLEAN, (entity, entityMetadata) -> entity.setFlag(EntityFlag.POWERED, ((DataValue<Boolean>) entityMetadata).value()))
                    .addTranslator(EntityDataSerializers.BOOLEAN, CreeperEntity::setIgnited)
                    .build();
            ENDERMAN = VanillaEntityType.inherited(EndermanEntity::new, mobEntityBase)
                    .type(EntityTypes.ENDERMAN)
                    .height(2.9f).width(0.6f)
                    .addTranslator(EntityDataSerializers.OPTIONAL_BLOCK_STATE, EndermanEntity::setCarriedBlock)
                    .addTranslator(EntityDataSerializers.BOOLEAN, EndermanEntity::setScreaming)
                    .addTranslator(EntityDataSerializers.BOOLEAN, EndermanEntity::setAngry)
                    .build();
            ENDERMITE = VanillaEntityType.inherited(MonsterEntity::new, mobEntityBase)
                    .type(EntityTypes.ENDERMITE)
                    .height(0.3f).width(0.4f)
                    .build();
            ENDER_DRAGON = VanillaEntityType.inherited(EnderDragonEntity::new, mobEntityBase)
                    .type(EntityTypes.ENDER_DRAGON)
                    .addTranslator(EntityDataSerializers.INT, EnderDragonEntity::setPhase)
                    .build();
            GHAST = VanillaEntityType.inherited(GhastEntity::new, mobEntityBase)
                    .type(EntityTypes.GHAST)
                    .heightAndWidth(4.0f)
                    .addTranslator(EntityDataSerializers.BOOLEAN, GhastEntity::setGhastAttacking)
                    .build();
            GIANT = VanillaEntityType.inherited(GiantEntity::new, mobEntityBase)
                    .type(EntityTypes.GIANT)
                    .height(1.8f).width(1.6f)
                    .bedrockDefinition(BedrockEntityDefinitions.ZOMBIE)
                    .build();
            IRON_GOLEM = VanillaEntityType.inherited(IronGolemEntity::new, mobEntityBase)
                    .type(EntityTypes.IRON_GOLEM)
                    .height(2.7f).width(1.4f)
                    .addTranslator(null) // "is player created", which doesn't seem to do anything clientside
                    .build();
            PHANTOM = VanillaEntityType.inherited(PhantomEntity::new, mobEntityBase)
                    .type(EntityTypes.PHANTOM)
                    .height(0.5f).width(0.9f)
                    .addTranslator(EntityDataSerializers.INT, PhantomEntity::setPhantomScale)
                    .build();
            SILVERFISH = VanillaEntityType.inherited(MonsterEntity::new, mobEntityBase)
                    .type(EntityTypes.SILVERFISH)
                    .height(0.3f).width(0.4f)
                    .build();
            SHULKER = VanillaEntityType.inherited(ShulkerEntity::new, mobEntityBase)
                    .type(EntityTypes.SHULKER)
                    .heightAndWidth(1f)
                    .addTranslator(EntityDataSerializers.DIRECTION, ShulkerEntity::setAttachedFace)
                    .addTranslator(EntityDataSerializers.BYTE, ShulkerEntity::setShulkerHeight)
                    .addTranslator(EntityDataSerializers.BYTE, ShulkerEntity::setShulkerColor)
                    .build();
            SKELETON = VanillaEntityType.inherited(SkeletonEntity::new, mobEntityBase)
                    .type(EntityTypes.SKELETON)
                    .height(1.8f).width(0.6f)
                    .addTranslator(EntityDataSerializers.BOOLEAN, SkeletonEntity::setConvertingToStray)
                    .build();
            SNOW_GOLEM = VanillaEntityType.inherited(SnowGolemEntity::new, mobEntityBase)
                    .type(EntityTypes.SNOW_GOLEM)
                    .height(1.9f).width(0.7f)
                    .addTranslator(EntityDataSerializers.BYTE, SnowGolemEntity::setSnowGolemFlags)
                    .build();
            SPIDER = VanillaEntityType.inherited(SpiderEntity::new, mobEntityBase)
                    .type(EntityTypes.SPIDER)
                    .height(0.9f).width(1.4f)
                    .addTranslator(EntityDataSerializers.BYTE, SpiderEntity::setSpiderFlags)
                    .build();
            CAVE_SPIDER = VanillaEntityType.inherited(SpiderEntity::new, SPIDER)
                    .type(EntityTypes.CAVE_SPIDER)
                    .height(0.5f).width(0.7f)
                    .build();
            STRAY = VanillaEntityType.inherited(AbstractSkeletonEntity::new, mobEntityBase)
                    .type(EntityTypes.STRAY)
                    .height(1.8f).width(0.6f)
                    .build();
            PARCHED = VanillaEntityType.inherited(AbstractSkeletonEntity::new, mobEntityBase)
                    .type(EntityTypes.PARCHED)
                    .height(1.8f).width(0.6f)
                    .build();
            VEX = VanillaEntityType.inherited(VexEntity::new, mobEntityBase)
                    .type(EntityTypes.VEX)
                    .height(0.8f).width(0.4f)
                    .addTranslator(EntityDataSerializers.BYTE, VexEntity::setVexFlags)
                    .build();
            WARDEN = VanillaEntityType.inherited(WardenEntity::new, mobEntityBase)
                    .type(EntityTypes.WARDEN)
                    .height(2.9f).width(0.9f)
                    .addTranslator(EntityDataSerializers.INT, WardenEntity::setAngerLevel)
                    .build();
            WITHER = VanillaEntityType.inherited(WitherEntity::new, mobEntityBase)
                    .type(EntityTypes.WITHER)
                    .height(3.5f).width(0.9f)
                    .addTranslator(EntityDataSerializers.INT, WitherEntity::setTarget1)
                    .addTranslator(EntityDataSerializers.INT, WitherEntity::setTarget2)
                    .addTranslator(EntityDataSerializers.INT, WitherEntity::setTarget3)
                    .addTranslator(EntityDataSerializers.INT, WitherEntity::setInvulnerableTicks)
                    .build();
            WITHER_SKELETON = VanillaEntityType.inherited(AbstractSkeletonEntity::new, mobEntityBase)
                    .type(EntityTypes.WITHER_SKELETON)
                    .height(2.4f).width(0.7f)
                    .build();
            ZOGLIN = VanillaEntityType.inherited(ZoglinEntity::new, mobEntityBase)
                    .type(EntityTypes.ZOGLIN)
                    .height(1.4f).width(1.3965f)
                    .addTranslator(EntityDataSerializers.BOOLEAN, ZoglinEntity::setBaby)
                    .build();
            ZOMBIE = VanillaEntityType.inherited(ZombieEntity::new, mobEntityBase)
                    .type(EntityTypes.ZOMBIE)
                    .height(1.8f).width(0.6f)
                    .bedrockDefinition(BedrockEntityDefinitions.ZOMBIE)
                    .addTranslator(EntityDataSerializers.BOOLEAN, ZombieEntity::setZombieBaby)
                    .addTranslator(null) // "set special type", doesn't do anything
                    .addTranslator(EntityDataSerializers.BOOLEAN, ZombieEntity::setConvertingToDrowned)
                    .build();
            ZOMBIE_VILLAGER = VanillaEntityType.inherited(ZombieVillagerEntity::new, ZOMBIE)
                    .type(EntityTypes.ZOMBIE_VILLAGER)
                    .height(1.8f).width(0.6f)
                    .bedrockIdentifier("minecraft:zombie_villager_v2")
                    .addTranslator(EntityDataSerializers.BOOLEAN, ZombieVillagerEntity::setTransforming)
                    .addTranslator(EntityDataSerializers.VILLAGER_DATA, ZombieVillagerEntity::setZombieVillagerData)
                    .addTranslator(null) // Villager data finalized
                    .build();
            ZOMBIFIED_PIGLIN = VanillaEntityType.inherited(ZombifiedPiglinEntity::new, ZOMBIE) //TODO test how zombie entity metadata is handled?
                    .type(EntityTypes.ZOMBIFIED_PIGLIN)
                    .height(1.95f).width(0.6f)
                    .bedrockIdentifier("minecraft:zombie_pigman")
                    .build();

            DROWNED = VanillaEntityType.inherited(ZOMBIE.factory(), ZOMBIE)
                    .type(EntityTypes.DROWNED)
                    .height(1.95f).width(0.6f)
                    .build();
            HUSK = VanillaEntityType.inherited(ZOMBIE.factory(), ZOMBIE)
                    .type(EntityTypes.HUSK)
                    .build();

            GUARDIAN = VanillaEntityType.inherited(GuardianEntity::new, mobEntityBase)
                    .type(EntityTypes.GUARDIAN)
                    .heightAndWidth(0.85f)
                    .addTranslator(null) // Moving //TODO
                    .addTranslator(EntityDataSerializers.INT, GuardianEntity::setGuardianTarget)
                    .build();
            ELDER_GUARDIAN = VanillaEntityType.inherited(ElderGuardianEntity::new, GUARDIAN)
                    .type(EntityTypes.ELDER_GUARDIAN)
                    .heightAndWidth(1.9975f)
                    .build();

            EntityTypeBase<AbstractFishEntity> abstractFishEntityBase = EntityTypeBase.baseInherited(AbstractFishEntity.class, mobEntityBase)
                    .addTranslator(null) // From bucket
                    .build();
            COD = VanillaEntityType.inherited(AbstractFishEntity::new, abstractFishEntityBase)
                    .type(EntityTypes.COD)
                    .height(0.25f).width(0.5f)
                    .build();
            PUFFERFISH = VanillaEntityType.inherited(PufferFishEntity::new, abstractFishEntityBase)
                    .type(EntityTypes.PUFFERFISH)
                    .heightAndWidth(0.7f)
                    .addTranslator(EntityDataSerializers.INT, PufferFishEntity::setPufferfishSize)
                    .build();
            SALMON = VanillaEntityType.inherited(AbstractFishEntity::new, abstractFishEntityBase)
                    .type(EntityTypes.SALMON)
                    .height(0.5f).width(0.7f)
                    .addTranslator(null) // Scale/variant - TODO
                    .build();
            TADPOLE = VanillaEntityType.inherited(TadpoleEntity::new, abstractFishEntityBase)
                    .type(EntityTypes.TADPOLE)
                    .height(0.3f).width(0.4f)
                    .addTranslator(null) // Age locked
                    .build();
            TROPICAL_FISH = VanillaEntityType.inherited(TropicalFishEntity::new, abstractFishEntityBase)
                    .type(EntityTypes.TROPICAL_FISH)
                    .heightAndWidth(0.6f)
                    .bedrockIdentifier("minecraft:tropicalfish")
                    .addTranslator(EntityDataSerializers.INT, TropicalFishEntity::setFishVariant)
                    .build();

            EntityTypeBase<BasePiglinEntity> abstractPiglinEntityBase = EntityTypeBase.baseInherited(BasePiglinEntity.class, mobEntityBase)
                    .addTranslator(EntityDataSerializers.BOOLEAN, BasePiglinEntity::setImmuneToZombification)
                    .build();
            PIGLIN = VanillaEntityType.inherited(PiglinEntity::new, abstractPiglinEntityBase)
                    .type(EntityTypes.PIGLIN)
                    .height(1.95f).width(0.6f)
                    .addTranslator(EntityDataSerializers.BOOLEAN, PiglinEntity::setBaby)
                    .addTranslator(EntityDataSerializers.BOOLEAN, PiglinEntity::setChargingCrossbow)
                    .addTranslator(EntityDataSerializers.BOOLEAN, PiglinEntity::setDancing)
                    .build();
            PIGLIN_BRUTE = VanillaEntityType.inherited(BasePiglinEntity::new, abstractPiglinEntityBase)
                    .type(EntityTypes.PIGLIN_BRUTE)
                    .height(1.95f).width(0.6f)
                    .build();

            EntityTypeBase<RaidParticipantEntity> raidParticipantEntityBase = EntityTypeBase.baseInherited(RaidParticipantEntity.class, mobEntityBase)
                    .addTranslator(null) // Celebrating //TODO
                    .build();
            EntityTypeBase<SpellcasterIllagerEntity> spellcasterEntityBase = EntityTypeBase.baseInherited(SpellcasterIllagerEntity.class, raidParticipantEntityBase)
                    .addTranslator(EntityDataSerializers.BYTE, SpellcasterIllagerEntity::setSpellType)
                    .build();
            EVOKER = VanillaEntityType.inherited(SpellcasterIllagerEntity::new, spellcasterEntityBase)
                    .type(EntityTypes.EVOKER)
                    .height(1.95f).width(0.6f)
                    .bedrockDefinition(BedrockEntityDefinitions.EVOCATION_ILLAGER)
                    .build();
            ILLUSIONER = VanillaEntityType.inherited(SpellcasterIllagerEntity::new, spellcasterEntityBase)
                    .type(EntityTypes.ILLUSIONER)
                    .height(1.95f).width(0.6f)
                    .bedrockDefinition(BedrockEntityDefinitions.EVOCATION_ILLAGER)
                    .build();
            PILLAGER = VanillaEntityType.inherited(PillagerEntity::new, raidParticipantEntityBase)
                    .type(EntityTypes.PILLAGER)
                    .height(1.8f).width(0.6f)
                    .addTranslator(EntityDataSerializers.BOOLEAN, PillagerEntity::setChargingCrossbow)
                    .build();
            RAVAGER = VanillaEntityType.inherited(RavagerEntity::new, raidParticipantEntityBase)
                    .type(EntityTypes.RAVAGER)
                    .height(1.9f).width(1.2f)
                    .build();
            VINDICATOR = VanillaEntityType.inherited(VindicatorEntity::new, raidParticipantEntityBase)
                    .type(EntityTypes.VINDICATOR)
                    .height(1.8f).width(0.6f)
                    .build();
            WITCH = VanillaEntityType.inherited(RaidParticipantEntity::new, raidParticipantEntityBase)
                    .type(EntityTypes.WITCH)
                    .height(1.8f).width(0.6f)
                    .addTranslator(null) // Using item
                    .build();
        }

        EntityTypeBase<AgeableEntity> ageableEntityBase = EntityTypeBase.baseInherited(AgeableEntity.class, mobEntityBase)
                .addTranslator(EntityDataSerializers.BOOLEAN, AgeableEntity::setBaby)
                .addTranslator(null) // Age locked
                .build();

        // Extends ageable
        {
            ARMADILLO = VanillaEntityType.inherited(ArmadilloEntity::new, ageableEntityBase)
                    .type(EntityTypes.ARMADILLO)
                    .height(0.65f).width(0.7f)
                    .property(ArmadilloEntity.STATE_PROPERTY)
                    .addTranslator(EntityDataSerializers.ARMADILLO_STATE, ArmadilloEntity::setArmadilloState)
                    .build();
            AXOLOTL = VanillaEntityType.inherited(AxolotlEntity::new, ageableEntityBase)
                    .type(EntityTypes.AXOLOTL)
                    .height(0.42f).width(0.7f)
                    .addTranslator(EntityDataSerializers.INT, AxolotlEntity::setVariant)
                    .addTranslator(EntityDataSerializers.BOOLEAN, AxolotlEntity::setPlayingDead)
                    .addTranslator(null) // From bucket
                    .build();
            BEE = VanillaEntityType.inherited(BeeEntity::new, ageableEntityBase)
                    .type(EntityTypes.BEE)
                    .heightAndWidth(0.6f)
                    .property(BeeEntity.NECTAR_PROPERTY)
                    .addTranslator(EntityDataSerializers.BYTE, BeeEntity::setBeeFlags)
                    .addTranslator(EntityDataSerializers.LONG, BeeEntity::setAngerTime)
                    .build();
            CHICKEN = VanillaEntityType.inherited(ChickenEntity::new, ageableEntityBase)
                    .type(EntityTypes.CHICKEN)
                    .height(0.7f).width(0.4f)
                    .property(TemperatureVariantAnimal.TEMPERATE_VARIANT_PROPERTY)
                    .addTranslator(EntityDataSerializers.CHICKEN_VARIANT, ChickenEntity::setVariant)
                    .addTranslator(null) // Sound variant
                    .build();
            COW = VanillaEntityType.inherited(CowEntity::new, ageableEntityBase)
                    .type(EntityTypes.COW)
                    .height(1.4f).width(0.9f)
                    .property(TemperatureVariantAnimal.TEMPERATE_VARIANT_PROPERTY)
                    .addTranslator(EntityDataSerializers.COW_VARIANT, CowEntity::setVariant)
                    .addTranslator(null) // Sound variant
                    .build();
            FOX = VanillaEntityType.inherited(FoxEntity::new, ageableEntityBase)
                    .type(EntityTypes.FOX)
                    .height(0.7f).width(0.6f)
                    .addTranslator(EntityDataSerializers.INT, FoxEntity::setFoxVariant)
                    .addTranslator(EntityDataSerializers.BYTE, FoxEntity::setFoxFlags)
                    .addTranslator(null) // Trusted player 1
                    .addTranslator(null) // Trusted player 2
                    .build();
            FROG = VanillaEntityType.inherited(FrogEntity::new, ageableEntityBase)
                    .type(EntityTypes.FROG)
                    .heightAndWidth(0.5f)
                    .addTranslator(EntityDataSerializers.FROG_VARIANT, FrogEntity::setVariant)
                    .addTranslator(EntityDataSerializers.OPTIONAL_UNSIGNED_INT, FrogEntity::setTongueTarget)
                    .build();
            HAPPY_GHAST = VanillaEntityType.inherited(HappyGhastEntity::new, ageableEntityBase)
                    .type(EntityTypes.HAPPY_GHAST)
                    .heightAndWidth(4f)
                    .property(HappyGhastEntity.CAN_MOVE_PROPERTY)
                    .addTranslator(null) // Is leash holder
                    .addTranslator(EntityDataSerializers.BOOLEAN, HappyGhastEntity::setStaysStill)
                    .build();
            HOGLIN = VanillaEntityType.inherited(HoglinEntity::new, ageableEntityBase)
                    .type(EntityTypes.HOGLIN)
                    .height(1.4f).width(1.3965f)
                    .addTranslator(EntityDataSerializers.BOOLEAN, HoglinEntity::setImmuneToZombification)
                    .build();
            GOAT = VanillaEntityType.inherited(GoatEntity::new, ageableEntityBase)
                    .type(EntityTypes.GOAT)
                    .height(1.3f).width(0.9f)
                    .addTranslator(EntityDataSerializers.BOOLEAN, GoatEntity::setScreamer)
                    .addTranslator(EntityDataSerializers.BOOLEAN, GoatEntity::setHasLeftHorn)
                    .addTranslator(EntityDataSerializers.BOOLEAN, GoatEntity::setHasRightHorn)
                    .build();
            MOOSHROOM = VanillaEntityType.inherited(MooshroomEntity::new, ageableEntityBase)
                    .type(EntityTypes.MOOSHROOM)
                    .height(1.4f).width(0.9f)
                    .addTranslator(EntityDataSerializers.INT, MooshroomEntity::setMooshroomVariant)
                    .build();
            OCELOT = VanillaEntityType.inherited(OcelotEntity::new, ageableEntityBase)
                    .type(EntityTypes.OCELOT)
                    .height(0.7f).width(0.6f)
                    .addTranslator(EntityDataSerializers.BOOLEAN, (ocelotEntity, entityMetadata) -> ocelotEntity.setFlag(EntityFlag.TRUSTING, ((DataValue<Boolean>) entityMetadata).value()))
                    .build();
            PANDA = VanillaEntityType.inherited(PandaEntity::new, ageableEntityBase)
                    .type(EntityTypes.PANDA)
                    .height(1.25f).width(1.125f)
                    .addTranslator(null) // Unhappy counter
                    .addTranslator(null) // Sneeze counter
                    .addTranslator(EntityDataSerializers.INT, PandaEntity::setEatingCounter)
                    .addTranslator(EntityDataSerializers.BYTE, PandaEntity::setMainGene)
                    .addTranslator(EntityDataSerializers.BYTE, PandaEntity::setHiddenGene)
                    .addTranslator(EntityDataSerializers.BYTE, PandaEntity::setPandaFlags)
                    .build();
            PIG = VanillaEntityType.inherited(PigEntity::new, ageableEntityBase)
                    .type(EntityTypes.PIG)
                    .heightAndWidth(0.9f)
                    .property(TemperatureVariantAnimal.TEMPERATE_VARIANT_PROPERTY)
                    .addTranslator(EntityDataSerializers.INT, PigEntity::setBoost)
                    .addTranslator(EntityDataSerializers.PIG_VARIANT, PigEntity::setVariant)
                    .addTranslator(null) // Sound variant
                    .build();
            POLAR_BEAR = VanillaEntityType.inherited(PolarBearEntity::new, ageableEntityBase)
                    .type(EntityTypes.POLAR_BEAR)
                    .height(1.4f).width(1.3f)
                    .addTranslator(EntityDataSerializers.BOOLEAN, (entity, entityMetadata) -> entity.setFlag(EntityFlag.STANDING, ((DataValue<Boolean>) entityMetadata).value()))
                    .build();
            RABBIT = VanillaEntityType.inherited(RabbitEntity::new, ageableEntityBase)
                    .type(EntityTypes.RABBIT)
                    .height(0.5f).width(0.4f)
                    .addTranslator(EntityDataSerializers.INT, RabbitEntity::setRabbitVariant)
                    .build();
            SHEEP = VanillaEntityType.inherited(SheepEntity::new, ageableEntityBase)
                    .type(EntityTypes.SHEEP)
                    .height(1.3f).width(0.9f)
                    .addTranslator(EntityDataSerializers.BYTE, SheepEntity::setSheepFlags)
                    .build();
            SNIFFER = VanillaEntityType.inherited(SnifferEntity::new, ageableEntityBase)
                    .type(EntityTypes.SNIFFER)
                    .height(1.75f).width(1.9f)
                    .addTranslator(EntityDataSerializers.SNIFFER_STATE, SnifferEntity::setSnifferState)
                    .addTranslator(null) // Integer, drop seed at tick
                    .build();
            STRIDER = VanillaEntityType.inherited(StriderEntity::new, ageableEntityBase)
                    .type(EntityTypes.STRIDER)
                    .height(1.7f).width(0.9f)
                    .addTranslator(EntityDataSerializers.INT, StriderEntity::setBoost)
                    .addTranslator(EntityDataSerializers.BOOLEAN, StriderEntity::setCold)
                    .build();
            TURTLE = VanillaEntityType.inherited(TurtleEntity::new, ageableEntityBase)
                    .type(EntityTypes.TURTLE)
                    .height(0.4f).width(1.2f)
                    .addTranslator(EntityDataSerializers.BOOLEAN, TurtleEntity::setPregnant)
                    .addTranslator(EntityDataSerializers.BOOLEAN, TurtleEntity::setLayingEgg)
                    .build();

            // Slime entities
            EntityTypeBase<AbstractCubeEntity> cubeEntityBase = VanillaEntityType.baseInherited(AbstractCubeEntity.class, ageableEntityBase)
                    .heightAndWidth(0.51f)
                    .addTranslator(EntityDataSerializers.INT, AbstractCubeEntity::setCubeScale)
                    .build();

            SLIME = VanillaEntityType.inherited(SlimeEntity::new, cubeEntityBase)
                    .type(EntityTypes.SLIME)
                    .build();
            MAGMA_CUBE = VanillaEntityType.inherited(MagmaCubeEntity::new, cubeEntityBase)
                    .type(EntityTypes.MAGMA_CUBE)
                    .build();
            SULFUR_CUBE = VanillaEntityType.inherited(SulfurCubeEntity::new, cubeEntityBase)
                    .type(EntityTypes.SULFUR_CUBE)
                    .property(SulfurCubeEntity.SULFUR_CUBE_ARCHETYPE_PROPERTY)
                    .heightAndWidth(0.98f)
                    .addTranslator(EntityDataSerializers.INT, SulfurCubeEntity::setMaxFuse)
                    .addTranslator(null) // From bucket
                    .build();

            VanillaEntityType<AbstractMerchantEntity> abstractVillagerEntityBase = VanillaEntityType.inherited(AbstractMerchantEntity::new, ageableEntityBase)
                    .addTranslator(null) // Unhappy ticks
                    .build(false);
            VILLAGER = VanillaEntityType.inherited(VillagerEntity::new, abstractVillagerEntityBase)
                    .type(EntityTypes.VILLAGER)
                    .height(1.8f).width(0.6f)
                    .bedrockIdentifier("minecraft:villager_v2")
                    .addTranslator(EntityDataSerializers.VILLAGER_DATA, VillagerEntity::setVillagerData)
                    .addTranslator(null) // Villager data finalized
                    .build();
            WANDERING_TRADER = VanillaEntityType.inherited(abstractVillagerEntityBase.factory(), abstractVillagerEntityBase)
                    .type(EntityTypes.WANDERING_TRADER)
                    .height(1.8f).width(0.6f)
                    .build();
        }

        // Water creatures (AgeableWaterCreature)
        {
            DOLPHIN = VanillaEntityType.inherited(DolphinEntity::new, ageableEntityBase)
                .type(EntityTypes.DOLPHIN)
                .height(0.6f).width(0.9f)
                .addTranslator(null) // "got fish"
                .addTranslator(null) // "moistness level"
                .build();
            SQUID = VanillaEntityType.inherited(SquidEntity::new, ageableEntityBase)
                .type(EntityTypes.SQUID)
                .heightAndWidth(0.8f)
                .build();
            GLOW_SQUID = VanillaEntityType.inherited(GlowSquidEntity::new, SQUID)
                .type(EntityTypes.GLOW_SQUID)
                .addTranslator(null) // Set dark ticks remaining, possible TODO
                .build();
        }

        // Horses
        {
            EntityTypeBase<AbstractHorseEntity> abstractHorseEntityBase = EntityTypeBase.baseInherited(AbstractHorseEntity.class, ageableEntityBase)
                    .addTranslator(EntityDataSerializers.BYTE, AbstractHorseEntity::setHorseFlags)
                    .build();
            CAMEL = VanillaEntityType.inherited(CamelEntity::new, abstractHorseEntityBase)
                    .type(EntityTypes.CAMEL)
                    .height(2.375f).width(1.7f)
                    .addTranslator(EntityDataSerializers.BOOLEAN, CamelEntity::setDashing)
                    .addTranslator(EntityDataSerializers.LONG, CamelEntity::setLastPoseTick)
                    .build();
            CAMEL_HUSK = VanillaEntityType.inherited(CamelHuskEntity::new, CAMEL)
                    .type(EntityTypes.CAMEL_HUSK)
                    .build();
            HORSE = VanillaEntityType.inherited(HorseEntity::new, abstractHorseEntityBase)
                    .type(EntityTypes.HORSE)
                    .height(1.6f).width(1.3965f)
                    .addTranslator(EntityDataSerializers.INT, HorseEntity::setHorseVariant)
                    .build();
            SKELETON_HORSE = VanillaEntityType.inherited(SkeletonHorseEntity::new, abstractHorseEntityBase)
                    .type(EntityTypes.SKELETON_HORSE)
                    .height(1.6f).width(1.3965f)
                    .build();
            ZOMBIE_HORSE = VanillaEntityType.inherited(ZombieHorseEntity::new, abstractHorseEntityBase)
                    .type(EntityTypes.ZOMBIE_HORSE)
                    .height(1.6f).width(1.3965f)
                    .build();
            EntityTypeBase<ChestedHorseEntity> chestedHorseEntityBase = EntityTypeBase.baseInherited(ChestedHorseEntity.class, abstractHorseEntityBase)
                    .addTranslator(EntityDataSerializers.BOOLEAN, (horseEntity, entityMetadata) -> horseEntity.setFlag(EntityFlag.CHESTED, ((DataValue<Boolean>) entityMetadata).value()))
                    .build();
            DONKEY = VanillaEntityType.inherited(ChestedHorseEntity::new, chestedHorseEntityBase)
                    .type(EntityTypes.DONKEY)
                    .height(1.6f).width(1.3965f)
                    .build();
            MULE = VanillaEntityType.inherited(ChestedHorseEntity::new, chestedHorseEntityBase)
                    .type(EntityTypes.MULE)
                    .height(1.6f).width(1.3965f)
                    .build();
            LLAMA = VanillaEntityType.inherited(LlamaEntity::new, chestedHorseEntityBase)
                    .type(EntityTypes.LLAMA)
                    .height(1.87f).width(0.9f)
                    .bedrockDefinition(BedrockEntityDefinitions.LLAMA)
                    .addTranslator(EntityDataSerializers.INT, LlamaEntity::setStrength)
                    .addTranslator(EntityDataSerializers.INT, (entity, entityMetadata) -> entity.getMetadata().put(EntityDataTypes.VARIANT, entityMetadata.value()))
                    .build();
            TRADER_LLAMA = VanillaEntityType.inherited(TraderLlamaEntity::new, LLAMA)
                    .type(EntityTypes.TRADER_LLAMA)
                    .bedrockDefinition(BedrockEntityDefinitions.LLAMA)
                    .build();
        }

        EntityTypeBase<TameableEntity> tameableEntityBase = EntityTypeBase.baseInherited(TameableEntity.class, ageableEntityBase)
                .addTranslator(EntityDataSerializers.BYTE, TameableEntity::setTameableFlags)
                .addTranslator(EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE, TameableEntity::setOwner)
                .build();

        {
            EntityTypeBase<AbstractNautilusEntity> abstractNautilusBase = EntityTypeBase.<AbstractNautilusEntity>baseInherited(null, tameableEntityBase) // No factory, is abstract
                .width(0.95f).height(0.875f)
                .addTranslator(EntityDataSerializers.BOOLEAN, AbstractNautilusEntity::setDashing)
                .build();

            NAUTILUS = VanillaEntityType.inherited(NautilusEntity::new, abstractNautilusBase)
                .type(EntityTypes.NAUTILUS)
                .build();

            ZOMBIE_NAUTILUS = VanillaEntityType.inherited(ZombieNautilusEntity::new, abstractNautilusBase)
                .type(EntityTypes.ZOMBIE_NAUTILUS)
                .property(ZombieNautilusEntity.VARIANT_ENUM_PROPERTY)
                .addTranslator(EntityDataSerializers.ZOMBIE_NAUTILUS_VARIANT, ZombieNautilusEntity::setVariant)
                .build();
        }

        CAT = VanillaEntityType.inherited(CatEntity::new, tameableEntityBase)
                .type(EntityTypes.CAT)
                .height(0.35f).width(0.3f)
                .addTranslator(EntityDataSerializers.CAT_VARIANT, CatEntity::setVariant)
                .addTranslator(EntityDataSerializers.BOOLEAN, CatEntity::setResting)
                .addTranslator(null) // "resting state one" //TODO
                .addTranslator(EntityDataSerializers.INT, CatEntity::setCollarColor)
                .addTranslator(null) // Sound variant
                .build();
        PARROT = VanillaEntityType.inherited(ParrotEntity::new, tameableEntityBase)
                .type(EntityTypes.PARROT)
                .height(0.9f).width(0.5f)
                .addTranslator(EntityDataSerializers.INT, (parrotEntity, entityMetadata) -> parrotEntity.getMetadata().put(EntityDataTypes.VARIANT, entityMetadata.value())) // Parrot color
                .build();
        WOLF = VanillaEntityType.inherited(WolfEntity::new, tameableEntityBase)
                .type(EntityTypes.WOLF)
                .height(0.85f).width(0.6f)
                .property(WolfEntity.SOUND_VARIANT)
                // "Begging" on wiki.vg, "Interested" in Nukkit - the tilt of the head
                .addTranslator(EntityDataSerializers.BOOLEAN, (wolfEntity, entityMetadata) -> wolfEntity.setFlag(EntityFlag.INTERESTED, ((DataValue<Boolean>) entityMetadata).value()))
                .addTranslator(EntityDataSerializers.INT, WolfEntity::setCollarColor)
                .addTranslator(EntityDataSerializers.LONG, WolfEntity::setWolfAngerTime)
                .addTranslator(EntityDataSerializers.WOLF_VARIANT, WolfEntity::setVariant)
                .addTranslator(null) // sound variant; these aren't clientsided anyways... right??
                .build();

        // As of 1.18 these don't track entity data at all
        ENDER_DRAGON_PART = VanillaEntityType.<EnderDragonPartEntity>builder(null)
                .bedrockDefinition(BedrockEntityDefinitions.ARMOR_STAND) // Emulated
                .build(false); // Never sent over the network

        PLAYER_ENTITY_OFFSET = PLAYER.offset();
    }

    private static VanillaEntityType<BoatEntity> buildBoat(EntityTypeBase<BoatEntity> base, EntityType EntityType, BoatEntity.BoatVariant variant) {
        return VanillaEntityType.inherited(context -> new BoatEntity(context, variant), base)
            .type(EntityType)
            .bedrockDefinition(BedrockEntityDefinitions.BOAT)
            .build();
    }

    private static VanillaEntityType<ChestBoatEntity> buildChestBoat(EntityTypeBase<ChestBoatEntity> base, EntityType EntityType, BoatEntity.BoatVariant variant) {
        return VanillaEntityType.inherited(context -> new ChestBoatEntity(context, variant), base)
            .type(EntityType)
            .bedrockDefinition(BedrockEntityDefinitions.CHEST_BOAT)
            .build();
    }

    public static void bootstrap() {
        // noop
    }

    private VanillaEntities() {
    }
}

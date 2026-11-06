/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.item.custom;

import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponent;
import org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponentMap;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserItemDataComponents;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaItemDataComponents;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaKineticWeapon;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.item.components.resolvable.ResolvableComponent;
import org.geysermc.geyser.item.components.resolvable.ResolvableRepairable;
import org.geysermc.geyser.item.components.resolvable.ResolvableToolProperties;
import org.geysermc.geyser.item.exception.InvalidItemComponentsException;
import org.geysermc.geyser.registry.populator.CustomItemRegistryPopulator;
import org.geysermc.geyser.util.MinecraftKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import org.geysermc.geyser.vanilla.protocol.GeyserDataComponents;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.sounds.SoundEvents;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Converts the custom item API's component values to vanilla component values. */
public class ComponentConverters {
    private static final Map<ItemDataComponent<?>, ResolvableComponentConverter<?>> converters = new HashMap<>();

    static {
        registerConverter(JavaItemDataComponents.CONSUMABLE, (itemMap, value) -> {
            net.minecraft.world.item.ItemUseAnimation convertedAnimation = switch (value.animation()) {
                case NONE -> net.minecraft.world.item.ItemUseAnimation.NONE;
                case EAT -> net.minecraft.world.item.ItemUseAnimation.EAT;
                case DRINK -> net.minecraft.world.item.ItemUseAnimation.DRINK;
                case BLOCK -> net.minecraft.world.item.ItemUseAnimation.BLOCK;
                case BOW -> net.minecraft.world.item.ItemUseAnimation.BOW;
                case SPEAR -> net.minecraft.world.item.ItemUseAnimation.SPEAR;
                case CROSSBOW -> net.minecraft.world.item.ItemUseAnimation.CROSSBOW;
                case SPYGLASS -> net.minecraft.world.item.ItemUseAnimation.SPYGLASS;
                case BRUSH -> net.minecraft.world.item.ItemUseAnimation.BRUSH;
            };
            itemMap.put(DataComponents.CONSUMABLE, new Consumable(value.consumeSeconds(), convertedAnimation, SoundEvents.GENERIC_EAT,
                true, List.of()));
        });

        registerConverter(JavaItemDataComponents.EQUIPPABLE, (itemMap, value) -> {
            EquipmentSlot convertedSlot = switch (value.slot()) {
                case HEAD -> EquipmentSlot.HEAD;
                case CHEST -> EquipmentSlot.CHEST;
                case LEGS -> EquipmentSlot.LEGS;
                case FEET -> EquipmentSlot.FEET;
                case BODY -> EquipmentSlot.BODY;
                case SADDLE -> EquipmentSlot.SADDLE;
            };
            itemMap.put(DataComponents.EQUIPPABLE, Equippable.builder(convertedSlot).setDispensable(false).setSwappable(false).setDamageOnHurt(false)
                .setEquipOnInteract(false).setCanBeSheared(false).build());
        });

        registerConverter(JavaItemDataComponents.FOOD, (itemMap, value) -> itemMap.put(DataComponents.FOOD,
            new FoodProperties(value.nutrition(), value.saturation(), value.canAlwaysEat())));

        registerConverter(JavaItemDataComponents.MAX_DAMAGE, DataComponents.MAX_DAMAGE);
        registerConverter(JavaItemDataComponents.MAX_STACK_SIZE, DataComponents.MAX_STACK_SIZE);

        registerConverter(JavaItemDataComponents.USE_COOLDOWN, (itemMap, value) -> itemMap.put(DataComponents.USE_COOLDOWN,
            new UseCooldown(value.seconds(), java.util.Optional.ofNullable(value.cooldownGroup())
                .map(MinecraftKey::identifierToKey).map(MinecraftKey::keyToVanilla))));

        registerConverter(JavaItemDataComponents.ENCHANTABLE, (itemMap, value) -> itemMap.put(DataComponents.ENCHANTABLE, new net.minecraft.world.item.enchantment.Enchantable(value)));

        registerConverter(JavaItemDataComponents.TOOL, (itemMap, value, consumer) -> {
            itemMap.put(DataComponents.TOOL,
                new Tool(List.of(), 1.0F, 1, value.canDestroyBlocksInCreative()));
            consumer.accept(new ResolvableToolProperties(value));
        });

        registerConverter(JavaItemDataComponents.REPAIRABLE, (itemMap, value, consumer) -> {
            // Resolve registry holders after configuration, and custom item registry populator will just use the identifiers of the Holders
            // and pass them to bedrock, if possible. This won't be perfect of course, since identifiers don't have to match bedrock ones
            consumer.accept(new ResolvableRepairable(value));
        });

        registerConverter(JavaItemDataComponents.ENCHANTMENT_GLINT_OVERRIDE, DataComponents.ENCHANTMENT_GLINT_OVERRIDE);

        registerConverter(JavaItemDataComponents.ATTACK_RANGE, (itemMap, value) -> itemMap.put(DataComponents.ATTACK_RANGE,
            new AttackRange(value.minReach(), value.maxReach(), value.minCreativeReach(), value.maxCreativeReach(),
                value.hitboxMargin(), 1.0F)));

        registerConverter(JavaItemDataComponents.KINETIC_WEAPON, (itemMap, value) -> itemMap.put(DataComponents.KINETIC_WEAPON,
            new KineticWeapon(0, value.delayTicks(), java.util.Optional.ofNullable(convertKineticWeaponCondition(value.dismountConditions())),
                java.util.Optional.empty(), java.util.Optional.empty(), 0.0F, 1.0F, java.util.Optional.empty(), java.util.Optional.empty())));

        registerConverter(JavaItemDataComponents.PIERCING_WEAPON, (itemMap, value) -> itemMap.put(DataComponents.PIERCING_WEAPON,
            new PiercingWeapon(false, false, java.util.Optional.empty(), java.util.Optional.empty())));

        // Deprecated since 26.3
        registerConverter(JavaItemDataComponents.SWING_ANIMATION, (itemMap, value) -> itemMap.put(DataComponents.ATTACK_ANIMATION,
            new SwingAnimation(net.minecraft.world.item.SwingAnimationType.WHACK, value.duration())));
        registerConverter(JavaItemDataComponents.ATTACK_ANIMATION, (itemMap, value) -> itemMap.put(DataComponents.ATTACK_ANIMATION,
            new SwingAnimation(net.minecraft.world.item.SwingAnimationType.WHACK, value.duration())));

        registerConverter(JavaItemDataComponents.USE_EFFECTS, (itemMap, value) -> itemMap.put(DataComponents.USE_EFFECTS,
            new UseEffects(false, true, value.speedMultiplier())));

        registerConverter(JavaItemDataComponents.COMPOSTABLE, (itemMap, value) -> itemMap.put(DataComponents.COMPOSTABLE, new net.minecraft.world.item.component.Compostable(new ResolvableInt.Constant(0))));
        registerConverter(JavaItemDataComponents.COOKING_FUEL, (itemMap, value) -> itemMap.put(DataComponents.COOKING_FUEL, new CookingFuel(new ResolvableInt.Constant(0), new ResolvableFloat.Constant(0))));
    }

    private static <T> void registerConverter(ItemDataComponent<T> component, DataComponentType<T> converted) {
        registerConverter(component, (itemMap, value) -> itemMap.put(converted, value));
    }

    private static <T> void registerConverter(ItemDataComponent<T> component, ComponentConverter<T> converter) {
        registerConverter(component, (itemMap, value, resolvableConsumer) -> converter.convertAndPut(itemMap, value));
    }

    private static <T> void registerConverter(ItemDataComponent<T> component, ResolvableComponentConverter<T> converter) {
        converters.put(component, converter);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static GeyserDataComponents convertComponentPatch(ItemDataComponentMap customDefinitionPatch, List<Identifier> customDefinitionRemovals, Consumer<ResolvableComponent<?>> resolvableConsumer) throws InvalidItemComponentsException {
        GeyserDataComponents converted = new GeyserDataComponents();
        for (ItemDataComponent<?> component : customDefinitionPatch.keySet()) {
            if (customDefinitionRemovals.contains(component.identifier())) {
                throw new InvalidItemComponentsException("Component " + component.identifier() + " was present both in the components to add and the components to remove");
            }
            ResolvableComponentConverter converter = converters.get(component);
            if (converter != null) {
                Object value = customDefinitionPatch.get(component);
                converter.convert(converted, value, resolvableConsumer);
            }
        }

        for (Identifier removed : customDefinitionRemovals) {
            DataComponentType<?> component = net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(MinecraftKey.keyToVanilla(MinecraftKey.identifierToKey(removed)));
            if (component != null) {
                converted.put(component, null);
            }
        }
        return converted;
    }

    @FunctionalInterface
    public interface ComponentConverter<T> {

        void convertAndPut(GeyserDataComponents itemMap, T value);
    }

    @FunctionalInterface
    public interface ResolvableComponentConverter<T> {

        void convert(GeyserDataComponents itemMap, T value, Consumer<ResolvableComponent<?>> resolvableConsumer);
    }

    private static KineticWeapon.Condition convertKineticWeaponCondition(JavaKineticWeapon.@Nullable Condition condition) {
        if (condition == null) {
            return null;
        }
        return new KineticWeapon.Condition(condition.maxDurationTicks(), condition.minSpeed(), condition.minRelativeSpeed());
    }
}

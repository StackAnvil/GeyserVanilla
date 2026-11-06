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

package org.geysermc.geyser.inventory;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.item.DyeColor;
import org.geysermc.geyser.inventory.item.Potion;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.components.resolvable.ResolvableComponentGetter;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.java.BuiltInJavaRegistries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.BundleCache;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.geyser.session.cache.tags.GeyserHolderSet;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.geyser.util.ColorUtils;
import net.minecraft.world.item.ItemStack;
import org.geysermc.geyser.vanilla.protocol.VanillaItems;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import org.geysermc.geyser.vanilla.protocol.GeyserDataComponents;
import net.minecraft.world.item.crafting.display.SlotDisplay.Empty;
import net.minecraft.world.item.crafting.display.SlotDisplay.ItemSlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

@Data
public class GeyserItemStack {
    public static final GeyserItemStack EMPTY = new GeyserItemStack(null, Items.AIR_ID, 0, null); // session can be null because air is a vanilla item

    private final ResolvableComponentGetter resolvableComponentGetter;
    private final int javaId;
    private int amount;
    private GeyserDataComponents components;
    private int netId;

    @EqualsAndHashCode.Exclude
    private BundleCache.BundleData bundleData;

    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Exclude
    private Item item;

    private GeyserItemStack(@Nullable GeyserSession session, int javaId, int amount, GeyserDataComponents components) {
        this(session == null ? ResolvableComponentGetter.EMPTY : session.getComponentCache(), javaId, amount, components, 1, null);
    }

    private GeyserItemStack(@NonNull ResolvableComponentGetter componentGetter, int javaId, int amount, GeyserDataComponents components, int netId, BundleCache.BundleData bundleData) {
        this.resolvableComponentGetter = componentGetter;
        this.javaId = javaId;
        this.amount = amount;
        this.components = components == null || components.isEmpty() ? null : components;
        this.netId = netId;
        this.bundleData = bundleData;
    }

    public static @NonNull GeyserItemStack of(@NonNull GeyserSession session, int javaId, int amount) {
        return of(session, javaId, amount, null);
    }

    public static @NonNull GeyserItemStack of(@NonNull GeyserSession session, int javaId, int amount, @Nullable GeyserDataComponents components) {
        return new GeyserItemStack(session, javaId, amount, components);
    }

    public static @NonNull GeyserItemStack from(@NonNull GeyserSession session, @Nullable ItemStack itemStack) {
        return itemStack == null || itemStack.isEmpty() ? EMPTY : new GeyserItemStack(session, VanillaItems.id(itemStack), itemStack.getCount(), GeyserDataComponents.from(itemStack.getComponentsPatch()));
    }

    public static @NonNull GeyserItemStack from(@NonNull GeyserSession session, @NonNull SlotDisplay slotDisplay) {
        return from(session, org.geysermc.geyser.vanilla.protocol.VanillaRecipeDisplays.firstStack(session.vanillaRegistries(), slotDisplay));
    }

    public int getJavaId() {
        return isEmpty() ? 0 : javaId;
    }

    public int getAmount() {
        return isEmpty() ? 0 : amount;
    }

    public boolean is(Item item) {
        return javaId == item.javaId();
    }

    public boolean is(GeyserSession session, Tag<Item> tag) {
        return session.javaRegistries().is(tag, javaId);
    }

    public boolean is(GeyserSession session, net.minecraft.core.HolderSet<net.minecraft.world.item.Item> set) {
        return set != null && session.vanillaRegistries().lookupOrThrow(net.minecraft.core.registries.Registries.ITEM)
            .get(javaId).filter(set::contains).isPresent();
    }



    public boolean isSameItem(GeyserItemStack other) {
        return javaId == other.javaId;
    }

    /**
     * Returns all components of this item - base and additional components sent over the network.
     * These are NOT modifiable! To add components, use {@link #getOrCreateComponents()}.
     *
     * @return the item's base data components and the "additional" ones that may exist.
     */
    public @Nullable GeyserDataComponents getAllComponents() {
        return isEmpty() ? null : asItem().gatherComponents(resolvableComponentGetter, components);
    }

    /**
     * @return the {@link GeyserDataComponents} patch that's sent over the network.
     */
    public @Nullable GeyserDataComponents getComponents() {
        return isEmpty() ? null : components;
    }

    /**
     * @return whether this GeyserItemStack has the given {@code component}.
     */
    public boolean has(DataComponentType<?> component) {
        return !isEmpty() && asItem().gatherComponents(resolvableComponentGetter, components).contains(component);
    }

    /**
     * @return whether this GeyserItemStack has any component modifications additional to
     * the base item components.
     */
    public boolean hasNonBaseComponents() {
        return components != null;
    }

    @NonNull
    public GeyserDataComponents getOrCreateComponents() {
        if (components == null) {
            return components = new GeyserDataComponents();
        }
        return components;
    }

    /**
     * Returns the stored data component for a given {@link DataComponentType}, or null.
     * <p>
     * This method will first check the additional components that may exist,
     * and fallback to the item's default (or, "base") components if need be.
     * @param type the {@link DataComponentType} to query
     * @return the value for said type, or null.
     * @param <T> the value's type
     */
    @Nullable
    public <T> T getComponent(@NonNull DataComponentType<T> type) {
        // A data component patch may contain null values to remove base components
        // e.g. an elytra without the glider component
        if (components != null && components.contains(type)) {
            return components.get(type);
        }

        return asItem().getComponent(resolvableComponentGetter, type);
    }

    public <T> T getComponentElseGet(@NonNull DataComponentType<T> type, Supplier<T> supplier) {
        T value = getComponent(type);
        return value == null ? supplier.get() : value;
    }

    public int getNetId() {
        return isEmpty() ? 0 : netId;
    }

    public int getBundleId() {
        if (isEmpty()) {
            return -1;
        }

        return bundleData == null ? -1 : bundleData.bundleId();
    }

    public void mergeBundleData(GeyserSession session, BundleCache.BundleData oldBundleData) {
        if (oldBundleData != null && this.bundleData != null) {
            // Old bundle; re-use old IDs
            this.bundleData.updateNetIds(session, oldBundleData);
        } else if (this.bundleData != null) {
            // New bundle; allocate new ID
            session.getBundleCache().markNewBundle(this.bundleData);
        }
    }

    public void add(int add) {
        amount += add;
    }

    public void sub(int sub) {
        amount -= sub;
    }

    public ItemStack getItemStack() {
        return getItemStack(amount);
    }

    public ItemStack getItemStack(int newAmount) {
        if (isEmpty()) {
            return ItemStack.EMPTY;
        }
        // Sync our updated bundle data to server, if applicable
        // Not fresh from server? Then we have changes to apply!~
        if (bundleData != null && !bundleData.freshFromServer()) {
            if (!bundleData.contents().isEmpty()) {
                getOrCreateComponents().put(DataComponents.BUNDLE_CONTENTS, bundleData.toComponent());
            } else {
                if (components != null) {
                    // Empty list = no component = should delete
                    components.remove(DataComponents.BUNDLE_CONTENTS);
                }
            }
        }
        return VanillaItems.stack(resolvableComponentGetter.vanillaRegistries(), javaId, newAmount, components);
    }

    public ItemData getItemData(GeyserSession session) {
        if (isEmpty()) {
            return ItemData.AIR;
        }
        ItemData.Builder itemData = ItemTranslator.translateToBedrock(session, javaId, amount, components);
        itemData.netId(getNetId());
        itemData.usingNetId(true);

        return session.getBundleCache().checkForBundle(this, itemData);
    }

    public ItemMapping getMapping(GeyserSession session) {
        return session.getItemMappings().getMapping(this.javaId);
    }

    public SlotDisplay asIngredient() {
        ItemStack itemStack = getItemStack(1);
        if (itemStack.isEmpty()) {
            return Empty.INSTANCE;
        }
        if (itemStack.getComponentsPatch().isEmpty()) {
            return new ItemSlotDisplay(itemStack.typeHolder());
        }
        return new ItemStackSlotDisplay(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(itemStack));
    }

    public int getMaxStackSize() {
        return getComponentElseGet(DataComponents.MAX_STACK_SIZE, () -> 1);
    }

    public int getMaxDamage() {
        return getComponentElseGet(DataComponents.MAX_DAMAGE, () -> 0);
    }

    public int getDamage() {
        // Damage can't be negative
        int damage = Math.max(this.getComponentElseGet(DataComponents.DAMAGE, () -> 0), 0);
        return Math.min(damage, this.getMaxDamage());
    }

    public boolean nextDamageWillBreak() {
        return this.isDamageable() && this.getDamage() >= this.getMaxDamage() - 1;
    }

    public boolean isDamageable() {
        return getComponent(DataComponents.MAX_DAMAGE) != null && getComponent(DataComponents.UNBREAKABLE) == null && getComponent(DataComponents.DAMAGE) != null;
    }

    public boolean isDamaged() {
        return isDamageable() && getDamage() > 0;
    }

    public Item asItem() {
        if (isEmpty()) {
            return Items.AIR;
        }
        if (item == null) {
            return (item = BuiltInJavaRegistries.ITEM.getOrThrow(javaId));
        }
        return item;
    }

    public boolean isEmpty() {
        return amount <= 0 || javaId == Items.AIR_ID;
    }

    public GeyserItemStack copy() {
        return copy(amount);
    }

    public GeyserItemStack copy(int newAmount) {
        return isEmpty() ? EMPTY : new GeyserItemStack(resolvableComponentGetter, javaId, newAmount, components == null ? null : components.copy(), netId, bundleData == null ? null : bundleData.copy());
    }
}

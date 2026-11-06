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

package org.geysermc.geyser.session.cache;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.geysermc.geyser.item.components.resolvable.ResolvableComponent;
import org.geysermc.geyser.item.components.resolvable.ResolvableComponentGetter;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.java.BuiltInJavaRegistries;
import org.geysermc.geyser.vanilla.protocol.GeyserDataComponents;
import org.geysermc.geyser.vanilla.protocol.VanillaCustomItems;

public class ComponentCache implements ResolvableComponentGetter {
    private final org.geysermc.geyser.session.GeyserSession session;
    private final Reference2ObjectMap<Item, GeyserDataComponents> resolvedComponents = new Reference2ObjectOpenHashMap<>();

    public ComponentCache(org.geysermc.geyser.session.GeyserSession session) {
        this.session = session;
    }

    @Override
    public GeyserDataComponents getResolvedComponents(Item item) {
        return resolvedComponents.get(item);
    }

    @Override
    public net.minecraft.core.RegistryAccess vanillaRegistries() {
        return session.vanillaRegistries();
    }

    public void resolveComponents() {
        resolvedComponents.clear();
        for (Item item : BuiltInJavaRegistries.ITEM) {
            var nativeItem = vanillaRegistries().lookupOrThrow(net.minecraft.core.registries.Registries.ITEM)
                .get(org.geysermc.geyser.util.MinecraftKey.keyToVanilla(item.javaKey()));
            GeyserDataComponents resolved = nativeItem.map(holder -> GeyserDataComponents.from(holder.components()))
                .orElse(null);
            boolean custom = nativeItem.filter(holder -> VanillaCustomItems.isCustom(holder.value())).isPresent();
            if (custom) {
                resolved = item.gatherComponents(ResolvableComponentGetter.EMPTY, null).copy();
            }
            if (!item.resolvableComponents().isEmpty()) {
                if (resolved == null) {
                    resolved = item.gatherComponents(ResolvableComponentGetter.EMPTY, null).copy();
                }
                for (ResolvableComponent<?> component : item.resolvableComponents()) {
                    component.resolve(vanillaRegistries(), resolved);
                }
            }
            if (resolved != null) {
                if (custom) {
                    nativeItem.orElseThrow().bindComponents(resolved.toVanilla().split().added());
                }
                resolvedComponents.put(item, resolved.immutableCopy());
            }
        }
    }
}

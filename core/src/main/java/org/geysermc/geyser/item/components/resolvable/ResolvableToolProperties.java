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

package org.geysermc.geyser.item.components.resolvable;

import org.geysermc.geyser.api.item.custom.v2.component.java.JavaTool;
import org.geysermc.geyser.impl.HoldersImpl;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.geyser.registry.java.JavaRegistryProvider;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.Tool;

import java.util.List;

public record ResolvableToolProperties(JavaTool properties) implements ResolvableComponent<Tool> {

    @Override
    public DataComponentType<Tool> type() {
        return DataComponents.TOOL;
    }

    @Override
    public Tool resolve(net.minecraft.core.RegistryAccess registries) {
        List<Tool.Rule> rules = properties.rules().stream()
            .map(rule -> new Tool.Rule(((HoldersImpl) rule.blocks()).toVanilla(registries, net.minecraft.core.registries.Registries.BLOCK), java.util.Optional.ofNullable(rule.speed()), java.util.Optional.empty()))
            .toList();
        return new Tool(rules, properties.defaultMiningSpeed(), 0, properties.canDestroyBlocksInCreative());
    }
}

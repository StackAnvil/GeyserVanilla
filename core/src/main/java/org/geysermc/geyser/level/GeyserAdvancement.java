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

package org.geysermc.geyser.level;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.session.cache.AdvancementsCache;
import org.geysermc.geyser.text.ChatColor;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.AdvancementType;

import java.util.List;

/**
 * Tracks the root of a native advancement for the Bedrock forms.
 */
public class GeyserAdvancement {
    private final AdvancementHolder advancement;
    private String rootId = null;

    public static GeyserAdvancement from(AdvancementHolder advancement) {
        return new GeyserAdvancement(advancement);
    }

    private GeyserAdvancement(AdvancementHolder advancement) {
        this.advancement = advancement;
    }

    @NonNull
    public String getId() {
        return this.advancement.id().toString();
    }

    @NonNull
    public AdvancementRequirements getRequirements() {
        return this.advancement.value().requirements();
    }

    public String getParentId() {
        return this.advancement.value().parent().map(Object::toString).orElse(null);
    }

    public DisplayInfo getDisplayData() {
        return this.advancement.value().display().orElse(null);
    }

    /**
     * @return Purple for challenges and green for normal advancements
     */
    public String getDisplayColor() {
        DisplayInfo displayData = getDisplayData();
        return displayData != null && displayData.type() == AdvancementType.CHALLENGE ? ChatColor.LIGHT_PURPLE : ChatColor.GREEN;
    }

    public @NonNull String getRootId(AdvancementsCache advancementsCache) {
        if (rootId == null) {
            if (getParentId() == null) {
                // We are the root ID
                this.rootId = this.advancement.id().toString();
            } else {
                // Go through our cache, and descend until we find the root ID
                GeyserAdvancement parent = advancementsCache.getStoredAdvancements().get(getParentId());
                if (parent == null) {
                    // Parent doesn't exist, is invalid, or couldn't be found for another reason
                    // So assuming there is no parent and this is the root
                    this.rootId = this.advancement.id().toString();
                } else if (parent.getParentId() == null) {
                    this.rootId = parent.getId();
                } else {
                    this.rootId = parent.getRootId(advancementsCache);
                }
            }
        }
        return rootId;
    }
}

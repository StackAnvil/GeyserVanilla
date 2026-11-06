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

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;

public enum BedrockMapIcon {
    ICON_WHITE_ARROW(MapDecorationTypes.PLAYER, 0),
    ICON_ITEM_FRAME(MapDecorationTypes.FRAME, 7),
    ICON_RED_ARROW(MapDecorationTypes.RED_MARKER, 2),
    ICON_BLUE_ARROW(MapDecorationTypes.BLUE_MARKER, 3),
    ICON_WHITE_CROSS(MapDecorationTypes.TARGET_X, 4, 0, 0, 0), // Doesn't exist on Bedrock, replaced with a black cross
    ICON_RED_POINTER(MapDecorationTypes.TARGET_POINT, 5),
    ICON_WHITE_CIRCLE(MapDecorationTypes.PLAYER_OFF_MAP, 6),
    ICON_SMALL_WHITE_CIRCLE(MapDecorationTypes.PLAYER_OFF_LIMITS, 13),
    ICON_MANSION(MapDecorationTypes.WOODLAND_MANSION, 14),
    ICON_TEMPLE(MapDecorationTypes.OCEAN_MONUMENT, 15),
    ICON_WHITE_BANNER(MapDecorationTypes.WHITE_BANNER, 13, 255, 255, 255),
    ICON_ORANGE_BANNER(MapDecorationTypes.ORANGE_BANNER, 13, 249, 128, 29),
    ICON_MAGENTA_BANNER(MapDecorationTypes.MAGENTA_BANNER, 13, 199, 78, 189),
    ICON_LIGHT_BLUE_BANNER(MapDecorationTypes.LIGHT_BLUE_BANNER, 13, 58, 179, 218),
    ICON_YELLOW_BANNER(MapDecorationTypes.YELLOW_BANNER, 13, 254, 216, 61),
    ICON_LIME_BANNER(MapDecorationTypes.LIME_BANNER, 13, 128, 199, 31),
    ICON_PINK_BANNER(MapDecorationTypes.PINK_BANNER, 13, 243, 139, 170),
    ICON_GRAY_BANNER(MapDecorationTypes.GRAY_BANNER, 13, 71, 79, 82),
    ICON_LIGHT_GRAY_BANNER(MapDecorationTypes.LIGHT_GRAY_BANNER, 13, 157, 157, 151),
    ICON_CYAN_BANNER(MapDecorationTypes.CYAN_BANNER, 13, 22, 156, 156),
    ICON_PURPLE_BANNER(MapDecorationTypes.PURPLE_BANNER, 13, 137, 50, 184),
    ICON_BLUE_BANNER(MapDecorationTypes.BLUE_BANNER, 13, 60, 68, 170),
    ICON_BROWN_BANNER(MapDecorationTypes.BROWN_BANNER, 13, 131, 84, 50),
    ICON_GREEN_BANNER(MapDecorationTypes.GREEN_BANNER, 13, 94, 124, 22),
    ICON_RED_BANNER(MapDecorationTypes.RED_BANNER, 13, 176, 46, 38),
    ICON_BLACK_BANNER(MapDecorationTypes.BLACK_BANNER, 13, 29, 29, 33),
    ICON_TREASURE_MARKER(MapDecorationTypes.RED_X, 4),
    ICON_DESERT_VILLAGE(MapDecorationTypes.DESERT_VILLAGE, 17),
    ICON_PLAINS_VILLAGE(MapDecorationTypes.PLAINS_VILLAGE, 18),
    ICON_SAVANNA_VILLAGE(MapDecorationTypes.SAVANNA_VILLAGE, 19),
    ICON_SNOWY_VILLAGE(MapDecorationTypes.SNOWY_VILLAGE, 20),
    ICON_TAIGA_VILLAGE(MapDecorationTypes.TAIGA_VILLAGE, 21),
    ICON_JUNGLE_TEMPLE(MapDecorationTypes.JUNGLE_TEMPLE, 22),
    ICON_SWAMP_HUT(MapDecorationTypes.SWAMP_HUT, 23),
    ICON_TRIAL_CHAMBERS(MapDecorationTypes.TRIAL_CHAMBERS, 24),
    ICON_ABANDONED_CAMP(MapDecorationTypes.ABANDONED_CAMP, 25),
    ICON_ANCIENT_CITY(MapDecorationTypes.ANCIENT_CITY, 26),
    ICON_DESERT_PYRAMID(MapDecorationTypes.DESERT_PYRAMID, 28),
    ICON_MINESHAFT(MapDecorationTypes.MINESHAFT, 27),
    ICON_OCEAN_RUIN_WARM(MapDecorationTypes.OCEAN_RUIN_WARM, 29);

    private static final BedrockMapIcon[] VALUES = values();

    private final Holder<MapDecorationType> iconType;

    @Getter
    private final int iconID;

    private final int red;
    private final int green;
    private final int blue;

    BedrockMapIcon(Holder<MapDecorationType> iconType, int iconID) {
        this(iconType, iconID, 255, 255, 255);
    }

    BedrockMapIcon(Holder<MapDecorationType> iconType, int iconID, int red, int green, int blue) {
        this.iconType = iconType;
        this.iconID = iconID;

        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    /**
     * Get the BedrockMapIcon for the Java MapIconType
     *
     * @param iconType A MapIconType
     * @return The mapping for a BedrockMapIcon
     */
    public static @Nullable BedrockMapIcon fromType(Holder<MapDecorationType> iconType) {
        for (BedrockMapIcon icon : VALUES) {
            if (icon.iconType.value().equals(iconType.value())) {
                return icon;
            }
        }

        return null;
    }

    /**
     * Get the ARGB value of a BedrockMapIcon
     *
     * @return ARGB as an int
     */
    public int toARGB() {
        final int alpha = 255;

        return ((alpha & 0xFF) << 24) |
                ((red & 0xFF) << 16) |
                ((green & 0xFF) << 8) |
                (blue & 0xFF);
    }
}

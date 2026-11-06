/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

@Getter
public enum EffectType {
    NONE(0, null, 0x000000),

    SPEED(1, MobEffects.SPEED.value(), 0x33EBFF),
    SLOWNESS(2, MobEffects.SLOWNESS.value(), 0x8BAFE0),
    HASTE(3, MobEffects.HASTE.value(), 0xD9C043),
    MINING_FATIGUE(4, MobEffects.MINING_FATIGUE.value(), 0x4A4217),
    STRENGTH(5, MobEffects.STRENGTH.value(), 0xFFC700),
    INSTANT_HEALTH(6, MobEffects.INSTANT_HEALTH.value(), 0xF82423),
    INSTANT_DAMAGE(7, MobEffects.INSTANT_DAMAGE.value(), 0xA9656A),
    JUMP_BOOST(8, MobEffects.JUMP_BOOST.value(), 0xFDFF84),
    NAUSEA(9, MobEffects.NAUSEA.value(), 0x551D4A),
    REGENERATION(10, MobEffects.REGENERATION.value(), 0xCD5CAB),
    RESISTANCE(11, MobEffects.RESISTANCE.value(), 0x9146F0),
    FIRE_RESISTANCE(12, MobEffects.FIRE_RESISTANCE.value(), 0xFF9900),
    WATER_BREATHING(13, MobEffects.WATER_BREATHING.value(), 0x98DAC0),
    INVISIBILITY(14, MobEffects.INVISIBILITY.value(), 0xF6F6F6),
    BLINDNESS(15, MobEffects.BLINDNESS.value(), 0x1F1F23),
    NIGHT_VISION(16, MobEffects.NIGHT_VISION.value(), 0xC2FF66),
    HUNGER(17, MobEffects.HUNGER.value(), 0x587653),
    WEAKNESS(18, MobEffects.WEAKNESS.value(), 0x484D48),
    POISON(19, MobEffects.POISON.value(), 0x87A363),
    WITHER(20, MobEffects.WITHER.value(), 0x736156),
    HEALTH_BOOST(21, MobEffects.HEALTH_BOOST.value(), 0xF87D23),
    ABSORPTION(22, MobEffects.ABSORPTION.value(), 0x2552A5),
    SATURATION(23, MobEffects.SATURATION.value(), 0xF82423),
    LEVITATION(24, MobEffects.LEVITATION.value(), 0xCEFFFF),
    FATAL_POISON(25, null, 0x4E9331), // Bedrock-exclusive effect, maybe useful later if we map to closest color
    CONDUIT_POWER(26, MobEffects.CONDUIT_POWER.value(), 0x1DC2D1),
    SLOW_FALLING(27, MobEffects.SLOW_FALLING.value(), 0xF3CFB9),
    BAD_OMEN(28, MobEffects.BAD_OMEN.value(), 0x0B6138),
    HERO_OF_THE_VILLAGE(29, MobEffects.HERO_OF_THE_VILLAGE.value(), 0x44FF44),
    DARKNESS(30, MobEffects.DARKNESS.value(), 0x292721),
    TRIAL_OMEN(31, MobEffects.TRIAL_OMEN.value(), 0x16A6A6),
    WIND_CHARGED(32, MobEffects.WIND_CHARGED.value(), 0xBDC9FF),
    WEAVING(33, MobEffects.WEAVING.value(), 0x78695A),
    OOZING(34, MobEffects.OOZING.value(), 0x99FFA3),
    INFESTED(35, MobEffects.INFESTED.value(), 0x8C9B8C),
    RAID_OMEN(36, MobEffects.RAID_OMEN.value(), 0xDE4058),
    BREATH_OF_THE_NAUTILUS(37, MobEffects.BREATH_OF_THE_NAUTILUS.value(), 0x00FFEE),

    // All Java-exclusive effects as of 1.16.2
    GLOWING(0, MobEffects.GLOWING.value(), 0x94A061),
    LUCK(0, MobEffects.LUCK.value(), 0x59C106),
    BAD_LUCK(0, MobEffects.UNLUCK.value(), 0xC0A44D),
    DOLPHINS_GRACE(0, MobEffects.DOLPHINS_GRACE.value(), 0x88A3BE);

    private final int bedrockId;
    private final MobEffect javaEffect;
    private final int color;

    EffectType(int bedrockId, MobEffect javaEffect, int color) {
        this.bedrockId = bedrockId;
        this.javaEffect = javaEffect;
        this.color = color;
    }

    public static EffectType fromJavaEffect(MobEffect effect) {
        for (EffectType type : values()) {
            if (type.getJavaEffect() == effect) {
                return type;
            }
        }
        return EffectType.NONE;
    }

    public static EffectType fromColor(int color) {
        color = color & 0xFFFFFF; // Ignore alpha channel
        for (EffectType type : values()) {
            if (type.getColor() == color) {
                return type;
            }
        }
        return EffectType.NONE;
    }
}

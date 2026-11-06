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

package org.geysermc.geyser.inventory.click;

import net.minecraft.world.inventory.ContainerInput;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Click {
    LEFT(ContainerInput.PICKUP, 0),
    LEFT_BUNDLE(ContainerInput.PICKUP, 0),
    LEFT_BUNDLE_FROM_CURSOR(ContainerInput.PICKUP, 0),
    RIGHT(ContainerInput.PICKUP, 1),
    RIGHT_BUNDLE(ContainerInput.PICKUP, 1),
    LEFT_SHIFT(ContainerInput.QUICK_MOVE, 0),
    DROP_ONE(ContainerInput.THROW, 0),
    DROP_ALL(ContainerInput.THROW, 1),
    LEFT_OUTSIDE(ContainerInput.PICKUP, 0),
    RIGHT_OUTSIDE(ContainerInput.PICKUP, 1),
    SWAP_TO_HOTBAR_1(ContainerInput.SWAP, 0),
    SWAP_TO_HOTBAR_2(ContainerInput.SWAP, 1),
    SWAP_TO_HOTBAR_3(ContainerInput.SWAP, 2),
    SWAP_TO_HOTBAR_4(ContainerInput.SWAP, 3),
    SWAP_TO_HOTBAR_5(ContainerInput.SWAP, 4),
    SWAP_TO_HOTBAR_6(ContainerInput.SWAP, 5),
    SWAP_TO_HOTBAR_7(ContainerInput.SWAP, 6),
    SWAP_TO_HOTBAR_8(ContainerInput.SWAP, 7),
    SWAP_TO_HOTBAR_9(ContainerInput.SWAP, 8);

    public static final int OUTSIDE_SLOT = -999;

    public final ContainerInput actionType;
    public final int action;
}

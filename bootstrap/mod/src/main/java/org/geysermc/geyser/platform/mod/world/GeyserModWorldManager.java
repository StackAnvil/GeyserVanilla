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

package org.geysermc.geyser.platform.mod.world;

import net.minecraft.world.level.GameType;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.level.GeyserWorldManager;
import org.geysermc.geyser.network.bedrock.GameProtocol;
import org.geysermc.geyser.platform.mod.host.ModWorldAccess;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Map;
import java.util.function.Consumer;

/** Translates server world data without accepting server-owned Minecraft objects. */
public class GeyserModWorldManager extends GeyserWorldManager {
    private final ModWorldAccess world;

    public GeyserModWorldManager(ModWorldAccess world) {
        this.world = world;
    }

    @Override
    public int getBlockAt(GeyserSession session, int x, int y, int z) {
        if (!hasOwnChunkCache()) {
            return super.getBlockAt(session, x, y, z);
        }
        return world.blockAt(session.getPlayerEntity().uuid(), x, y, z);
    }

    @Override
    public boolean hasOwnChunkCache() {
        return world.protocolVersion() == GameProtocol.getJavaProtocolVersion();
    }

    @Override
    public GameType getDefaultGameMode(GeyserSession session) {
        return GameType.byId(world.defaultGameMode());
    }

    @Override
    public void getDecoratedPotData(GeyserSession session, Vector3i pos, Consumer<Map<String, String>> apply) {
        world.decoratedPotAssets(session.getPlayerEntity().uuid(), pos.getX(), pos.getY(), pos.getZ(), apply);
    }
}

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

package org.geysermc.geyser.translator.protocol.java.level;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData.MapPatch;

import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.MapDecoration;
import org.cloudburstmc.protocol.bedrock.data.MapTrackedObject;
import org.geysermc.geyser.level.BedrockMapIcon;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.util.ARGB;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.DimensionUtils;

import java.util.ArrayList;

@Translator(packet = ClientboundMapItemDataPacket.class)
public class JavaMapItemDataTranslator extends PacketTranslator<ClientboundMapItemDataPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundMapItemDataPacket packet) {
        org.cloudburstmc.protocol.bedrock.packet.ClientboundMapItemDataPacket mapItemDataPacket = new org.cloudburstmc.protocol.bedrock.packet.ClientboundMapItemDataPacket();

        mapItemDataPacket.setUniqueMapId(packet.mapId().id());
        mapItemDataPacket.setDimensionId(DimensionUtils.javaToBedrock(session));
        mapItemDataPacket.setLocked(packet.locked());
        mapItemDataPacket.setOrigin(Vector3i.ZERO); // Required since 1.19.20
        mapItemDataPacket.setScale(packet.scale());
        // Required as of 1.19.50
        mapItemDataPacket.setTrackedEntityIds(new LongArrayList(new long[]{packet.mapId().id()}));

        MapPatch data = packet.colorPatch().orElse(null);
        if (data != null) {
            mapItemDataPacket.setXOffset(data.startX());
            mapItemDataPacket.setYOffset(data.startY());
            mapItemDataPacket.setWidth(data.width());
            mapItemDataPacket.setHeight(data.height());

            // Every int entry is an ARGB color
            int[] colors = new int[data.mapColors().length];

            int idx = 0;
            for (byte colorId : data.mapColors()) {
                colors[idx++] = ARGB.toABGR(MapColor.getColorFromPackedId(colorId & 0xFF));
            }

            mapItemDataPacket.setColors(colors);
        }

        // Bedrock needs an entity id to display an icon
        if (packet.decorations().isPresent()) {
            mapItemDataPacket.setTrackedObjects(new ArrayList<>(packet.decorations().get().size()));
            mapItemDataPacket.setDecorations(new ArrayList<>(packet.decorations().get().size()));

            int id = 0;
            for (var icon : packet.decorations().get()) {
                BedrockMapIcon bedrockMapIcon = BedrockMapIcon.fromType(icon.type());

                if (bedrockMapIcon == null) {
                    continue;
                }

                mapItemDataPacket.getTrackedObjects().add(new MapTrackedObject(id));
                mapItemDataPacket.getDecorations().add(new MapDecoration(bedrockMapIcon.getIconID(), icon.rot(), icon.x(), icon.y(), "", bedrockMapIcon.toARGB()));
                id++;
            }
        }

        // Client will ignore if sent too early
        if (session.isSentSpawnPacket()) {
            session.sendUpstreamPacket(mapItemDataPacket);
        } else {
            session.getUpstream().queuePostStartGamePacket(mapItemDataPacket);
        }
    }
}

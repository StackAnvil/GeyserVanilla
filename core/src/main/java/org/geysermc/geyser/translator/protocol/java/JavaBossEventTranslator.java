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

package org.geysermc.geyser.translator.protocol.java;

import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.network.chat.Component;
import java.util.UUID;
import org.geysermc.geyser.translator.text.MessageTranslator;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.BossBar;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

@Translator(packet = ClientboundBossEventPacket.class)
public class JavaBossEventTranslator extends PacketTranslator<ClientboundBossEventPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundBossEventPacket packet) {
        packet.dispatch(new ClientboundBossEventPacket.Handler() {
            @Override
            public void add(UUID id, Component title, float progress, BossBarColor color, BossBarOverlay overlay,
                            boolean darkenScreen, boolean playMusic, boolean createFog) {
                long entityId = session.getEntityCache().nextEntityId();
                var bossBar = new BossBar(session, entityId, MessageTranslator.fromVanilla(title, session.vanillaRegistries()),
                    progress, bedrockColor(color), 1, 0);
                session.getEntityCache().addBossBar(id, bossBar);
            }

            @Override
            public void remove(UUID id) {
                session.getEntityCache().removeBossBar(id);
            }

            @Override
            public void updateProgress(UUID id, float progress) {
                BossBar bossBar = session.getEntityCache().getBossBar(id);
                if (bossBar != null) {
                    bossBar.updateHealth(progress);
                }
            }

            @Override
            public void updateName(UUID id, Component title) {
                BossBar bossBar = session.getEntityCache().getBossBar(id);
                if (bossBar != null) {
                    bossBar.updateTitle(MessageTranslator.fromVanilla(title, session.vanillaRegistries()));
                }
            }

            @Override
            public void updateStyle(UUID id, BossBarColor color, BossBarOverlay overlay) {
                BossBar bossBar = session.getEntityCache().getBossBar(id);
                if (bossBar != null) {
                    bossBar.updateColor(bedrockColor(color));
                }
            }
        });
    }

    /** Bedrock id 6 is REBECCA_PURPLE, which has no Java equivalent */
    private static int bedrockColor(BossBarColor color) {
        return switch (color) {
            case PINK -> 0;
            case BLUE -> 1;
            case RED -> 2;
            case GREEN -> 3;
            case YELLOW -> 4;
            case PURPLE -> 5;
            case WHITE -> 7;
        };
    }
}

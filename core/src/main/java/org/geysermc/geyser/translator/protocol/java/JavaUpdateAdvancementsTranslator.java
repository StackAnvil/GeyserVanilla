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

import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import org.geysermc.geyser.level.GeyserAdvancement;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.AdvancementsCache;
import org.geysermc.geyser.text.MinecraftLocale;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.text.MessageTranslator;

import java.util.Locale;

@Translator(packet = ClientboundUpdateAdvancementsPacket.class)
public class JavaUpdateAdvancementsTranslator extends PacketTranslator<ClientboundUpdateAdvancementsPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundUpdateAdvancementsPacket packet) {
        AdvancementsCache advancementsCache = session.getAdvancementsCache();
        if (packet.shouldReset()) {
            advancementsCache.getStoredAdvancements().clear();
            advancementsCache.getStoredAdvancementProgress().clear();
        }

        for (var removed : packet.removed()) {
            advancementsCache.getStoredAdvancements().remove(removed.toString());
            advancementsCache.getStoredAdvancementProgress().remove(removed.toString());
        }

        for (var positioned : packet.added()) {
            var holder = positioned.advancement();
            String id = holder.id().toString();
            // The server evaluates visibility, including completed hidden advancements.
            if (holder.value().display().isPresent()) {
                advancementsCache.getStoredAdvancements().put(id, GeyserAdvancement.from(holder));
                var progress = advancementsCache.getStoredAdvancementProgress().get(id);
                if (progress != null) {
                    progress.update(holder.value().requirements());
                }
            } else {
                advancementsCache.getStoredAdvancements().remove(id);
            }
        }

        packet.progress().forEach((id, progress) -> {
            var advancement = advancementsCache.getStoredAdvancements().get(id.toString());
            if (advancement != null) {
                progress.update(advancement.getRequirements());
            }
            advancementsCache.getStoredAdvancementProgress().put(id.toString(), progress);
        });

        sendAdvancementToasts(session, packet);
    }

    /**
     * Handle all advancements progress updates
     */
    public void sendAdvancementToasts(GeyserSession session, ClientboundUpdateAdvancementsPacket packet) {
        if (packet.shouldReset()) {
            // Advancements are being cleared, so they can't be granted
            return;
        }
        for (var advancementId : packet.progress().keySet()) {
            GeyserAdvancement advancement = session.getAdvancementsCache().getStoredAdvancements().get(advancementId.toString());
            if (advancement != null && advancement.getDisplayData() != null) {
                if (advancement.getDisplayData().showToast() && session.getAdvancementsCache().isEarned(advancement)) {
                    String frameType = advancement.getDisplayData().type().toString().toLowerCase(Locale.ROOT);
                    String frameTitle = advancement.getDisplayColor() + MinecraftLocale.getLocaleString("advancements.toast." + frameType, session.locale());
                    String advancementName = MessageTranslator.convertMessage(advancement.getDisplayData().title(), session.locale());

                    session.sendToast(frameTitle, advancementName);
                }
            }
        }
    }
}

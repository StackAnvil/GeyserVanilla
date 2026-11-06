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

package org.geysermc.geyser.translator.protocol.java.scoreboard;

import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.scoreboard.Scoreboard;
import org.geysermc.geyser.scoreboard.ScoreboardUpdater;
import org.geysermc.geyser.scoreboard.Team;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;

import java.util.Arrays;

@Translator(packet = ClientboundSetPlayerTeamPacket.class)
public class JavaSetPlayerTeamTranslator extends PacketTranslator<ClientboundSetPlayerTeamPacket> {
    private final GeyserLogger logger = GeyserImpl.getInstance().getLogger();

    @Override
    public void translate(GeyserSession session, ClientboundSetPlayerTeamPacket packet) {
        var teamAction = packet.getTeamAction();
        var playerAction = packet.getPlayerAction();
        var parameters = packet.getParameters().orElse(null);
        String name = packet.getName();
        String[] players = packet.getPlayers().toArray(String[]::new);
        if (logger.isDebug()) {
            logger.debug("Team packet (" + session.bedrockUsername() + ") " + name + " " + teamAction + " "
                + playerAction + " " + Arrays.toString(players));
        }
        if (teamAction == null && playerAction != null && players.length == 0) {
            return;
        }
        int pps = session.getWorldCache().increaseAndGetScoreboardPacketsPerSecond();
        Scoreboard scoreboard = session.getWorldCache().getScoreboard();
        if (teamAction == ClientboundSetPlayerTeamPacket.Action.ADD) {
            var properties = java.util.Objects.requireNonNull(parameters);
            scoreboard.registerNewTeam(name, players,
                org.geysermc.geyser.translator.text.MessageTranslator.fromVanilla(properties.playerPrefix(), session.vanillaRegistries()),
                org.geysermc.geyser.translator.text.MessageTranslator.fromVanilla(properties.playerSuffix(), session.vanillaRegistries()),
                properties.nameTagVisibility(), properties.color().orElse(null));
        } else {
            Team team = scoreboard.getTeam(name);
            if (team == null) {
                if (logger.isDebug()) {
                    logger.debug("Scoreboard team " + name + " is not registered for update " + teamAction + "/" + playerAction);
                }
                return;
            }
            if (teamAction == ClientboundSetPlayerTeamPacket.Action.REMOVE) {
                scoreboard.removeTeam(name);
            } else if (parameters != null) {
                team.updateProperties(
                    org.geysermc.geyser.translator.text.MessageTranslator.fromVanilla(parameters.playerPrefix(), session.vanillaRegistries()),
                    org.geysermc.geyser.translator.text.MessageTranslator.fromVanilla(parameters.playerSuffix(), session.vanillaRegistries()),
                    parameters.nameTagVisibility(), parameters.color().orElse(null));
            } else if (playerAction == ClientboundSetPlayerTeamPacket.Action.ADD) {
                team.addEntities(players);
            } else if (playerAction == ClientboundSetPlayerTeamPacket.Action.REMOVE) {
                team.removeEntities(players);
            }
        }

        // ScoreboardUpdater will handle it for us if the packets per second
        // (for score and team packets) is higher than the first threshold
        if (pps < ScoreboardUpdater.FIRST_SCORE_PACKETS_PER_SECOND_THRESHOLD) {
            scoreboard.onUpdate();
        }
    }
}

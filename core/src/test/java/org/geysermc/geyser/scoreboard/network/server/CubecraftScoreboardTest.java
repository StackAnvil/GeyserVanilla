/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.scoreboard.network.server;

import org.geysermc.geyser.scoreboard.VanillaScoreboardPackets;

import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacket;
import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacketMatch;
import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNoNextPacket;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.spawnPlayer;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.mockContextScoreboard;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.cloudburstmc.protocol.bedrock.data.ScoreInfo;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.RemoveObjectivePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetDisplayObjectivePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetScorePacket;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetDisplayObjectiveTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetObjectiveTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetPlayerTeamTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetScoreTranslator;
import net.minecraft.world.scores.Team.CollisionRule;
import net.minecraft.world.scores.Team.Visibility;
import net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType;
import net.minecraft.world.scores.TeamColor;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import org.junit.jupiter.api.Test;

public class CubecraftScoreboardTest {
    @Test
    void test() {
        mockContextScoreboard(context -> {
            var setTeamTranslator = new JavaSetPlayerTeamTranslator();
            var setObjectiveTranslator = new JavaSetObjectiveTranslator();
            var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();
            var setScoreTranslator = new JavaSetScoreTranslator();

            // unused
            context.translate(setTeamTranslator, VanillaScoreboardPackets.createTeam("SB_NoName", Component.text("SB_NoName"), Component.empty(), Component.empty(), true, true, Visibility.NEVER, CollisionRule.NEVER, null, new String[0]));
            assertNoNextPacket(context);

            context.translate(
                setObjectiveTranslator,
                VanillaScoreboardPackets.objective("sidebar", ClientboundSetObjectivePacket.METHOD_ADD, Component.text("sidebar"), RenderType.INTEGER, null)
            );
            assertNoNextPacket(context);

            context.translate(
                setDisplayObjectiveTranslator,
                VanillaScoreboardPackets.display(net.minecraft.world.scores.DisplaySlot.SIDEBAR, "sidebar")
            );
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("0");
                packet.setDisplayName("sidebar");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });


            // Now they're going to create a bunch of teams and add players to those teams in a very inefficient way.
            // Presumably this is a leftover from an old system, as these don't seem to do anything but hide their nametags.
            // For which you could just use a single team.


            context.translate(setTeamTranslator, VanillaScoreboardPackets.createTeam("2i|1", Component.text("2i|1"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null, new String[0]));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("2i|1", Component.text("2i|1"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.DARK_GRAY));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("2i|1", Component.text("2i|1"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.DARK_GRAY));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("2i|1", Component.text("2i|1"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.DARK_GRAY));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("2i|1", Component.text("2i|1"), Component.empty(), Component.empty(), false, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.DARK_GRAY));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("2i|1", Component.text("2i|1"), Component.empty(), Component.empty(), false, false, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.DARK_GRAY));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("2i|1", Component.text("2i|1"), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.ALWAYS, TeamColor.DARK_GRAY));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("2i|1", Component.text("2i|1"), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.NEVER, TeamColor.DARK_GRAY));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.addPlayers("2i|1", new String[] { "A_Player" }));

            context.translate(setTeamTranslator, VanillaScoreboardPackets.createTeam("1y|11", Component.text("1y|11"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null, new String[0]));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("1y|11", Component.text("1y|11"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("1y|11", Component.text("1y|11"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("1y|11", Component.text("1y|11"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("1y|11", Component.text("1y|11"), Component.empty(), Component.empty(), false, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("1y|11", Component.text("1y|11"), Component.empty(), Component.empty(), false, false, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("1y|11", Component.text("1y|11"), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("1y|11", Component.text("1y|11"), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.NEVER, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.addPlayers("1y|11", new String[] { "B_Player" }));

            context.translate(setTeamTranslator, VanillaScoreboardPackets.addPlayers("2i|1", new String[] { "C_Player" }));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.addPlayers("2i|1", new String[] { "D_Player" }));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.addPlayers("1y|11", new String[] { "E_Player" }));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.addPlayers("2i|1", new String[] { "F_Player" }));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.addPlayers("2i|1", new String[] { "G_Player" }));

            context.translate(setTeamTranslator, VanillaScoreboardPackets.createTeam("2e|3", Component.text("2e|3"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null, new String[0]));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("2e|3", Component.text("2e|3"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.BLUE));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("2e|3", Component.text("2e|3"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.BLUE));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("2e|3", Component.text("2e|3"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.BLUE));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("2e|3", Component.text("2e|3"), Component.empty(), Component.empty(), false, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.BLUE));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("2e|3", Component.text("2e|3"), Component.empty(), Component.empty(), false, false, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.BLUE));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("2e|3", Component.text("2e|3"), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.ALWAYS, TeamColor.BLUE));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("2e|3", Component.text("2e|3"), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.NEVER, TeamColor.BLUE));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.addPlayers("2e|3", new String[] { "H_Player" }));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.addPlayers("2i|1", new String[] { "I_Player" }));

            context.translate(setTeamTranslator, VanillaScoreboardPackets.createTeam("22|9", Component.text("22|9"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null, new String[0]));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("22|9", Component.text("22|9"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("22|9", Component.text("22|9"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("22|9", Component.text("22|9"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("22|9", Component.text("22|9"), Component.empty(), Component.empty(), false, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("22|9", Component.text("22|9"), Component.empty(), Component.empty(), false, false, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("22|9", Component.text("22|9"), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("22|9", Component.text("22|9"), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.NEVER, TeamColor.AQUA));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.addPlayers("22|9", new String[] { "J_Player" }));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.addPlayers("2i|1", new String[] { "K_Player" }));

            context.translate(setTeamTranslator, VanillaScoreboardPackets.createTeam("26|7", Component.text("26|7"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null, new String[0]));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("26|7", Component.text("26|7"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("26|7", Component.text("26|7"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("26|7", Component.text("26|7"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("26|7", Component.text("26|7"), Component.empty(), Component.empty(), false, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("26|7", Component.text("26|7"), Component.empty(), Component.empty(), false, false, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("26|7", Component.text("26|7"), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.ALWAYS, TeamColor.AQUA));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("26|7", Component.text("26|7"), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.NEVER, TeamColor.AQUA));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.addPlayers("26|7", new String[] { "L_Player" }));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.addPlayers("2e|3", new String[] { "M_Player" }));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.addPlayers("2i|1", new String[] { "N_Player" }));

            context.translate(setTeamTranslator, VanillaScoreboardPackets.createTeam("1u|13", Component.text("1u|13"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null, new String[0]));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("1u|13", Component.text("1u|13"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("1u|13", Component.text("1u|13"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("1u|13", Component.text("1u|13"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("1u|13", Component.text("1u|13"), Component.empty(), Component.empty(), false, true, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("1u|13", Component.text("1u|13"), Component.empty(), Component.empty(), false, false, Visibility.ALWAYS, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("1u|13", Component.text("1u|13"), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.ALWAYS, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.updateTeam("1u|13", Component.text("1u|13"), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.NEVER, TeamColor.LIGHT_PURPLE));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.addPlayers("1u|13", new String[] { "O_Player" }));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.addPlayers("2i|1", new String[] { "P_Player" }));
            context.translate(setTeamTranslator, VanillaScoreboardPackets.addPlayers("2i|1", new String[] { "Q_Player" }));

            assertNoNextPacket(context);


            // Now that those teams are created and people added to it, they set the final sidebar name and add the lines to it.
            // They're also not doing this efficiently, because they don't add the players when the team is created.
            // Instead, they send an additional packet.


            context.translate(
                setObjectiveTranslator,
                VanillaScoreboardPackets.objective("sidebar", ClientboundSetObjectivePacket.METHOD_CHANGE, Component.empty()
                        .append(Component.text(
                            "CubeCraft", NamedTextColor.WHITE, TextDecoration.BOLD)), RenderType.INTEGER, null));
            assertNextPacket(context, () -> {
                var packet = new RemoveObjectivePacket();
                packet.setObjectiveId("0");
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("0");
                packet.setDisplayName("§f§lCubeCraft");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });

            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.createTeam("SB_l-0", Component.text("SB_l-0"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null, new String[0]));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.addPlayers("SB_l-0", new String[] {"§0§0"}));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.updateTeam("SB_l-0", Component.text("SB_l-0"), Component.empty().append(Component.text("", NamedTextColor.BLACK)), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, VanillaScoreboardPackets.score("§0§0", "sidebar", 10));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 10, "§r§0§0§r")));
                return packet;
            });

            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.createTeam("SB_l-1", Component.text("SB_l-1"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null, new String[0]));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.addPlayers("SB_l-1", new String[] {"§0§1"}));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.updateTeam("SB_l-1", Component.text("SB_l-1"), Component.empty()
                        .append(Component.textOfChildren(
                            Component.text("User: ", TextColor.color(0x3aa9ff)),
                            Component.text("Tim203", NamedTextColor.WHITE))), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, VanillaScoreboardPackets.score("§0§1", "sidebar", 9));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(2, "0", 9, "§bUser: §r§fTim203§r§0§1§r")));
                return packet;
            });

            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.createTeam("SB_l-2", Component.text("SB_l-2"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null, new String[0]));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.addPlayers("SB_l-2", new String[] {"§0§2"}));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.updateTeam("SB_l-2", Component.text("SB_l-2"), Component.empty()
                        .append(Component.textOfChildren(
                            Component.text("Rank: ", TextColor.color(0x3aa9ff)),
                            Component.text("\uE1AB ", NamedTextColor.WHITE))), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, VanillaScoreboardPackets.score("§0§2", "sidebar", 8));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(3, "0", 8, "§bRank: §r§f\uE1AB §r§0§2§r")));
                return packet;
            });

            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.createTeam("SB_l-3", Component.text("SB_l-3"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null, new String[0]));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.addPlayers("SB_l-3", new String[] {"§0§3"}));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.updateTeam("SB_l-3", Component.text("SB_l-3"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, VanillaScoreboardPackets.score("§0§3", "sidebar", 7));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(4, "0", 7, "§r§0§3§r")));
                return packet;
            });

            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.createTeam("SB_l-4", Component.text("SB_l-4"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null, new String[0]));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.addPlayers("SB_l-4", new String[] {"§0§4"}));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.updateTeam("SB_l-4", Component.text("SB_l-4"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, VanillaScoreboardPackets.score("§0§4", "sidebar", 6));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(5, "0", 6, "§r§0§4§r")));
                return packet;
            });

            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.createTeam("SB_l-5", Component.text("SB_l-5"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null, new String[0]));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.addPlayers("SB_l-5", new String[] {"§0§5"}));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.updateTeam("SB_l-5", Component.text("SB_l-5"), Component.empty().append(Component.text("", NamedTextColor.DARK_BLUE)), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, VanillaScoreboardPackets.score("§0§5", "sidebar", 5));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(6, "0", 5, "§r§0§5§r")));
                return packet;
            });

            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.createTeam("SB_l-6", Component.text("SB_l-6"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null, new String[0]));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.addPlayers("SB_l-6", new String[] {"§0§6"}));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.updateTeam("SB_l-6", Component.text("SB_l-6"), Component.empty()
                        .append(Component.textOfChildren(
                            Component.text("Lobby: ", TextColor.color(0x3aa9ff)),
                            Component.text("EU #10", NamedTextColor.WHITE))), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, VanillaScoreboardPackets.score("§0§6", "sidebar", 4));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(7, "0", 4, "§bLobby: §r§fEU #10§r§0§6§r")));
                return packet;
            });

            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.createTeam("SB_l-7", Component.text("SB_l-7"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null, new String[0]));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.addPlayers("SB_l-7", new String[] {"§0§7"}));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.updateTeam("SB_l-7", Component.text("SB_l-7"), Component.empty()
                        .append(Component.textOfChildren(
                            Component.text("Players: ", TextColor.color(0x3aa9ff)),
                            Component.text("783", NamedTextColor.WHITE))), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, VanillaScoreboardPackets.score("§0§7", "sidebar", 3));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(8, "0", 3, "§bPlayers: §r§f783§r§0§7§r")));
                return packet;
            });

            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.createTeam("SB_l-8", Component.text("SB_l-8"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null, new String[0]));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.addPlayers("SB_l-8", new String[] {"§0§8"}));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.updateTeam("SB_l-8", Component.text("SB_l-8"), Component.empty().append(Component.text("", NamedTextColor.DARK_GREEN)), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, VanillaScoreboardPackets.score("§0§8", "sidebar", 2));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(9, "0", 2, "§r§0§8§r")));
                return packet;
            });

            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.createTeam("SB_l-9", Component.text("SB_l-9"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null, new String[0]));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.addPlayers("SB_l-9", new String[] {"§0§9"}));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.updateTeam("SB_l-9", Component.text("SB_l-9"), Component.empty().append(Component.text("24/09/24 (g2208)", TextColor.color(0x777777))), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, VanillaScoreboardPackets.score("§0§9", "sidebar", 1));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(10, "0", 1, "§824/09/24 (g2208)§r§0§9§r")));
                return packet;
            });

            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.createTeam("SB_l-10", Component.text("SB_l-10"), Component.empty(), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null, new String[0]));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.addPlayers("SB_l-10", new String[] {"§0§a"}));
            context.translate(
                setTeamTranslator,
                VanillaScoreboardPackets.updateTeam("SB_l-10", Component.text("SB_l-10"), Component.empty().append(Component.text("play.cubecraft.net", NamedTextColor.GOLD)), Component.empty(), true, true, Visibility.ALWAYS, CollisionRule.ALWAYS, null));
            assertNoNextPacket(context);

            context.translate(setScoreTranslator, VanillaScoreboardPackets.score("§0§a", "sidebar", 0));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(11, "0", 0, "§6play.cubecraft.net§r§0§a§r")));
                return packet;
            });

            // after this we get a ClientboundPlayerInfoUpdatePacket with the action UPDATE_DISPLAY_NAME,
            // but that one is only shown in the tablist so we don't have to handle that.
            // And after that we get each player's ClientboundPlayerInfoUpdatePacket with also a UPDATE_DISPLAY_NAME,
            // which is also not interesting for us.
            // CubeCraft seems to use two armor stands per player: 1 for the rank badge and 1 for the player name.
            // So the only thing we have to verify is that the nametag is hidden

            spawnPlayer(context, "A_Player", 2);
            assertNextPacketMatch(context, AddPlayerPacket.class, packet -> {
                assertEquals(2, packet.getRuntimeEntityId());
                assertEquals("", packet.getMetadata().get(EntityDataTypes.NAME));
            });

            spawnPlayer(context, "B_Player", 3);
            assertNextPacketMatch(context, AddPlayerPacket.class, packet -> {
                assertEquals(3, packet.getRuntimeEntityId());
                assertEquals("", packet.getMetadata().get(EntityDataTypes.NAME));
            });

            spawnPlayer(context, "E_Player", 4);
            assertNextPacketMatch(context, AddPlayerPacket.class, packet -> {
                assertEquals(4, packet.getRuntimeEntityId());
                assertEquals("", packet.getMetadata().get(EntityDataTypes.NAME));
            });

            spawnPlayer(context, "H_Player", 5);
            assertNextPacketMatch(context, AddPlayerPacket.class, packet -> {
                assertEquals(5, packet.getRuntimeEntityId());
                assertEquals("", packet.getMetadata().get(EntityDataTypes.NAME));
            });

            spawnPlayer(context, "J_Player", 6);
            assertNextPacketMatch(context, AddPlayerPacket.class, packet -> {
                assertEquals(6, packet.getRuntimeEntityId());
                assertEquals("", packet.getMetadata().get(EntityDataTypes.NAME));
            });

            spawnPlayer(context, "K_Player", 7);
            assertNextPacketMatch(context, AddPlayerPacket.class, packet -> {
                assertEquals(7, packet.getRuntimeEntityId());
                assertEquals("", packet.getMetadata().get(EntityDataTypes.NAME));
            });

            spawnPlayer(context, "L_Player", 8);
            assertNextPacketMatch(context, AddPlayerPacket.class, packet -> {
                assertEquals(8, packet.getRuntimeEntityId());
                assertEquals("", packet.getMetadata().get(EntityDataTypes.NAME));
            });

            spawnPlayer(context, "O_Player", 9);
            assertNextPacketMatch(context, AddPlayerPacket.class, packet -> {
                assertEquals(9, packet.getRuntimeEntityId());
                assertEquals("", packet.getMetadata().get(EntityDataTypes.NAME));
            });
        });
    }
}

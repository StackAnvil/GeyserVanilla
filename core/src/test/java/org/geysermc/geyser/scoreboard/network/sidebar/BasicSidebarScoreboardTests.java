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

package org.geysermc.geyser.scoreboard.network.sidebar;

import org.geysermc.geyser.scoreboard.VanillaScoreboardPackets;

import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacket;
import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNoNextPacket;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.mockContextScoreboard;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.spawnPlayerSilently;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.cloudburstmc.protocol.bedrock.data.ScoreInfo;
import org.cloudburstmc.protocol.bedrock.packet.RemoveObjectivePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetDisplayObjectivePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetScorePacket;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetDisplayObjectiveTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetObjectiveTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetPlayerTeamTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetScoreTranslator;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.world.scores.Team.CollisionRule;
import net.minecraft.world.scores.Team.Visibility;
import net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType;
import net.minecraft.world.scores.TeamColor;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import org.junit.jupiter.api.Test;

/*
Identical to playerlist
 */
public class BasicSidebarScoreboardTests {
    @Test
    void displayAndRemove() {
        mockContextScoreboard(context -> {
            var setObjectiveTranslator = new JavaSetObjectiveTranslator();
            var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();

            context.translate(
                setObjectiveTranslator,
                VanillaScoreboardPackets.objective("objective", ClientboundSetObjectivePacket.METHOD_ADD, Component.text("objective"), RenderType.INTEGER, null)
            );
            assertNoNextPacket(context);

            context.translate(
                setDisplayObjectiveTranslator,
                VanillaScoreboardPackets.display(net.minecraft.world.scores.DisplaySlot.SIDEBAR, "objective")
            );
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("0");
                packet.setDisplayName("objective");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });

            context.translate(
                setDisplayObjectiveTranslator,
                VanillaScoreboardPackets.display(net.minecraft.world.scores.DisplaySlot.SIDEBAR, "")
            );
            assertNextPacket(context, () -> {
                var packet = new RemoveObjectivePacket();
                packet.setObjectiveId("0");
                return packet;
            });
        });
    }

    @Test
    void displayNameColors() {
        mockContextScoreboard(context -> {
            var setObjectiveTranslator = new JavaSetObjectiveTranslator();
            var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();

            context.translate(
                setObjectiveTranslator,
                VanillaScoreboardPackets.objective("objective", ClientboundSetObjectivePacket.METHOD_ADD, Component.text("objective", NamedTextColor.AQUA, TextDecoration.BOLD), RenderType.INTEGER, null)
            );
            assertNoNextPacket(context);

            context.translate(
                setDisplayObjectiveTranslator,
                VanillaScoreboardPackets.display(net.minecraft.world.scores.DisplaySlot.SIDEBAR, "objective")
            );
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("0");
                packet.setDisplayName("§b§lobjective");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });
        });
    }

    @Test
    void numberFormatFixed() {
        mockContextScoreboard(context -> {
            var setObjectiveTranslator = new JavaSetObjectiveTranslator();
            var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();
            var setScoreTranslator = new JavaSetScoreTranslator();

            context.translate(
                setObjectiveTranslator,
                VanillaScoreboardPackets.objective("objective", ClientboundSetObjectivePacket.METHOD_ADD, Component.text("objective", NamedTextColor.AQUA, TextDecoration.BOLD), RenderType.INTEGER, VanillaScoreboardPackets.fixed(Component.text("format", NamedTextColor.AQUA)))
            );
            assertNoNextPacket(context);

            context.translate(
                setDisplayObjectiveTranslator,
                VanillaScoreboardPackets.display(net.minecraft.world.scores.DisplaySlot.SIDEBAR, "objective")
            );
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("0");
                packet.setDisplayName("§b§lobjective");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });

            context.translate(setScoreTranslator, VanillaScoreboardPackets.score("Tim203", "objective", 3));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 3, "Tim203 §r§bformat")));
                return packet;
            });
        });
    }

    @Test
    void override() {
        mockContextScoreboard(context -> {
            var setObjectiveTranslator = new JavaSetObjectiveTranslator();
            var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();
            var setScoreTranslator = new JavaSetScoreTranslator();

            context.translate(
                setObjectiveTranslator,
                VanillaScoreboardPackets.objective("objective1", ClientboundSetObjectivePacket.METHOD_ADD, Component.text("objective1"), RenderType.INTEGER, null)
            );

            context.translate(
                setObjectiveTranslator,
                VanillaScoreboardPackets.objective("objective2", ClientboundSetObjectivePacket.METHOD_ADD, Component.text("objective2"), RenderType.INTEGER, null)
            );

            context.translate(setScoreTranslator, VanillaScoreboardPackets.score("Tim203", "objective1", 1));
            context.translate(setScoreTranslator, VanillaScoreboardPackets.score("Tim203", "objective2", 2));
            assertNoNextPacket(context);


            context.translate(
                setDisplayObjectiveTranslator,
                VanillaScoreboardPackets.display(net.minecraft.world.scores.DisplaySlot.SIDEBAR, "objective2")
            );

            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("0");
                packet.setDisplayName("objective2");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 2, "Tim203")));
                return packet;
            });
            assertNoNextPacket(context);


            context.translate(
                setDisplayObjectiveTranslator,
                VanillaScoreboardPackets.display(net.minecraft.world.scores.DisplaySlot.SIDEBAR, "objective1")
            );

            assertNextPacket(context, () -> {
                var packet = new RemoveObjectivePacket();
                packet.setObjectiveId("0");
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("2");
                packet.setDisplayName("objective1");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(3, "2", 1, "Tim203")));
                return packet;
            });
        });
    }

    @Test
    void scoreShouldUpdateWhenTeamChanges() {
        mockContextScoreboard(context -> {
            var setObjectiveTranslator = new JavaSetObjectiveTranslator();
            var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();
            var setPlayerTeamTranslator = new JavaSetPlayerTeamTranslator();
            var setScoreTranslator = new JavaSetScoreTranslator();

            context.translate(
                setObjectiveTranslator,
                VanillaScoreboardPackets.objective("objective", ClientboundSetObjectivePacket.METHOD_ADD, Component.text("objective"), RenderType.INTEGER, null)
            );
            context.translate(setScoreTranslator, VanillaScoreboardPackets.score("team-player", "objective", 1));
            assertNoNextPacket(context);

            context.translate(
                setDisplayObjectiveTranslator,
                VanillaScoreboardPackets.display(net.minecraft.world.scores.DisplaySlot.SIDEBAR, "objective")
            );
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("0");
                packet.setDisplayName("objective");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 1, "team-player")));
                return packet;
            });
            assertNoNextPacket(context);

            context.translate(
                setPlayerTeamTranslator,
                VanillaScoreboardPackets.createTeam("team", Component.empty(), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.NEVER, null, new String[] {"team-player"})
            );
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.REMOVE);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 0)));
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 1, "§rteam-player§r")));
                return packet;
            });
            assertNoNextPacket(context);

            context.translate(
                setPlayerTeamTranslator,
                VanillaScoreboardPackets.updateTeam("team", Component.empty(), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.NEVER, TeamColor.BLACK)
            );
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.REMOVE);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 0)));
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 1, "§0§r§0team-player§r§0")));
                return packet;
            });
            assertNoNextPacket(context);

            context.translate(
                setPlayerTeamTranslator,
                VanillaScoreboardPackets.updateTeam("team", Component.empty(), Component.text("hi"), Component.empty(), false, false, Visibility.NEVER, CollisionRule.NEVER, TeamColor.BLACK)
            );
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.REMOVE);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 0)));
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 1, "§0hi§r§0team-player§r§0")));
                return packet;
            });
            assertNoNextPacket(context);
        });
    }

    @Test
    void updateScoresWhenObjectiveNumberFormatChanges() {
        mockContextScoreboard(context -> {
            var setObjectiveTranslator = new JavaSetObjectiveTranslator();
            var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();
            var setScoreTranslator = new JavaSetScoreTranslator();

            spawnPlayerSilently(context, "player1", 2);

            context.translate(
                setObjectiveTranslator,
                VanillaScoreboardPackets.objective("objective1", ClientboundSetObjectivePacket.METHOD_ADD, Component.text("objective1"), RenderType.INTEGER, null)
            );
            context.translate(setScoreTranslator, VanillaScoreboardPackets.score("player1", "objective1", 1));
            assertNoNextPacket(context);

            context.translate(
                setDisplayObjectiveTranslator,
                VanillaScoreboardPackets.display(net.minecraft.world.scores.DisplaySlot.SIDEBAR, "objective1")
            );
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("0");
                packet.setDisplayName("objective1");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 1, "player1")));
                return packet;
            });
            assertNoNextPacket(context);

            context.translate(
                setScoreTranslator,
                VanillaScoreboardPackets.score("player2", "objective1", 2, null, VanillaScoreboardPackets.fixed(Component.text("abc")))
            );
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(2, "0", 2, "player2 §rabc")));
                return packet;
            });

            spawnPlayerSilently(context, "player2", 3);

            // First check just updating the number format
            context.translate(
                setObjectiveTranslator,
                VanillaScoreboardPackets.objective("objective1", ClientboundSetObjectivePacket.METHOD_CHANGE, Component.text("objective1"), RenderType.INTEGER, VanillaScoreboardPackets.fixed(Component.text("hi")))
            );
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.REMOVE);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 0)));
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 1, "player1 §rhi")));
                return packet;
            });
            assertNoNextPacket(context);

            // Ensure state is cleanly reset for the propagated number format score, by updating a score that has its own number format
            context.translate(
                setScoreTranslator,
                VanillaScoreboardPackets.score("player2", "objective1", 3, null, VanillaScoreboardPackets.fixed(Component.text("abc")))
            );
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(2, "0", 3, "player2 §rabc")));
                return packet;
            });
            assertNoNextPacket(context);

            // But also check updating both the objective name and format
            context.translate(
                setObjectiveTranslator,
                VanillaScoreboardPackets.objective("objective1", ClientboundSetObjectivePacket.METHOD_CHANGE, Component.text("obj"), RenderType.INTEGER, VanillaScoreboardPackets.fixed(Component.text("hello")))
            );
            assertNextPacket(context, () -> {
                var packet = new RemoveObjectivePacket();
                packet.setObjectiveId("0");
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("0");
                packet.setDisplayName("obj");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(
                    new ScoreInfo(2, "0", 3, "player2 §rabc"),
                    new ScoreInfo(1, "0", 1, "player1 §rhello")
                ));
                return packet;
            });
            assertNoNextPacket(context);

            // Ensure state is cleanly reset for the propagated number format score, by updating a score that has its own number format
            context.translate(
                setScoreTranslator,
                VanillaScoreboardPackets.score("player2", "objective1", 4, null, VanillaScoreboardPackets.fixed(Component.text("abc")))
            );
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(2, "0", 4, "player2 §rabc")));
                return packet;
            });
            assertNoNextPacket(context);
        });
    }
}

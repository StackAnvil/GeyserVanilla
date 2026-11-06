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

package org.geysermc.geyser.scoreboard.network.sidebar;

import org.geysermc.geyser.scoreboard.VanillaScoreboardPackets;

import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacket;
import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNoNextPacket;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.mockContextScoreboard;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.protocol.bedrock.data.ScoreInfo;
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

public class SidebarTeamColorTests {
    /**
     * There used to be an issue where if the session player was on a team without color
     * then a display slot that has no team color (e.g. below name) could be used instead.
     */
    @Test
    void sidebarColorDetectionIssue() {
        mockContextScoreboard(context -> {
            var setObjectiveTranslator = new JavaSetObjectiveTranslator();
            var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();
            var setPlayerTeamTranslator = new JavaSetPlayerTeamTranslator();

            context.translate(
                setPlayerTeamTranslator,
                VanillaScoreboardPackets.createTeam("team", Component.empty(), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.NEVER, null, new String[] {"Tim203"})
            );

            context.translate(
                setObjectiveTranslator,
                VanillaScoreboardPackets.objective("objective1", ClientboundSetObjectivePacket.METHOD_ADD, Component.text("objective"), RenderType.INTEGER, null)
            );

            context.translate(
                setDisplayObjectiveTranslator,
                VanillaScoreboardPackets.display(net.minecraft.world.scores.DisplaySlot.BELOW_NAME, "objective1")
            );

            context.translate(
                setObjectiveTranslator,
                VanillaScoreboardPackets.objective("objective2", ClientboundSetObjectivePacket.METHOD_ADD, Component.text("objective"), RenderType.INTEGER, null)
            );
            assertNoNextPacket(context);

            context.translate(
                setDisplayObjectiveTranslator,
                VanillaScoreboardPackets.display(net.minecraft.world.scores.DisplaySlot.SIDEBAR, "objective2")
            );
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("1");
                packet.setDisplayName("objective");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });
        });
    }

    /**
     * Bedrock doesn't have the concept of specific sidebars based on which color the player's team has.
     * We simulate this, so we have to reset the sidebar when the player has switched, even if from Java's perspective nothing has changed.
     */
    @Test
    void sidebarColorSwitchStateShouldReset() {
        mockContextScoreboard(context -> {
            var setObjectiveTranslator = new JavaSetObjectiveTranslator();
            var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();
            var setPlayerTeamTranslator = new JavaSetPlayerTeamTranslator();
            var setScoreTranslator = new JavaSetScoreTranslator();

            context.translate(
                setPlayerTeamTranslator,
                VanillaScoreboardPackets.createTeam("team", Component.empty(), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.NEVER, TeamColor.GREEN, new String[] {"Tim203"})
            );

            // Create the first sidebar team, should work just fine.

            context.translate(
                setObjectiveTranslator,
                VanillaScoreboardPackets.objective("green", ClientboundSetObjectivePacket.METHOD_ADD, Component.text("green"), RenderType.INTEGER, null)
            );
            context.translate(setScoreTranslator, VanillaScoreboardPackets.score("abc", "green", 1));
            context.translate(setDisplayObjectiveTranslator, VanillaScoreboardPackets.display(net.minecraft.world.scores.DisplaySlot.TEAM_GREEN, "green"));
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("0");
                packet.setDisplayName("green");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 1, "abc")));
                return packet;
            });
            assertNoNextPacket(context);

            context.translate(
                setObjectiveTranslator,
                VanillaScoreboardPackets.objective("blue", ClientboundSetObjectivePacket.METHOD_ADD, Component.text("blue"), RenderType.INTEGER, null)
            );
            context.translate(setScoreTranslator, VanillaScoreboardPackets.score("abc", "blue", 2));
            context.translate(setDisplayObjectiveTranslator, VanillaScoreboardPackets.display(net.minecraft.world.scores.DisplaySlot.TEAM_BLUE, "blue"));
            assertNoNextPacket(context);

            // Update the team color the user is in to point it to another sidebar.
            // The old objective should be removed as sidebar and the sidebar with the new color should be displayed cleanly.

            context.translate(
                setPlayerTeamTranslator,
                VanillaScoreboardPackets.updateTeam("team", Component.empty(), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.NEVER, TeamColor.BLUE)
            );
            assertNextPacket(context, () -> {
                var packet = new RemoveObjectivePacket();
                packet.setObjectiveId("0");
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("2");
                packet.setDisplayName("blue");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(3, "2", 2, "abc")));
                return packet;
            });
            assertNoNextPacket(context);

            // Here's what we have to gate, when the user switches back to the old color objective that's been displayed before,
            // the state of that objective needs to be reset too. It's no longer an UPDATE, but an ADD for the user.

            context.translate(
                setPlayerTeamTranslator,
                VanillaScoreboardPackets.updateTeam("team", Component.empty(), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.NEVER, TeamColor.GREEN)
            );
            assertNextPacket(context, () -> {
                var packet = new RemoveObjectivePacket();
                packet.setObjectiveId("2");
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("0");
                packet.setDisplayName("green");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 1, "abc")));
                return packet;
            });
            assertNoNextPacket(context);

            // While we're at it, lets also do the cases of switching from- and to the generic sidebar slot.

            context.translate(
                setObjectiveTranslator,
                VanillaScoreboardPackets.objective("generic", ClientboundSetObjectivePacket.METHOD_ADD, Component.text("generic"), RenderType.INTEGER, null)
            );
            context.translate(setScoreTranslator, VanillaScoreboardPackets.score("abc", "generic", 3));
            context.translate(setDisplayObjectiveTranslator, VanillaScoreboardPackets.display(net.minecraft.world.scores.DisplaySlot.SIDEBAR, "generic"));
            assertNoNextPacket(context);

            context.translate(
                setPlayerTeamTranslator,
                VanillaScoreboardPackets.updateTeam("team", Component.empty(), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.NEVER, null)
            );
            assertNextPacket(context, () -> {
                var packet = new RemoveObjectivePacket();
                packet.setObjectiveId("0");
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("4");
                packet.setDisplayName("generic");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(5, "4", 3, "abc")));
                return packet;
            });
            assertNoNextPacket(context);

            context.translate(
                setPlayerTeamTranslator,
                VanillaScoreboardPackets.updateTeam("team", Component.empty(), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.NEVER, TeamColor.BLUE)
            );
            assertNextPacket(context, () -> {
                var packet = new RemoveObjectivePacket();
                packet.setObjectiveId("4");
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("2");
                packet.setDisplayName("blue");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(3, "2", 2, "abc")));
                return packet;
            });
            assertNoNextPacket(context);

            // While we're at it, also do the cases of switching from- and to a team that has a color but no specific sidebar for it.

            context.translate(
                setPlayerTeamTranslator,
                VanillaScoreboardPackets.updateTeam("team", Component.empty(), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.NEVER, TeamColor.RED)
            );
            assertNextPacket(context, () -> {
                var packet = new RemoveObjectivePacket();
                packet.setObjectiveId("2");
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("4");
                packet.setDisplayName("generic");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(5, "4", 3, "abc")));
                return packet;
            });
            assertNoNextPacket(context);

            context.translate(
                setPlayerTeamTranslator,
                VanillaScoreboardPackets.updateTeam("team", Component.empty(), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.NEVER, TeamColor.GREEN)
            );
            assertNextPacket(context, () -> {
                var packet = new RemoveObjectivePacket();
                packet.setObjectiveId("4");
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("0");
                packet.setDisplayName("green");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 1, "abc")));
                return packet;
            });
            assertNoNextPacket(context);
        });
    }
}

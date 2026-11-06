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

package org.geysermc.geyser.scoreboard.network;

import org.geysermc.geyser.scoreboard.VanillaScoreboardPackets;

import net.minecraft.network.syncher.SynchedEntityData.DataValue;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.protocol.bedrock.data.ScoreInfo;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket;
import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetDisplayObjectivePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetScorePacket;
import org.geysermc.geyser.GeyserResources;
import org.geysermc.geyser.entity.type.living.monster.EnderDragonPartEntity;
import org.geysermc.geyser.registry.java.BuiltInJavaRegistries;
import org.geysermc.geyser.translator.protocol.java.entity.JavaAddEntityTranslator;
import org.geysermc.geyser.translator.protocol.java.entity.JavaSetEntityDataTranslator;
import org.geysermc.geyser.translator.protocol.java.entity.player.JavaPlayerInfoUpdateTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaResetScorePacket;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetDisplayObjectiveTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetObjectiveTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetPlayerTeamTranslator;
import org.geysermc.geyser.translator.protocol.java.scoreboard.JavaSetScoreTranslator;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.scores.Team.CollisionRule;
import net.minecraft.world.scores.Team.Visibility;
import net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType;
import net.minecraft.world.scores.TeamColor;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacket;
import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacketMatch;
import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNextPacketType;
import static org.geysermc.geyser.scoreboard.network.util.AssertUtils.assertNoNextPacket;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContext.mockContext;
import static org.geysermc.geyser.scoreboard.network.util.GeyserMockContextScoreboard.mockContextScoreboard;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for issues reported on GitHub.
 */
public class ScoreboardIssueTests {

    @BeforeAll
    static void bootstrapJavaRegistries() {
        // Needed because some tests here require a proper ENTITY_TYPE registry.
        // This is a temp fix: ideally we wouldn't have a single globally initialised registry for all tests
        // (because some future tests may want to test registering to a registry).
        mockContext(() -> {
            org.geysermc.geyser.vanilla.protocol.VanillaBootstrap.initialize();
            BuiltInJavaRegistries.bootstrap();
            BuiltInJavaRegistries.freeze();
        });
    }

    /**
     * Test for <a href="https://github.com/GeyserMC/Geyser/issues/5078">#5078</a>
     */
    @Test
    void entityWithoutType() {
        // dragon entity parts are an entity in Geyser, but do not have an entity type
        mockContextScoreboard(context -> {
            // EntityUtils#translatedEntityName used to not take null EntityType's into account,
            // so it used to throw an exception
            assertDoesNotThrow(() -> {
                // dragon entity parts are not spawned using a packet, so we manually create an instance
                var dragonHeadPart = new EnderDragonPartEntity(context.session(), 2, 2, 1, 1);

                String displayName = dragonHeadPart.getDisplayName(true);
                assertEquals("entity.unregistered_sadface", displayName);
            });
        });
    }

    /**
     * Test for <a href="https://github.com/GeyserMC/Geyser/issues/5089">#5089</a>.
     * It follows the reproduction steps with all the packets it sends along its way.
     * Tested with the 2.0.0-SNAPSHOT version.
     * Note that this exact issue is actually 2 issues:
     * <ul>
     * <li>
     *     An issue caused by remainders of code that was part of the initial PR that added support for players.
     *     The code is now more streamlined.
     * </li>
     * <li>Armor stands are excluded from team visibility checks (the only living entity)</li>
     * </ul>
     */
    @Test
    void nameNotUpdating() {
        mockContextScoreboard(context -> {
            var playerInfoUpdateTranslator = new JavaPlayerInfoUpdateTranslator();
            var setPlayerTeamTranslator = new JavaSetPlayerTeamTranslator();
            var addEntityTranslator = new JavaAddEntityTranslator();
            var setEntityDataTranslator = new JavaSetEntityDataTranslator();


            // first command, create NPC


            var npcUuid = UUID.fromString("b0eb01d7-52c9-4730-9fd3-2c03fcb00d6e");
            context.translate(
                playerInfoUpdateTranslator,
                VanillaScoreboardPackets.playerInfo(
                    EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED),
                    List.of(new ClientboundPlayerInfoUpdatePacket.Entry(npcUuid, new GameProfile(npcUuid, "1297"), false, 0, GameType.SURVIVAL, null, false, 0, null))));
            assertNoNextPacket(context);

            context.translate(
                setPlayerTeamTranslator,
                VanillaScoreboardPackets.createTeam("npc_team_1297", Component.empty(), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.NEVER, TeamColor.WHITE, new String[0])
            );
            context.translate(
                setPlayerTeamTranslator,
                VanillaScoreboardPackets.addPlayers("npc_team_1297", new String[]{ "1297" }));

            context.translate(addEntityTranslator, new ClientboundAddEntityPacket(1297, npcUuid, 1, 2, 3, 5, 4, EntityTypes.PLAYER, 0, net.minecraft.world.phys.Vec3.ZERO, 6));
            // then it updates the displayed skin parts, which isn't relevant for us

            assertNextPacketMatch(context, AddPlayerPacket.class, packet -> {
                assertEquals(3, packet.getRuntimeEntityId());
                assertEquals(npcUuid, packet.getUuid());
                assertEquals("1297", packet.getUsername());
                assertEquals((byte) 1, packet.getMetadata().get(EntityDataTypes.NAMETAG_ALWAYS_SHOW));
                assertEquals("", packet.getMetadata().get(EntityDataTypes.NAME));
            });
            assertNoNextPacket(context);


            // second command, create hologram


            var hologramUuid = UUID.fromString("b1586291-5f68-44dc-847d-6c123c5b8cbf");
            context.translate(
                addEntityTranslator,
                new ClientboundAddEntityPacket(1298, hologramUuid, 6, 5, 4, 2, 3, EntityTypes.ARMOR_STAND, 0, net.minecraft.world.phys.Vec3.ZERO, 1));

            assertNextPacketMatch(context, AddEntityPacket.class, packet -> {
                assertEquals(4, packet.getRuntimeEntityId());
                assertEquals("minecraft:armor_stand", packet.getIdentifier());
            });

            // metadata set: invisible, custom name, custom name visible
            context.translate(setEntityDataTranslator, new ClientboundSetEntityDataPacket(1298, List.of(
                new DataValue<>(0, EntityDataSerializers.BYTE, (byte) 0x20),
                new DataValue<>(2, EntityDataSerializers.OPTIONAL_COMPONENT, Optional.of(net.minecraft.network.chat.Component.literal("tesss"))),
                new DataValue<>(3, EntityDataSerializers.BOOLEAN, true)
            )));

            assertNextPacketMatch(context, SetEntityDataPacket.class, packet -> {
                assertEquals(4, packet.getRuntimeEntityId());
                var metadata = packet.getMetadata();
                assertEquals(0.0f, metadata.get(EntityDataTypes.SCALE));
                assertEquals("tesss", metadata.get(EntityDataTypes.NAME));
                assertEquals((byte) 1, metadata.get(EntityDataTypes.NAMETAG_ALWAYS_SHOW));
            });
            // because the armor stand turned invisible and has a nametag (nametag is hidden when invisible)
            assertNextPacketType(context, MoveEntityAbsolutePacket.class);

            context.translate(
                setPlayerTeamTranslator,
                VanillaScoreboardPackets.createTeam("npc_team_1298", Component.empty(), Component.empty(), Component.empty(), false, false, Visibility.NEVER, CollisionRule.NEVER, TeamColor.WHITE, new String[0])
            );
            context.translate(
                setPlayerTeamTranslator,
                VanillaScoreboardPackets.addPlayers("npc_team_1298", new String[]{ hologramUuid.toString() }));

            assertNextPacket(context, () -> {
                var packet = new SetEntityDataPacket();
                packet.getMetadata().put(EntityDataTypes.NAME, "§f§r§ftesss§r§f");
                packet.setRuntimeEntityId(4);
                return packet;
            });
        });
    }

    /**
     * Test for <a href="https://github.com/GeyserMC/Geyser/issues/6609">#6609</a>.
     *
     * <p>There used to be an issue where a removed (reset) score got stuck in a cache. So when a score with
     * an identical name was added back immediately after, the removed score resurfaced and the score was not
     * send to the Bedrock client because that old score didn't have an update.</p>
     */
    @Test
    void resetThenReaddSameLine() {
        mockContextScoreboard(context -> {
            var setObjectiveTranslator = new JavaSetObjectiveTranslator();
            var setDisplayObjectiveTranslator = new JavaSetDisplayObjectiveTranslator();
            var setScoreTranslator = new JavaSetScoreTranslator();
            var resetScoreTranslator = new JavaResetScorePacket();

            context.translate(setObjectiveTranslator, VanillaScoreboardPackets.objective("objective", ClientboundSetObjectivePacket.METHOD_ADD, Component.text("objective"), RenderType.INTEGER, null));
            context.translate(setDisplayObjectiveTranslator,
                VanillaScoreboardPackets.display(net.minecraft.world.scores.DisplaySlot.SIDEBAR, "objective"));
            assertNextPacket(context, () -> {
                var packet = new SetDisplayObjectivePacket();
                packet.setObjectiveId("0");
                packet.setDisplayName("objective");
                packet.setCriteria("dummy");
                packet.setDisplaySlot("sidebar");
                packet.setSortOrder(1);
                return packet;
            });

            context.translate(setScoreTranslator, VanillaScoreboardPackets.score("line1", "objective", 1));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 1, "line1")));
                return packet;
            });
            assertNoNextPacket(context);

            // the plugin clears its board
            context.translate(resetScoreTranslator, new ClientboundResetScorePacket("line1", "objective"));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.REMOVE);
                packet.setInfos(List.of(new ScoreInfo(1, "0", 0)));
                return packet;
            });
            assertNoNextPacket(context);

            // and re-adds the identical line, which must be sent to Bedrock again
            context.translate(setScoreTranslator, VanillaScoreboardPackets.score("line1", "objective", 1));
            assertNextPacket(context, () -> {
                var packet = new SetScorePacket();
                packet.setAction(SetScorePacket.Action.SET);
                packet.setInfos(List.of(new ScoreInfo(2, "0", 1, "line1")));
                return packet;
            });
            assertNoNextPacket(context);
        });
    }
}

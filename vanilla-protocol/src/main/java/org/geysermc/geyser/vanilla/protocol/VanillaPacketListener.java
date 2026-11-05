/*
 * Copyright (c) 2026 StackAnvil
 * SPDX-License-Identifier: MIT
 */
package org.geysermc.geyser.vanilla.protocol;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.protocol.Packet;

/** Forwards vanilla's typed listener callbacks to Geyser's packet translator dispatch. */
final class VanillaPacketListener implements
    net.minecraft.network.protocol.login.ClientLoginPacketListener,
    net.minecraft.network.protocol.configuration.ClientConfigurationPacketListener,
    net.minecraft.network.protocol.game.ClientGamePacketListener,
    net.minecraft.network.protocol.status.ClientStatusPacketListener {
    private final ConnectionProtocol phase;
    private final Consumer<Packet<?>> packets;
    private final Consumer<DisconnectionDetails> disconnect;
    private final BooleanSupplier accepting;

    VanillaPacketListener(ConnectionProtocol phase, Consumer<Packet<?>> packets,
                          Consumer<DisconnectionDetails> disconnect, BooleanSupplier accepting) {
        this.phase = phase;
        this.packets = packets;
        this.disconnect = disconnect;
        this.accepting = accepting;
    }

    @Override
    public ConnectionProtocol protocol() {
        return phase;
    }

    @Override
    public boolean isAcceptingMessages() {
        return accepting.getAsBoolean();
    }

    @Override
    public void onDisconnect(DisconnectionDetails details) {
        disconnect.accept(details);
    }

    @Override
    public void handleAddEntity(net.minecraft.network.protocol.game.ClientboundAddEntityPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleAddObjective(net.minecraft.network.protocol.game.ClientboundSetObjectivePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleAddTransientBlockPacket(net.minecraft.network.protocol.game.ClientboundAddTransientBlockPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleAnimate(net.minecraft.network.protocol.game.ClientboundAnimatePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleAwardStats(net.minecraft.network.protocol.game.ClientboundAwardStatsPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleBlockChangedAck(net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleBlockDestruction(net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleBlockEntityData(net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleBlockEvent(net.minecraft.network.protocol.game.ClientboundBlockEventPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleBlockUpdate(net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleBossUpdate(net.minecraft.network.protocol.game.ClientboundBossEventPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleBundlePacket(net.minecraft.network.protocol.game.ClientboundBundlePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleChangeDifficulty(net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleChunkBatchFinished(net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleChunkBatchStart(net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleChunkBlocksUpdate(net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleChunksBiomes(net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleClearDialog(net.minecraft.network.protocol.common.ClientboundClearDialogPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleCodeOfConduct(net.minecraft.network.protocol.configuration.ClientboundCodeOfConductPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleCommandSuggestions(net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleCommands(net.minecraft.network.protocol.game.ClientboundCommandsPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleCompression(net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleConfigurationFinished(net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleConfigurationStart(net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleContainerClose(net.minecraft.network.protocol.game.ClientboundContainerClosePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleContainerContent(net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleContainerSetData(net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleContainerSetSlot(net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleCustomChatCompletions(net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleCustomPayload(net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleCustomQuery(net.minecraft.network.protocol.login.ClientboundCustomQueryPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleCustomReportDetails(net.minecraft.network.protocol.common.ClientboundCustomReportDetailsPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleDamageEvent(net.minecraft.network.protocol.game.ClientboundDamageEventPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleDebugBlockValue(net.minecraft.network.protocol.game.ClientboundDebugBlockValuePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleDebugChunkValue(net.minecraft.network.protocol.game.ClientboundDebugChunkValuePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleDebugEntityValue(net.minecraft.network.protocol.game.ClientboundDebugEntityValuePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleDebugEvent(net.minecraft.network.protocol.game.ClientboundDebugEventPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleDebugSample(net.minecraft.network.protocol.game.ClientboundDebugSamplePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleDeleteChat(net.minecraft.network.protocol.game.ClientboundDeleteChatPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleDisconnect(net.minecraft.network.protocol.common.ClientboundDisconnectPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleDisconnect(net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleDisguisedChat(net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleEnabledFeatures(net.minecraft.network.protocol.configuration.ClientboundUpdateEnabledFeaturesPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleEntityEvent(net.minecraft.network.protocol.game.ClientboundEntityEventPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleEntityLinkPacket(net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleEntityPositionSync(net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleExplosion(net.minecraft.network.protocol.game.ClientboundExplodePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleForgetLevelChunk(net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleGameEvent(net.minecraft.network.protocol.game.ClientboundGameEventPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleGameRuleValues(net.minecraft.network.protocol.game.ClientboundGameRuleValuesPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleGameTestHighlightPos(net.minecraft.network.protocol.game.ClientboundGameTestHighlightPosPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleHello(net.minecraft.network.protocol.login.ClientboundHelloPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleHurtAnimation(net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleInitializeBorder(net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleItemCooldown(net.minecraft.network.protocol.game.ClientboundCooldownPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleKeepAlive(net.minecraft.network.protocol.common.ClientboundKeepAlivePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleLevelChunkWithLight(net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleLevelEvent(net.minecraft.network.protocol.game.ClientboundLevelEventPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleLightUpdatePacket(net.minecraft.network.protocol.game.ClientboundLightUpdatePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleLoginFinished(net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleLogin(net.minecraft.network.protocol.game.ClientboundLoginPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleLookAt(net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleLowDiskSpaceWarning(net.minecraft.network.protocol.game.ClientboundLowDiskSpaceWarningPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleMapItemData(net.minecraft.network.protocol.game.ClientboundMapItemDataPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleMerchantOffers(net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleMinecartAlongTrack(net.minecraft.network.protocol.game.ClientboundMoveMinecartPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleMountScreenOpen(net.minecraft.network.protocol.game.ClientboundMountScreenOpenPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleMoveEntity(net.minecraft.network.protocol.game.ClientboundMoveEntityPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleMovePlayer(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleMoveVehicle(net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleOpenBook(net.minecraft.network.protocol.game.ClientboundOpenBookPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleOpenScreen(net.minecraft.network.protocol.game.ClientboundOpenScreenPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleOpenSignEditor(net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleParticleEvent(net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handlePing(net.minecraft.network.protocol.common.ClientboundPingPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handlePlaceRecipe(net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handlePlayerAbilities(net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handlePlayerChat(net.minecraft.network.protocol.game.ClientboundPlayerChatPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handlePlayerCombatEnd(net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handlePlayerCombatEnter(net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handlePlayerCombatKill(net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handlePlayerInfoRemove(net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handlePlayerInfoUpdate(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handlePongResponse(net.minecraft.network.protocol.ping.ClientboundPongResponsePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handlePostEffects(net.minecraft.network.protocol.common.ClientboundPostEffectsPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleProjectilePowerPacket(net.minecraft.network.protocol.game.ClientboundProjectilePowerPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleRecipeBookAdd(net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleRecipeBookRemove(net.minecraft.network.protocol.game.ClientboundRecipeBookRemovePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleRecipeBookSettings(net.minecraft.network.protocol.game.ClientboundRecipeBookSettingsPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleRegistryData(net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleRemoveEntities(net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleRemoveMobEffect(net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleRequestCookie(net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleResetChat(net.minecraft.network.protocol.configuration.ClientboundResetChatPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleResetScore(net.minecraft.network.protocol.game.ClientboundResetScorePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleResourcePackPop(net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleResourcePackPush(net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleRespawn(net.minecraft.network.protocol.game.ClientboundRespawnPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleRotateMob(net.minecraft.network.protocol.game.ClientboundRotateHeadPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleRotatePlayer(net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSelectAdvancementsTab(net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSelectKnownPacks(net.minecraft.network.protocol.configuration.ClientboundSelectKnownPacks packet) {
        packets.accept(packet);
    }

    @Override
    public void handleServerData(net.minecraft.network.protocol.game.ClientboundServerDataPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleServerLinks(net.minecraft.network.protocol.common.ClientboundServerLinksPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetBorderCenter(net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetBorderLerpSize(net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetBorderSize(net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetBorderWarningDelay(net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetBorderWarningDistance(net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetCamera(net.minecraft.network.protocol.game.ClientboundSetCameraPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetChunkCacheCenter(net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetChunkCacheRadius(net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetCursorItem(net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetDisplayObjective(net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetEntityData(net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetEntityMotion(net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetEntityPassengersPacket(net.minecraft.network.protocol.game.ClientboundSetPassengersPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetEquipment(net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetExperience(net.minecraft.network.protocol.game.ClientboundSetExperiencePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetHealth(net.minecraft.network.protocol.game.ClientboundSetHealthPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetHeldSlot(net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetPlayerInventory(net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetPlayerTeamPacket(net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetScore(net.minecraft.network.protocol.game.ClientboundSetScorePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetSimulationDistance(net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetSpawn(net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSetTime(net.minecraft.network.protocol.game.ClientboundSetTimePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleShowDialog(net.minecraft.network.protocol.common.ClientboundShowDialogPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSoundEntityEvent(net.minecraft.network.protocol.game.ClientboundSoundEntityPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSoundEvent(net.minecraft.network.protocol.game.ClientboundSoundPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleStatusResponse(net.minecraft.network.protocol.status.ClientboundStatusResponsePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleStopSoundEvent(net.minecraft.network.protocol.game.ClientboundStopSoundPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleStoreCookie(net.minecraft.network.protocol.common.ClientboundStoreCookiePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSwingAnimation(net.minecraft.network.protocol.game.ClientboundSwingAnimationPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleSystemChat(net.minecraft.network.protocol.game.ClientboundSystemChatPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleTabListCustomisation(net.minecraft.network.protocol.game.ClientboundTabListPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleTagQueryPacket(net.minecraft.network.protocol.game.ClientboundTagQueryPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleTakeItemEntity(net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleTeleportEntity(net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleTestInstanceBlockStatus(net.minecraft.network.protocol.game.ClientboundTestInstanceBlockStatus packet) {
        packets.accept(packet);
    }

    @Override
    public void handleTickingState(net.minecraft.network.protocol.game.ClientboundTickingStatePacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleTickingStep(net.minecraft.network.protocol.game.ClientboundTickingStepPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleTitlesClear(net.minecraft.network.protocol.game.ClientboundClearTitlesPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleTransfer(net.minecraft.network.protocol.common.ClientboundTransferPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleUpdateAdvancementsPacket(net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleUpdateAttributes(net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleUpdateMobEffect(net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleUpdateRecipes(net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleUpdateTags(net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void handleWaypoint(net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void setActionBarText(net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void setSubtitleText(net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void setTitleText(net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket packet) {
        packets.accept(packet);
    }

    @Override
    public void setTitlesAnimation(net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket packet) {
        packets.accept(packet);
    }

}

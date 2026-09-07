package com.deisdev.preserve;

import net.fabricmc.api.ModInitializer;
import com.deisdev.preserve.command.PreserveCommands;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class PreserveMod implements ModInitializer {

    @Override
    public void onInitialize() {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(com.deisdev.preserve.network.GameplayRequest.TYPE, com.deisdev.preserve.network.GameplayRequest.STREAM_CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(com.deisdev.preserve.network.GameplayPayload.TYPE, com.deisdev.preserve.network.GameplayPayload.STREAM_CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(com.deisdev.preserve.network.GameplayRequest.TYPE,
                (payload, context) -> com.deisdev.preserve.network.GameplayQueries.handle(context.player(), payload));
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(com.deisdev.preserve.network.SerumStatusRequest.TYPE, com.deisdev.preserve.network.SerumStatusRequest.STREAM_CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(com.deisdev.preserve.network.SerumStatusPayload.TYPE, com.deisdev.preserve.network.SerumStatusPayload.STREAM_CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(com.deisdev.preserve.network.SerumStatusRequest.TYPE,
                (payload, context) -> com.deisdev.preserve.network.SerumStatusQueries.handle(context.player(), payload));
        com.deisdev.preserve.rules.FabricRules.register();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(
                (player, joined) -> com.deisdev.preserve.network.GameplayQueries.sync(player));
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(com.deisdev.preserve.network.InspectionRequest.TYPE, com.deisdev.preserve.network.InspectionRequest.STREAM_CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(com.deisdev.preserve.network.InspectionPayload.TYPE, com.deisdev.preserve.network.InspectionPayload.STREAM_CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(com.deisdev.preserve.network.InspectionRequest.TYPE,
                (payload, context) -> com.deisdev.preserve.network.InspectionQueries.handle(context.player(), payload));
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
                com.deisdev.preserve.network.ChunkTreatmentsPayload.TYPE, com.deisdev.preserve.network.ChunkTreatmentsPayload.STREAM_CODEC);
        Preserve.init();
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(com.deisdev.preserve.item.PreserveItems.INGREDIENTS_TAB)
                .register(output -> com.deisdev.preserve.item.PreserveItems.fillCreativeTab(output::accept));
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(com.deisdev.preserve.item.PreserveItems.TOOLS_TAB)
                .register(output -> com.deisdev.preserve.item.PreserveItems.fillToolsTab(output::accept));
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> PreserveCommands.register(dispatcher));
    }
}

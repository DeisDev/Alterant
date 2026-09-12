package com.deisdev.alterant;

import net.fabricmc.api.ModInitializer;
import com.deisdev.alterant.command.AlterantCommands;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class AlterantMod implements ModInitializer {

    @Override
    public void onInitialize() {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(com.deisdev.alterant.network.GameplayRequest.TYPE, com.deisdev.alterant.network.GameplayRequest.STREAM_CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(com.deisdev.alterant.network.GameplayPayload.TYPE, com.deisdev.alterant.network.GameplayPayload.STREAM_CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(com.deisdev.alterant.network.GameplayRequest.TYPE,
                (payload, context) -> com.deisdev.alterant.network.GameplayQueries.handle(context.player(), payload));
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(com.deisdev.alterant.network.SerumStatusRequest.TYPE, com.deisdev.alterant.network.SerumStatusRequest.STREAM_CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(com.deisdev.alterant.network.SerumStatusPayload.TYPE, com.deisdev.alterant.network.SerumStatusPayload.STREAM_CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(com.deisdev.alterant.network.SerumStatusRequest.TYPE,
                (payload, context) -> com.deisdev.alterant.network.SerumStatusQueries.handle(context.player(), payload));
        com.deisdev.alterant.rules.FabricRules.register();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(
                (player, joined) -> com.deisdev.alterant.network.GameplayQueries.sync(player));
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(com.deisdev.alterant.network.InspectionRequest.TYPE, com.deisdev.alterant.network.InspectionRequest.STREAM_CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(com.deisdev.alterant.network.InspectionPayload.TYPE, com.deisdev.alterant.network.InspectionPayload.STREAM_CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(com.deisdev.alterant.network.InspectionRequest.TYPE,
                (payload, context) -> com.deisdev.alterant.network.InspectionQueries.handle(context.player(), payload));
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
                com.deisdev.alterant.network.ChunkTreatmentsPayload.TYPE, com.deisdev.alterant.network.ChunkTreatmentsPayload.STREAM_CODEC);
        Alterant.init();
        com.deisdev.alterant.item.SolventDispenser.register();
        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntities((basin, side) -> side == null
                ? net.fabricmc.fabric.api.transfer.v1.storage.Storage.empty()
                : net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of((com.deisdev.alterant.basin.ReclaimingBasinEntity) basin, side), com.deisdev.alterant.basin.AlterantBlocks.BASIN_ENTITY.get());
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(com.deisdev.alterant.item.AlterantItems.INGREDIENTS_TAB)
                .register(output -> com.deisdev.alterant.item.AlterantItems.fillCreativeTab(output::accept));
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(com.deisdev.alterant.item.AlterantItems.TOOLS_TAB)
                .register(output -> com.deisdev.alterant.item.AlterantItems.fillToolsTab(output::accept));
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> AlterantCommands.register(dispatcher));
    }
}

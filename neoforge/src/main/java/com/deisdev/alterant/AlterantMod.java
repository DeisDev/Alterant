package com.deisdev.alterant;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import com.deisdev.alterant.command.AlterantCommands;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(Constants.MOD_ID)
public final class AlterantMod {

    public AlterantMod(IEventBus eventBus) {
        NeoForge.EVENT_BUS.addListener(com.deisdev.alterant.rules.NeoForgeRules::register);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.OnDatapackSyncEvent event) ->
                event.getRelevantPlayers().forEach(com.deisdev.alterant.network.GameplayQueries::sync));
        eventBus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) ->
                event.registrar("2").playToClient(com.deisdev.alterant.network.ChunkTreatmentsPayload.TYPE,
                        com.deisdev.alterant.network.ChunkTreatmentsPayload.STREAM_CODEC)
                        .playToClient(com.deisdev.alterant.network.GameplayPayload.TYPE, com.deisdev.alterant.network.GameplayPayload.STREAM_CODEC)
                        .playToServer(com.deisdev.alterant.network.GameplayRequest.TYPE, com.deisdev.alterant.network.GameplayRequest.STREAM_CODEC,
                                (payload, context) -> com.deisdev.alterant.network.GameplayQueries.handle((net.minecraft.server.level.ServerPlayer) context.player(), payload))
                        .playToClient(com.deisdev.alterant.network.SerumStatusPayload.TYPE, com.deisdev.alterant.network.SerumStatusPayload.STREAM_CODEC)
                        .playToServer(com.deisdev.alterant.network.SerumStatusRequest.TYPE, com.deisdev.alterant.network.SerumStatusRequest.STREAM_CODEC,
                                (payload, context) -> com.deisdev.alterant.network.SerumStatusQueries.handle((net.minecraft.server.level.ServerPlayer) context.player(), payload))
                        .playToClient(com.deisdev.alterant.network.InspectionPayload.TYPE, com.deisdev.alterant.network.InspectionPayload.STREAM_CODEC)
                        .playToServer(com.deisdev.alterant.network.InspectionRequest.TYPE, com.deisdev.alterant.network.InspectionRequest.STREAM_CODEC,
                                (payload, context) -> com.deisdev.alterant.network.InspectionQueries.handle((net.minecraft.server.level.ServerPlayer) context.player(), payload)));
        Alterant.init();
        com.deisdev.alterant.platform.NeoForgePlatformHelper.registerContent(eventBus);
        eventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(com.deisdev.alterant.item.SolventDispenser::register));
        eventBus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) ->
                event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, com.deisdev.alterant.basin.AlterantBlocks.BASIN_ENTITY.get(),
                        (basin, side) -> side == null ? net.neoforged.neoforge.transfer.EmptyResourceHandler.instance()
                                : new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(basin, side)));
        eventBus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(com.deisdev.alterant.item.AlterantItems.INGREDIENTS_TAB)) { com.deisdev.alterant.item.AlterantItems.fillCreativeTab(event::accept); }
            if (event.getTabKey().equals(com.deisdev.alterant.item.AlterantItems.TOOLS_TAB)) { com.deisdev.alterant.item.AlterantItems.fillToolsTab(event::accept); }
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> AlterantCommands.register(event.getDispatcher()));
    }
}

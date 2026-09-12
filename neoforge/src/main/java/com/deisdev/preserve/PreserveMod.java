package com.deisdev.preserve;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import com.deisdev.preserve.command.PreserveCommands;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(Constants.MOD_ID)
public final class PreserveMod {

    public PreserveMod(IEventBus eventBus) {
        NeoForge.EVENT_BUS.addListener(com.deisdev.preserve.rules.NeoForgeRules::register);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.OnDatapackSyncEvent event) ->
                event.getRelevantPlayers().forEach(com.deisdev.preserve.network.GameplayQueries::sync));
        eventBus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) ->
                event.registrar("2").playToClient(com.deisdev.preserve.network.ChunkTreatmentsPayload.TYPE,
                        com.deisdev.preserve.network.ChunkTreatmentsPayload.STREAM_CODEC)
                        .playToClient(com.deisdev.preserve.network.GameplayPayload.TYPE, com.deisdev.preserve.network.GameplayPayload.STREAM_CODEC)
                        .playToServer(com.deisdev.preserve.network.GameplayRequest.TYPE, com.deisdev.preserve.network.GameplayRequest.STREAM_CODEC,
                                (payload, context) -> com.deisdev.preserve.network.GameplayQueries.handle((net.minecraft.server.level.ServerPlayer) context.player(), payload))
                        .playToClient(com.deisdev.preserve.network.SerumStatusPayload.TYPE, com.deisdev.preserve.network.SerumStatusPayload.STREAM_CODEC)
                        .playToServer(com.deisdev.preserve.network.SerumStatusRequest.TYPE, com.deisdev.preserve.network.SerumStatusRequest.STREAM_CODEC,
                                (payload, context) -> com.deisdev.preserve.network.SerumStatusQueries.handle((net.minecraft.server.level.ServerPlayer) context.player(), payload))
                        .playToClient(com.deisdev.preserve.network.InspectionPayload.TYPE, com.deisdev.preserve.network.InspectionPayload.STREAM_CODEC)
                        .playToServer(com.deisdev.preserve.network.InspectionRequest.TYPE, com.deisdev.preserve.network.InspectionRequest.STREAM_CODEC,
                                (payload, context) -> com.deisdev.preserve.network.InspectionQueries.handle((net.minecraft.server.level.ServerPlayer) context.player(), payload)));
        Preserve.init();
        com.deisdev.preserve.platform.NeoForgePlatformHelper.registerContent(eventBus);
        eventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(com.deisdev.preserve.item.SolventDispenser::register));
        eventBus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) ->
                event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, com.deisdev.preserve.basin.PreserveBlocks.BASIN_ENTITY.get(),
                        (basin, side) -> side == null ? net.neoforged.neoforge.transfer.EmptyResourceHandler.instance()
                                : new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(basin, side)));
        eventBus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(com.deisdev.preserve.item.PreserveItems.INGREDIENTS_TAB)) { com.deisdev.preserve.item.PreserveItems.fillCreativeTab(event::accept); }
            if (event.getTabKey().equals(com.deisdev.preserve.item.PreserveItems.TOOLS_TAB)) { com.deisdev.preserve.item.PreserveItems.fillToolsTab(event::accept); }
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> PreserveCommands.register(event.getDispatcher()));
    }
}

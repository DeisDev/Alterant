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
        eventBus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) ->
                event.registrar("1").playToClient(com.deisdev.preserve.network.ChunkTreatmentsPayload.TYPE,
                        com.deisdev.preserve.network.ChunkTreatmentsPayload.STREAM_CODEC));
        Preserve.init();
        com.deisdev.preserve.platform.NeoForgePlatformHelper.registerContent(eventBus);
        eventBus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(com.deisdev.preserve.item.PreserveItems.INGREDIENTS_TAB)) { com.deisdev.preserve.item.PreserveItems.fillCreativeTab(event::accept); }
            if (event.getTabKey().equals(com.deisdev.preserve.item.PreserveItems.TOOLS_TAB)) { com.deisdev.preserve.item.PreserveItems.fillToolsTab(event::accept); }
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> PreserveCommands.register(event.getDispatcher()));
    }
}

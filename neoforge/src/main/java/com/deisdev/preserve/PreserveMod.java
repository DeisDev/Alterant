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
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> PreserveCommands.register(event.getDispatcher()));
    }
}

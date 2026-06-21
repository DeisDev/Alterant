package com.deisdev.preserve;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import com.deisdev.preserve.command.PreserveCommands;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(Constants.MOD_ID)
public final class PreserveMod {

    public PreserveMod(IEventBus eventBus) {
        Preserve.init();
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> PreserveCommands.register(event.getDispatcher()));
    }
}

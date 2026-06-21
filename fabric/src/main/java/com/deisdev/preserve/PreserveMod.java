package com.deisdev.preserve;

import net.fabricmc.api.ModInitializer;
import com.deisdev.preserve.command.PreserveCommands;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class PreserveMod implements ModInitializer {

    @Override
    public void onInitialize() {
        Preserve.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> PreserveCommands.register(dispatcher));
    }
}

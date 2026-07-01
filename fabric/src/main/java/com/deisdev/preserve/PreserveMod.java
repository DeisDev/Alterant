package com.deisdev.preserve;

import net.fabricmc.api.ModInitializer;
import com.deisdev.preserve.command.PreserveCommands;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class PreserveMod implements ModInitializer {

    @Override
    public void onInitialize() {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
                com.deisdev.preserve.network.ChunkTreatmentsPayload.TYPE, com.deisdev.preserve.network.ChunkTreatmentsPayload.STREAM_CODEC);
        Preserve.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> PreserveCommands.register(dispatcher));
    }
}

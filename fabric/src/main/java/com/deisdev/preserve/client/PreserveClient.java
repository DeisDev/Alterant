package com.deisdev.preserve.client;

import com.deisdev.preserve.network.ChunkTreatmentsPayload;
import com.deisdev.preserve.network.TreatmentSync;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class PreserveClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ChunkTreatmentsPayload.TYPE,
                (payload, context) -> TreatmentSync.receive(context.client().level, payload));
    }
}

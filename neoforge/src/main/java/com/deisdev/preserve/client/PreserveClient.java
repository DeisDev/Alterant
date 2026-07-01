package com.deisdev.preserve.client;

import com.deisdev.preserve.network.ChunkTreatmentsPayload;
import com.deisdev.preserve.network.TreatmentSync;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = "deisdev", value = Dist.CLIENT)
public final class PreserveClient {
    @SubscribeEvent
    public static void registerPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(ChunkTreatmentsPayload.TYPE, (payload, context) -> TreatmentSync.receive(Minecraft.getInstance().level, payload));
    }
}

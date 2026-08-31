package com.deisdev.preserve.client;

import com.deisdev.preserve.network.ChunkTreatmentsPayload;
import com.deisdev.preserve.network.TreatmentSync;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class PreserveClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientConfig.initialize(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
        ClientInspection.init(payload -> { if (ClientPlayNetworking.canSend(payload.type())) { ClientPlayNetworking.send(payload); } });
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(ClientInspection::tick);
        ClientPlayNetworking.registerGlobalReceiver(com.deisdev.preserve.network.InspectionPayload.TYPE,
                (payload, context) -> ClientInspection.receive(context.client(), payload));
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementBefore(net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.CHAT,
                net.minecraft.resources.Identifier.parse("deisdev:inspection"), ToolOverlay::hud);
        net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents.END_EXTRACTION.register(context ->
                ((OverlayRenderState) context.levelState()).preserve$overlay(ToolOverlay.extract(context.level())));
        net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.COLLECT_SUBMITS.register(context ->
                ToolOverlay.submit(context.levelState(), context.poseStack(), context.submitNodeCollector()));
        ClientPlayNetworking.registerGlobalReceiver(ChunkTreatmentsPayload.TYPE,
                (payload, context) -> TreatmentSync.receive(context.client().level, payload));
    }
}

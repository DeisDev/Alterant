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
    public static void clientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) { ClientInspection.tick(Minecraft.getInstance()); }
    @SubscribeEvent
    public static void registerLayers(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        event.registerBelow(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CHAT, net.minecraft.resources.Identifier.parse("deisdev:inspection"), ToolOverlay::hud);
    }
    @SubscribeEvent
    public static void extract(net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent event) {
        ((OverlayRenderState) event.getRenderState()).preserve$overlay(ToolOverlay.extract(event.getLevel()));
    }
    @SubscribeEvent
    public static void submit(net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent event) {
        ToolOverlay.submit(event.getLevelRenderState(), event.getPoseStack(), event.getSubmitNodeCollector());
    }
    @SubscribeEvent
    public static void registerPayloads(RegisterClientPayloadHandlersEvent event) {
        ClientInspection.init(net.neoforged.neoforge.client.network.ClientPacketDistributor::sendToServer);
        event.register(com.deisdev.preserve.network.InspectionPayload.TYPE, (payload, context) -> ClientInspection.receive(Minecraft.getInstance(), payload));
        event.register(ChunkTreatmentsPayload.TYPE, (payload, context) -> TreatmentSync.receive(Minecraft.getInstance().level, payload));
    }
}

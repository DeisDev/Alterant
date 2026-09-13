package com.deisdev.alterant.client;

import com.deisdev.alterant.network.ChunkTreatmentsPayload;
import com.deisdev.alterant.network.TreatmentSync;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class AlterantClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        com.deisdev.alterant.client.coating.CoatingModelBridge.initialize(FabricCoatingModels::resolve);
        com.deisdev.alterant.mixin.client.CoatingPipelineInvoker.alterant$register(com.deisdev.alterant.client.coating.CoatingPipeline.PIPELINE);
        net.minecraft.client.gui.screens.MenuScreens.register(com.deisdev.alterant.item.AlterantMenus.RECLAMATION.get(), ReclamationScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(com.deisdev.alterant.item.AlterantMenus.GROWTH_LIMIT.get(), GrowthLimitScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(com.deisdev.alterant.item.AlterantMenus.TRANSFER_POLICY.get(), TransferPolicyScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(com.deisdev.alterant.item.AlterantMenus.BASIN.get(), ReclaimingBasinScreen::new);
        ClientGameplay.init(payload -> { if (!ClientPlayNetworking.canSend(payload.type())) { return false; } ClientPlayNetworking.send(payload); return true; });
        SerumFeedback.init(payload -> { if (ClientPlayNetworking.canSend(payload.type())) { ClientPlayNetworking.send(payload); } });
        ClientPlayNetworking.registerGlobalReceiver(com.deisdev.alterant.network.GameplayPayload.TYPE,
                (payload, context) -> ClientGameplay.receive(context.client(), payload));
        ClientPlayNetworking.registerGlobalReceiver(com.deisdev.alterant.network.SerumStatusPayload.TYPE,
                (payload, context) -> SerumFeedback.receive(context.client(), payload));
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(SerumFeedback::tick);
        ClientConfig.initialize(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
        ClientInspection.init(payload -> { if (ClientPlayNetworking.canSend(payload.type())) { ClientPlayNetworking.send(payload); } });
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(ClientInspection::tick);
        ClientPlayNetworking.registerGlobalReceiver(com.deisdev.alterant.network.InspectionPayload.TYPE,
                (payload, context) -> ClientInspection.receive(context.client(), payload));
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementBefore(net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.CHAT,
                net.minecraft.resources.Identifier.parse("alterant:inspection"), ToolOverlay::hud);
        net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents.END_EXTRACTION.register(context -> {
            ((OverlayRenderState) context.levelState()).alterant$coatings(com.deisdev.alterant.client.coating.CoatingRenderer.extract(context.level(), context.levelState()));
            ((OverlayRenderState) context.levelState()).alterant$overlay(ToolOverlay.extract(context.level()));
            ((OverlayRenderState) context.levelState()).alterant$serumCard(SerumFeedback.extract(net.minecraft.client.Minecraft.getInstance()));
        });
        net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            ToolOverlay.submit(context.levelState(), context.poseStack(), context.submitNodeCollector());
            com.deisdev.alterant.client.coating.CoatingRenderer.submit(context.levelState(), context.poseStack(), context.submitNodeCollector());
        });
        ClientPlayNetworking.registerGlobalReceiver(ChunkTreatmentsPayload.TYPE,
                (payload, context) -> TreatmentSync.receive(context.client().level, payload));
    }
}

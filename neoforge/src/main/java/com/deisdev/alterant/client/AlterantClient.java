package com.deisdev.alterant.client;

import com.deisdev.alterant.network.ChunkTreatmentsPayload;
import com.deisdev.alterant.network.TreatmentSync;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = "alterant", value = Dist.CLIENT)
public final class AlterantClient {
    @SubscribeEvent
    public static void menus(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(com.deisdev.alterant.item.AlterantMenus.RECLAMATION.get(), ReclamationScreen::new);
        event.register(com.deisdev.alterant.item.AlterantMenus.GROWTH_LIMIT.get(), GrowthLimitScreen::new);
        event.register(com.deisdev.alterant.item.AlterantMenus.TRANSFER_POLICY.get(), TransferPolicyScreen::new);
        event.register(com.deisdev.alterant.item.AlterantMenus.BASIN.get(), ReclaimingBasinScreen::new);
    }
    @SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        ClientConfig.initialize(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
        if (net.neoforged.fml.ModList.get().isLoaded("yet_another_config_lib_v3")) {
            net.neoforged.fml.ModList.get().getModContainerById("alterant").orElseThrow().registerExtensionPoint(
                    net.neoforged.neoforge.client.gui.IConfigScreenFactory.class, (container, parent) -> AlterantConfigScreen.create(parent));
        }
    }
    @SubscribeEvent
    public static void clientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        ClientInspection.tick(Minecraft.getInstance()); SerumFeedback.tick(Minecraft.getInstance());
    }
    @SubscribeEvent
    public static void registerLayers(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent event) {
        event.registerBelow(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CHAT, net.minecraft.resources.Identifier.parse("alterant:inspection"), ToolOverlay::hud);
    }
    @SubscribeEvent
    public static void extract(net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent event) {
        ((OverlayRenderState) event.getRenderState()).alterant$overlay(ToolOverlay.extract(event.getLevel()));
        ((OverlayRenderState) event.getRenderState()).alterant$serumCard(SerumFeedback.extract(Minecraft.getInstance()));
    }
    @SubscribeEvent
    public static void submit(net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent event) {
        ToolOverlay.submit(event.getLevelRenderState(), event.getPoseStack(), event.getSubmitNodeCollector());
    }
    @SubscribeEvent
    public static void registerPayloads(RegisterClientPayloadHandlersEvent event) {
        ClientGameplay.init(payload -> { net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload); return true; });
        SerumFeedback.init(net.neoforged.neoforge.client.network.ClientPacketDistributor::sendToServer);
        event.register(com.deisdev.alterant.network.GameplayPayload.TYPE, (payload, context) -> ClientGameplay.receive(Minecraft.getInstance(), payload));
        event.register(com.deisdev.alterant.network.SerumStatusPayload.TYPE, (payload, context) -> SerumFeedback.receive(Minecraft.getInstance(), payload));
        ClientInspection.init(net.neoforged.neoforge.client.network.ClientPacketDistributor::sendToServer);
        event.register(com.deisdev.alterant.network.InspectionPayload.TYPE, (payload, context) -> ClientInspection.receive(Minecraft.getInstance(), payload));
        event.register(ChunkTreatmentsPayload.TYPE, (payload, context) -> TreatmentSync.receive(Minecraft.getInstance().level, payload));
    }
}

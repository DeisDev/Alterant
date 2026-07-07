package com.deisdev.preserve.platform;

import com.deisdev.preserve.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;

public class FabricPlatformHelper implements IPlatformHelper {
    @Override public boolean transferInProgress() {
        return net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.getLifecycle()
                != net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.Lifecycle.NONE;
    }
    @Override public com.deisdev.preserve.rules.RuleLoad loadedRules(net.minecraft.server.MinecraftServer server) {
        return com.deisdev.preserve.rules.FabricRules.get(server);
    }
    @Override public void sendTreatments(net.minecraft.server.level.ServerPlayer player, com.deisdev.preserve.network.ChunkTreatmentsPayload payload) {
        if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, payload.type())) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
        }
    }

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}

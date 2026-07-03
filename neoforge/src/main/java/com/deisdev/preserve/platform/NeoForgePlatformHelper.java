package com.deisdev.preserve.platform;

import com.deisdev.preserve.platform.services.IPlatformHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

public class NeoForgePlatformHelper implements IPlatformHelper {
    @Override public com.deisdev.preserve.rules.RuleLoad loadedRules(net.minecraft.server.MinecraftServer server) {
        return com.deisdev.preserve.rules.NeoForgeRules.get(server);
    }
    @Override public void sendTreatments(net.minecraft.server.level.ServerPlayer player, com.deisdev.preserve.network.ChunkTreatmentsPayload payload) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public String getPlatformName() {

        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return !FMLLoader.getCurrent().isProduction();
    }
}

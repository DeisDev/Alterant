package com.deisdev.preserve.rules;

import net.fabricmc.fabric.api.resource.v1.DataResourceLoader;
import net.fabricmc.fabric.api.resource.v1.DataResourceStore;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.PreparableReloadListener;

public final class FabricRules {
    private static final DataResourceStore.Key<RuleLoad> KEY = new DataResourceStore.Key<>();
    private FabricRules() {}

    public static void register() {
        DataResourceLoader.get().registerReloadListener(RuleLoad.ID, new SimpleReloadListener<RuleLoad>() {
            @Override protected RuleLoad prepare(PreparableReloadListener.SharedState state) {
                return RuleLoad.prepare(state.resourceManager(), state.get(ResourceLoader.REGISTRY_LOOKUP_KEY), FabricLoader.getInstance()::isModLoaded);
            }
            @Override protected void apply(RuleLoad load, PreparableReloadListener.SharedState state) {
                // Throw before committing the resource set. Vanilla /reload retains its previous valid resources.
                state.get(DataResourceLoader.DATA_RESOURCE_STORE_KEY).put(KEY, load.requireValid());
            }
        });
    }

    public static RuleLoad get(MinecraftServer server) { return ((DataResourceStore) server).getOrThrow(KEY); }
}

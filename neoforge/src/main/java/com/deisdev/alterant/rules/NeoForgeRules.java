package com.deisdev.alterant.rules;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.resource.ContextAwareReloadListener;
import net.neoforged.neoforge.resource.ListenerKey;

public final class NeoForgeRules extends ContextAwareReloadListener {
    private static final ListenerKey<NeoForgeRules> KEY = ListenerKey.create(RuleLoad.ID);
    private RuleLoad loaded;

    public static void register(AddServerReloadListenersEvent event) { event.addRetainedListener(KEY, new NeoForgeRules()); }
    public static RuleLoad get(MinecraftServer server) { return server.getServerResources().managers().getListener(KEY).loaded; }

    @Override public CompletableFuture<Void> reload(SharedState state, Executor prepareExecutor, PreparationBarrier barrier, Executor applyExecutor) {
        // The context lookup includes the tags being loaded, rather than the previous reload's bound tags.
        return CompletableFuture.supplyAsync(() -> RuleLoad.prepare(state.resourceManager(), getRegistryLookup(), ModList.get()::isLoaded), prepareExecutor)
                .thenCompose(barrier::wait).thenAcceptAsync(result -> loaded = result.requireValid(), applyExecutor);
    }
}

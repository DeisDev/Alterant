package com.deisdev.alterant.client.coating;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** Exact block-entity IDs. Unknown special renderers have no fabricated fallback geometry. */
public final class CoatingAdapterRegistry {
    private static final Map<Identifier,CoatingSurfaceAdapter> ADAPTERS = new HashMap<>();
    private static final Map<Identifier,CoatingSurfaceAdapter> DEFAULTS = new HashMap<>();
    static {
        var containers = new ContainerCoatingAdapter();
        for (String id : new String[]{"chest","trapped_chest","ender_chest","shulker_box"}) {
            DEFAULTS.put(Identifier.withDefaultNamespace(id),containers);
        }
    }
    private CoatingAdapterRegistry() {}
    public static void register(Identifier blockEntityType, CoatingSurfaceAdapter adapter) {
        if (ADAPTERS.putIfAbsent(blockEntityType,java.util.Objects.requireNonNull(adapter)) != null) {
            throw new IllegalArgumentException("Coating adapter already registered for " + blockEntityType);
        }
    }
    public static CoatingSurfaceAdapter find(ClientLevel level, BlockPos pos) {
        var entity = level.getBlockEntity(pos);
        if (entity == null) { return null; }
        var id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType());
        return ADAPTERS.getOrDefault(id,DEFAULTS.get(id));
    }
}

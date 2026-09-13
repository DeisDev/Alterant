package com.deisdev.alterant.client.coating;

import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

/** Client-only extension for special renderers. Resolve on extraction; return copied geometry, never world references. */
public interface CoatingSurfaceAdapter {
    enum Support { SUPPORTED, PARTIAL, UNSUPPORTED }
    record Result(Support support, List<CoatingSurface> surfaces, String limitation) {
        public Result { surfaces = List.copyOf(surfaces); }
        public static Result unsupported(String reason) { return new Result(Support.UNSUPPORTED,List.of(),reason); }
    }
    Result resolve(ClientLevel level, BlockPos pos, float partialTick);
    /** Dynamic adapters are extracted within the visible-block budget; atlas animation does not use this route. */
    default boolean dynamic() { return false; }
    /** An immutable equality key, changed only when a copied surface/transform changes. No world reference may escape this method. */
    default Object revision(ClientLevel level, BlockPos pos, float partialTick) { return level.getGameTime(); }
}

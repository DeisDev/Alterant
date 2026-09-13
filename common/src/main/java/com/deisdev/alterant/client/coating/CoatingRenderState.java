package com.deisdev.alterant.client.coating;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;

/** Geometry is immutable; the liveness token prevents an invalidated in-flight batch from drawing. */
public record CoatingRenderState(List<Batch> batches, boolean debugGeometry) {
    public static final CoatingRenderState EMPTY = new CoatingRenderState(List.of());
    public CoatingRenderState(List<Batch> batches) { this(batches,false); }
    public CoatingRenderState { batches = List.copyOf(batches); }
    public record Batch(BlockPos origin, List<CoatingMesh> meshes, AtomicBoolean live) {
        public Batch { origin = origin.immutable(); meshes = List.copyOf(meshes); }
        public void invalidate() { live.set(false); }
    }
}

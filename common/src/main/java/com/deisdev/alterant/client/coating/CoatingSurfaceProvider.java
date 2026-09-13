package com.deisdev.alterant.client.coating;

import com.mojang.blaze3d.vertex.QuadInstance;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.Vec3;

/** Ordinary terrain models, using the pinned renderer's variant seed, offset, culling and light calculator. */
public final class CoatingSurfaceProvider {
    private CoatingSurfaceProvider() {}

    public static List<CoatingSurface> resolve(ClientLevel level, BlockPos pos) {
        var adapter = CoatingAdapterRegistry.find(level,pos);
        if (adapter != null) { return adapter.resolve(level,pos,Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)).surfaces(); }
        return resolveModel(level,pos);
    }
    /** The ordinary terrain portion, for adapters which add separately rendered parts. */
    public static List<CoatingSurface> resolveModel(ClientLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.getRenderShape() != RenderShape.MODEL) { return List.of(); }
        var client = Minecraft.getInstance();
        var model = client.getModelManager().getBlockStateModelSet().get(state);
        var output = new ArrayList<CoatingSurface>();
        var seen = new HashSet<String>();
        var lighter = new BlockModelLighter();
        var light = new QuadInstance();
        var offset = state.getOffset(pos);
        for (var resolved : CoatingModelBridge.resolve(level,pos,state,model)) {
            var quad = resolved.geometry();
            // Zero emission and no tint: pigment receives substrate AO, never a biome color or emission.
            var material = quad.materialInfo();
            var unlit = new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                    quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(),
                    new BakedQuad.MaterialInfo(material.sprite(), material.layer(), material.itemRenderType(), -1, material.shade(), 0));
            if (client.options.ambientOcclusion().get() && resolved.ambientOcclusion()) {
                lighter.prepareQuadAmbientOcclusion(level, state, pos, unlit, light);
            } else { lighter.prepareQuadFlat(level, state, pos, -1, unlit, light); }
            var vertices = new ArrayList<CoatingSurface.Vertex>(4);
            for (int i = 0; i < 4; i++) {
                var p = quad.position(i);
                int packed = light.getLightCoords(i);
                vertices.add(new CoatingSurface.Vertex(new Vec3(p.x(), p.y(), p.z()).add(offset),
                        UVPair.unpackU(quad.packedUV(i)), UVPair.unpackV(quad.packedUV(i)),
                        (light.getColor(i) & 255) / 255.0, packed & 65535, (packed >>> 16) & 65535));
            }
            var a = vertices.get(0).position();
            var edge = vertices.get(1).position().subtract(a);
            var normal = edge.cross(vertices.get(2).position().subtract(a)).normalize();
            if (normal.lengthSqr() < 0.5) { continue; }
            String key = quad.direction().getSerializedName() + ":" + vertices.stream().map(v -> v.position().toString()).toList();
            if (!seen.add(key)) { continue; }
            boolean organic = state.is(BlockTags.LEAVES) || !state.isCollisionShapeFullBlock(level, pos)
                    && material.layer() == ChunkSectionLayer.CUTOUT && Math.abs(normal.y) < 0.1 && Math.abs(normal.x * normal.z) > 0.1;
            double minEdge = Math.min(edge.length(), vertices.get(2).position().subtract(vertices.get(1).position()).length());
            output.add(new CoatingSurface(vertices, normal, quad.direction(), material.sprite().atlasLocation(),
                    material.layer() == ChunkSectionLayer.CUTOUT, key, organic ? "organic" : minEdge < 0.5 ? "narrow" : "broad"));
        }
        return List.copyOf(output);
    }
}

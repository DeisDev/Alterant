package com.deisdev.alterant.client.coating;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class CoatingMeshBuilder {
    private final Map<Identifier, List<CoatingMesh.Vertex>> batches = new LinkedHashMap<>();
    public void add(CoatingSurface surface, TextureAtlasSprite sprite, int density, Vec3 offset) {
        var batch = batches.computeIfAbsent(surface.sourceAtlas(), unused -> new ArrayList<>());
        for (var tile : CoatingUvMapper.map(surface, density, sprite.contents().width(), sprite.contents().height())) {
            var polygon = tile.vertices();
            // A convex clipped polygon is triangulated, with a repeated last vertex for QUADS submission.
            if (polygon.size() == 4) {
                for (var v : polygon) { batch.add(vertex(v, tile, surface, sprite, offset)); }
            } else {
                for (int i = 1; i + 1 < polygon.size(); i++) {
                    batch.add(vertex(polygon.getFirst(), tile, surface, sprite, offset));
                    batch.add(vertex(polygon.get(i), tile, surface, sprite, offset));
                    batch.add(vertex(polygon.get(i+1), tile, surface, sprite, offset));
                    batch.add(vertex(polygon.get(i+1), tile, surface, sprite, offset));
                }
            }
        }
    }
    private static CoatingMesh.Vertex vertex(CoatingUvMapper.Mapped vertex, CoatingUvMapper.Tile tile,
                                             CoatingSurface surface, TextureAtlasSprite sprite, Vec3 offset) {
        var p = vertex.source().position().add(surface.normal().scale(CoatingUvMapper.BIAS)).add(offset);
        double u = Math.clamp(vertex.u()-tile.x(), 0, 1), v = Math.clamp(vertex.v()-tile.y(), 0, 1);
        // Atlas sprite padding handles mipmaps. Stay infinitesimally inside the selected sprite at tile edges.
        float cu = (float)Math.clamp(u, 0.00001, 0.99999);
        float cv = (float)Math.clamp(v, 0.00001, 0.99999);
        int shade = (int)Math.clamp(Math.round(vertex.source().shade()*255), 0, 255);
        int light = (int)Math.round(vertex.source().blockLight()) | ((int)Math.round(vertex.source().skyLight()) << 16);
        return new CoatingMesh.Vertex(sprite.contents().name(), (float)p.x, (float)p.y, (float)p.z, cu, cv,
                (int)Math.round(Math.clamp(vertex.source().sourceU(), 0, 1)*65535),
                (int)Math.round(Math.clamp(vertex.source().sourceV(), 0, 1)*65535),
                0xff000000 | shade*0x010101, light, surface.sourceCutout());
    }
    public List<CoatingMesh> build() {
        return batches.entrySet().stream().filter(e -> !e.getValue().isEmpty()).map(e -> new CoatingMesh(e.getKey(), e.getValue())).toList();
    }
}

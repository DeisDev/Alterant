package com.deisdev.alterant.client.coating;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Copied model geometry. Positions are block-local; source UVs remain atlas-local. */
public record CoatingSurface(List<Vertex> vertices, Vec3 normal, Direction face, Identifier sourceAtlas,
                             boolean sourceCutout, String group, String motif, Vec3 mappingNormal) {
    public CoatingSurface { vertices = List.copyOf(vertices); }
    public CoatingSurface(List<Vertex> vertices, Vec3 normal, Direction face, Identifier sourceAtlas, boolean sourceCutout, String group, String motif) {
        this(vertices, normal, face, sourceAtlas, sourceCutout, group, motif, normal);
    }

    public record Vertex(Vec3 position, double sourceU, double sourceV, double shade, double blockLight, double skyLight, Vec3 coatingPosition) {
        public Vertex(Vec3 position, double sourceU, double sourceV, double shade, double blockLight, double skyLight) {
            this(position, sourceU, sourceV, shade, blockLight, skyLight, position);
        }
        public Vertex lerp(Vertex other, double t) {
            return new Vertex(position.lerp(other.position, t), mix(sourceU, other.sourceU, t), mix(sourceV, other.sourceV, t),
                    mix(shade, other.shade, t), mix(blockLight, other.blockLight, t), mix(skyLight, other.skyLight, t), coatingPosition.lerp(other.coatingPosition, t));
        }
        private static double mix(double a, double b, double t) { return a + (b - a) * t; }
    }
}

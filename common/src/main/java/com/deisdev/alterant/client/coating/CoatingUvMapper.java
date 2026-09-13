package com.deisdev.alterant.client.coating;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/** Fixed physical texel density; tile clipping interpolates every source attribute. */
public final class CoatingUvMapper {
    public static final double BIAS = 1.0 / 2048;
    private static final double EPSILON = 1.0e-8;
    private CoatingUvMapper() {}

    public record Mapped(CoatingSurface.Vertex source, double u, double v) {
        Mapped lerp(Mapped other, double t) { return new Mapped(source.lerp(other.source, t), u + (other.u-u)*t, v + (other.v-v)*t); }
    }
    public record Tile(List<Mapped> vertices, int x, int y) { public Tile { vertices = List.copyOf(vertices); } }

    /** FNV-1a over explicit UTF-8 keys; stable across enum reorder, JVMs and clients. */
    public static long seed(String dimension, long position, String formulation, String surface) {
        long hash = 0xcbf29ce484222325L;
        for (byte value : (dimension + "\0" + position + "\0" + formulation + "\0" + surface).getBytes(StandardCharsets.UTF_8)) {
            hash = (hash ^ (value & 255)) * 0x100000001b3L;
        }
        return hash;
    }

    public static List<Tile> map(CoatingSurface surface, int pixelsPerBlock, int width, int height) {
        var normal = surface.mappingNormal();
        Vec3 uAxis = Math.abs(normal.y) > 0.999 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0).cross(normal).normalize();
        Vec3 vAxis = normal.cross(uAxis).normalize();
        var mapped = surface.vertices().stream().map(v -> new Mapped(v, v.coatingPosition().dot(uAxis)*pixelsPerBlock/width,
                v.coatingPosition().dot(vAxis)*pixelsPerBlock/height)).toList();
        double offsetU = 0, offsetV = 0;
        if (!surface.motif().equals("broad")) {
            // Put the compact motif at the center, aligned to physical pixels, without rescaling a rail.
            offsetU = 0.5 - Math.rint(mapped.stream().mapToDouble(Mapped::u).average().orElse(0)*width)/width;
            offsetV = 0.5 - Math.rint(mapped.stream().mapToDouble(Mapped::v).average().orElse(0)*height)/height;
        }
        final double shiftU = offsetU, shiftV = offsetV;
        mapped = mapped.stream().map(v -> new Mapped(v.source, v.u+shiftU, v.v+shiftV)).toList();
        int minU = (int)Math.floor(mapped.stream().mapToDouble(Mapped::u).min().orElse(0)+EPSILON);
        int maxU = (int)Math.floor(mapped.stream().mapToDouble(Mapped::u).max().orElse(0)-EPSILON);
        int minV = (int)Math.floor(mapped.stream().mapToDouble(Mapped::v).min().orElse(0)+EPSILON);
        int maxV = (int)Math.floor(mapped.stream().mapToDouble(Mapped::v).max().orElse(0)-EPSILON);
        if ((long)(maxU-minU+1)*(maxV-minV+1) > 1024) { return List.of(); }
        var result = new ArrayList<Tile>();
        for (int x = minU; x <= maxU; x++) { for (int y = minV; y <= maxV; y++) {
            var polygon = clip(clip(clip(clip(mapped, true, x, true), true, x+1, false), false, y, true), false, y+1, false);
            if (polygon.size() >= 3) { result.add(new Tile(polygon, x, y)); }
        } }
        return List.copyOf(result);
    }

    private static List<Mapped> clip(List<Mapped> input, boolean u, double bound, boolean above) {
        if (input.isEmpty()) { return List.of(); }
        var output = new ArrayList<Mapped>();
        var previous = input.getLast();
        double p = (u ? previous.u : previous.v) - bound;
        for (var current : input) {
            double c = (u ? current.u : current.v) - bound;
            boolean inside = above ? c >= -EPSILON : c <= EPSILON;
            boolean wasInside = above ? p >= -EPSILON : p <= EPSILON;
            if (inside != wasInside) { output.add(previous.lerp(current, p/(p-c))); }
            if (inside) { output.add(current); }
            previous = current; p = c;
        }
        return output;
    }
}

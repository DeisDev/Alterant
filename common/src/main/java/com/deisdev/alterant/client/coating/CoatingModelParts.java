package com.deisdev.alterant.client.coating;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Copies baked model-part polygons after their native pose, retaining coordinates local to each moving part. */
public final class CoatingModelParts {
    private CoatingModelParts() {}
    public static List<CoatingSurface> copy(ModelPart root, PoseStack pose, TextureAtlasSprite sprite, int light, ClientLevel level) {
        var surfaces = new ArrayList<CoatingSurface>();
        root.visit(pose,(transform,path,cubeIndex,cube) -> {
            for (int index=0; index<cube.polygons.length; index++) {
                var polygon = cube.polygons[index];
                var localNormal = polygon.normal();
                var transformedNormal = transform.transformNormal(localNormal,new Vector3f());
                var normal = new Vec3(transformedNormal.x,transformedNormal.y,transformedNormal.z).normalize();
                var direction = Direction.getApproximateNearest(normal);
                var vertices = new ArrayList<CoatingSurface.Vertex>();
                for (var vertex : polygon.vertices()) {
                    var local = new Vec3(vertex.worldX(),vertex.worldY(),vertex.worldZ());
                    var p = transform.pose().transformPosition(vertex.worldX(),vertex.worldY(),vertex.worldZ(),new Vector3f());
                    vertices.add(new CoatingSurface.Vertex(new Vec3(p.x,p.y,p.z),sprite.getU(vertex.u()),sprite.getV(vertex.v()),
                            level.cardinalLighting().byFace(direction),light&65535,light>>>16,local));
                }
                double shortest = Math.min(vertices.get(0).position().distanceTo(vertices.get(1).position()),vertices.get(1).position().distanceTo(vertices.get(2).position()));
                surfaces.add(new CoatingSurface(vertices,normal,direction,sprite.atlasLocation(),true,path+"/"+cubeIndex+"/"+index,
                        shortest<0.5?"narrow":"broad",new Vec3(localNormal.x(),localNormal.y(),localNormal.z())));
            }
        });
        return List.copyOf(surfaces);
    }
}

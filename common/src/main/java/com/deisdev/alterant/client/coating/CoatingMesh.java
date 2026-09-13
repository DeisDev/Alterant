package com.deisdev.alterant.client.coating;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.resources.Identifier;

/** Immutable CPU batch. The vanilla feature renderer owns its frame upload and GPU lifetime. */
public record CoatingMesh(Identifier sourceAtlas, List<Vertex> vertices) {
    public CoatingMesh { vertices = List.copyOf(vertices); }
    /** Coating UVs are sprite-local; only the identifier is retained between frames. */
    public record Vertex(Identifier sprite, float x, float y, float z, float u, float v, int sourceU, int sourceV,
                         int color, int light, boolean mask) {}
    /** Conservative allowance for Java vertices, section references and the selected frame upload; excludes atlases. */
    public long bytes() { return (long)vertices.size() * 96; }
    public void draw(PoseStack.Pose pose, VertexConsumer output) {
        Identifier currentSprite = null;
        VertexConsumer textured = null;
        for (var v : vertices) {
            if (!v.sprite.equals(currentSprite)) {
                currentSprite = v.sprite;
                var sprite = CoatingVisualRegistry.sprite(currentSprite);
                // Vanilla's wrapper also tells Sodium that this sprite needs animation updates.
                textured = sprite == null ? null : sprite.wrap(output);
            }
            if (textured == null) { continue; }
            output.addVertex(pose.pose(), v.x, v.y, v.z).setColor(v.color)
                    .setUv1(v.sourceU, v.sourceV).setLight(v.light).setNormal(v.mask ? 1 : 0, 0, 0);
            textured.setUv(v.u, v.v);
        }
    }
}

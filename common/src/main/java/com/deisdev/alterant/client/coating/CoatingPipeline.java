package com.deisdev.alterant.client.coating;

import com.deisdev.alterant.mixin.client.CoatingRenderTypeInvoker;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

/** Solid feature pass: after opaque terrain, before glass/water. Minecraft owns uploads and retirement. */
public final class CoatingPipeline {
    public static final RenderPipeline PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.parse("alterant:pipeline/coating"))
            .withVertexShader(Identifier.parse("alterant:core/coating"))
            .withFragmentShader(Identifier.parse("alterant:core/coating"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.FOG)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER1_SAMPLER2)
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(DepthStencilState.DEFAULT).build();
    private static final Map<Identifier, RenderType> TYPES = new HashMap<>();
    private CoatingPipeline() {}
    public static RenderType type(Identifier sourceAtlas) {
        return TYPES.computeIfAbsent(sourceAtlas, atlas -> CoatingRenderTypeInvoker.alterant$create("alterant_coating", RenderSetup.builder(PIPELINE)
                .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS).withTexture("Sampler1", atlas).useLightmap().createRenderSetup()));
    }
    public static void clear() { TYPES.clear(); }
}

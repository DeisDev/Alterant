package com.deisdev.alterant.client;

import com.deisdev.alterant.client.coating.CoatingModelBridge;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/** Captures Fabric's emitted, transformed quads with the same world context and cull test as terrain. */
final class FabricCoatingModels {
    private FabricCoatingModels() {}
    static List<CoatingModelBridge.Quad> resolve(ClientLevel level, BlockPos pos, BlockState state, BlockStateModel model) {
        var result = new ArrayList<CoatingModelBridge.Quad>();
        var emitter = Renderer.get().quadEmitter(quad -> {
            if (CoatingModelBridge.culled(level,pos,state,quad.cullFace())) { return; }
            var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(quad.atlas().getId());
            var sprite = atlas.spriteFinder().find(quad);
            result.add(new CoatingModelBridge.Quad(quad.toBakedQuad(sprite),quad.ambientOcclusion()!=TriState.FALSE));
        });
        model.emitQuads(emitter,level,pos,state,RandomSource.create(state.getSeed(pos)),face -> CoatingModelBridge.culled(level,pos,state,face));
        return List.copyOf(result);
    }
}

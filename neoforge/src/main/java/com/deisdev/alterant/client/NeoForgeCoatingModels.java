package com.deisdev.alterant.client;

import com.deisdev.alterant.client.coating.CoatingModelBridge;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.state.BlockState;

/** NeoForge's contextual collector reads the level's model-data snapshot, including custom model parts. */
final class NeoForgeCoatingModels {
    private NeoForgeCoatingModels() {}
    static List<CoatingModelBridge.Quad> resolve(ClientLevel level, BlockPos pos, BlockState state, BlockStateModel model) {
        var parts = new ArrayList<BlockStateModelPart>();
        model.collectParts(level,pos,state,RandomSource.create(state.getSeed(pos)),parts);
        var result = new ArrayList<CoatingModelBridge.Quad>();
        for (var part:parts) for (int side=0;side<=6;side++) {
            var face=side==6?null:Direction.values()[side];
            if (!CoatingModelBridge.culled(level,pos,state,face)) for (var quad:part.getQuads(face)) {
                result.add(new CoatingModelBridge.Quad(quad,part.ambientOcclusion()!=TriState.FALSE));
            }
        }
        return List.copyOf(result);
    }
}

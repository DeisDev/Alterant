package com.deisdev.alterant.client.coating;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Loader-owned model context stays outside shared rendering code. Quads are copied before lighting. */
public final class CoatingModelBridge {
    public record Quad(BakedQuad geometry, boolean ambientOcclusion) {}
    @FunctionalInterface public interface Resolver {
        List<Quad> resolve(ClientLevel level, BlockPos pos, BlockState state, BlockStateModel model);
    }
    private static Resolver resolver = CoatingModelBridge::vanilla;
    private CoatingModelBridge() {}
    public static void initialize(Resolver bridge) { resolver = java.util.Objects.requireNonNull(bridge); }
    public static List<Quad> resolve(ClientLevel level, BlockPos pos, BlockState state, BlockStateModel model) {
        return resolver.resolve(level,pos,state,model);
    }
    public static boolean culled(ClientLevel level, BlockPos pos, BlockState state, Direction face) {
        return face != null && !Block.shouldRenderFace(state,level.getBlockState(pos.relative(face)),face);
    }
    public static List<Quad> parts(ClientLevel level, BlockPos pos, BlockState state, List<BlockStateModelPart> parts) {
        var result = new ArrayList<Quad>();
        for (var part : parts) for (int side=0;side<=6;side++) {
            var face=side==6?null:Direction.values()[side];
            if (!culled(level,pos,state,face)) for (var quad:part.getQuads(face)) { result.add(new Quad(quad,part.useAmbientOcclusion())); }
        }
        return List.copyOf(result);
    }
    private static List<Quad> vanilla(ClientLevel level, BlockPos pos, BlockState state, BlockStateModel model) {
        var parts = new ArrayList<BlockStateModelPart>();
        model.collectParts(RandomSource.create(state.getSeed(pos)),parts);
        return parts(level,pos,state,parts);
    }
}

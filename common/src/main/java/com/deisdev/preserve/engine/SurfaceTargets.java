package com.deisdev.preserve.engine;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Shared preview/application geometry: center first, then rows in a fixed world-axis order. */
public final class SurfaceTargets {
    private SurfaceTargets() {}
    public static List<BlockPos> positions(BlockPos center, Direction face) {
        var positions = new ArrayList<BlockPos>(9);
        positions.add(center.immutable());
        for (int row = -1; row <= 1; row++) {
            for (int column = -1; column <= 1; column++) {
                if (row == 0 && column == 0) { continue; }
                positions.add(switch (face.getAxis()) {
                    case X -> center.offset(0, row, column);
                    case Y -> center.offset(column, 0, row);
                    case Z -> center.offset(column, row, 0);
                });
            }
        }
        return List.copyOf(positions);
    }
    public static boolean exposed(Level level, BlockPos pos, Direction face) {
        var outside = pos.relative(face);
        // Check loaded state before reading either side. ClientLevel.hasChunkAt is not an existence check.
        if (!loaded(level, pos) || !loaded(level, outside) || level.isOutsideBuildHeight(pos) || level.getBlockState(pos).isAir()) { return false; }
        return !level.getBlockState(outside).isFaceSturdy(level, outside, face.getOpposite());
    }
    private static boolean loaded(Level level, BlockPos pos) { return level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4); }
    public static BlockHitResult hit(BlockPos pos, Direction face) {
        return new BlockHitResult(Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5), face, pos, false);
    }
}

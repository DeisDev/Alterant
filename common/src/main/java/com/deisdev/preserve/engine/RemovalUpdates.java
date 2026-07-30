package com.deisdev.preserve.engine;

import com.deisdev.preserve.api.Action;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CoralBlock;
import net.minecraft.world.level.block.CoralPlantBlock;
import net.minecraft.world.level.block.CoralFanBlock;
import net.minecraft.world.level.block.CoralWallFanBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.FallingBlock;
import com.deisdev.preserve.mixin.FallingBlockAccessor;

final class RemovalUpdates {
    private RemovalUpdates() {}
    static void validate(ServerLevel level, Treatment treatment) {
        if (refreshesShape(level, treatment)) {
            var pos = BlockPos.of(treatment.position());
            for (var direction : Direction.values()) {
                if (!level.hasChunkAt(pos.relative(direction))) { throw new IllegalArgumentException("Load neighboring chunks before restoring this shape"); }
            }
        }
    }

    private static boolean refreshesShape(ServerLevel level, Treatment treatment) {
        if (!treatment.actions().contains(Action.STRUCTURAL_CHANGE)) { return false; }
        var block = level.getBlockState(BlockPos.of(treatment.position())).getBlock();
        return block instanceof FenceBlock || block instanceof WallBlock || block instanceof StairBlock;
    }

    static void afterRemoval(ServerLevel level, Treatment treatment) {
        var pos = BlockPos.of(treatment.position());
        var block = level.getBlockState(pos).getBlock();
        if (refreshesShape(level, treatment)) {
            var state = level.getBlockState(pos);
            level.setBlockAndUpdate(pos, Block.updateFromNeighbourShapes(state, level, pos));
        }
        if (treatment.actions().contains(Action.GRAVITY) && !treatment.actions().contains(Action.SCHEDULED_BLOCK_TICK) && block instanceof FallingBlock) {
            level.scheduleTick(pos, block, ((FallingBlockAccessor) block).preserve$fallDelay());
        }
        if (treatment.actions().contains(Action.ENVIRONMENTAL_CHANGE) && !treatment.actions().contains(Action.SCHEDULED_BLOCK_TICK)
                && (block instanceof CoralBlock || block instanceof CoralPlantBlock || block instanceof CoralFanBlock || block instanceof CoralWallFanBlock)) {
            // Selective coral protection consumes the canceled transformation, not the entire tick route.
            // Recheck through the native scheduler with coral's normal delay. Temporal work already retains its original delay.
            level.scheduleTick(pos, block, 60 + level.getRandom().nextInt(40));
        }
    }
}

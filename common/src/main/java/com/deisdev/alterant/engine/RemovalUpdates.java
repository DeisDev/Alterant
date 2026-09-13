package com.deisdev.alterant.engine;

import com.deisdev.alterant.api.Action;
import com.deisdev.alterant.api.PreservationException;
import com.deisdev.alterant.mixin.FallingBlockAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CoralBlock;
import net.minecraft.world.level.block.CoralFanBlock;
import net.minecraft.world.level.block.CoralPlantBlock;
import net.minecraft.world.level.block.CoralWallFanBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;

final class RemovalUpdates {
    private RemovalUpdates() {}
    static void validate(ServerLevel level, Treatment treatment) {
        if (treatment.actions().contains(Action.STRUCTURAL_CHANGE)) {
            var pos = BlockPos.of(treatment.position());
            int radius = level.getBlockState(pos).getBlock() instanceof TrapDoorBlock ? 2 : refreshesShape(level, treatment) ? 1 : 0;
            if (radius == 0) { return; }
            // A radius below one chunk can span at most two chunks per axis. Include redstone's adjacent conductors.
            for (int x : new int[] {-radius, radius}) {
                for (int z : new int[] {-radius, radius}) {
                    if (!level.hasChunkAt(pos.offset(x, 0, z))) { throw new PreservationException(Component.translatable("error.alterant.shape_neighbor_load")); }
                }
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
        if (treatment.actions().contains(Action.STRUCTURAL_CHANGE) && block instanceof TrapDoorBlock) {
            level.getBlockState(pos).handleNeighborChanged(level, pos, block, null, false);
        }
        if (treatment.actions().contains(Action.GRAVITY) && !treatment.actions().contains(Action.SCHEDULED_BLOCK_TICK) && block instanceof FallingBlock) {
            level.scheduleTick(pos, block, ((FallingBlockAccessor) block).alterant$fallDelay());
        }
        if (treatment.actions().contains(Action.ENVIRONMENTAL_CHANGE) && !treatment.actions().contains(Action.SCHEDULED_BLOCK_TICK)
                && (block instanceof CoralBlock || block instanceof CoralPlantBlock || block instanceof CoralFanBlock || block instanceof CoralWallFanBlock)) {
            // Selective coral protection consumes the canceled transformation, not the entire tick route.
            // Recheck through the native scheduler with coral's normal delay. Temporal work already retains its original delay.
            level.scheduleTick(pos, block, 60 + level.getRandom().nextInt(40));
        }
    }
}

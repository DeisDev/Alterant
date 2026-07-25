package com.deisdev.preserve.engine;

import com.deisdev.preserve.api.Action;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CoralBlock;
import net.minecraft.world.level.block.CoralPlantBlock;
import net.minecraft.world.level.block.CoralFanBlock;
import net.minecraft.world.level.block.CoralWallFanBlock;

final class RemovalUpdates {
    private RemovalUpdates() {}
    static void afterRemoval(ServerLevel level, Treatment treatment) {
        var pos = BlockPos.of(treatment.position());
        var block = level.getBlockState(pos).getBlock();
        if (treatment.actions().contains(Action.ENVIRONMENTAL_CHANGE) && !treatment.actions().contains(Action.SCHEDULED_BLOCK_TICK)
                && (block instanceof CoralBlock || block instanceof CoralPlantBlock || block instanceof CoralFanBlock || block instanceof CoralWallFanBlock)) {
            // Selective coral protection consumes the canceled transformation, not the entire tick route.
            // Recheck through the native scheduler with coral's normal delay. Temporal work already retains its original delay.
            level.scheduleTick(pos, block, 60 + level.getRandom().nextInt(40));
        }
    }
}

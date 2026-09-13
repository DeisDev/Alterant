package com.deisdev.alterant.integration;

import com.deisdev.alterant.api.PreservationContext;
import com.deisdev.alterant.api.PreservationException;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;

final class VanillaTargets {
    private VanillaTargets() {}
    static List<BlockPos> resolve(PreservationContext context) {
        var state = context.state();
        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            var otherPos = context.pos().relative(ChestBlock.getConnectedDirection(state));
            if (!context.level().hasChunkAt(otherPos)) { throw new PreservationException(Component.translatable("error.alterant.chest_load")); }
            var other = context.level().getBlockState(otherPos);
            if (!other.is(state.getBlock()) || other.getValue(ChestBlock.TYPE) == ChestType.SINGLE
                    || other.getValue(ChestBlock.TYPE) == state.getValue(ChestBlock.TYPE)
                    || other.getValue(ChestBlock.FACING) != state.getValue(ChestBlock.FACING)
                    || !otherPos.relative(ChestBlock.getConnectedDirection(other)).equals(context.pos())) {
                throw new PreservationException(Component.translatable("error.alterant.chest_incomplete"));
            }
            return List.of(context.pos(), otherPos);
        }
        return List.of(context.pos());
    }
}

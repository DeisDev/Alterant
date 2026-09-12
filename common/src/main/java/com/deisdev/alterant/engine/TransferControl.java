package com.deisdev.alterant.engine;

import com.deisdev.alterant.api.Action;
import com.deisdev.alterant.api.Formulation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Transfer policy is independent of ticking, player use and energy. */
public final class TransferControl {
    private TransferControl() {}
    public static boolean blocked(Level level, BlockPos pos, boolean insertion, Direction face) {
        if (!level.hasChunkAt(pos)) { return true; }
        if (at(level, pos, insertion, face, false)) { return true; }
        var other = chestPartner(level, pos);
        return other != null && (!level.hasChunkAt(other) || at(level, other, insertion, face, false));
    }
    static boolean at(Level level, BlockPos pos, boolean insertion, Direction face, boolean energyOrManual) {
        var record = ((PreservationLevel) level).alterant$treatments().get(pos.asLong());
        if (record == null || !record.actions().contains(Action.RESOURCE_TRANSFER)) { return false; }
        return record.formulation() != Formulation.TRANSFER_SEAL
                || !energyOrManual && record.options().transfer().orElse(TransferPolicy.DEFAULT).blocks(insertion, face);
    }
    static BlockPos chestPartner(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                ? pos.relative(ChestBlock.getConnectedDirection(state)) : null;
    }
}

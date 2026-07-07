package com.deisdev.preserve.engine;

import com.deisdev.preserve.api.Action;
import java.lang.ref.WeakReference;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Handles acquired before coating still consult the current state; the guard itself never retains a world. */
public final class TransferGuard {
    private final WeakReference<Level> level;
    private final WeakReference<BlockEntity> entity;
    private final boolean entityBound;
    private final BlockPos pos;

    public TransferGuard(Level level, BlockPos pos) {
        this.level = new WeakReference<>(level);
        this.pos = pos.immutable();
        var current = level.hasChunkAt(pos) ? level.getBlockEntity(pos) : null;
        entity = new WeakReference<>(current);
        entityBound = current != null;
    }

    public boolean allowsMutation() {
        var world = level.get();
        if (world == null || world.isClientSide() || !world.hasChunkAt(pos)) { return false; }
        if (world instanceof net.minecraft.server.level.ServerLevel server && !server.getServer().isSameThread()) { return false; }
        var expected = entity.get();
        if (entityBound && (expected == null || expected.isRemoved() || world.getBlockEntity(pos) != expected)) { return false; }
        // Vanilla exposes a combined double-chest handler from either half. Guard its other known member too.
        if (expected instanceof net.minecraft.world.level.block.entity.ChestBlockEntity) {
            var state = world.getBlockState(pos);
            if (state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock
                    && state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE) != net.minecraft.world.level.block.state.properties.ChestType.SINGLE) {
                var other = pos.relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(state));
                if (!world.hasChunkAt(other) || TickGate.blocks(world, other, Action.RESOURCE_TRANSFER)) { return false; }
            }
        }
        return !TickGate.blocks(world, pos, Action.RESOURCE_TRANSFER);
    }
}

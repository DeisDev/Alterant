package com.deisdev.alterant.engine;

import java.lang.ref.WeakReference;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Identity captured when a handler is acquired, including an absent or unloaded endpoint. */
final class TransferEndpoint {
    final BlockPos pos;
    private final WeakReference<BlockEntity> entity;
    private final boolean entityBound;
    private final Block block;
    TransferEndpoint(Level level, BlockPos pos) {
        this(level, pos, level.hasChunkAt(pos) ? level.getBlockEntity(pos) : null);
    }
    TransferEndpoint(Level level, BlockPos pos, BlockEntity expected) {
        this.pos = pos.immutable();
        var loaded = level.hasChunkAt(pos);
        var current = expected;
        entity = new WeakReference<>(current); entityBound = current != null;
        block = loaded ? level.getBlockState(pos).getBlock() : null;
    }
    boolean valid(Level world) {
        if (block == null || !world.hasChunkAt(pos) || world.getBlockState(pos).getBlock() != block) { return false; }
        var expected = entity.get();
        return entityBound ? expected != null && !expected.isRemoved() && world.getBlockEntity(pos) == expected : world.getBlockEntity(pos) == null;
    }
}

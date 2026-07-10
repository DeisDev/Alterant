package com.deisdev.preserve.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Short-lived callback context. Adapters must never retain its level or block entity beyond the callback. */
public record PreservationContext(ServerLevel level, BlockPos pos, BlockState state, Formulation formulation, String owner) {
    public PreservationContext { pos = pos.immutable(); }
}

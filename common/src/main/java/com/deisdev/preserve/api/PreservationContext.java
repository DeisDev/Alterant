package com.deisdev.preserve.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Short-lived callback context. Mask and untreated surface permission checks have no formulation. Never retain its world beyond the callback. */
public record PreservationContext(ServerLevel level, BlockPos pos, BlockState state, @org.jspecify.annotations.Nullable Formulation formulation, String owner) {
    public PreservationContext { pos = pos.immutable(); }
}

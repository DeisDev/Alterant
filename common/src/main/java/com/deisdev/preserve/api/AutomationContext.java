package com.deisdev.preserve.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** The actual actuator, never a remembered owner or synthetic player. Exists only during a server operation. */
public record AutomationContext(ServerLevel level, BlockPos source, BlockState state, Kind kind) {
    public enum Kind { DISPENSER }
    public AutomationContext {
        java.util.Objects.requireNonNull(level); java.util.Objects.requireNonNull(state); java.util.Objects.requireNonNull(kind);
        source = source.immutable();
    }
}

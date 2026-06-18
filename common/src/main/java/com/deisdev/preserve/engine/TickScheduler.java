package com.deisdev.preserve.engine;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ticks.ScheduledTick;

/** The bridge is implemented on each world's existing vanilla scheduler. */
public interface TickScheduler<T> {
    void preserve$bind(PreservationService service, boolean fluid);
    List<ScheduledTick<T>> preserve$take(BlockPos pos);
}

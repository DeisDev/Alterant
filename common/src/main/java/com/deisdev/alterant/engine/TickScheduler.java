package com.deisdev.alterant.engine;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ticks.ScheduledTick;

/** The bridge is implemented on each world's existing vanilla scheduler. */
public interface TickScheduler<T> {
    record Pending<T>(ScheduledTick<T> tick, boolean collected) {}
    void alterant$bind(PreservationService service, boolean fluid);
    List<Pending<T>> alterant$take(BlockPos pos);
}

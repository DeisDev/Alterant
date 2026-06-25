package com.deisdev.preserve.engine;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;

/** Bounded return to vanilla queues, including the collected-plus-future case at a single identity. */
public final class DeferredTickStore {
    private static final int RESUME_BUDGET = 256;
    private final ServerLevel level;
    private final TreatmentStore store;
    private final LongLinkedOpenHashSet active = new LongLinkedOpenHashSet();
    private boolean returning;

    public DeferredTickStore(ServerLevel level, TreatmentStore store) {
        this.level = level;
        this.store = store;
    }

    boolean isReturning() { return returning; }

    public void start(Treatment record) {
        if (record.deferred().isEmpty()) { return; }
        var ticks = record.deferred().stream().sorted(Comparator.comparingLong(DeferredTick::remainingDelay)
                .thenComparingInt(DeferredTick::priority).thenComparingLong(DeferredTick::order)).toList();
        store.putResuming(new ResumingTicks(record.position(), record.blockId(), ticks, level.getGameTime()));
        pump(record.position());
        if (store.resuming(record.position()) != null) { active.add(record.position()); }
    }

    public void tick() {
        // Only resumptions are visited, with a fixed budget. Frozen targets are never scanned each tick.
        int count = Math.min(RESUME_BUDGET, active.size());
        for (int i = 0; i < count; i++) {
            long position = active.removeFirstLong();
            pump(position);
            var remaining = store.resuming(position);
            if (remaining != null && remaining.startedAt() >= 0) { active.add(position); }
        }
    }

    public void cancel(long position) {
        active.remove(position);
        store.removeResuming(position);
    }

    public void chunkReady(long chunkKey) {
        for (var work : store.chunkResumptions(chunkKey)) {
            if (work.startedAt() < 0) {
                store.putResuming(new ResumingTicks(work.position(), work.blockId(), work.ticks(), level.getGameTime()));
            }
            active.add(work.position());
        }
    }

    public void chunkUnloaded(long chunkKey) {
        for (var work : store.chunkResumptions(chunkKey)) {
            active.remove(work.position());
            if (work.startedAt() < 0) { continue; }
            var remaining = work.ticks().stream().map(tick -> new DeferredTick(tick.fluid(), tick.type(),
                    Math.max(0, tick.resumeTime(work.startedAt()) - level.getGameTime()), tick.priority(), tick.order(), tick.collected())).toList();
            store.putResuming(new ResumingTicks(work.position(), work.blockId(), remaining, -1));
        }
    }

    private void pump(long position) {
        var work = store.resuming(position);
        if (work == null || work.startedAt() < 0) { return; }
        var pos = BlockPos.of(position);
        if (!level.hasChunkAt(pos)) { chunkUnloaded(TreatmentStore.chunkKey(position)); return; }
        if (!BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).equals(work.blockId())) {
            cancel(position);
            return;
        }
        var waiting = new ArrayList<DeferredTick>();
        for (var tick : work.ticks()) {
            if (!schedule(pos, tick, work.startedAt())) { waiting.add(tick); }
        }
        if (waiting.isEmpty()) { store.removeResuming(position); }
        else if (!waiting.equals(work.ticks())) { store.putResuming(new ResumingTicks(position, work.blockId(), waiting, work.startedAt())); }
    }

    private boolean schedule(BlockPos pos, DeferredTick tick, long start) {
        // Only this insertion bypasses the logical occupancy of work still held by Preserve.
        returning = true;
        try {
        if (tick.fluid()) {
            var type = level.getFluidState(pos).getType();
            if (!BuiltInRegistries.FLUID.getKey(type).equals(tick.type())) { return true; }
            var scheduler = level.getFluidTicks();
            if (scheduler.hasScheduledTick(pos, type) || scheduler.willTickThisTick(pos, type)) { return false; }
            scheduler.schedule(new ScheduledTick<>(type, pos, tick.resumeTime(start), TickPriority.byValue(tick.priority()), tick.order()));
        } else {
            var type = level.getBlockState(pos).getBlock();
            if (!BuiltInRegistries.BLOCK.getKey(type).equals(tick.type())) { return true; }
            var scheduler = level.getBlockTicks();
            if (scheduler.hasScheduledTick(pos, type) || scheduler.willTickThisTick(pos, type)) { return false; }
            scheduler.schedule(new ScheduledTick<>(type, pos, tick.resumeTime(start), TickPriority.byValue(tick.priority()), tick.order()));
        }
            return true;
        } finally {
            returning = false;
        }
    }
}

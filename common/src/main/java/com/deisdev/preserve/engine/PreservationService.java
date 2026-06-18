package com.deisdev.preserve.engine;

import com.deisdev.preserve.api.Action;
import com.deisdev.preserve.api.Formulation;
import com.deisdev.preserve.mixin.SavedDataStorageAccessor;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;

/** Authoritative operations, called only on the server thread; tick gates only read the store. */
public final class PreservationService {
    public record Result(boolean changed, String message) {}
    private final ServerLevel level;
    private final TreatmentStore store;
    private final LongOpenHashSet inProgress = new LongOpenHashSet();

    public PreservationService(ServerLevel level) {
        this.level = level;
        var storage = level.getDataStorage();
        var file = TreatmentStore.TYPE.id().withSuffix(".dat")
                .resolveAgainst(((SavedDataStorageAccessor) storage).preserve$dataFolder());
        var loaded = storage.get(TreatmentStore.TYPE);
        // Vanilla logs decoding errors then returns null. An existing file must never be replaced with an empty store.
        if (loaded == null && Files.exists(file)) {
            throw new IllegalStateException("Cannot read Preserve treatments at " + file
                    + ". Restore a backup or use the matching Preserve version; the file was not overwritten.");
        }
        store = loaded == null ? storage.computeIfAbsent(TreatmentStore.TYPE) : loaded;
        ((PreservationLevel) level).preserve$setTreatments(store);
        scheduler(false).preserve$bind(this, false);
        scheduler(true).preserve$bind(this, true);
    }

    public static PreservationService get(ServerLevel level) {
        return java.util.Objects.requireNonNull(((PreservationLevel) level).preserve$service(), "Preserve level not initialized");
    }

    public TreatmentStore store() { return store; }

    @SuppressWarnings("unchecked")
    private <T> TickScheduler<T> scheduler(boolean fluid) {
        return (TickScheduler<T>) (Object) (fluid ? level.getFluidTicks() : level.getBlockTicks());
    }

    public Result applyTemporal(BlockPos pos, String owner, boolean replace) {
        checkThread();
        if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos)) { return new Result(false, "Target is not loaded"); }
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.is(Blocks.MOVING_PISTON)
                || state.is(Blocks.PISTON_HEAD) || state.is(Blocks.NETHER_PORTAL)
                || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY)) {
            return new Result(false, "This target cannot be preserved safely");
        }
        Treatment old = store.get(pos.asLong());
        if (old != null && old.formulation() == Formulation.TEMPORAL_STASIS) { return new Result(false, "Already treated"); }
        if (old != null && !replace) { return new Result(false, "Remove the existing coating first"); }
        if (!inProgress.add(pos.asLong())) { return new Result(false, "Target is busy"); }
        try {
            var actions = EnumSet.of(Action.BLOCK_ENTITY_TICK, Action.SCHEDULED_BLOCK_TICK,
                    Action.RANDOM_BLOCK_TICK, Action.SCHEDULED_FLUID_TICK, Action.RANDOM_FLUID_TICK,
                    Action.PRECIPITATION, Action.CLIENT_TICK, Action.BLOCK_EVENT, Action.PISTON_MOVEMENT);
            var record = new Treatment(pos.asLong(), Formulation.TEMPORAL_STASIS,
                    BuiltInRegistries.BLOCK.getKey(state.getBlock()), actions, Map.of(), List.of(),
                    List.of("deisdev:standard_ticks"), Map.of(), owner);
            store.put(record);
            capturePending(pos);
            level.getChunkAt(pos).markUnsaved();
            return new Result(true, "Standard ticks paused; external controllers and absolute-time work require integration");
        } finally {
            inProgress.remove(pos.asLong());
        }
    }

    public Result remove(BlockPos pos) {
        checkThread();
        if (!level.hasChunkAt(pos)) { return new Result(false, "Target is not loaded"); }
        if (!inProgress.add(pos.asLong())) { return new Result(false, "Target is busy"); }
        try {
            Treatment treatment = store.remove(pos.asLong());
            if (treatment == null) { return new Result(false, "No coating here"); }
            if (matches(pos, treatment)) {
                // At most two entries per target; scheduling once is bounded and retains native priority/order.
                treatment.deferred().stream().sorted(java.util.Comparator.comparingLong(DeferredTick::order))
                        .forEach(tick -> resume(pos, tick));
            }
            level.getChunkAt(pos).markUnsaved();
            return new Result(true, "Coating removed");
        } finally {
            inProgress.remove(pos.asLong());
        }
    }

    /** Real removal/replacement discards obsolete work, without invoking any machine lifecycle method. */
    public void destroyed(BlockPos pos) {
        checkThread();
        store.remove(pos.asLong());
    }

    private boolean matches(BlockPos pos, Treatment record) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).equals(record.blockId());
    }

    public void capturePending(BlockPos pos) {
        for (ScheduledTick<Object> tick : this.<Object>scheduler(false).preserve$take(pos)) { retain(tick, false); }
        for (ScheduledTick<Object> tick : this.<Object>scheduler(true).preserve$take(pos)) { retain(tick, true); }
    }

    /** Returns true for suspended targets, including obsolete identities the vanilla dispatch would skip. */
    public boolean retain(ScheduledTick<?> tick, boolean fluid) {
        Treatment record = store.get(tick.pos().asLong());
        Action route = fluid ? Action.SCHEDULED_FLUID_TICK : Action.SCHEDULED_BLOCK_TICK;
        if (record == null || !record.actions().contains(route)) { return false; }
        if (!level.hasChunkAt(tick.pos())) { return false; }
        BlockState state = level.getBlockState(tick.pos());
        Object expected = fluid ? state.getFluidState().getType() : state.getBlock();
        if (tick.type() != expected) { return true; }
        Identifier id = fluid ? BuiltInRegistries.FLUID.getKey((Fluid) tick.type()) : BuiltInRegistries.BLOCK.getKey((Block) tick.type());
        long now = level.getGameTime();
        long delay = tick.triggerTick() <= now ? 0 : tick.triggerTick() - now;
        Treatment retained = record.retain(new DeferredTick(fluid, id, delay, tick.priority().getValue(), tick.subTickOrder()));
        if (retained != record) { store.put(retained); }
        return true;
    }

    public boolean hasRetained(BlockPos pos, Object type, boolean fluid) {
        Treatment record = store.get(pos.asLong());
        if (record == null) { return false; }
        Identifier id = fluid ? BuiltInRegistries.FLUID.getKey((Fluid) type) : BuiltInRegistries.BLOCK.getKey((Block) type);
        return record.deferred().stream().anyMatch(tick -> tick.fluid() == fluid && tick.type().equals(id));
    }

    public void chunkReady(net.minecraft.world.level.chunk.LevelChunk chunk) {
        for (Treatment record : store.chunkSnapshot(chunk.getPos().pack())) {
            BlockPos pos = BlockPos.of(record.position());
            if (!matches(pos, record)) { store.remove(record.position()); }
            else { capturePending(pos); }
        }
    }

    private void resume(BlockPos pos, DeferredTick tick) {
        if (tick.fluid()) {
            var current = level.getFluidState(pos).getType();
            if (BuiltInRegistries.FLUID.getKey(current).equals(tick.type())) {
                level.getFluidTicks().schedule(new ScheduledTick<>(current, pos.immutable(), tick.resumeTime(level.getGameTime()),
                        TickPriority.byValue(tick.priority()), level.nextSubTickCount()));
            }
        } else {
            var current = level.getBlockState(pos).getBlock();
            if (BuiltInRegistries.BLOCK.getKey(current).equals(tick.type())) {
                level.getBlockTicks().schedule(new ScheduledTick<>(current, pos.immutable(), tick.resumeTime(level.getGameTime()),
                        TickPriority.byValue(tick.priority()), level.nextSubTickCount()));
            }
        }
    }

    private void checkThread() {
        if (!level.getServer().isSameThread()) { throw new IllegalStateException("Preserve mutations require the server thread"); }
    }
}

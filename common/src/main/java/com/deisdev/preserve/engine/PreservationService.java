package com.deisdev.preserve.engine;

import com.deisdev.preserve.api.Action;
import com.deisdev.preserve.api.Formulation;
import com.deisdev.preserve.network.TreatmentSync;
import com.deisdev.preserve.rules.RuleRegistry;
import com.deisdev.preserve.rules.BlockCondition;
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

/** Authoritative operations, called only on the server thread; tick gates only read the store. */
public final class PreservationService {
    public record Result(boolean changed, String message) {}
    private final ServerLevel level;
    private final TreatmentStore store;
    private final DeferredTickStore deferred;
    private final LongOpenHashSet inProgress = new LongOpenHashSet();

    public PreservationService(ServerLevel level) {
        this.level = level;
        RuleRegistry.get(level.getServer());
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
        deferred = new DeferredTickStore(level, store);
        ((PreservationLevel) level).preserve$setTreatments(store);
        scheduler(false).preserve$bind(this, false);
        scheduler(true).preserve$bind(this, true);
    }

    public static PreservationService get(ServerLevel level) {
        return java.util.Objects.requireNonNull(((PreservationLevel) level).preserve$service(), "Preserve level not initialized");
    }

    public TreatmentStore store() { return store; }
    public void tickResumptions() { deferred.tick(); }
    public void chunkUnloaded(net.minecraft.world.level.ChunkPos chunk) { deferred.chunkUnloaded(chunk.pack()); }

    @SuppressWarnings("unchecked")
    private <T> TickScheduler<T> scheduler(boolean fluid) {
        return (TickScheduler<T>) (Object) (fluid ? level.getFluidTicks() : level.getBlockTicks());
    }

    public Result applyTemporal(BlockPos pos, String owner, boolean replace) {
        return apply(pos, Formulation.TEMPORAL_STASIS, owner, replace);
    }

    public Result apply(BlockPos pos, Formulation formulation, String owner, boolean replace) {
        checkThread();
        if (com.deisdev.preserve.platform.Services.PLATFORM.transferInProgress()) { return new Result(false, "Wait for the current transfer to finish"); }
        if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos)) { return new Result(false, "Target is not loaded"); }
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.is(Blocks.MOVING_PISTON)
                || state.is(Blocks.PISTON_HEAD) || state.is(Blocks.NETHER_PORTAL)
                || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY)) {
            return new Result(false, "This target cannot be preserved safely");
        }
        var rules = RuleRegistry.get(level.getServer());
        var decision = rules.evaluate(state, formulation);
        if (!decision.allowed()) { return new Result(false, decision.denial()); }
        if (formulation == Formulation.TEMPORAL_STASIS && !rules.policy().allowPartial()) {
            return new Result(false, "The server requires a verified integration for this target");
        }
        Treatment old = store.get(pos.asLong());
        if (old == null && store.chunkSize(net.minecraft.world.level.ChunkPos.pack(pos)) >= rules.policy().chunkLimit()) {
            return new Result(false, "This chunk has reached its coating limit");
        }
        if (store.resuming(pos.asLong()) != null) { return new Result(false, "Pending work is resuming; try again shortly"); }
        if (old != null && old.formulation() == formulation) { return new Result(false, "Already treated"); }
        if (old != null && !replace) { return new Result(false, "Remove the existing coating first"); }
        if (!inProgress.add(pos.asLong())) { return new Result(false, "Target is busy"); }
        try {
            var actions = EnumSet.noneOf(Action.class);
            var structure = new java.util.HashMap<String, String>();
            for (var protection : decision.protections()) {
                actions.add(protection.action());
                for (String property : protection.properties()) {
                    structure.put(property, BlockCondition.valueName(state, state.getBlock().getStateDefinition().getProperty(property)));
                }
            }
            // A future deliberate switch must retain existing queued work; its normal resumption is coordinated on removal.
            if (old != null && !old.deferred().isEmpty() && !actions.containsAll(old.actions())) {
                return new Result(false, "Remove the existing coating before switching its suspended tick routes");
            }
            var record = new Treatment(pos.asLong(), formulation,
                    BuiltInRegistries.BLOCK.getKey(state.getBlock()), actions, structure, old == null ? List.of() : old.deferred(),
                    decision.protections().stream().map(protection -> protection.rule().toString()).distinct().toList(), Map.of(), owner,
                    decision.protections());
            store.put(record);
            capturePending(pos);
            level.getChunkAt(pos).markUnsaved();
            TreatmentSync.changed(level, pos);
            return new Result(true, formulation == Formulation.TEMPORAL_STASIS
                    ? "Standard ticks paused; external controllers and absolute-time work require integration" : "Coating applied");
        } finally {
            inProgress.remove(pos.asLong());
        }
    }

    public Result remove(BlockPos pos) {
        checkThread();
        if (com.deisdev.preserve.platform.Services.PLATFORM.transferInProgress()) { return new Result(false, "Wait for the current transfer to finish"); }
        if (!level.hasChunkAt(pos)) { return new Result(false, "Target is not loaded"); }
        if (!inProgress.add(pos.asLong())) { return new Result(false, "Target is busy"); }
        try {
            Treatment treatment = store.remove(pos.asLong());
            if (treatment == null) { return new Result(false, "No coating here"); }
            if (matches(pos, treatment)) {
                deferred.start(treatment);
            }
            level.getChunkAt(pos).markUnsaved();
            TreatmentSync.changed(level, pos);
            return new Result(true, "Coating removed");
        } finally {
            inProgress.remove(pos.asLong());
        }
    }

    /** Real removal/replacement discards obsolete work, without invoking any machine lifecycle method. */
    public void destroyed(BlockPos pos) {
        checkThread();
        var removed = store.remove(pos.asLong());
        deferred.cancel(pos.asLong());
        if (removed != null) { TreatmentSync.changed(level, pos); }
    }

    private boolean matches(BlockPos pos, Treatment record) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).equals(record.blockId());
    }

    public void capturePending(BlockPos pos) {
        var treatment = store.get(pos.asLong());
        if (treatment == null) { return; }
        if (treatment.actions().contains(Action.SCHEDULED_BLOCK_TICK)) {
            for (var pending : this.<Object>scheduler(false).preserve$take(pos)) { retain(pending.tick(), false, pending.collected()); }
        }
        if (treatment.actions().contains(Action.SCHEDULED_FLUID_TICK)) {
            for (var pending : this.<Object>scheduler(true).preserve$take(pos)) { retain(pending.tick(), true, pending.collected()); }
        }
    }

    /** Returns true for suspended targets, including obsolete identities the vanilla dispatch would skip. */
    public boolean retain(ScheduledTick<?> tick, boolean fluid) {
        return retain(tick, fluid, false);
    }

    public boolean retain(ScheduledTick<?> tick, boolean fluid, boolean collected) {
        var resuming = store.resuming(tick.pos().asLong());
        if (resuming != null && !deferred.isReturning() && !collected && hasRetained(tick.pos(), tick.type(), fluid)) {
            // A future native identity remains occupied while the already-collected callback resumes first.
            return true;
        }
        Treatment record = store.get(tick.pos().asLong());
        Action route = fluid ? Action.SCHEDULED_FLUID_TICK : Action.SCHEDULED_BLOCK_TICK;
        if (record == null || !record.actions().contains(route)) { return false; }
        checkThread();
        if (!level.hasChunkAt(tick.pos())) { return false; }
        BlockState state = level.getBlockState(tick.pos());
        Object expected = fluid ? state.getFluidState().getType() : state.getBlock();
        if (tick.type() != expected) { return true; }
        Identifier id = fluid ? BuiltInRegistries.FLUID.getKey((Fluid) tick.type()) : BuiltInRegistries.BLOCK.getKey((Block) tick.type());
        long now = level.getGameTime();
        long delay = tick.triggerTick() <= now ? 0 : tick.triggerTick() - now;
        Treatment retained = record.retain(new DeferredTick(fluid, id, delay, tick.priority().getValue(), tick.subTickOrder(), collected));
        if (retained != record) { store.put(retained); }
        return true;
    }

    public boolean hasRetained(BlockPos pos, Object type, boolean fluid) {
        if (deferred.isReturning()) { return false; }
        Treatment record = store.get(pos.asLong());
        var resuming = store.resuming(pos.asLong());
        if (record == null && resuming == null) { return false; }
        Identifier id = fluid ? BuiltInRegistries.FLUID.getKey((Fluid) type) : BuiltInRegistries.BLOCK.getKey((Block) type);
        var pending = record != null ? record.deferred() : resuming.ticks();
        return pending.stream().anyMatch(tick -> !tick.collected() && tick.fluid() == fluid && tick.type().equals(id));
    }

    public void chunkReady(net.minecraft.world.level.chunk.LevelChunk chunk) {
        for (Treatment record : store.chunkSnapshot(chunk.getPos().pack())) {
            BlockPos pos = BlockPos.of(record.position());
            if (!matches(pos, record)) { store.remove(record.position()); }
            else { capturePending(pos); }
        }
        deferred.chunkReady(chunk.getPos().pack());
    }

    private void checkThread() {
        if (!level.getServer().isSameThread()) { throw new IllegalStateException("Preserve mutations require the server thread"); }
    }
}

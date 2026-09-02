package com.deisdev.preserve.engine;

import com.deisdev.preserve.api.Action;
import com.deisdev.preserve.api.Formulation;
import com.deisdev.preserve.api.PreservationContext;
import com.deisdev.preserve.api.PreservationPermission.Change;
import com.deisdev.preserve.integration.IntegrationRegistry;
import com.deisdev.preserve.integration.PlayerAccess;
import com.deisdev.preserve.item.CompoundCharge;
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
    public record Result(int changedPositions, String message) {
        public Result { if (message == null || message.isBlank()) { message = "Preservation could not complete"; } }
        public Result(boolean changed, String message) { this(changed ? 1 : 0, message); }
        public boolean changed() { return changedPositions > 0; }
    }
    private record Prepared(Treatment old, Treatment next, PreservationContext context,
                            net.minecraft.world.level.block.entity.BlockEntity entity) {}
    private final ServerLevel level;
    private final TreatmentStore store;
    private final DeferredTickStore deferred;
    private final LongOpenHashSet inProgress = new LongOpenHashSet();

    public PreservationService(ServerLevel level) {
        this.level = level;
        IntegrationRegistry.lock();
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

    /** Called only for a chunk Minecraft is actively ticking. Unloaded time never consumes serum. */
    public void tickSerums(net.minecraft.world.level.chunk.LevelChunk chunk) {
        var records = store.acceleratedChunk(chunk.getPos().pack());
        for (var record : records) {
            var pos = BlockPos.of(record.position());
            if (!matches(pos, record)) { destroyed(pos); }
            else if (!record.acceleration().orElseThrow().elapse()) {
                store.remove(record.position());
                changed(pos);
            }
        }
        if (!records.isEmpty()) { store.setDirty(); }
    }

    public <T> ScheduledTick<T> accelerateScheduled(ScheduledTick<T> tick, boolean fluid) {
        if (fluid) { return tick; }
        var record = store.get(tick.pos().asLong());
        if (record == null || !record.actions().contains(Action.ACCELERATE_SCHEDULED_BLOCK) || record.acceleration().isEmpty()
                || !level.hasChunkAt(tick.pos()) || level.getBlockState(tick.pos()).getBlock() != tick.type()) { return tick; }
        long now = level.getGameTime();
        long remaining = tick.triggerTick() <= now ? 0 : tick.triggerTick() - now;
        return new ScheduledTick<>(tick.type(), tick.pos(), now + record.acceleration().get().delay(remaining), tick.priority(), tick.subTickOrder());
    }

    private void acceleratePending(BlockPos pos) {
        for (var pending : this.<Block>scheduler(false).preserve$take(pos)) {
            // Preserve type/position, priority and order; the insertion hook scales the remaining delay once.
            level.getBlockTicks().schedule(pending.tick());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> TickScheduler<T> scheduler(boolean fluid) {
        return (TickScheduler<T>) (Object) (fluid ? level.getFluidTicks() : level.getBlockTicks());
    }

    public Result applyTemporal(BlockPos pos, String owner, boolean replace) {
        return apply(pos, Formulation.TEMPORAL_STASIS, owner, replace);
    }

    public Result apply(BlockPos pos, Formulation formulation, String owner, boolean replace) {
        return apply(pos, formulation, owner, replace, TargetLink.LIMIT);
    }

    /** Available charges are checked for the entire logical target before any mutation. */
    public Result apply(BlockPos pos, Formulation formulation, String owner, boolean replace, int available) {
        return apply(pos, formulation, owner, replace, available, null);
    }

    /** The item check must be read-only and verify the held tool, formulation and available charges on the server. */
    public Result applyFromPlayer(BlockPos pos, Formulation formulation, net.minecraft.server.level.ServerPlayer player,
                                  boolean replace, int available, java.util.function.BooleanSupplier itemReady) {
        return apply(pos, formulation, player.getStringUUID(), replace, available, new PlayerAccess(player, itemReady));
    }

    /** The brush's inventory cost commits with the coating, before scheduler notifications or adapter observations. */
    public Result applyWithBrush(BlockPos pos, net.minecraft.server.level.ServerPlayer player, boolean replace) {
        return applyWithBrush(pos, player, replace, TargetLink.LIMIT);
    }

    Result applyWithBrush(BlockPos pos, net.minecraft.server.level.ServerPlayer player, boolean replace, int limit) {
        checkThread();
        CompoundCharge cost;
        try { cost = CompoundCharge.capture(player); }
        catch (IllegalArgumentException error) { return new Result(false, error.getMessage()); }
        return apply(pos, cost.formulation(), player.getStringUUID(), replace, Math.min(limit, cost.available()), new PlayerAccess(player, cost::ready), cost);
    }

    private Result apply(BlockPos pos, Formulation formulation, String owner, boolean replace, int available, PlayerAccess access) {
        return apply(pos, formulation, owner, replace, available, access, null);
    }

    private Result apply(BlockPos pos, Formulation formulation, String owner, boolean replace, int available, PlayerAccess access, CompoundCharge cost) {
        checkThread();
        if (CleanupJob.get(level.getServer()).blocksApplication()) { return new Result(false, "Uninstall preparation blocks new coatings; cancel cleanup to continue playing"); }
        if (com.deisdev.preserve.platform.Services.PLATFORM.transferInProgress()) { return new Result(false, "Wait for the current transfer to finish"); }
        if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos)) { return new Result(false, "Target is not loaded"); }
        if (!inProgress.add(pos.asLong())) { return new Result(false, "Target is busy"); }
        var locked = new LongOpenHashSet();
        locked.add(pos.asLong());
        try {
            var prepared = new java.util.ArrayList<Prepared>();
            CompoundCharge.Prepared payment = null;
            try {
                var context = new PreservationContext(level, pos, level.getBlockState(pos), formulation, owner);
                var entity = level.getBlockEntity(pos);
                if (access != null) { access.validate(context, Change.APPLY); }
                var targets = IntegrationRegistry.targets(context);
                if (targets.size() > available) { return new Result(false, "Not enough charges for the entire linked target"); }
                lockTargets(targets, locked);
                if (level.getBlockState(pos) != context.state() || level.getBlockEntity(pos) != entity) {
                    return new Result(false, "Target changed during integration validation");
                }
                var link = targets.size() == 1 ? java.util.Optional.<TargetLink>empty()
                        : java.util.Optional.of(new TargetLink(java.util.UUID.randomUUID(), targets.stream().map(BlockPos::asLong).toList()));
                for (var target : targets) { prepared.add(prepare(target, formulation, owner, replace, link, access)); }
                var added = new java.util.HashMap<Long, Integer>();
                for (var entry : prepared) {
                    if (entry.old() == null) { added.merge(TreatmentStore.chunkKey(entry.next().position()), 1, Integer::sum); }
                    validateIdentity(entry);
                    if (access != null) { access.validate(entry.context(), Change.APPLY); }
                }
                int limit = RuleRegistry.get(level.getServer()).policy().chunkLimit();
                for (var entry : added.entrySet()) {
                    if (store.chunkSize(entry.getKey()) + entry.getValue() > limit) { return new Result(false, "This chunk has reached its coating limit"); }
                }
                if (access != null) { access.validateItem(); }
                if (cost != null) { payment = cost.prepare(prepared.size()); }
            } catch (RuntimeException error) { return new Result(false, error.getMessage()); }
            // Publish the whole group before collecting work or notifying integrations; no callback can see half a coating.
            for (var entry : prepared) { store.put(entry.next()); }
            if (payment != null) { payment.commit(); }
            for (var entry : prepared) {
                capturePending(entry.context().pos());
                changed(entry.context().pos());
            }
            for (var entry : prepared) { IntegrationRegistry.observed(entry.context(), entry.next().adapters(), true); }
            boolean complete = prepared.stream().allMatch(entry -> entry.next().adapters().stream().anyMatch(com.deisdev.preserve.integration.AdapterSnapshot::complete));
            return new Result(prepared.size(), formulation == Formulation.TEMPORAL_STASIS
                    ? (complete ? "Machine paused with its registered integration"
                        : "Standard ticks paused; external controllers and absolute-time work require integration") : "Coating applied");
        } finally {
            for (long position : locked) { inProgress.remove(position); }
        }
    }

    private Prepared prepare(BlockPos pos, Formulation formulation, String owner, boolean replace, java.util.Optional<TargetLink> link, PlayerAccess access) {
        if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos)) { throw new IllegalArgumentException("Load every member of the linked target first"); }
        BlockState state = level.getBlockState(pos);
        if (unsafe(state)) {
            throw new IllegalArgumentException("This target cannot be preserved safely");
        }
        var rules = RuleRegistry.get(level.getServer());
        var decision = rules.evaluate(state, formulation);
        if (!decision.allowed()) { throw new IllegalArgumentException(decision.denial()); }
        Treatment old = store.get(pos.asLong());
        if (store.resuming(pos.asLong()) != null) { throw new IllegalArgumentException("Pending work is resuming; try again shortly"); }
        if (old != null && old.formulation() == formulation) { throw new IllegalArgumentException("Already treated"); }
        if (old != null && !replace) { throw new IllegalArgumentException("Remove the existing coating first"); }
        if (old != null && (!old.adapters().isEmpty() || old.link().isPresent())) {
            throw new IllegalArgumentException("Remove the integrated or linked coating before switching formulations");
        }
        var context = new PreservationContext(level, pos, state, formulation, owner);
        var entity = level.getBlockEntity(pos);
        if (access != null) { access.validate(context, Change.APPLY); }
        var integration = IntegrationRegistry.prepare(context);
        if (formulation == Formulation.TEMPORAL_STASIS && !rules.policy().allowPartial() && !integration.complete()) {
            throw new IllegalArgumentException("The server requires a verified integration for this target");
        }
        var actions = EnumSet.noneOf(Action.class);
        var structure = new java.util.HashMap<String, String>();
        for (var protection : decision.protections()) {
            actions.add(protection.action());
            for (String property : protection.properties()) {
                structure.put(property, BlockCondition.valueName(state, state.getBlock().getStateDefinition().getProperty(property)));
            }
        }
        if (old != null && !old.deferred().isEmpty() && !actions.containsAll(old.actions())) {
            throw new IllegalArgumentException("Remove the existing coating before switching its suspended tick routes");
        }
        var record = new Treatment(pos.asLong(), formulation, BuiltInRegistries.BLOCK.getKey(state.getBlock()), actions, structure,
                old == null ? List.of() : old.deferred(), decision.protections().stream().map(protection -> protection.rule().toString()).distinct().toList(),
                Map.of(), owner, decision.protections(), integration.snapshots(), link);
        return new Prepared(old, record, context, entity);
    }

    public Result remove(BlockPos pos) {
        return remove(pos, null);
    }

    public Result removeFromPlayer(BlockPos pos, net.minecraft.server.level.ServerPlayer player, java.util.function.BooleanSupplier itemReady) {
        return remove(pos, new PlayerAccess(player, itemReady));
    }

    private Result remove(BlockPos pos, PlayerAccess access) {
        checkThread();
        if (com.deisdev.preserve.platform.Services.PLATFORM.transferInProgress()) { return new Result(false, "Wait for the current transfer to finish"); }
        if (!level.hasChunkAt(pos)) { return new Result(false, "Target is not loaded"); }
        if (!inProgress.add(pos.asLong())) { return new Result(false, "Target is busy"); }
        var locked = new LongOpenHashSet();
        locked.add(pos.asLong());
        try {
            Treatment treatment = store.get(pos.asLong());
            if (treatment == null) { return new Result(false, "No coating here"); }
            var prepared = new java.util.ArrayList<Prepared>();
            try {
                var targets = treatment.link().map(link -> link.members().stream().map(BlockPos::of).toList()).orElseGet(() -> List.of(pos));
                lockTargets(targets, locked);
                for (var target : targets) {
                    if (!level.hasChunkAt(target)) { return new Result(false, "Load every member of the linked target before removal"); }
                    var record = store.get(target.asLong());
                    // A broken/replaced member can be absent or belong to a newer group. Never remove that newer coating.
                    if (record == null || !record.link().equals(treatment.link())) { continue; }
                    var context = new PreservationContext(level, target, level.getBlockState(target), record.formulation(), record.owner());
                    if (access != null) { access.validate(context, Change.REMOVE); }
                    prepared.add(new Prepared(record, record, context, level.getBlockEntity(target)));
                    if (matches(target, record)) {
                        IntegrationRegistry.validateResume(record.adapters());
                        RemovalUpdates.validate(level, record);
                    }
                }
                for (var entry : prepared) {
                    if (matches(entry.context().pos(), entry.old())) { IntegrationRegistry.resume(entry.context(), entry.old().adapters()); }
                }
                for (var entry : prepared) {
                    validateIdentity(entry);
                    RemovalUpdates.validate(level, entry.old());
                    if (access != null) { access.validate(entry.context(), Change.REMOVE); }
                }
                if (access != null) { access.validateItem(); }
            } catch (RuntimeException error) { return new Result(false, "Coating retained: " + error.getMessage()); }
            for (var entry : prepared) { store.remove(entry.old().position()); }
            for (var entry : prepared) {
                if (matches(entry.context().pos(), entry.old())) {
                    deferred.start(entry.old());
                    RemovalUpdates.afterRemoval(level, entry.old());
                }
                changed(entry.context().pos());
            }
            for (var entry : prepared) {
                if (matches(entry.context().pos(), entry.old())) { IntegrationRegistry.observed(entry.context(), entry.old().adapters(), false); }
            }
            return new Result(prepared.size(), "Coating removed");
        } finally {
            for (long position : locked) { inProgress.remove(position); }
        }
    }

    private void lockTargets(List<BlockPos> targets, LongOpenHashSet locked) {
        for (var target : targets) {
            if (locked.contains(target.asLong())) { continue; }
            if (!inProgress.add(target.asLong())) { throw new IllegalArgumentException("Linked target is busy"); }
            locked.add(target.asLong());
        }
    }

    private void validateIdentity(Prepared entry) {
        var pos = entry.context().pos();
        if (!level.hasChunkAt(pos) || level.getBlockState(pos) != entry.context().state()
                || level.getBlockEntity(pos) != entry.entity() || store.get(pos.asLong()) != entry.old()) {
            throw new IllegalArgumentException("Target changed during integration validation");
        }
    }

    private void changed(BlockPos pos) {
        level.getChunkAt(pos).markUnsaved();
        TreatmentSync.changed(level, pos);
    }

    static boolean unsafe(BlockState state) {
        return state.isAir() || state.getBlock() instanceof LiquidBlock || state.is(Blocks.MOVING_PISTON)
                || state.is(Blocks.PISTON_HEAD) || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY);
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

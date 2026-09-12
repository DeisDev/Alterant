package com.deisdev.preserve.engine;

import com.deisdev.preserve.Constants;
import com.deisdev.preserve.api.Action;
import com.deisdev.preserve.mixin.SavedDataStorageAccessor;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.storage.LevelResource;

/** One operator-started job owned by its server. No static world references, synchronous chunk waits or tick scans. */
public final class CleanupJob {
    public enum Phase { IDLE, CHECKING_FILES, CHECKING_CHUNKS, REMOVING, LOADING, RETURNING_WORK, SAVING, VERIFYING, COMPLETE, FAILED, CANCELED }
    private record ChunkTask(ServerLevel level, long key) {}
    private final MinecraftServer server;
    private Phase phase = Phase.IDLE;
    private List<ChunkTask> chunks = List.of();
    private List<Path> saveFiles = List.of();
    private final ArrayDeque<Long> positions = new ArrayDeque<>();
    private final ArrayDeque<ChunkPos> toLoad = new ArrayDeque<>();
    private final List<ChunkPos> leases = new ArrayList<>();
    private List<Long> group = List.of();
    private ServerLevel level;
    private long position;
    private int checkIndex, chunkIndex, removed, maxLeases, loadingSince;
    private long saveFailures;
    private String detail = "Back up the world before starting";
    private UUID operator;
    private CompletableFuture<?> work;
    private CompletableFuture<Optional<CompoundTag>> chunkRead;

    public CleanupJob(MinecraftServer server) { this.server = server; }
    public static CleanupJob get(MinecraftServer server) { return ((CleanupServer) server).preserve$cleanup(); }
    public Phase phase() { return phase; }
    public int maxLeases() { return maxLeases; }
    public int leasedChunks() { return leases.size(); }
    public boolean blocksApplication() { return phase != Phase.IDLE && phase != Phase.CANCELED; }
    public boolean running() { return phase.ordinal() >= Phase.CHECKING_FILES.ordinal() && phase.ordinal() <= Phase.VERIFYING.ordinal(); }
    public int remaining() {
        int remaining = 0;
        for (var dimension : server.getAllLevels()) {
            var store = PreservationService.get(dimension).store();
            remaining += store.size() + store.resumingSize() + store.maskSize();
        }
        return remaining;
    }
    public String status() { return "Cleanup " + phase.name().toLowerCase(java.util.Locale.ROOT) + ": " + removed + " coatings removed, " + remaining() + " records remaining. " + detail; }

    public boolean start(UUID operator) {
        requireThread();
        if (running() || phase == Phase.COMPLETE) { return false; }
        clearWork();
        this.operator = operator;
        removed = checkIndex = chunkIndex = maxLeases = 0;
        saveFailures = ((CleanupServer) server).preserve$saveFailures();
        phase = Phase.CHECKING_FILES;
        detail = "Checking every dimension; new coatings are disabled until cancellation or shutdown";
        try {
            var dimensions = new ArrayList<ServerLevel>();
            server.getAllLevels().forEach(dimensions::add);
            dimensions.sort(Comparator.comparing(value -> value.dimension().identifier().toString()));
            var tasks = new ArrayList<ChunkTask>();
            var files = new ArrayList<Path>();
            for (var dimension : dimensions) {
                for (long key : PreservationService.get(dimension).store().pendingChunks()) { tasks.add(new ChunkTask(dimension, key)); }
                files.add(TreatmentStore.TYPE.id().withSuffix(".dat").resolveAgainst(((SavedDataStorageAccessor) dimension.getDataStorage()).preserve$dataFolder()));
            }
            chunks = List.copyOf(tasks); saveFiles = List.copyOf(files);
            var root = server.getWorldPath(LevelResource.ROOT).resolve("dimensions");
            var known = Set.copyOf(saveFiles);
            work = CompletableFuture.runAsync(() -> {
                try { CleanupFiles.checkUnknownDimensions(root, known); }
                catch (IOException error) { throw new CompletionException(error); }
            }, Util.ioPool());
            return true;
        } catch (RuntimeException error) { fail(error); return false; }
    }

    public void tick() {
        if (!running() && phase != Phase.COMPLETE) { return; }
        try {
            if (((CleanupServer) server).preserve$saveFailures() != saveFailures) { throw new IllegalStateException("A chunk save failed; restore a backup before uninstalling"); }
            switch (phase) {
                case CHECKING_FILES -> {
                    if (!work.isDone()) { return; }
                    work.join(); work = null;
                    phase = Phase.CHECKING_CHUNKS;
                    detail = "Checking existing chunk files before loading any treated area";
                }
                case CHECKING_CHUNKS -> checkChunk();
                case REMOVING -> nextTarget();
                case LOADING -> loadGroup();
                case RETURNING_WORK -> {
                    var store = PreservationService.get(level).store();
                    for (long member : group) { if (store.resuming(member) != null) { return; } }
                    releaseTickets(); group = List.of();
                    phase = Phase.REMOVING;
                }
                case SAVING -> {
                    if (remaining() != 0) { throw new IllegalStateException("Records remain; cleanup cannot be certified"); }
                    // Force each empty payload into this save so native pending writes are joined, then verify the files.
                    for (var dimension : server.getAllLevels()) { PreservationService.get(dimension).store().setDirty(); }
                    server.saveEverything(false, true, true);
                    var files = saveFiles;
                    work = CompletableFuture.runAsync(() -> {
                        try { CleanupFiles.verifyEmpty(files); }
                        catch (IOException error) { throw new CompletionException(error); }
                    }, Util.ioPool());
                    phase = Phase.VERIFYING;
                    detail = "Checking that native saves contain no retained Preserve work";
                }
                case VERIFYING -> {
                    if (!work.isDone()) { return; }
                    work.join();
                    if (remaining() != 0) { throw new IllegalStateException("Records changed during cleanup verification"); }
                    phase = Phase.COMPLETE;
                    detail = "Saved cleanup verified. Stop the server before removing Preserve";
                    clearWork(); announce();
                }
                default -> { }
            }
        } catch (RuntimeException error) { fail(error); }
    }

    private void checkChunk() {
        if (chunkRead != null) {
            if (!chunkRead.isDone()) { return; }
            var data = chunkRead.join(); chunkRead = null;
            if (data.isEmpty() || SerializableChunkData.getChunkStatusFromTag(data.get()).getChunkType() != ChunkType.LEVELCHUNK) {
                var task = chunks.get(checkIndex);
                throw new IllegalStateException("A treated chunk is missing or unfinished: " + task.level().dimension().identifier() + " " + ChunkPos.unpack(task.key()));
            }
            checkIndex++;
        }
        if (checkIndex == chunks.size()) { phase = Phase.REMOVING; return; }
        var task = chunks.get(checkIndex);
        var pos = ChunkPos.unpack(task.key());
        if (task.level().getChunkSource().getChunkNow(pos.x(), pos.z()) != null) { checkIndex++; }
        else { chunkRead = task.level().getChunkSource().chunkMap.read(pos); }
    }

    private void nextTarget() {
        if (positions.isEmpty()) {
            if (chunkIndex == chunks.size()) { phase = Phase.SAVING; return; }
            var task = chunks.get(chunkIndex++);
            level = task.level();
            var store = PreservationService.get(level).store();
            var next = new java.util.LinkedHashSet<Long>();
            for (var record : store.chunkSnapshot(task.key())) { next.add(record.position()); }
            for (var record : store.chunkResumptions(task.key())) { next.add(record.position()); }
            for (var mask : store.chunkMasks(task.key())) { next.add(mask.position()); }
            positions.addAll(next);
            return;
        }
        position = positions.removeFirst();
        var store = PreservationService.get(level).store();
        var record = store.get(position);
        if (record == null && store.resuming(position) == null && store.mask(position) == null) { return; }
        group = record == null ? List.of(position) : record.link().map(TargetLink::members).orElse(List.of(position));
        var needed = new it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet();
        for (long member : group) {
            var pos = BlockPos.of(member);
            if (level.isOutsideBuildHeight(pos)) { throw new IllegalStateException("A saved target is outside build height: " + pos); }
            needed.add(TreatmentStore.chunkKey(member));
            var coating = store.get(member);
            if (coating != null && coating.actions().contains(Action.STRUCTURAL_CHANGE)) {
                for (int x : new int[] {-2, 2}) { for (int z : new int[] {-2, 2}) { needed.add(TreatmentStore.chunkKey(pos.offset(x, 0, z).asLong())); } }
            }
        }
        if (needed.size() > 16) { throw new IllegalStateException("Saved linked target exceeds the cleanup chunk bound"); }
        for (long key : needed) { toLoad.add(ChunkPos.unpack(key)); }
        loadingSince = server.getTickCount();
        phase = Phase.LOADING;
        detail = "Loading " + level.dimension().identifier() + " near " + BlockPos.of(position).toShortString();
    }

    private void loadGroup() {
        if (server.getTickCount() - loadingSince > 1200) { throw new IllegalStateException("Chunk loading timed out; cleanup is incomplete"); }
        if (work != null) {
            if (!work.isDone()) { return; }
            work.join(); work = null;
        }
        if (!toLoad.isEmpty()) {
            var pos = toLoad.removeFirst();
            leases.add(pos); maxLeases = Math.max(maxLeases, leases.size());
            // Radius one gives normal block ticking, plus the neighbors native scheduled callbacks need.
            work = level.getChunkSource().addTicketAndLoadWithRadius(CleanupTickets.TYPE.get(), pos, 1);
            return;
        }
        for (long member : group) { if (!level.hasChunkAt(BlockPos.of(member))) { return; } }
        var service = PreservationService.get(level);
        if (service.store().get(position) != null) {
            var result = service.removeForUninstall(BlockPos.of(position));
            if (!result.changed()) { throw new IllegalStateException(result.message()); }
            removed += result.changedPositions();
        }
        for (long member : group) { service.clearMaskForUninstall(BlockPos.of(member)); }
        phase = Phase.RETURNING_WORK;
        detail = "Waiting for retained work to enter the normal scheduler; remaining delays are preserved";
    }

    public void cancel() {
        requireThread(); clearWork(); phase = Phase.CANCELED;
        detail = "Cleanup canceled; remaining records are retained and new coatings are enabled";
        announce();
    }
    public void close() { clearWork(); }
    private void releaseTickets() {
        if (level != null) { for (var chunk : leases) { level.getChunkSource().removeTicketWithRadius(CleanupTickets.TYPE.get(), chunk, 1); } }
        leases.clear();
    }
    private void clearWork() {
        releaseTickets(); positions.clear(); toLoad.clear(); chunks = List.of(); saveFiles = List.of(); group = List.of();
        level = null; work = null; chunkRead = null;
    }
    private void fail(RuntimeException error) {
        clearWork(); phase = Phase.FAILED;
        Throwable cause = error;
        while (cause instanceof CompletionException && cause.getCause() != null) { cause = cause.getCause(); }
        detail = "Cleanup is incomplete: " + java.util.Objects.toString(cause.getMessage(), cause.getClass().getSimpleName());
        Constants.LOG.error("Preserve uninstall cleanup failed", error);
        announce();
    }
    private void announce() {
        Constants.LOG.info(status());
        var player = operator == null ? null : server.getPlayerList().getPlayer(operator);
        if (player != null) { player.sendSystemMessage(Component.literal(status())); }
    }
    private void requireThread() { if (!server.isSameThread()) { throw new IllegalStateException("Cleanup requires the server thread"); } }
}

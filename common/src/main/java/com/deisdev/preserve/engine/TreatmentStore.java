package com.deisdev.preserve.engine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/** Per-dimension, sparse chunk index. Contains values only, never live levels or block entities. */
public final class TreatmentStore extends SavedData {
    public static final int SCHEMA = 1;
    private record Payload(int schema, List<Treatment> records, List<ResumingTicks> resuming) {}
    private static final Codec<Payload> PAYLOAD = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("schema").forGetter(Payload::schema),
            Treatment.CODEC.listOf().fieldOf("records").forGetter(Payload::records),
            ResumingTicks.CODEC.listOf().optionalFieldOf("resuming", List.of()).forGetter(Payload::resuming)
    ).apply(instance, Payload::new));
    public static final Codec<TreatmentStore> CODEC = PAYLOAD.flatXmap(TreatmentStore::decode,
            store -> DataResult.success(new Payload(SCHEMA, store.snapshot(), List.copyOf(store.resuming.values()))));
    public static final SavedDataType<TreatmentStore> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("deisdev", "treatments"), TreatmentStore::new, CODEC, DataFixTypes.LEVEL);

    private final Long2ObjectMap<Long2ObjectMap<Treatment>> chunks = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<Long2ObjectMap<Treatment>> acceleratedChunks = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<ResumingTicks> resuming = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<LongOpenHashSet> resumingChunks = new Long2ObjectOpenHashMap<>();
    private int size;
    private long revision;

    private static DataResult<TreatmentStore> decode(Payload payload) {
        if (payload.schema != SCHEMA) {
            return DataResult.error(() -> "Unsupported Preserve save schema " + payload.schema + "; expected " + SCHEMA);
        }
        var store = new TreatmentStore();
        for (Treatment treatment : payload.records) {
            if (treatment.formulation().accelerates() != treatment.acceleration().isPresent()) {
                return DataResult.error(() -> "Serum treatment must retain its strength and lifetime");
            }
            if (store.get(treatment.position()) != null) {
                return DataResult.error(() -> "Duplicate Preserve position " + treatment.position());
            }
            store.put(treatment);
        }
        for (ResumingTicks work : payload.resuming) {
            if (store.resuming(work.position()) != null || store.get(work.position()) != null) {
                return DataResult.error(() -> "Duplicate or conflicting Preserve resumption " + work.position());
            }
            store.putResuming(work);
        }
        store.setDirty(false);
        return DataResult.success(store);
    }

    public @Nullable Treatment get(long position) {
        var chunk = chunks.get(chunkKey(position));
        return chunk == null ? null : chunk.get(position);
    }

    public void put(Treatment treatment) {
        removeAcceleration(treatment.position());
        if (treatment.acceleration().isPresent()) {
            acceleratedChunks.computeIfAbsent(chunkKey(treatment.position()), key -> new Long2ObjectOpenHashMap<>()).put(treatment.position(), treatment);
        }
        var chunk = chunks.computeIfAbsent(chunkKey(treatment.position()), key -> new Long2ObjectOpenHashMap<>());
        if (chunk.put(treatment.position(), treatment) == null) { size++; }
        setDirty();
        revision++;
    }

    public @Nullable Treatment remove(long position) {
        long key = chunkKey(position);
        var chunk = chunks.get(key);
        if (chunk == null) { return null; }
        Treatment removed = chunk.remove(position);
        if (removed != null) {
            removeAcceleration(position);
            size--;
            if (chunk.isEmpty()) { chunks.remove(key); }
            setDirty();
            revision++;
        }
        return removed;
    }

    public int size() { return size; }
    public List<Treatment> acceleratedChunk(long key) {
        var chunk = acceleratedChunks.get(key);
        return chunk == null ? List.of() : List.copyOf(chunk.values());
    }
    private void removeAcceleration(long position) {
        long key = chunkKey(position);
        var chunk = acceleratedChunks.get(key);
        if (chunk != null) {
            chunk.remove(position);
            if (chunk.isEmpty()) { acceleratedChunks.remove(key); }
        }
    }
    public int resumingSize() { return resuming.size(); }
    /** Explicit administrative traversal only; snapshot chunk keys without copying every treatment. */
    public List<Long> pendingChunks() {
        var keys = new LongOpenHashSet(chunks.keySet());
        keys.addAll(resumingChunks.keySet());
        return java.util.Arrays.stream(keys.toLongArray()).sorted().boxed().toList();
    }
    public long revision() { return revision; }

    public int chunkSize(long chunkKey) {
        var chunk = chunks.get(chunkKey);
        return chunk == null ? 0 : chunk.size();
    }

    public @Nullable ResumingTicks resuming(long position) { return resuming.get(position); }

    public void putResuming(ResumingTicks work) {
        resuming.put(work.position(), work);
        resumingChunks.computeIfAbsent(chunkKey(work.position()), key -> new LongOpenHashSet()).add(work.position());
        setDirty();
    }

    public void removeResuming(long position) {
        if (resuming.remove(position) == null) { return; }
        long chunkKey = chunkKey(position);
        var positions = resumingChunks.get(chunkKey);
        positions.remove(position);
        if (positions.isEmpty()) { resumingChunks.remove(chunkKey); }
        setDirty();
    }

    public List<ResumingTicks> chunkResumptions(long chunkKey) {
        var positions = resumingChunks.get(chunkKey);
        if (positions == null) { return List.of(); }
        var result = new ArrayList<ResumingTicks>(positions.size());
        for (long position : positions) { result.add(resuming.get(position)); }
        return List.copyOf(result);
    }

    public List<Treatment> chunkSnapshot(long chunkKey) {
        var chunk = chunks.get(chunkKey);
        return chunk == null ? List.of() : List.copyOf(chunk.values());
    }

    /** Saving/explicit inspection only; never call this from a tick gate. */
    public List<Treatment> snapshot() {
        var result = new ArrayList<Treatment>(size);
        chunks.values().forEach(chunk -> result.addAll(chunk.values()));
        result.sort(Comparator.comparingLong(Treatment::position));
        return List.copyOf(result);
    }

    public static long chunkKey(long position) {
        return ChunkPos.pack(BlockPos.getX(position) >> 4, BlockPos.getZ(position) >> 4);
    }
}

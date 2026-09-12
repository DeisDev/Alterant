package com.deisdev.alterant.integration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import net.minecraft.resources.Identifier;

/** Bounded, loader-neutral data captured by a cooperative adapter; never an arbitrary machine NBT backup. */
public record AdapterSnapshot(Identifier id, int version, Map<String, String> data, boolean complete) {
    public static final int MAX_ADAPTERS = 16;
    public static final int MAX_DATA_BYTES = 16 * 1024;
    public static final int MAX_TARGET_BYTES = 64 * 1024;
    private static final Codec<Map<String, String>> DATA_CODEC = Codec.unboundedMap(Codec.string(0, 128), Codec.string(0, 4096))
            .validate(value -> validData(value) ? com.mojang.serialization.DataResult.success(value)
                    : com.mojang.serialization.DataResult.error(() -> "Alterant adapter data exceeds its saved-data budget"));
    public static final Codec<AdapterSnapshot> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("id").forGetter(AdapterSnapshot::id),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("version").forGetter(AdapterSnapshot::version),
            DATA_CODEC.fieldOf("data").forGetter(AdapterSnapshot::data),
            Codec.BOOL.optionalFieldOf("complete", false).forGetter(AdapterSnapshot::complete)
    ).apply(i, AdapterSnapshot::new));
    public AdapterSnapshot {
        java.util.Objects.requireNonNull(id);
        if (version < 1 || !validData(data)) {
            throw new IllegalArgumentException("Invalid Alterant adapter snapshot bounds");
        }
        data = Map.copyOf(data);
    }
    private static boolean validData(Map<String, String> data) {
        return data.size() <= 64 && data.entrySet().stream().allMatch(entry -> entry.getKey().length() <= 128 && entry.getValue().length() <= 4096)
                && bytes(data) <= MAX_DATA_BYTES;
    }
    private static long bytes(Map<String, String> data) {
        long size = 0;
        for (var entry : data.entrySet()) { size += entry.getKey().getBytes(StandardCharsets.UTF_8).length + entry.getValue().getBytes(StandardCharsets.UTF_8).length; }
        return size;
    }
    public long dataBytes() { return bytes(data); }
    public static void validateTarget(java.util.List<AdapterSnapshot> snapshots) {
        if (snapshots.size() > MAX_ADAPTERS || snapshots.stream().mapToLong(AdapterSnapshot::dataBytes).sum() > MAX_TARGET_BYTES) {
            throw new IllegalArgumentException("Adapters exceed this target's saved-data budget");
        }
    }
}

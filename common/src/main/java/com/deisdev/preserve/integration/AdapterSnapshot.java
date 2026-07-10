package com.deisdev.preserve.integration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.resources.Identifier;

/** Bounded, loader-neutral data captured by a cooperative adapter; never an arbitrary machine NBT backup. */
public record AdapterSnapshot(Identifier id, int version, Map<String, String> data, boolean complete) {
    public static final Codec<AdapterSnapshot> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("id").forGetter(AdapterSnapshot::id),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("version").forGetter(AdapterSnapshot::version),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("data").forGetter(AdapterSnapshot::data),
            Codec.BOOL.optionalFieldOf("complete", false).forGetter(AdapterSnapshot::complete)
    ).apply(i, AdapterSnapshot::new));
    public AdapterSnapshot {
        java.util.Objects.requireNonNull(id);
        data = Map.copyOf(data);
        if (version < 1 || data.size() > 64 || data.entrySet().stream().anyMatch(entry -> entry.getKey().length() > 128 || entry.getValue().length() > 4096)) {
            throw new IllegalArgumentException("Invalid Preserve adapter snapshot bounds");
        }
    }
}

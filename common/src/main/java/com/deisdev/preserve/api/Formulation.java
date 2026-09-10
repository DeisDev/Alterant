package com.deisdev.preserve.api;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Stable identifiers and initial jar capacities; these are distinct policies, not levels. */
public enum Formulation implements StringRepresentable {
    GROWTH_INHIBITOR("growth_inhibitor", 32),
    PRESERVING_SEALANT("preserving_sealant", 16),
    STRUCTURAL_STASIS("structural_stasis", 8),
    TEMPORAL_STASIS("temporal_stasis", 4),
    TIME_SERUM("time_serum", 1),
    SUSPICIOUS_TIME_SERUM("suspicious_time_serum", 1),
    REFINED_TIME_SERUM("refined_time_serum", 1),
    ENDURING_TIME_SERUM("enduring_time_serum", 1),
    OVERCHARGED_TIME_SERUM("overcharged_time_serum", 1);

    public static final Codec<Formulation> CODEC = StringRepresentable.fromEnum(Formulation::values);
    private final String id;
    private final int capacity;

    Formulation(String id, int capacity) {
        this.id = id;
        this.capacity = capacity;
    }

    @Override
    public String getSerializedName() { return id; }

    public int capacity() { return capacity; }
    public boolean accelerates() { return switch (this) {
            case TIME_SERUM, SUSPICIOUS_TIME_SERUM, REFINED_TIME_SERUM, ENDURING_TIME_SERUM, OVERCHARGED_TIME_SERUM -> true;
            default -> false;
        }; }
}

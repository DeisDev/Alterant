package com.deisdev.preserve.engine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

/** Bounded, versioned options captured with a coating, independent of subsequent held-jar configuration. */
public record TreatmentOptions(int schema, Optional<GrowthLimit> growth, Optional<TransferPolicy> transfer) {
    public TreatmentOptions(int schema, Optional<GrowthLimit> growth) { this(schema, growth, Optional.empty()); }
    public static final TreatmentOptions EMPTY = new TreatmentOptions(1, Optional.empty());
    public static final Codec<TreatmentOptions> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 1).fieldOf("schema").forGetter(TreatmentOptions::schema),
            GrowthLimit.CODEC.optionalFieldOf("growth").forGetter(TreatmentOptions::growth),
            TransferPolicy.CODEC.optionalFieldOf("transfer").forGetter(TreatmentOptions::transfer)
    ).apply(i, TreatmentOptions::new));
    public TreatmentOptions {
        java.util.Objects.requireNonNull(growth);
        java.util.Objects.requireNonNull(transfer);
        if (schema != 1) { throw new IllegalArgumentException("Invalid treatment options schema"); }
    }
}

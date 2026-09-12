package com.deisdev.preserve.engine;

import com.deisdev.preserve.item.ResidueFamily;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Value earned by one paid position, fixed at application time, never inferred on removal. */
public record RecoveryEntitlement(int policy, ResidueFamily family, int units) {
    public static final Codec<RecoveryEntitlement> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 1).fieldOf("policy").forGetter(RecoveryEntitlement::policy),
            ResidueFamily.CODEC.fieldOf("family").forGetter(RecoveryEntitlement::family),
            Codec.intRange(1, 1).fieldOf("units").forGetter(RecoveryEntitlement::units)
    ).apply(i, RecoveryEntitlement::new));
    public RecoveryEntitlement {
        java.util.Objects.requireNonNull(family);
        if (policy != 1 || units != 1) { throw new IllegalArgumentException("Unsupported recovery entitlement"); }
    }
}

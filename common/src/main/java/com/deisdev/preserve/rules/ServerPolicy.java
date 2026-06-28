package com.deisdev.preserve.rules;

import com.deisdev.preserve.api.Formulation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;

/** Administrators' settings precede every allow rule and adapter. */
public record ServerPolicy(Set<Formulation> disabled, boolean allowPartial, int chunkLimit, int areaLimit) {
    public static final ServerPolicy DEFAULT = new ServerPolicy(Set.of(), true, 4096, 9);
    public static final Codec<ServerPolicy> CODEC = RecordCodecBuilder.create(i -> i.group(
            Formulation.CODEC.listOf().xmap(Set::copyOf, List::copyOf).optionalFieldOf("disabled_formulations", Set.of()).forGetter(ServerPolicy::disabled),
            Codec.BOOL.optionalFieldOf("allow_partial_coverage", true).forGetter(ServerPolicy::allowPartial),
            Codec.intRange(1, 4096).optionalFieldOf("coatings_per_chunk", 4096).forGetter(ServerPolicy::chunkLimit),
            Codec.intRange(1, 9).optionalFieldOf("area_limit", 9).forGetter(ServerPolicy::areaLimit)
    ).apply(i, ServerPolicy::new));
    public ServerPolicy { disabled = Set.copyOf(disabled); }
}

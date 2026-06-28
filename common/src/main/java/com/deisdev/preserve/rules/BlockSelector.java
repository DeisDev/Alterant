package com.deisdev.preserve.rules;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;

/** IDs and tags are alternatives; entity types and state predicates further restrict the selection. */
public record BlockSelector(boolean all, Set<Identifier> blocks, Set<Identifier> tags,
                            Set<Identifier> blockEntityTypes, Map<String, String> state) {
    private static final Codec<Set<Identifier>> IDS = Identifier.CODEC.listOf(0, 4096).xmap(Set::copyOf, List::copyOf);
    public static final Codec<BlockSelector> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("all", false).forGetter(BlockSelector::all),
            IDS.optionalFieldOf("blocks", Set.of()).forGetter(BlockSelector::blocks),
            IDS.optionalFieldOf("tags", Set.of()).forGetter(BlockSelector::tags),
            IDS.optionalFieldOf("block_entity_types", Set.of()).forGetter(BlockSelector::blockEntityTypes),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("state", Map.of()).forGetter(BlockSelector::state)
    ).apply(i, BlockSelector::new));

    public BlockSelector {
        blocks = Set.copyOf(blocks);
        tags = Set.copyOf(tags);
        blockEntityTypes = Set.copyOf(blockEntityTypes);
        state = Map.copyOf(state);
        if (all && (!blocks.isEmpty() || !tags.isEmpty())) { throw new IllegalArgumentException("all cannot be combined with blocks or tags"); }
        if (!all && blocks.isEmpty() && tags.isEmpty() && blockEntityTypes.isEmpty()) {
            throw new IllegalArgumentException("A selector needs blocks, tags, block entity types, or explicit all");
        }
    }
}

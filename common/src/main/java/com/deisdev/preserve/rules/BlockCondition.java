package com.deisdev.preserve.rules;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/** Resolved IDs instead of live tags: an existing coating keeps its policy across reload and restart. */
public record BlockCondition(boolean all, Set<Identifier> blocks, Map<String, String> state) {
    public static final BlockCondition ANY = new BlockCondition(true, Set.of(), Map.of());
    public static final Codec<BlockCondition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("all").forGetter(BlockCondition::all),
            Identifier.CODEC.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("blocks").forGetter(BlockCondition::blocks),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("state").forGetter(BlockCondition::state)
    ).apply(i, BlockCondition::new));

    public BlockCondition { blocks = Set.copyOf(blocks); state = Map.copyOf(state); }

    public boolean matches(BlockState candidate) {
        if (!all && !blocks.contains(BuiltInRegistries.BLOCK.getKey(candidate.getBlock()))) { return false; }
        for (var entry : state.entrySet()) {
            var property = candidate.getBlock().getStateDefinition().getProperty(entry.getKey());
            if (property == null || !valueName(candidate, property).equals(entry.getValue())) { return false; }
        }
        return true;
    }

    public static <T extends Comparable<T>> String valueName(BlockState state, net.minecraft.world.level.block.state.properties.Property<T> property) {
        return property.getName(state.getValue(property));
    }
}

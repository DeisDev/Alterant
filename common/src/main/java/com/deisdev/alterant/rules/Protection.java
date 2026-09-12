package com.deisdev.alterant.rules;

import com.deisdev.alterant.api.Action;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;

/** One winning action policy, captured with the coating instead of silently changing on /reload. */
public record Protection(Identifier rule, Action action, BlockCondition source, BlockCondition target, Set<String> properties) {
    public static final Codec<Protection> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("rule").forGetter(Protection::rule),
            Action.CODEC.fieldOf("action").forGetter(Protection::action),
            BlockCondition.CODEC.fieldOf("source").forGetter(Protection::source),
            BlockCondition.CODEC.fieldOf("target").forGetter(Protection::target),
            Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("properties").forGetter(Protection::properties)
    ).apply(i, Protection::new));
    public Protection { properties = Set.copyOf(properties); }
}

package com.deisdev.preserve.rules;

import com.deisdev.preserve.api.Action;
import com.deisdev.preserve.api.Formulation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;

/** Pack path: data/<namespace>/deisdev/rules/<path>.json; id must match namespace:path. */
public record RuleDefinition(int schema, Identifier id, int priority, Set<String> requiresMods, boolean optional,
                             BlockSelector selector, Set<Formulation> formulations, Set<Action> actions,
                             Set<String> structuralProperties, boolean deny, String reason,
                             Optional<BlockSelector> source, Optional<BlockSelector> target) {
    public static final Codec<RuleDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 1).fieldOf("schema").forGetter(RuleDefinition::schema),
            Identifier.CODEC.fieldOf("id").forGetter(RuleDefinition::id),
            Codec.intRange(-10000, 10000).optionalFieldOf("priority", 0).forGetter(RuleDefinition::priority),
            Codec.STRING.listOf(0, 64).xmap(Set::copyOf, List::copyOf).optionalFieldOf("requires_mods", Set.of()).forGetter(RuleDefinition::requiresMods),
            Codec.BOOL.optionalFieldOf("optional", false).forGetter(RuleDefinition::optional),
            BlockSelector.CODEC.fieldOf("selector").forGetter(RuleDefinition::selector),
            Formulation.CODEC.listOf(1, Formulation.values().length).xmap(Set::copyOf, List::copyOf).fieldOf("formulations").forGetter(RuleDefinition::formulations),
            Action.CODEC.listOf(0, Action.values().length).xmap(Set::copyOf, List::copyOf).optionalFieldOf("actions", Set.of()).forGetter(RuleDefinition::actions),
            Codec.STRING.listOf(0, 16).xmap(Set::copyOf, List::copyOf).optionalFieldOf("structural_properties", Set.of()).forGetter(RuleDefinition::structuralProperties),
            Codec.BOOL.optionalFieldOf("deny", false).forGetter(RuleDefinition::deny),
            Codec.STRING.optionalFieldOf("reason", "").forGetter(RuleDefinition::reason),
            BlockSelector.CODEC.optionalFieldOf("source").forGetter(RuleDefinition::source),
            BlockSelector.CODEC.optionalFieldOf("target").forGetter(RuleDefinition::target)
    ).apply(i, RuleDefinition::new));

    public RuleDefinition {
        requiresMods = Set.copyOf(requiresMods);
        formulations = Set.copyOf(formulations);
        actions = Set.copyOf(actions);
        structuralProperties = Set.copyOf(structuralProperties);
        if (schema != 1 || formulations.isEmpty()) { throw new IllegalArgumentException("Unsupported schema or empty formulations"); }
        if (optional && requiresMods.isEmpty()) { throw new IllegalArgumentException("Optional rules must declare their required mods"); }
        if (requiresMods.stream().anyMatch(mod -> !mod.matches("[a-z][a-z0-9_-]{1,63}"))) { throw new IllegalArgumentException("Invalid required mod ID"); }
        if (deny && (reason.isBlank() || !actions.isEmpty() || source.isPresent() || target.isPresent())) {
            throw new IllegalArgumentException("Hard denials need a reason and cannot declare actions or operation conditions");
        }
        if (!deny && actions.isEmpty()) { throw new IllegalArgumentException("A protection rule needs actions"); }
        if (!structuralProperties.isEmpty() && !actions.contains(Action.STRUCTURAL_CHANGE)) {
            throw new IllegalArgumentException("Structural properties require structural_change");
        }
        if (actions.contains(Action.STRUCTURAL_CHANGE) && structuralProperties.isEmpty()) {
            throw new IllegalArgumentException("Structural changes require explicit properties");
        }
        for (var formulation : formulations) {
            if (formulation == Formulation.TEMPORAL_STASIS) { continue; }
            Set<Action> allowed = switch (formulation) {
                case GROWTH_INHIBITOR -> Set.of(Action.NATURAL_GROWTH);
                case PRESERVING_SEALANT -> Set.of(Action.ENVIRONMENTAL_CHANGE);
                case STRUCTURAL_STASIS -> Set.of(Action.STRUCTURAL_CHANGE, Action.PISTON_MOVEMENT, Action.GRAVITY);
                case TIME_SERUM, SUSPICIOUS_TIME_SERUM -> Set.of(Action.ACCELERATE_BLOCK_ENTITY, Action.ACCELERATE_RANDOM_BLOCK, Action.ACCELERATE_SCHEDULED_BLOCK);
                default -> throw new AssertionError(formulation);
            };
            if (!allowed.containsAll(actions)) { throw new IllegalArgumentException("Selective formulations cannot suppress unrelated actions"); }
        }
        if ((source.isPresent() || target.isPresent()) && actions.stream().anyMatch(action -> action != Action.NATURAL_GROWTH
                && action != Action.ENVIRONMENTAL_CHANGE && action != Action.STRUCTURAL_CHANGE && action != Action.GRAVITY)) {
            throw new IllegalArgumentException("Operation conditions require a semantic action with an audited source/target boundary");
        }
    }
}

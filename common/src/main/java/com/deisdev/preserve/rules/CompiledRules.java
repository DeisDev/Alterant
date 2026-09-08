package com.deisdev.preserve.rules;

import com.deisdev.preserve.api.Action;
import com.deisdev.preserve.api.Formulation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Immutable block index compiled once per reload. No registry walks occur during protection checks. */
public final class CompiledRules {
    private record Selector(Set<Block> blocks, BlockCondition condition) {}
    private record Rule(RuleDefinition definition, BlockCondition selector, BlockCondition source, BlockCondition target) {}
    public record Decision(List<Protection> protections, String denial, boolean requiresIntegration) {
        public Decision(List<Protection> protections, String denial) { this(protections, denial, false); }
        public Decision { protections = List.copyOf(protections); }
        public boolean allowed() { return denial.isEmpty() && !protections.isEmpty(); }
    }
    private final ServerPolicy policy;
    private final Map<Block, List<Rule>> byBlock;

    private CompiledRules(ServerPolicy policy, Map<Block, List<Rule>> byBlock) {
        this.policy = policy;
        this.byBlock = Map.copyOf(byBlock);
    }

    public ServerPolicy policy() { return policy; }
    public CompiledRules withPolicy(ServerPolicy next) { return new CompiledRules(next, byBlock); }

    public static CompiledRules compile(List<RuleDefinition> definitions, ServerPolicy policy,
                                        HolderLookup.Provider registries, Predicate<String> modLoaded) {
        return compile(definitions, policy, registries, modLoaded, id -> {
            var result = new HashSet<Block>();
            registries.lookupOrThrow(Registries.BLOCK).get(TagKey.create(Registries.BLOCK, id))
                    .orElseThrow(() -> new IllegalArgumentException("Missing block tag " + id)).forEach(holder -> result.add(holder.value()));
            return Set.copyOf(result);
        });
    }

    public static CompiledRules compile(List<RuleDefinition> definitions, ServerPolicy policy,
                                        HolderLookup.Provider registries, Predicate<String> modLoaded,
                                        java.util.function.Function<net.minecraft.resources.Identifier, Set<Block>> tags) {
        if (definitions.size() > 1024) { throw new IllegalArgumentException("At most 1024 Preserve rules are supported"); }
        var ids = new HashSet<net.minecraft.resources.Identifier>();
        var index = new HashMap<Block, List<Rule>>();
        for (var definition : definitions) {
            if (!ids.add(definition.id())) { throw new IllegalArgumentException("Duplicate Preserve rule " + definition.id()); }
            if (!definition.requiresMods().stream().allMatch(modLoaded)) {
                if (definition.optional()) { continue; }
                throw new IllegalArgumentException(definition.id() + " requires missing mods: " + definition.requiresMods());
            }
            try {
                var selector = resolve(definition.selector(), registries, tags);
                for (Block block : selector.blocks()) {
                    if (block instanceof net.minecraft.world.level.block.TrapDoorBlock && !definition.structuralProperties().isEmpty()
                            && !definition.structuralProperties().equals(Set.of("open", "powered"))) {
                        throw new IllegalArgumentException("Trapdoor position profiles must select exactly open and powered together");
                    }
                    for (String property : definition.structuralProperties()) {
                        if (block.getStateDefinition().getProperty(property) == null) {
                            throw new IllegalArgumentException("Unknown structural property " + property + " on " + BuiltInRegistries.BLOCK.getKey(block));
                        }
                    }
                }
                var source = definition.source().map(value -> resolve(value, registries, tags).condition()).orElse(BlockCondition.ANY);
                var target = definition.target().map(value -> resolve(value, registries, tags).condition()).orElse(BlockCondition.ANY);
                var rule = new Rule(definition, selector.condition(), source, target);
                for (Block block : selector.blocks()) {
                    var rules = index.computeIfAbsent(block, ignored -> new ArrayList<>());
                    rules.add(rule);
                    if (rules.size() > 64) { throw new IllegalArgumentException("At most 64 rules may select one block"); }
                }
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException(definition.id() + ": " + error.getMessage(), error);
            }
        }
        // Hard denials are checked first. For each remaining action, higher priority then lexical ID wins.
        var order = Comparator.<Rule>comparingInt(rule -> rule.definition().priority()).reversed()
                .thenComparing(rule -> rule.definition().id().toString());
        index.replaceAll((block, rules) -> rules.stream().sorted(order).toList());
        return new CompiledRules(policy, index);
    }

    public Decision evaluate(BlockState state, Formulation formulation) {
        if (policy.disabled().contains(formulation)) { return new Decision(List.of(), "This compound is disabled"); }
        var candidates = byBlock.getOrDefault(state.getBlock(), List.of());
        var actions = new EnumMap<Action, Protection>(Action.class);
        for (var rule : candidates) {
            var definition = rule.definition();
            if (!definition.formulations().contains(formulation) || !rule.selector().matches(state)) { continue; }
            if (definition.deny()) { return new Decision(List.of(), definition.reason()); }
            for (var action : definition.actions()) {
                actions.putIfAbsent(action, new Protection(definition.id(), action, rule.source(), rule.target(), definition.structuralProperties()));
            }
        }
        if (!state.isRandomlyTicking()) { actions.remove(Action.ACCELERATE_RANDOM_BLOCK); }
        if (!state.hasBlockEntity()) { actions.remove(Action.ACCELERATE_BLOCK_ENTITY); }
        return new Decision(List.copyOf(actions.values()), actions.isEmpty() ? "No supported protection applies to this target" : "", actions.isEmpty());
    }

    private static Selector resolve(BlockSelector selector, HolderLookup.Provider registries,
                                    java.util.function.Function<net.minecraft.resources.Identifier, Set<Block>> tags) {
        var blocks = new HashSet<Block>();
        var lookup = registries.lookupOrThrow(Registries.BLOCK);
        if (selector.all() || (selector.blocks().isEmpty() && selector.tags().isEmpty())) {
            lookup.listElements().forEach(holder -> blocks.add(holder.value()));
        }
        for (var id : selector.blocks()) {
            var key = net.minecraft.resources.ResourceKey.create(Registries.BLOCK, id);
            blocks.add(lookup.get(key).orElseThrow(() -> new IllegalArgumentException("Missing block " + id)).value());
        }
        for (var id : selector.tags()) {
            blocks.addAll(tags.apply(id));
        }
        if (!selector.blockEntityTypes().isEmpty()) {
            var types = selector.blockEntityTypes().stream().map(id -> BuiltInRegistries.BLOCK_ENTITY_TYPE.get(id)
                    .orElseThrow(() -> new IllegalArgumentException("Missing block entity type " + id)).value()).toList();
            blocks.removeIf(block -> types.stream().noneMatch(type -> type.isValid(block.defaultBlockState())));
        }
        for (Block block : blocks) {
            for (var entry : selector.state().entrySet()) {
                var property = block.getStateDefinition().getProperty(entry.getKey());
                if (property == null || property.getValue(entry.getValue()).isEmpty()) {
                    throw new IllegalArgumentException("Invalid state " + entry + " on " + BuiltInRegistries.BLOCK.getKey(block));
                }
            }
        }
        boolean all = selector.all() && selector.blockEntityTypes().isEmpty();
        var ids = all ? Set.<net.minecraft.resources.Identifier>of()
                : blocks.stream().map(BuiltInRegistries.BLOCK::getKey).collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new Selector(Set.copyOf(blocks), new BlockCondition(all, ids, selector.state()));
    }
}

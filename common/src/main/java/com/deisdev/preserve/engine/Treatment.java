package com.deisdev.preserve.engine;

import com.deisdev.preserve.api.Action;
import com.deisdev.preserve.api.Formulation;
import com.deisdev.preserve.integration.AdapterSnapshot;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;

/** Immutable snapshot. Rules reload without changing an existing coating's behavior. */
public record Treatment(long position, Formulation formulation, Identifier blockId, Set<Action> actions,
                        Map<String, String> structure, List<DeferredTick> deferred,
                        List<String> profiles, Map<String, String> adapterData, String owner,
                        List<com.deisdev.preserve.rules.Protection> protections, List<AdapterSnapshot> adapters, Optional<TargetLink> link) {
    public static final Codec<Treatment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("position").forGetter(Treatment::position),
            Formulation.CODEC.fieldOf("formulation").forGetter(Treatment::formulation),
            Identifier.CODEC.fieldOf("block").forGetter(Treatment::blockId),
            Action.CODEC.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("actions").forGetter(Treatment::actions),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("structure").forGetter(Treatment::structure),
            DeferredTick.CODEC.listOf(0, 4).fieldOf("deferred").forGetter(Treatment::deferred),
            Codec.STRING.listOf().fieldOf("profiles").forGetter(Treatment::profiles),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("adapter_data").forGetter(Treatment::adapterData),
            Codec.STRING.fieldOf("owner").forGetter(Treatment::owner),
            com.deisdev.preserve.rules.Protection.CODEC.listOf(0, 14).optionalFieldOf("protections", List.of()).forGetter(Treatment::protections),
            AdapterSnapshot.CODEC.listOf(0, 16).optionalFieldOf("adapters", List.of()).forGetter(Treatment::adapters),
            TargetLink.CODEC.optionalFieldOf("link").forGetter(Treatment::link)
    ).apply(instance, Treatment::new));

    public Treatment {
        java.util.Objects.requireNonNull(formulation);
        java.util.Objects.requireNonNull(blockId);
        java.util.Objects.requireNonNull(owner);
        actions = Set.copyOf(actions);
        structure = Map.copyOf(structure);
        deferred = List.copyOf(deferred);
        profiles = List.copyOf(profiles);
        adapterData = Map.copyOf(adapterData);
        protections = List.copyOf(protections);
        adapters = List.copyOf(adapters);
        if (link.isPresent() && !link.get().members().contains(position)) { throw new IllegalArgumentException("Linked target does not include this position"); }
        if (adapters.size() > 16 || adapters.stream().map(AdapterSnapshot::id).distinct().count() != adapters.size()) {
            throw new IllegalArgumentException("Adapter identities must be unique and bounded");
        }
        if (deferred.size() > 4 || deferred.stream().map(tick -> (tick.fluid() ? 2 : 0) + (tick.collected() ? 1 : 0)).distinct().count() != deferred.size()) {
            throw new IllegalArgumentException("A target can retain collected and queued work for its block and fluid");
        }
    }

    /** Schema 1's first payloads predate semantic policy snapshots; their standard action gates remain valid. */
    public Treatment(long position, Formulation formulation, Identifier blockId, Set<Action> actions,
                     Map<String, String> structure, List<DeferredTick> deferred, List<String> profiles,
                     Map<String, String> adapterData, String owner) {
        this(position, formulation, blockId, actions, structure, deferred, profiles, adapterData, owner, List.of(), List.of(), Optional.empty());
    }

    public Treatment(long position, Formulation formulation, Identifier blockId, Set<Action> actions,
                     Map<String, String> structure, List<DeferredTick> deferred, List<String> profiles,
                     Map<String, String> adapterData, String owner, List<com.deisdev.preserve.rules.Protection> protections) {
        this(position, formulation, blockId, actions, structure, deferred, profiles, adapterData, owner, protections, List.of(), Optional.empty());
    }

    public Treatment(long position, Formulation formulation, Identifier blockId, Set<Action> actions,
                     Map<String, String> structure, List<DeferredTick> deferred, List<String> profiles,
                     Map<String, String> adapterData, String owner, List<com.deisdev.preserve.rules.Protection> protections,
                     List<AdapterSnapshot> adapters) {
        this(position, formulation, blockId, actions, structure, deferred, profiles, adapterData, owner, protections, adapters, Optional.empty());
    }

    public Treatment retain(DeferredTick tick) {
        // Minecraft deduplicates by type identity and position, keeping the first scheduled tick.
        if (deferred.stream().anyMatch(previous -> previous.sameIdentity(tick))) { return this; }
        var next = new ArrayList<>(deferred);
        next.add(tick);
        return new Treatment(position, formulation, blockId, actions, structure, next, profiles, adapterData, owner, protections, adapters, link);
    }
}

package com.deisdev.preserve.engine;

import com.deisdev.preserve.api.Action;
import com.deisdev.preserve.api.Formulation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;

/** Immutable snapshot. Rules reload without changing an existing coating's behavior. */
public record Treatment(long position, Formulation formulation, Identifier blockId, Set<Action> actions,
                        Map<String, String> structure, List<DeferredTick> deferred,
                        List<String> profiles, Map<String, String> adapterData, String owner) {
    public static final Codec<Treatment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("position").forGetter(Treatment::position),
            Formulation.CODEC.fieldOf("formulation").forGetter(Treatment::formulation),
            Identifier.CODEC.fieldOf("block").forGetter(Treatment::blockId),
            Action.CODEC.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("actions").forGetter(Treatment::actions),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("structure").forGetter(Treatment::structure),
            DeferredTick.CODEC.listOf(0, 2).fieldOf("deferred").forGetter(Treatment::deferred),
            Codec.STRING.listOf().fieldOf("profiles").forGetter(Treatment::profiles),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("adapter_data").forGetter(Treatment::adapterData),
            Codec.STRING.fieldOf("owner").forGetter(Treatment::owner)
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
        if (deferred.size() > 2 || deferred.stream().map(DeferredTick::fluid).distinct().count() != deferred.size()) {
            throw new IllegalArgumentException("A target can retain one matching block tick and one fluid tick");
        }
    }

    public Treatment retain(DeferredTick tick) {
        // Minecraft deduplicates by type identity and position, keeping the first scheduled tick.
        if (deferred.stream().anyMatch(previous -> previous.sameIdentity(tick))) { return this; }
        var next = new ArrayList<>(deferred);
        next.add(tick);
        return new Treatment(position, formulation, blockId, actions, structure, next, profiles, adapterData, owner);
    }
}

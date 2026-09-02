package com.deisdev.preserve.api;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Explicit causal boundaries. Persisted names must not be repurposed across schema versions. */
public enum Action implements StringRepresentable {
    BLOCK_ENTITY_TICK,
    SCHEDULED_BLOCK_TICK,
    RANDOM_BLOCK_TICK,
    SCHEDULED_FLUID_TICK,
    RANDOM_FLUID_TICK,
    PRECIPITATION,
    CLIENT_TICK,
    NATURAL_GROWTH,
    ENVIRONMENTAL_CHANGE,
    STRUCTURAL_CHANGE,
    PISTON_MOVEMENT,
    BLOCK_EVENT,
    PLAYER_USE,
    RESOURCE_TRANSFER,
    GRAVITY,
    ACCELERATE_BLOCK_ENTITY,
    ACCELERATE_RANDOM_BLOCK,
    ACCELERATE_SCHEDULED_BLOCK;

    public static final Codec<Action> CODEC = StringRepresentable.fromEnum(Action::values);

    @Override
    public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
}

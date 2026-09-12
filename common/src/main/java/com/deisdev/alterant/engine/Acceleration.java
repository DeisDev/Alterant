package com.deisdev.alterant.engine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Per-target progress only. No world references or changes to Minecraft's clock. */
public final class Acceleration {
    public static final Codec<Acceleration> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.doubleRange(1.01, 8).fieldOf("multiplier").forGetter(Acceleration::multiplier),
            Codec.intRange(0, 1728000).fieldOf("remaining_ticks").forGetter(Acceleration::remainingTicks),
            Codec.doubleRange(0, 0.9999999999999999).optionalFieldOf("entity_phase", 0.0).forGetter(a -> a.entityPhase),
            Codec.doubleRange(0, 0.9999999999999999).optionalFieldOf("random_phase", 0.0).forGetter(a -> a.randomPhase)
    ).apply(i, Acceleration::new));
    private final double multiplier;
    private int remainingTicks;
    private double entityPhase;
    private double randomPhase;

    public Acceleration(double multiplier, int remainingTicks) { this(multiplier, remainingTicks, 0, 0); }
    private Acceleration(double multiplier, int remainingTicks, double entityPhase, double randomPhase) {
        if (!Double.isFinite(multiplier) || multiplier <= 1 || multiplier > 8 || remainingTicks < 0 || remainingTicks > 1728000) {
            throw new IllegalArgumentException("Invalid acceleration snapshot");
        }
        this.multiplier = multiplier; this.remainingTicks = remainingTicks;
        this.entityPhase = entityPhase; this.randomPhase = randomPhase;
    }
    public double multiplier() { return multiplier; }
    public int remainingTicks() { return remainingTicks; }
    public boolean elapse() { if (remainingTicks == 0) { return false; } remainingTicks--; return true; }
    public int invocations(boolean random) {
        double total = multiplier + (random ? randomPhase : entityPhase);
        int count = (int) total;
        if (random) { randomPhase = total - count; } else { entityPhase = total - count; }
        return count;
    }
    public long delay(long remaining) {
        // The native scheduler can dispatch an identity at most once per game tick.
        return remaining <= 0 ? 0 : Math.max(1, (long) Math.ceil(remaining / multiplier));
    }
}

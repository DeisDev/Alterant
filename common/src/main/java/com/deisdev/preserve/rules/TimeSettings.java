package com.deisdev.preserve.rules;

import com.deisdev.preserve.api.Formulation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

/** Reloaded atomically with rules; applications snapshot strength and lifetime. */
public record TimeSettings(Profile regular, Suspicious suspicious, Profile refined, Profile enduring, Profile overcharged,
                           int enchantLevel, int enchantLevelsSpent, int lapisCost, int bookshelves) {
    public record Profile(double multiplier, int durationTicks) {
        public static final Codec<Profile> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(1.01, 8).fieldOf("multiplier").forGetter(Profile::multiplier),
                Codec.intRange(1, 1728000).fieldOf("duration_ticks").forGetter(Profile::durationTicks)
        ).apply(i, Profile::new));
        public Profile { speed(multiplier); duration(durationTicks); }
    }
    public record Outcome(double multiplier, int weight) {
        public static final Codec<Outcome> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(1.01, 8).fieldOf("multiplier").forGetter(Outcome::multiplier),
                Codec.intRange(1, 1000000).fieldOf("weight").forGetter(Outcome::weight)
        ).apply(i, Outcome::new));
        public Outcome {
            speed(multiplier);
            if (weight < 1 || weight > 1000000) { throw new IllegalArgumentException("Invalid outcome weight"); }
        }
    }
    public record Suspicious(int durationTicks, List<Outcome> outcomes) {
        public static final List<Outcome> DEFAULT_OUTCOMES = List.of(new Outcome(1.5, 10), new Outcome(2, 40),
                new Outcome(3, 30), new Outcome(4, 18), new Outcome(8, 2));
        public static final Codec<Suspicious> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, 1728000).fieldOf("duration_ticks").forGetter(Suspicious::durationTicks),
                Outcome.CODEC.listOf(1, 16).fieldOf("outcomes").forGetter(Suspicious::outcomes)
        ).apply(i, Suspicious::new));
        public Suspicious {
            duration(durationTicks); outcomes = List.copyOf(outcomes);
            if (outcomes.size() > 16 || outcomes.isEmpty()
                    || outcomes.stream().map(Outcome::multiplier).distinct().count() != outcomes.size()) {
                throw new IllegalArgumentException("Use 1-16 distinct weighted outcomes");
            }
        }
        public int totalWeight() { return outcomes.stream().mapToInt(Outcome::weight).sum(); }
        public double percentage(int index) { return 100.0 * outcomes.get(index).weight() / totalWeight(); }
        public double minimum() { return outcomes.stream().mapToDouble(Outcome::multiplier).min().orElseThrow(); }
        public double maximum() { return outcomes.stream().mapToDouble(Outcome::multiplier).max().orElseThrow(); }
        /** Integer half-open intervals give exact, testable boundaries without rounding bias. */
        public double weightedRoll(int roll) {
            if (roll < 0 || roll >= totalWeight()) { throw new IllegalArgumentException("Roll outside weight table"); }
            for (var outcome : outcomes) { if (roll < outcome.weight()) { return outcome.multiplier(); } roll -= outcome.weight(); }
            throw new IllegalStateException("Invalid weight table");
        }
        public double roll(net.minecraft.util.RandomSource random) {
            return weightedRoll(random.nextInt(totalWeight()));
        }
    }
    public static final TimeSettings DEFAULT = new TimeSettings(new Profile(2, 3600),
            new Suspicious(2400, Suspicious.DEFAULT_OUTCOMES), new Profile(4, 2400),
            new Profile(2, 9600), new Profile(8, 1200), 35, 3, 3, 15);
    public static final Codec<TimeSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Profile.CODEC.optionalFieldOf("regular", DEFAULT.regular()).forGetter(TimeSettings::regular),
            Suspicious.CODEC.optionalFieldOf("suspicious", DEFAULT.suspicious()).forGetter(TimeSettings::suspicious),
            Profile.CODEC.optionalFieldOf("refined", DEFAULT.refined()).forGetter(TimeSettings::refined),
            Profile.CODEC.optionalFieldOf("enduring", DEFAULT.enduring()).forGetter(TimeSettings::enduring),
            Profile.CODEC.optionalFieldOf("overcharged", DEFAULT.overcharged()).forGetter(TimeSettings::overcharged),
            Codec.intRange(1, 100).optionalFieldOf("enchant_level", 35).forGetter(TimeSettings::enchantLevel),
            Codec.intRange(1, 100).optionalFieldOf("enchant_levels_spent", 3).forGetter(TimeSettings::enchantLevelsSpent),
            Codec.intRange(1, 64).optionalFieldOf("lapis_cost", 3).forGetter(TimeSettings::lapisCost),
            Codec.intRange(0, 32).optionalFieldOf("bookshelves", 15).forGetter(TimeSettings::bookshelves)
    ).apply(i, TimeSettings::new));
    public TimeSettings {
        java.util.Objects.requireNonNull(regular); java.util.Objects.requireNonNull(suspicious);
        java.util.Objects.requireNonNull(refined); java.util.Objects.requireNonNull(enduring); java.util.Objects.requireNonNull(overcharged);
        if (enchantLevelsSpent < 1 || enchantLevelsSpent > enchantLevel || enchantLevel > 100
                || lapisCost < 1 || lapisCost > 64 || bookshelves < 0 || bookshelves > 32) { throw new IllegalArgumentException("Invalid enchanting settings"); }
    }
    public Profile profile(Formulation formulation) {
        return switch (formulation) {
            case TIME_SERUM -> regular;
            case REFINED_TIME_SERUM -> refined;
            case ENDURING_TIME_SERUM -> enduring;
            case OVERCHARGED_TIME_SERUM -> overcharged;
            default -> throw new IllegalArgumentException("Not a fixed-speed serum: " + formulation);
        };
    }
    public int durationTicks(Formulation formulation) { return formulation == Formulation.SUSPICIOUS_TIME_SERUM ? suspicious.durationTicks() : profile(formulation).durationTicks(); }
    private static void speed(double value) { if (!Double.isFinite(value) || value < 1.01 || value > 8) { throw new IllegalArgumentException("Serum speed must be 1.01-8"); } }
    private static void duration(int ticks) { if (ticks < 1 || ticks > 1728000) { throw new IllegalArgumentException("Invalid serum duration"); } }
}

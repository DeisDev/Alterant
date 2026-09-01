package com.deisdev.preserve.rules;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Reloaded atomically with rules; applications snapshot strength and lifetime. */
public record TimeSettings(double multiplier, double suspiciousMin, double suspiciousMax, int durationTicks,
                           int enchantLevel, int enchantLevelsSpent, int lapisCost, int bookshelves) {
    public static final TimeSettings DEFAULT = new TimeSettings(2, 1.5, 8, 24000, 35, 3, 3, 15);
    public static final Codec<TimeSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.doubleRange(1.01, 8).optionalFieldOf("multiplier", 2.0).forGetter(TimeSettings::multiplier),
            Codec.doubleRange(1.01, 8).optionalFieldOf("suspicious_min", 1.5).forGetter(TimeSettings::suspiciousMin),
            Codec.doubleRange(1.01, 8).optionalFieldOf("suspicious_max", 8.0).forGetter(TimeSettings::suspiciousMax),
            Codec.intRange(1, 1728000).optionalFieldOf("duration_ticks", 24000).forGetter(TimeSettings::durationTicks),
            Codec.intRange(1, 100).optionalFieldOf("enchant_level", 35).forGetter(TimeSettings::enchantLevel),
            Codec.intRange(1, 100).optionalFieldOf("enchant_levels_spent", 3).forGetter(TimeSettings::enchantLevelsSpent),
            Codec.intRange(1, 64).optionalFieldOf("lapis_cost", 3).forGetter(TimeSettings::lapisCost),
            Codec.intRange(0, 32).optionalFieldOf("bookshelves", 15).forGetter(TimeSettings::bookshelves)
    ).apply(i, TimeSettings::new));
    public TimeSettings {
        if (!Double.isFinite(multiplier) || !Double.isFinite(suspiciousMin) || !Double.isFinite(suspiciousMax)
                || multiplier <= 1 || multiplier > 8 || suspiciousMin <= 1 || suspiciousMax > 8 || suspiciousMin > suspiciousMax
                || durationTicks < 1 || durationTicks > 1728000 || enchantLevelsSpent < 1 || enchantLevelsSpent > enchantLevel
                || enchantLevel > 100 || lapisCost < 1 || lapisCost > 64 || bookshelves < 0 || bookshelves > 32) {
            throw new IllegalArgumentException("Invalid time-serum settings");
        }
    }
}

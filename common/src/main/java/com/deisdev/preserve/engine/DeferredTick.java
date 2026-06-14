package com.deisdev.preserve.engine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

/** One native scheduler identity, with delay measured at suspension, never elapsed pause time. */
public record DeferredTick(boolean fluid, Identifier type, long remainingDelay, int priority, long order) {
    public static final Codec<DeferredTick> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("fluid").forGetter(DeferredTick::fluid),
            Identifier.CODEC.fieldOf("type").forGetter(DeferredTick::type),
            Codec.LONG.validate(value -> value >= 0 ? com.mojang.serialization.DataResult.success(value)
                    : com.mojang.serialization.DataResult.error(() -> "Negative retained delay"))
                    .fieldOf("remaining_delay").forGetter(DeferredTick::remainingDelay),
            Codec.intRange(-3, 3).fieldOf("priority").forGetter(DeferredTick::priority),
            Codec.LONG.fieldOf("order").forGetter(DeferredTick::order)
    ).apply(instance, DeferredTick::new));

    public DeferredTick {
        java.util.Objects.requireNonNull(type);
        if (remainingDelay < 0 || priority < -3 || priority > 3) {
            throw new IllegalArgumentException("Invalid retained tick delay or priority");
        }
    }

    public boolean sameIdentity(DeferredTick other) {
        return fluid == other.fluid && type.equals(other.type);
    }

    public long resumeTime(long now) {
        return now > Long.MAX_VALUE - remainingDelay ? Long.MAX_VALUE : now + remainingDelay;
    }
}

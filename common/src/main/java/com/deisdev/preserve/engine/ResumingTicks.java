package com.deisdev.preserve.engine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.resources.Identifier;

/** Work waiting to enter the native scheduler after thaw; -1 means the chunk is unloaded. */
public record ResumingTicks(long position, Identifier blockId, List<DeferredTick> ticks, long startedAt) {
    public static final Codec<ResumingTicks> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("position").forGetter(ResumingTicks::position),
            Identifier.CODEC.fieldOf("block").forGetter(ResumingTicks::blockId),
            DeferredTick.CODEC.listOf(1, 4).fieldOf("ticks").forGetter(ResumingTicks::ticks),
            Codec.LONG.fieldOf("started_at").forGetter(ResumingTicks::startedAt)
    ).apply(instance, ResumingTicks::new));

    public ResumingTicks { ticks = List.copyOf(ticks); }
}

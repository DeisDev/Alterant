package com.deisdev.preserve.engine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/** A position marker, independent of treatment, paid recovery and ticking policy. */
public record MaskMark(long position, Identifier block, Direction face) {
    public static final Codec<MaskMark> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("position").forGetter(MaskMark::position),
            Identifier.CODEC.fieldOf("block").forGetter(MaskMark::block),
            Direction.CODEC.fieldOf("face").forGetter(MaskMark::face)
    ).apply(i, MaskMark::new));
}

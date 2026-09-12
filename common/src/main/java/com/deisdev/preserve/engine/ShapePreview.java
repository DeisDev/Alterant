package com.deisdev.preserve.engine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/** An item-only drawing hint, never an authorization to change the world. Not persisted on the tool. */
public record ShapePreview(Identifier dimension, long position, BlockState before, ShapePattern pattern) {
    public static final Codec<ShapePreview> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("dimension").forGetter(ShapePreview::dimension), Codec.LONG.fieldOf("position").forGetter(ShapePreview::position),
            BlockState.CODEC.fieldOf("before").forGetter(ShapePreview::before), ShapePattern.CODEC.fieldOf("pattern").forGetter(ShapePreview::pattern)
    ).apply(i, ShapePreview::new));
}

package com.deisdev.alterant.engine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

/** World-relative faces. An unsided lookup conservatively intersects every selected face. */
public record TransferPolicy(int schema, Mode mode, int faces) {
    public enum Mode implements StringRepresentable {
        BOTH, INSERT, EXTRACT;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);
    }
    public static final TransferPolicy DEFAULT = new TransferPolicy(1, Mode.BOTH, 63);
    public static final Codec<TransferPolicy> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 1).fieldOf("schema").forGetter(TransferPolicy::schema),
            Mode.CODEC.fieldOf("mode").forGetter(TransferPolicy::mode),
            Codec.intRange(1, 63).fieldOf("faces").forGetter(TransferPolicy::faces)
    ).apply(i, TransferPolicy::new));
    public TransferPolicy {
        java.util.Objects.requireNonNull(mode);
        if (schema != 1 || faces < 1 || faces > 63) { throw new IllegalArgumentException("Invalid transfer policy"); }
    }
    public boolean selects(Direction face) { return face == null || (faces & 1 << face.ordinal()) != 0; }
    public boolean blocks(boolean insertion, Direction face) {
        return selects(face) && (mode == Mode.BOTH || mode == (insertion ? Mode.INSERT : Mode.EXTRACT));
    }
}

package com.deisdev.preserve.engine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

/** A future-growth limit, never a command to rewrite the plant's current age or height. */
public record GrowthLimit(int schema, Mode mode, int target) {
    public enum Mode implements StringRepresentable {
        STAGE, HEIGHT;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);
    }
    public static final GrowthLimit DEFAULT = new GrowthLimit(1, Mode.STAGE, 3);
    private record Stored(int schema, Mode mode, int target) {}
    public static final Codec<GrowthLimit> CODEC = RecordCodecBuilder.<Stored>create(i -> i.group(
            Codec.intRange(1, 1).fieldOf("schema").forGetter(Stored::schema),
            Mode.CODEC.fieldOf("mode").forGetter(Stored::mode),
            Codec.intRange(0, 16).fieldOf("target").forGetter(Stored::target)
    ).apply(i, Stored::new)).comapFlatMap(stored -> {
        try { return DataResult.success(new GrowthLimit(stored.schema(), stored.mode(), stored.target())); }
        catch (IllegalArgumentException error) { return DataResult.error(error::getMessage); }
    }, limit -> new Stored(limit.schema(), limit.mode(), limit.target()));
    public GrowthLimit {
        java.util.Objects.requireNonNull(mode);
        if (schema != 1 || target < (mode == Mode.HEIGHT ? 1 : 0) || target > (mode == Mode.HEIGHT ? 16 : 7)) {
            throw new IllegalArgumentException("Invalid growth limit");
        }
    }
}

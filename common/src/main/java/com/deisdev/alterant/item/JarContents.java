package com.deisdev.alterant.item;

import com.deisdev.alterant.api.Formulation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Persist actual contents, including full jars. Missing data is never interpreted as a free refill. */
public record JarContents(int schema, Formulation formulation, int capacity, int remaining) {
    public static final Codec<JarContents> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 1).fieldOf("schema").forGetter(JarContents::schema),
            Formulation.CODEC.fieldOf("formulation").forGetter(JarContents::formulation),
            Codec.intRange(1, 4096).fieldOf("capacity").forGetter(JarContents::capacity),
            Codec.intRange(0, 4096).fieldOf("remaining").forGetter(JarContents::remaining)
    ).apply(i, JarContents::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, JarContents> STREAM_CODEC = new StreamCodec<>() {
        @Override public JarContents decode(RegistryFriendlyByteBuf buffer) {
            int schema = buffer.readVarInt();
            int formulation = buffer.readVarInt();
            int capacity = buffer.readVarInt();
            int remaining = buffer.readVarInt();
            if (formulation < 0 || formulation >= Formulation.values().length) { throw new io.netty.handler.codec.DecoderException("Invalid jar formulation"); }
            try { return new JarContents(schema, Formulation.values()[formulation], capacity, remaining); }
            catch (IllegalArgumentException error) { throw new io.netty.handler.codec.DecoderException("Invalid jar contents", error); }
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, JarContents value) {
            buffer.writeVarInt(value.schema());
            buffer.writeVarInt(value.formulation().ordinal());
            buffer.writeVarInt(value.capacity());
            buffer.writeVarInt(value.remaining());
        }
    };
    public JarContents {
        java.util.Objects.requireNonNull(formulation);
        if (schema != 1 || capacity < 1 || capacity > 4096 || remaining < 0 || remaining > capacity) {
            throw new IllegalArgumentException("Invalid jar schema, capacity or remaining uses");
        }
    }
    public static JarContents full(Formulation formulation) { return new JarContents(1, formulation, formulation.capacity(), formulation.capacity()); }

    /** Capacity increases never add uses; decreases cap usable contents. Old jars remain usable, without a reload mutation. */
    public int remainingFor(Formulation expected) { return formulation == expected ? Math.min(remaining, expected.capacity()) : 0; }

    /** Old-capacity jars cannot become recipe-eligible just because a lower capacity makes a partial jar look full. */
    public boolean isFull(Formulation expected) { return formulation == expected && capacity == expected.capacity() && remaining == capacity; }

    public JarContents spend(Formulation expected, int uses) {
        int available = remainingFor(expected);
        if (uses <= 0 || uses > available) { throw new IllegalArgumentException("Not enough compound"); }
        return new JarContents(1, expected, expected.capacity(), available - uses);
    }
}

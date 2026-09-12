package com.deisdev.preserve.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Four independent bounded balances. A lower acceptance limit never changes saved contents. */
public record ReclamationContents(int dried, int sealant, int lattice, int chronal) {
    public static final int CAPACITY = 256;
    public static final ReclamationContents EMPTY = new ReclamationContents(0, 0, 0, 0);
    public static final Codec<ReclamationContents> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, CAPACITY).fieldOf("dried").forGetter(ReclamationContents::dried),
            Codec.intRange(0, CAPACITY).fieldOf("sealant").forGetter(ReclamationContents::sealant),
            Codec.intRange(0, CAPACITY).fieldOf("lattice").forGetter(ReclamationContents::lattice),
            Codec.intRange(0, CAPACITY).fieldOf("chronal").forGetter(ReclamationContents::chronal)
    ).apply(i, ReclamationContents::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReclamationContents> STREAM_CODEC = new StreamCodec<>() {
        @Override public ReclamationContents decode(RegistryFriendlyByteBuf buffer) {
            try { return new ReclamationContents(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()); }
            catch (IllegalArgumentException error) { throw new io.netty.handler.codec.DecoderException("Invalid reclamation contents", error); }
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, ReclamationContents value) {
            buffer.writeVarInt(value.dried); buffer.writeVarInt(value.sealant); buffer.writeVarInt(value.lattice); buffer.writeVarInt(value.chronal);
        }
    };
    public ReclamationContents {
        if (dried < 0 || dried > CAPACITY || sealant < 0 || sealant > CAPACITY || lattice < 0 || lattice > CAPACITY || chronal < 0 || chronal > CAPACITY) {
            throw new IllegalArgumentException("Invalid reclamation counters");
        }
    }
    public int total() { return dried + sealant + lattice + chronal; }
    public int count(ResidueFamily family) { return switch (family) {
        case DRIED_COMPOUND -> dried; case SEALANT_SCRAP -> sealant; case LATTICE_FRAGMENTS -> lattice; case CHRONAL_DROSS -> chronal;
    }; }
    public int room(ResidueFamily family, int limit) { return Math.max(0, Math.min(CAPACITY, Math.max(0, limit)) - count(family)); }
    public ReclamationContents add(ResidueFamily family, int units) {
        if (units < 0 || units > room(family, CAPACITY)) { throw new IllegalArgumentException("Reclamation jar is full"); }
        return with(family, count(family) + units);
    }
    public ReclamationContents spend(ResidueFamily family, int units) {
        if (units < 0 || units > count(family)) { throw new IllegalArgumentException("Not enough residue"); }
        return with(family, count(family) - units);
    }
    private ReclamationContents with(ResidueFamily family, int value) { return switch (family) {
        case DRIED_COMPOUND -> new ReclamationContents(value, sealant, lattice, chronal);
        case SEALANT_SCRAP -> new ReclamationContents(dried, value, lattice, chronal);
        case LATTICE_FRAGMENTS -> new ReclamationContents(dried, sealant, value, chronal);
        case CHRONAL_DROSS -> new ReclamationContents(dried, sealant, lattice, value);
    }; }
}

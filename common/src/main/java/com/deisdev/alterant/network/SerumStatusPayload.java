package com.deisdev.alterant.network;

import com.deisdev.alterant.api.Formulation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Public treatment feedback only: no rule, permission, inventory or integration inspection data. */
public record SerumStatusPayload(int request, Identifier dimension, long position, Identifier block,
                                 int formulation, double multiplier, int remainingTicks, boolean ticking) implements CustomPacketPayload {
    /** Public sample carried with the coating update, so feedback does not wait for another round trip. */
    public record Sample(Identifier block, double multiplier, int remainingTicks, boolean ticking) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Sample> STREAM_CODEC = StreamCodec.of((buffer, value) -> {
            buffer.writeIdentifier(value.block); buffer.writeDouble(value.multiplier);
            buffer.writeVarInt(value.remainingTicks); buffer.writeBoolean(value.ticking);
        }, buffer -> new Sample(buffer.readIdentifier(), buffer.readDouble(), buffer.readVarInt(), buffer.readBoolean()));
        public Sample {
            java.util.Objects.requireNonNull(block);
            if (!Double.isFinite(multiplier) || multiplier <= 1 || multiplier > 8 || remainingTicks < 0 || remainingTicks > 1728000) {
                throw new IllegalArgumentException("Invalid serum sample");
            }
        }
    }
    public static final Type<SerumStatusPayload> TYPE = new Type<>(Identifier.parse("alterant:serum_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SerumStatusPayload> STREAM_CODEC = StreamCodec.of((buffer, value) -> {
        buffer.writeVarInt(value.request); buffer.writeIdentifier(value.dimension); buffer.writeLong(value.position); buffer.writeIdentifier(value.block);
        buffer.writeByte(value.formulation); buffer.writeDouble(value.multiplier); buffer.writeVarInt(value.remainingTicks); buffer.writeBoolean(value.ticking);
    }, buffer -> new SerumStatusPayload(buffer.readVarInt(), buffer.readIdentifier(), buffer.readLong(), buffer.readIdentifier(),
            buffer.readByte(), buffer.readDouble(), buffer.readVarInt(), buffer.readBoolean()));
    public SerumStatusPayload {
        if (request < 0 || formulation < -1 || formulation >= Formulation.values().length || (formulation >= 0 && !Formulation.values()[formulation].accelerates())
                || !Double.isFinite(multiplier) || multiplier < 1 || multiplier > 8 || remainingTicks < 0 || remainingTicks > 1728000) {
            throw new IllegalArgumentException("Invalid serum status");
        }
    }
    @Override public Type<SerumStatusPayload> type() { return TYPE; }
}

package com.deisdev.preserve.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** A single local target, never a client-supplied coordinate set, item, formulation or coverage claim. */
public record InspectionRequest(int request, Identifier dimension, long position) implements CustomPacketPayload {
    public static final Type<InspectionRequest> TYPE = new Type<>(Identifier.parse("deisdev:inspect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InspectionRequest> STREAM_CODEC = StreamCodec.of((buffer, value) -> {
        buffer.writeVarInt(value.request); buffer.writeUtf(value.dimension.toString(), 256); buffer.writeLong(value.position);
    }, buffer -> new InspectionRequest(buffer.readVarInt(), Identifier.parse(buffer.readUtf(256)), buffer.readLong()));
    public InspectionRequest {
        java.util.Objects.requireNonNull(dimension);
        if (request < 0 || dimension.toString().length() > 256) { throw new IllegalArgumentException("Invalid inspection request bounds"); }
    }
    @Override public Type<InspectionRequest> type() { return TYPE; }
}

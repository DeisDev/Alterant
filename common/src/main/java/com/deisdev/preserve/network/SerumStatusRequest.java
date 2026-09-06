package com.deisdev.preserve.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SerumStatusRequest(int request, Identifier dimension, long position) implements CustomPacketPayload {
    public static final Type<SerumStatusRequest> TYPE = new Type<>(Identifier.parse("deisdev:serum_status_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SerumStatusRequest> STREAM_CODEC = StreamCodec.of((buffer, value) -> {
        buffer.writeVarInt(value.request); buffer.writeIdentifier(value.dimension); buffer.writeLong(value.position);
    }, buffer -> new SerumStatusRequest(buffer.readVarInt(), buffer.readIdentifier(), buffer.readLong()));
    public SerumStatusRequest { if (request < 0) { throw new IllegalArgumentException("Invalid serum request"); } }
    @Override public Type<SerumStatusRequest> type() { return TYPE; }
}

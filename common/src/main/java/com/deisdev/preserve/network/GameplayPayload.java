package com.deisdev.preserve.network;

import com.deisdev.preserve.rules.GameplaySettingsFile;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record GameplayPayload(int request, long revision, boolean editable, boolean overridden, Status status, String json) implements CustomPacketPayload {
    public enum Status { LOADED, SAVED, DENIED, STALE, INVALID, IO_ERROR, BUSY }
    public static final Type<GameplayPayload> TYPE = new Type<>(Identifier.parse("deisdev:gameplay_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GameplayPayload> STREAM_CODEC = StreamCodec.of((buffer, value) -> {
        buffer.writeVarInt(value.request); buffer.writeVarLong(value.revision); buffer.writeBoolean(value.editable);
        buffer.writeBoolean(value.overridden); buffer.writeEnum(value.status); buffer.writeUtf(value.json, GameplaySettingsFile.MAX_JSON);
    }, buffer -> new GameplayPayload(buffer.readVarInt(), buffer.readVarLong(), buffer.readBoolean(), buffer.readBoolean(),
            buffer.readEnum(Status.class), buffer.readUtf(GameplaySettingsFile.MAX_JSON)));
    @Override public Type<GameplayPayload> type() { return TYPE; }
}

package com.deisdev.alterant.network;

import com.deisdev.alterant.rules.GameplaySettingsFile;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record GameplayRequest(int request, long revision, Action action, String json) implements CustomPacketPayload {
    public enum Action { READ, SAVE, RESET }
    public static final Type<GameplayRequest> TYPE = new Type<>(Identifier.parse("alterant:gameplay_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GameplayRequest> STREAM_CODEC = StreamCodec.of((buffer, value) -> {
        buffer.writeVarInt(value.request); buffer.writeVarLong(value.revision); buffer.writeEnum(value.action);
        buffer.writeUtf(value.json, GameplaySettingsFile.MAX_JSON);
    }, buffer -> new GameplayRequest(buffer.readVarInt(), buffer.readVarLong(), buffer.readEnum(Action.class), buffer.readUtf(GameplaySettingsFile.MAX_JSON)));
    public GameplayRequest {
        if (request < 0 || revision < 0 || json.length() > GameplaySettingsFile.MAX_JSON) { throw new IllegalArgumentException("Invalid gameplay request"); }
    }
    @Override public Type<GameplayRequest> type() { return TYPE; }
}

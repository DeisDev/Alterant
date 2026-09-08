package com.deisdev.preserve.network;

import com.deisdev.preserve.api.Action;
import com.deisdev.preserve.api.Formulation;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;

/** Bounded, one-chunk snapshots/deltas. Contains no inventories, owner identities or private adapter data. */
public record ChunkTreatmentsPayload(Identifier dimension, long chunk, long revision, boolean snapshot, List<Entry> entries)
        implements CustomPacketPayload {
    public static final int MAX_ENTRIES = 4096;
    public static final Type<ChunkTreatmentsPayload> TYPE = new Type<>(Identifier.parse("deisdev:chunk_treatments_v2"));
    public record Entry(long position, int formulation, int actions, Optional<SerumStatusPayload.Sample> serum) {
        public Entry(long position, int formulation, int actions) { this(position, formulation, actions, Optional.empty()); }
        public Entry {
            if (serum.isPresent() && (formulation < 0 || formulation >= Formulation.values().length || !Formulation.values()[formulation].accelerates())) {
                throw new IllegalArgumentException("Serum sample requires a serum treatment");
            }
        }
    }
    public static final StreamCodec<RegistryFriendlyByteBuf, ChunkTreatmentsPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeIdentifier(payload.dimension);
                buffer.writeLong(payload.chunk);
                buffer.writeVarLong(payload.revision);
                buffer.writeBoolean(payload.snapshot);
                buffer.writeVarInt(payload.entries.size());
                for (var entry : payload.entries) {
                    buffer.writeLong(entry.position);
                    buffer.writeByte(entry.formulation);
                    buffer.writeVarInt(entry.actions);
                    buffer.writeBoolean(entry.serum.isPresent());
                    entry.serum.ifPresent(sample -> SerumStatusPayload.Sample.STREAM_CODEC.encode(buffer, sample));
                }
            }, buffer -> {
                var dimension = buffer.readIdentifier();
                long chunk = buffer.readLong();
                long revision = buffer.readVarLong();
                boolean snapshot = buffer.readBoolean();
                int count = buffer.readVarInt();
                if (revision < 0 || count < 0 || count > MAX_ENTRIES) { throw new DecoderException("Invalid Preserve snapshot bounds"); }
                var entries = new ArrayList<Entry>(count);
                for (int i = 0; i < count; i++) {
                    long position = buffer.readLong();
                    int formulation = buffer.readByte();
                    int actions = buffer.readVarInt();
                    if (com.deisdev.preserve.engine.TreatmentStore.chunkKey(position) != chunk
                            || formulation < -1 || formulation >= Formulation.values().length
                            || actions < 0 || (actions >>> Action.values().length) != 0) {
                        throw new DecoderException("Invalid Preserve treatment entry");
                    }
                    var serum = buffer.readBoolean() ? Optional.of(SerumStatusPayload.Sample.STREAM_CODEC.decode(buffer)) : Optional.<SerumStatusPayload.Sample>empty();
                    entries.add(new Entry(position, formulation, actions, serum));
                }
                return new ChunkTreatmentsPayload(dimension, chunk, revision, snapshot, entries);
            });

    public ChunkTreatmentsPayload {
        entries = List.copyOf(entries);
        if (entries.size() > MAX_ENTRIES || revision < 0) { throw new IllegalArgumentException("Preserve snapshot exceeds bounds"); }
    }

    @Override public Type<ChunkTreatmentsPayload> type() { return TYPE; }
}

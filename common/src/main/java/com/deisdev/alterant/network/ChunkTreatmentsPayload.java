package com.deisdev.alterant.network;

import com.deisdev.alterant.api.Action;
import com.deisdev.alterant.api.Formulation;
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
    public static final int MAX_ENTRIES = 8192;
    public static final Type<ChunkTreatmentsPayload> TYPE = new Type<>(Identifier.parse("alterant:chunk_treatments_v2"));
    public record Entry(long position, int formulation, int actions, Optional<SerumStatusPayload.Sample> serum,
                        Optional<com.deisdev.alterant.engine.MaskMark> mask, Optional<com.deisdev.alterant.engine.TargetLink> link,
                        com.deisdev.alterant.engine.TreatmentOptions options) {
        public Entry(long position, int formulation, int actions, Optional<SerumStatusPayload.Sample> serum, Optional<com.deisdev.alterant.engine.MaskMark> mask, Optional<com.deisdev.alterant.engine.TargetLink> link) {
            this(position, formulation, actions, serum, mask, link, com.deisdev.alterant.engine.TreatmentOptions.EMPTY);
        }
        public Entry(long position, int formulation, int actions, Optional<SerumStatusPayload.Sample> serum, Optional<com.deisdev.alterant.engine.MaskMark> mask) {
            this(position, formulation, actions, serum, mask, Optional.empty());
        }
        public Entry(long position, int formulation, int actions) { this(position, formulation, actions, Optional.empty(), Optional.empty()); }
        public Entry(long position, int formulation, int actions, Optional<SerumStatusPayload.Sample> serum) { this(position, formulation, actions, serum, Optional.empty()); }
        public Entry {
            if (link.isPresent() && (formulation < 0 || !link.get().members().contains(position))) { throw new IllegalArgumentException("Linked preview requires its treatment position"); }
            if (mask.isPresent() && mask.get().position() != position) { throw new IllegalArgumentException("Mask position must match its entry"); }
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
                    buffer.writeBoolean(entry.mask.isPresent());
                    entry.mask.ifPresent(mask -> { buffer.writeIdentifier(mask.block()); buffer.writeEnum(mask.face()); });
                    buffer.writeBoolean(entry.link.isPresent());
                    entry.link.ifPresent(link -> { buffer.writeUUID(link.id()); buffer.writeVarInt(link.members().size()); link.members().forEach(buffer::writeLong); });
                    buffer.writeBoolean(!entry.options.equals(com.deisdev.alterant.engine.TreatmentOptions.EMPTY));
                    if (!entry.options.equals(com.deisdev.alterant.engine.TreatmentOptions.EMPTY)) { net.minecraft.network.codec.ByteBufCodecs.fromCodec(com.deisdev.alterant.engine.TreatmentOptions.CODEC).encode(buffer, entry.options); }
                }
            }, buffer -> {
                var dimension = buffer.readIdentifier();
                long chunk = buffer.readLong();
                long revision = buffer.readVarLong();
                boolean snapshot = buffer.readBoolean();
                int count = buffer.readVarInt();
                if (revision < 0 || count < 0 || count > MAX_ENTRIES) { throw new DecoderException("Invalid Alterant snapshot bounds"); }
                var entries = new ArrayList<Entry>(count);
                for (int i = 0; i < count; i++) {
                    long position = buffer.readLong();
                    int formulation = buffer.readByte();
                    int actions = buffer.readVarInt();
                    if (com.deisdev.alterant.engine.TreatmentStore.chunkKey(position) != chunk
                            || formulation < -1 || formulation >= Formulation.values().length
                            || actions < 0 || (actions >>> Action.values().length) != 0) {
                        throw new DecoderException("Invalid Alterant treatment entry");
                    }
                    var serum = buffer.readBoolean() ? Optional.of(SerumStatusPayload.Sample.STREAM_CODEC.decode(buffer)) : Optional.<SerumStatusPayload.Sample>empty();
                    var mask = buffer.readBoolean() ? Optional.of(new com.deisdev.alterant.engine.MaskMark(position, buffer.readIdentifier(), buffer.readEnum(net.minecraft.core.Direction.class)))
                            : Optional.<com.deisdev.alterant.engine.MaskMark>empty();
                    var link = Optional.<com.deisdev.alterant.engine.TargetLink>empty();
                    if (buffer.readBoolean()) {
                        var id = buffer.readUUID(); int members = buffer.readVarInt();
                        if (members < 2 || members > com.deisdev.alterant.engine.TargetLink.LIMIT) { throw new DecoderException("Invalid linked preview bounds"); }
                        var positions = new ArrayList<Long>(members);
                        for (int member = 0; member < members; member++) { positions.add(buffer.readLong()); }
                        link = Optional.of(new com.deisdev.alterant.engine.TargetLink(id, positions));
                    }
                    var options = buffer.readBoolean() ? net.minecraft.network.codec.ByteBufCodecs.fromCodec(com.deisdev.alterant.engine.TreatmentOptions.CODEC).decode(buffer) : com.deisdev.alterant.engine.TreatmentOptions.EMPTY;
                    entries.add(new Entry(position, formulation, actions, serum, mask, link, options));
                }
                return new ChunkTreatmentsPayload(dimension, chunk, revision, snapshot, entries);
            });

    public ChunkTreatmentsPayload {
        entries = List.copyOf(entries);
        if (entries.size() > MAX_ENTRIES || revision < 0) { throw new IllegalArgumentException("Alterant snapshot exceeds bounds"); }
    }

    @Override public Type<ChunkTreatmentsPayload> type() { return TYPE; }
}

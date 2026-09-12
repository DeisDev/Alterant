package com.deisdev.alterant.network;

import com.deisdev.alterant.api.Action;
import com.deisdev.alterant.api.Formulation;
import com.deisdev.alterant.api.PreservationInspection.Coverage;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Public protection details only. No inventory, owner, adapter snapshot or distant treatment data. */
public record InspectionPayload(int request, Identifier dimension, long position, Identifier block, int selection,
                                Formulation formulation, boolean treated, boolean applicable, Coverage coverage, int positions,
                                int actions, int conditionalActions, String properties, String reason, List<String> limitations, List<String> details, int areaLimit)
        implements CustomPacketPayload {
    public static final int TEXT_LIMIT = 160;
    public static final int LIMITATIONS_LIMIT = 8;
    public static final int DETAILS_LIMIT = 16;
    public static final Type<InspectionPayload> TYPE = new Type<>(Identifier.parse("alterant:inspection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InspectionPayload> STREAM_CODEC = StreamCodec.of((buffer, value) -> {
        buffer.writeVarInt(value.request); buffer.writeUtf(value.dimension.toString(), 256); buffer.writeLong(value.position); buffer.writeUtf(value.block.toString(), 256);
        buffer.writeByte(value.selection); buffer.writeByte(value.formulation.ordinal()); buffer.writeBoolean(value.treated); buffer.writeBoolean(value.applicable);
        buffer.writeByte(value.coverage.ordinal()); buffer.writeByte(value.positions); buffer.writeVarInt(value.actions); buffer.writeVarInt(value.conditionalActions);
        buffer.writeUtf(value.properties, TEXT_LIMIT); buffer.writeUtf(value.reason, TEXT_LIMIT); buffer.writeVarInt(value.limitations.size());
        for (var limitation : value.limitations) { buffer.writeUtf(limitation, TEXT_LIMIT); }
        buffer.writeVarInt(value.details.size());
        for (var detail : value.details) { buffer.writeUtf(detail, TEXT_LIMIT); }
        buffer.writeByte(value.areaLimit);
    }, buffer -> {
        int request = buffer.readVarInt(); var dimension = Identifier.parse(buffer.readUtf(256)); long position = buffer.readLong(); var block = Identifier.parse(buffer.readUtf(256));
        int selection = buffer.readByte(), formulation = buffer.readByte(); boolean treated = buffer.readBoolean(), applicable = buffer.readBoolean();
        int coverage = buffer.readByte(), positions = buffer.readByte(), actions = buffer.readVarInt(), conditional = buffer.readVarInt();
        String properties = buffer.readUtf(TEXT_LIMIT), reason = buffer.readUtf(TEXT_LIMIT);
        int count = buffer.readVarInt();
        if (count < 0 || count > LIMITATIONS_LIMIT || formulation < 0 || formulation >= Formulation.values().length || coverage < 0 || coverage >= Coverage.values().length) {
            throw new DecoderException("Invalid inspection response bounds");
        }
        var limitations = new ArrayList<String>(count);
        for (int i = 0; i < count; i++) { limitations.add(buffer.readUtf(TEXT_LIMIT)); }
        int detailCount = buffer.readVarInt();
        if (detailCount < 0 || detailCount > DETAILS_LIMIT) { throw new DecoderException("Invalid inspection detail count"); }
        var details = new ArrayList<String>(detailCount);
        for (int i = 0; i < detailCount; i++) { details.add(buffer.readUtf(TEXT_LIMIT)); }
        return new InspectionPayload(request, dimension, position, block, selection, Formulation.values()[formulation], treated, applicable, Coverage.values()[coverage],
                positions, actions, conditional, properties, reason, limitations, details, buffer.readByte());
    });
    public InspectionPayload {
        java.util.Objects.requireNonNull(dimension); java.util.Objects.requireNonNull(block);
        java.util.Objects.requireNonNull(formulation); java.util.Objects.requireNonNull(coverage);
        limitations = List.copyOf(limitations);
        details = List.copyOf(details);
        if (request < 0 || dimension.toString().length() > 256 || block.toString().length() > 256 || selection < -1 || selection >= Formulation.values().length
                || positions < 0 || positions > 16 || actions < 0 || (actions >>> Action.values().length) != 0 || (conditionalActions & ~actions) != 0
                || properties.length() > TEXT_LIMIT || reason.length() > TEXT_LIMIT || limitations.size() > LIMITATIONS_LIMIT
                || limitations.stream().anyMatch(text -> text.length() > TEXT_LIMIT) || details.size() > DETAILS_LIMIT
                || details.stream().anyMatch(text -> text.length() > TEXT_LIMIT) || areaLimit < 1 || areaLimit > 9) {
            throw new IllegalArgumentException("Invalid inspection response bounds");
        }
    }
    public static String bounded(String text) {
        if (text == null) { return "Inspection unavailable"; }
        return text.length() <= TEXT_LIMIT ? text : text.substring(0, TEXT_LIMIT - 3) + "...";
    }
    @Override public Type<InspectionPayload> type() { return TYPE; }
}

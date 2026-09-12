package com.deisdev.preserve.network;

import com.deisdev.preserve.engine.PreservationLevel;
import com.deisdev.preserve.engine.Treatment;
import com.deisdev.preserve.platform.Services;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class TreatmentSync {
    private TreatmentSync() {}

    public static void snapshot(ServerPlayer player, ServerLevel level, ChunkPos chunk) {
        var store = ((PreservationLevel) level).preserve$treatments();
        var positions = new java.util.LinkedHashSet<Long>();
        store.chunkSnapshot(chunk.pack()).forEach(record -> positions.add(record.position()));
        store.chunkMasks(chunk.pack()).forEach(mask -> positions.add(mask.position()));
        Services.PLATFORM.sendTreatments(player, new ChunkTreatmentsPayload(level.dimension().identifier(), chunk.pack(),
                store.revision(), true, positions.stream().map(pos -> entry(level, pos)).toList()));
    }

    public static void changed(ServerLevel level, BlockPos pos) {
        var store = ((PreservationLevel) level).preserve$treatments();
        var update = entry(level, pos.asLong());
        var payload = new ChunkTreatmentsPayload(level.dimension().identifier(), ChunkPos.pack(pos), store.revision(), false, List.of(update));
        for (var player : level.getChunkSource().chunkMap.getPlayers(ChunkPos.unpack(ChunkPos.pack(pos)), false)) {
            Services.PLATFORM.sendTreatments(player, payload);
        }
    }

    public static void receive(Level level, ChunkTreatmentsPayload payload) {
        if (level == null || !level.isClientSide() || !level.dimension().identifier().equals(payload.dimension())) { return; }
        var chunk = ChunkPos.unpack(payload.chunk());
        if (!level.getChunkSource().hasChunk(chunk.x(), chunk.z())) { return; }
        ((PreservationLevel) level).preserve$clientTreatments().accept(payload, level.getGameTime());
    }

    private static ChunkTreatmentsPayload.Entry entry(ServerLevel level, long position) {
        var store = ((PreservationLevel) level).preserve$treatments();
        var record = store.get(position);
        var marker = java.util.Optional.ofNullable(store.mask(position));
        if (record == null) { return new ChunkTreatmentsPayload.Entry(position, -1, 0, java.util.Optional.empty(), marker); }
        int mask = 0;
        for (var action : record.actions()) { mask |= 1 << action.ordinal(); }
        var serum = record.acceleration().map(effect -> new SerumStatusPayload.Sample(record.blockId(), effect.multiplier(), effect.remainingTicks(),
                level.shouldTickBlocksAt(BlockPos.of(record.position())) && level.tickRateManager().runsNormally()));
        return new ChunkTreatmentsPayload.Entry(record.position(), record.formulation().ordinal(), mask, serum, marker, record.link(), record.options());
    }
}

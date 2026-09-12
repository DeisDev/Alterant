package com.deisdev.preserve.network;

import com.deisdev.preserve.api.Action;
import com.deisdev.preserve.api.Formulation;
import com.deisdev.preserve.engine.Treatment;
import com.deisdev.preserve.engine.TreatmentStore;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

/** Owned by a client level; reconnect and dimension changes therefore cannot retain another world's state. */
public final class ClientTreatments {
    public record SerumSample(SerumStatusPayload payload, long receivedAt) {}
    private final Long2ObjectOpenHashMap<SerumSample> serums = new Long2ObjectOpenHashMap<>();
    private final Long2LongOpenHashMap revisions = new Long2LongOpenHashMap();
    private final TreatmentStore store;
    public ClientTreatments(TreatmentStore store) {
        this.store = store;
        revisions.defaultReturnValue(-1);
    }

    public void accept(ChunkTreatmentsPayload payload) {
        accept(payload, 0);
    }

    public SerumSample serum(long position) { return serums.get(position); }

    public void accept(ChunkTreatmentsPayload payload, long gameTime) {
        if (!payload.snapshot() && revisions.get(payload.chunk()) == -1) { return; }
        if (payload.revision() <= revisions.get(payload.chunk())) { return; }
        if (payload.snapshot()) { clearRecords(payload.chunk()); }
        for (var entry : payload.entries()) {
            serums.remove(entry.position());
            store.removeMask(entry.position()); entry.mask().ifPresent(store::putMask);
            if (entry.formulation() == -1) { store.remove(entry.position()); continue; }
            var actions = EnumSet.noneOf(Action.class);
            for (var action : Action.values()) { if ((entry.actions() & (1 << action.ordinal())) != 0) { actions.add(action); } }
            store.put(new Treatment(entry.position(), Formulation.values()[entry.formulation()], Identifier.parse("deisdev:client_marker"),
                    actions, Map.of(), List.of(), List.of(), Map.of(), "", List.of(), List.of(), entry.link(), java.util.Optional.empty(), java.util.Optional.empty(), entry.options()));
            entry.serum().ifPresent(sample -> serums.put(entry.position(), new SerumSample(new SerumStatusPayload(0, payload.dimension(), entry.position(),
                    sample.block(), entry.formulation(), sample.multiplier(), sample.remainingTicks(), sample.ticking()), gameTime)));
        }
        revisions.put(payload.chunk(), payload.revision());
    }

    public void unload(long chunkKey) {
        clearRecords(chunkKey);
        revisions.remove(chunkKey);
    }

    public void retainChunks(java.util.function.LongPredicate loaded) {
        var iterator = revisions.keySet().iterator();
        while (iterator.hasNext()) {
            long chunk = iterator.nextLong();
            if (!loaded.test(chunk)) { clearRecords(chunk); iterator.remove(); }
        }
    }

    private void clearRecords(long chunkKey) {
        for (var mask : store.chunkMasks(chunkKey)) { store.removeMask(mask.position()); }
        for (var record : store.chunkSnapshot(chunkKey)) { store.remove(record.position()); serums.remove(record.position()); }
    }
}

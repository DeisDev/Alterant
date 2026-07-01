package com.deisdev.preserve.network;

import com.deisdev.preserve.api.Action;
import com.deisdev.preserve.api.Formulation;
import com.deisdev.preserve.engine.Treatment;
import com.deisdev.preserve.engine.TreatmentStore;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

/** Owned by a client level; reconnect and dimension changes therefore cannot retain another world's state. */
public final class ClientTreatments {
    private final Long2LongOpenHashMap revisions = new Long2LongOpenHashMap();
    private final TreatmentStore store;
    public ClientTreatments(TreatmentStore store) {
        this.store = store;
        revisions.defaultReturnValue(-1);
    }

    public void accept(ChunkTreatmentsPayload payload) {
        if (!payload.snapshot() && revisions.get(payload.chunk()) == -1) { return; }
        if (payload.revision() <= revisions.get(payload.chunk())) { return; }
        if (payload.snapshot()) { clearRecords(payload.chunk()); }
        for (var entry : payload.entries()) {
            if (entry.formulation() == -1) { store.remove(entry.position()); continue; }
            var actions = EnumSet.noneOf(Action.class);
            for (var action : Action.values()) { if ((entry.actions() & (1 << action.ordinal())) != 0) { actions.add(action); } }
            store.put(new Treatment(entry.position(), Formulation.values()[entry.formulation()], Identifier.parse("deisdev:client_marker"),
                    actions, Map.of(), List.of(), List.of(), Map.of(), ""));
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
        for (var record : store.chunkSnapshot(chunkKey)) { store.remove(record.position()); }
    }
}

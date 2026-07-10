package com.deisdev.preserve.integration;

import com.deisdev.preserve.Constants;
import com.deisdev.preserve.api.PreservationAdapter;
import com.deisdev.preserve.api.PreservationContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;

/** Process-wide registration contains adapter code only; captured world data belongs to individual treatments. */
public final class IntegrationRegistry {
    public record Prepared(List<AdapterSnapshot> snapshots, boolean complete) { public Prepared { snapshots = List.copyOf(snapshots); } }
    private static final Map<Identifier, PreservationAdapter> ADAPTERS = new ConcurrentHashMap<>();
    private static volatile List<PreservationAdapter> ordered = List.of();
    private static boolean locked;
    private IntegrationRegistry() {}

    public static synchronized void register(PreservationAdapter adapter) {
        if (locked) { throw new IllegalStateException("Register Preserve adapters during mod initialization, before the first server starts"); }
        if (adapter.dataVersion() < 1 || ADAPTERS.putIfAbsent(adapter.id(), adapter) != null) { throw new IllegalArgumentException("Invalid or duplicate adapter " + adapter.id()); }
        ordered = ADAPTERS.values().stream().sorted(Comparator.comparing(value -> value.id().toString())).toList();
    }

    public static synchronized void lock() { locked = true; }

    public static Prepared prepare(PreservationContext context) {
        var snapshots = new ArrayList<AdapterSnapshot>();
        boolean complete = false;
        for (var adapter : ordered) {
            if (!adapter.supports(context)) { continue; }
            var denial = adapter.validate(context);
            if (denial.isPresent()) { throw new IllegalArgumentException(adapter.id() + ": " + denial.get()); }
            boolean covered = adapter.completeCoverage(context);
            snapshots.add(new AdapterSnapshot(adapter.id(), adapter.dataVersion(), adapter.capture(context), covered));
            complete |= covered;
            if (snapshots.size() > 16) { throw new IllegalArgumentException("Too many adapters select this target"); }
        }
        return new Prepared(snapshots, complete);
    }

    public static void resume(PreservationContext context, List<AdapterSnapshot> snapshots) {
        // Resolve the whole set first. Removing an optional integration cannot silently thaw its saved machines.
        for (var snapshot : snapshots) { requireAdapter(snapshot); }
        for (var snapshot : snapshots) { requireAdapter(snapshot).resume(context, snapshot.data()); }
    }

    public static boolean available(List<AdapterSnapshot> snapshots) {
        return snapshots.stream().allMatch(snapshot -> {
            var adapter = ADAPTERS.get(snapshot.id());
            return adapter != null && adapter.dataVersion() == snapshot.version();
        });
    }

    public static void observed(PreservationContext context, List<AdapterSnapshot> snapshots, boolean paused) {
        for (var snapshot : snapshots) {
            try {
                var adapter = requireAdapter(snapshot);
                if (paused) { adapter.afterPause(context, snapshot.data()); }
                else { adapter.afterResume(context, snapshot.data()); }
            } catch (RuntimeException error) { Constants.LOG.error("Preserve adapter observation failed: {}", snapshot.id(), error); }
        }
    }

    private static PreservationAdapter requireAdapter(AdapterSnapshot snapshot) {
        var adapter = ADAPTERS.get(snapshot.id());
        if (adapter == null || adapter.dataVersion() != snapshot.version()) {
            throw new IllegalStateException("Restore compatible adapter " + snapshot.id() + " (data version " + snapshot.version() + ") before thawing");
        }
        return adapter;
    }
}

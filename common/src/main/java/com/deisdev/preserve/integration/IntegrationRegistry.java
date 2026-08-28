package com.deisdev.preserve.integration;

import com.deisdev.preserve.Constants;
import com.deisdev.preserve.api.PreservationAdapter;
import com.deisdev.preserve.api.PreservationContext;
import com.deisdev.preserve.api.PreservationPermission;
import com.deisdev.preserve.engine.TargetLink;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;

/** Process-wide registration contains adapter code only; captured world data belongs to individual treatments. */
public final class IntegrationRegistry {
    public record Prepared(List<AdapterSnapshot> snapshots, boolean complete) { public Prepared { snapshots = List.copyOf(snapshots); } }
    public record Description(List<String> adapters, boolean complete) { public Description { adapters = List.copyOf(adapters); } }
    private static final Map<Identifier, PreservationAdapter> ADAPTERS = new ConcurrentHashMap<>();
    private static volatile List<PreservationAdapter> ordered = List.of();
    private static final Map<Identifier, PreservationPermission> PERMISSIONS = new ConcurrentHashMap<>();
    private static volatile List<PreservationPermission> permissions = List.of();
    private static boolean locked;
    private IntegrationRegistry() {}

    public static synchronized void register(PreservationAdapter adapter) {
        if (locked) { throw new IllegalStateException("Register Preserve adapters during mod initialization, before the first server starts"); }
        if (adapter.dataVersion() < 1 || ADAPTERS.putIfAbsent(adapter.id(), adapter) != null) { throw new IllegalArgumentException("Invalid or duplicate adapter " + adapter.id()); }
        ordered = ADAPTERS.values().stream().sorted(Comparator.comparing(value -> value.id().toString())).toList();
    }

    public static synchronized void lock() { locked = true; }

    public static synchronized void registerPermission(PreservationPermission permission) {
        if (locked) { throw new IllegalStateException("Register Preserve permissions during mod initialization"); }
        if (PERMISSIONS.putIfAbsent(permission.id(), permission) != null) { throw new IllegalArgumentException("Duplicate permission adapter " + permission.id()); }
        permissions = PERMISSIONS.values().stream().sorted(Comparator.comparing(value -> value.id().toString())).toList();
    }

    public static void checkPermissions(net.minecraft.server.level.ServerPlayer player, PreservationContext context, PreservationPermission.Change change) {
        for (var permission : permissions) {
            var denial = permission.denial(player, context, change);
            if (denial.isPresent()) { throw new IllegalArgumentException(denial.get()); }
        }
    }

    public static List<net.minecraft.core.BlockPos> targets(PreservationContext context) {
        var result = TargetLink.validate(context.pos(), VanillaTargets.resolve(context));
        int matching = 0;
        for (var adapter : ordered) {
            if (!adapter.supports(context)) { continue; }
            if (++matching > AdapterSnapshot.MAX_ADAPTERS) { throw new IllegalArgumentException("Too many adapters select this target"); }
            var selected = TargetLink.validate(context.pos(), adapter.targets(context));
            if (selected.size() == 1) { continue; }
            if (result.size() != 1 && !result.equals(selected)) { throw new IllegalArgumentException("Adapters disagree about the linked target"); }
            result = selected;
        }
        return result;
    }

    public static Prepared prepare(PreservationContext context) {
        var snapshots = new ArrayList<AdapterSnapshot>();
        boolean complete = false;
        for (var adapter : ordered) {
            if (!adapter.supports(context)) { continue; }
            if (snapshots.size() == AdapterSnapshot.MAX_ADAPTERS) { throw new IllegalArgumentException("Too many adapters select this target"); }
            var denial = adapter.validate(context);
            if (denial.isPresent()) { throw new IllegalArgumentException(adapter.id() + ": " + denial.get()); }
            boolean covered = adapter.completeCoverage(context);
            snapshots.add(new AdapterSnapshot(adapter.id(), adapter.dataVersion(), adapter.capture(context), covered));
            complete |= covered;
            AdapterSnapshot.validateTarget(snapshots);
        }
        return new Prepared(snapshots, complete);
    }

    public static Description describe(PreservationContext context) {
        var adapters = new ArrayList<String>();
        boolean complete = false;
        for (var adapter : ordered) {
            if (!adapter.supports(context)) { continue; }
            if (adapters.size() == AdapterSnapshot.MAX_ADAPTERS) { throw new IllegalArgumentException("Too many adapters select this target"); }
            var denial = adapter.validate(context);
            if (denial.isPresent()) { throw new IllegalArgumentException(adapter.id() + ": " + denial.get()); }
            adapters.add(adapter.id() + ": " + adapter.description());
            complete |= adapter.completeCoverage(context);
        }
        return new Description(adapters, complete);
    }

    public static void resume(PreservationContext context, List<AdapterSnapshot> snapshots) {
        // Resolve the whole set first. Removing an optional integration cannot silently thaw its saved machines.
        for (var snapshot : snapshots) { requireAdapter(snapshot); }
        for (var snapshot : snapshots) { requireAdapter(snapshot).resume(context, snapshot.data()); }
    }

    public static void validateResume(List<AdapterSnapshot> snapshots) {
        for (var snapshot : snapshots) { requireAdapter(snapshot); }
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

package com.deisdev.preserve.api;

import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * Register during mod initialization. Every callback runs on the server thread, in lexical adapter-ID order.
 * Selection, validation and capture must be read-only. They run before a coating or deferred tick is changed.
 * Before resumption, all required adapter IDs/versions are checked. resume runs while the coating still blocks
 * execution and must be idempotent: throwing keeps the target frozen and permits a later retry.
 * afterPause/afterResume are observation callbacks; their failures are logged without undoing a successful operation.
 */
public interface PreservationAdapter {
    Identifier id();
    default int dataVersion() { return 1; }
    boolean supports(PreservationContext context);
    /** Loaded members of one logical target, including the clicked position; at most sixteen nearby positions. */
    default java.util.List<net.minecraft.core.BlockPos> targets(PreservationContext context) { return java.util.List.of(context.pos()); }
    default Optional<String> validate(PreservationContext context) { return Optional.empty(); }
    default Map<String, String> capture(PreservationContext context) { return Map.of(); }
    default void resume(PreservationContext context, Map<String, String> saved) {}
    default void afterPause(PreservationContext context, Map<String, String> saved) {}
    default void afterResume(PreservationContext context, Map<String, String> saved) {}
    /** A tested integration can cover its own external scheduler, clock and automation beyond standard routes. */
    default boolean completeCoverage(PreservationContext context) { return false; }
    default String description() { return id().toString(); }
}

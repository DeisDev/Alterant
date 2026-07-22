package com.deisdev.preserve.engine;

import com.deisdev.preserve.api.Action;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Nestable, exception-safe outcome for one natural operation; never inferred from a lower-level write or stack trace. */
public final class NaturalGrowth {
    private static final ScopedValue<Attempt> CURRENT = ScopedValue.newInstance();
    private static final class Attempt {
        private final Level level;
        private final BlockPos source;
        private boolean blocked;
        private Attempt(Level level, BlockPos source) { this.level = level; this.source = source.immutable(); }
    }
    private NaturalGrowth() {}
    public static void run(Level level, BlockPos source, Runnable operation) {
        // The empty path creates no scope object. Queries still distinguish nested callbacks at another source.
        if (!CURRENT.isBound() && !TickGate.blocks(level, source, Action.NATURAL_GROWTH)) { operation.run(); return; }
        ScopedValue.where(CURRENT, new Attempt(level, source)).run(operation);
    }
    public static boolean blocks(Level level, BlockPos source, BlockPos target) {
        boolean blocked = PolicyEngine.blocks(level, source, Action.NATURAL_GROWTH, source, target);
        if (blocked && active(level, source)) { CURRENT.get().blocked = true; }
        return blocked;
    }
    public static boolean wasBlocked(Level level, BlockPos source) { return active(level, source) && CURRENT.get().blocked; }
    private static boolean active(Level level, BlockPos source) {
        return CURRENT.isBound() && CURRENT.get().level == level && CURRENT.get().source.equals(source);
    }
}

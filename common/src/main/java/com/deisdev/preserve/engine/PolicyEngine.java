package com.deisdev.preserve.engine;

import com.deisdev.preserve.api.Action;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Semantic hooks supply the real source and destination before executing an operation. */
public final class PolicyEngine {
    private PolicyEngine() {}

    public static boolean blocks(Level level, BlockPos treated, Action action, BlockPos source, BlockPos target) {
        return blocksProperty(level, treated, action, source, target, "");
    }

    public static boolean blocksProperty(Level level, BlockPos treated, Action action, BlockPos source, BlockPos target, String property) {
        var record = ((PreservationLevel) level).preserve$treatments().get(treated.asLong());
        if (record == null || !record.actions().contains(action)) { return false; }
        // A protection check must not load neighboring chunks just to inspect a conditional operation.
        if (!level.hasChunkAt(source) || !level.hasChunkAt(target)) { return true; }
        for (var protection : record.protections()) {
            if (protection.action() == action && (property.isEmpty() || protection.properties().contains(property))
                    && protection.source().matches(level.getBlockState(source)) && protection.target().matches(level.getBlockState(target))) {
                return true;
            }
        }
        return false;
    }
}

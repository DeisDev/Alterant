package com.deisdev.alterant.engine;

import com.deisdev.alterant.api.Action;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class TickGate {
    private TickGate() {}

    public static boolean blocks(Level level, BlockPos pos, Action action) {
        Treatment treatment = ((PreservationLevel) level).alterant$treatments().get(pos.asLong());
        return treatment != null && treatment.actions().contains(action);
    }
}

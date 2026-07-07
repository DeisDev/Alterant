package com.deisdev.preserve.engine;

import com.deisdev.preserve.api.Action;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class InteractionGate {
    private InteractionGate() {}

    public static boolean blocked(Container container) { return blocked(container, 16); }

    private static boolean blocked(Container container, int depth) {
        if (container instanceof BlockEntity entity && entity.getLevel() != null) {
            return TickGate.blocks(entity.getLevel(), entity.getBlockPos(), Action.RESOURCE_TRANSFER);
        }
        if (container instanceof ContainerParts parts) {
            return depth == 0 || blocked(parts.preserve$first(), depth - 1) || blocked(parts.preserve$second(), depth - 1);
        }
        return false;
    }
}

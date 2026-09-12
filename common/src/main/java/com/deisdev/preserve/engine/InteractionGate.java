package com.deisdev.preserve.engine;

import com.deisdev.preserve.api.Action;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class InteractionGate {
    private InteractionGate() {}

    public static boolean blocked(Container container) { return blocked(container, true, null) || blocked(container, false, null); }
    public static boolean blocked(Container container, boolean insertion, net.minecraft.core.Direction face) { return blocked(container, insertion, face, 16); }

    private static boolean blocked(Container container, boolean insertion, net.minecraft.core.Direction face, int depth) {
        if (container instanceof BlockEntity entity && entity.getLevel() != null) {
            return entity.isRemoved() || !entity.getLevel().hasChunkAt(entity.getBlockPos())
                    || entity.getLevel().getBlockEntity(entity.getBlockPos()) != entity
                    || TransferControl.blocked(entity.getLevel(), entity.getBlockPos(), insertion, face);
        }
        if (container instanceof ContainerParts parts) {
            return depth == 0 || blocked(parts.preserve$first(), insertion, face, depth - 1) || blocked(parts.preserve$second(), insertion, face, depth - 1);
        }
        return false;
    }
}

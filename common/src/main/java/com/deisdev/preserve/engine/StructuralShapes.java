package com.deisdev.preserve.engine;

import com.deisdev.preserve.api.Action;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** Only the pure shape proposal from the three audited vanilla updateShape methods passes here. */
public final class StructuralShapes {
    private static final List<Property<?>> FENCE = List.of(FenceBlock.NORTH, FenceBlock.EAST, FenceBlock.SOUTH, FenceBlock.WEST);
    private static final List<Property<?>> WALL = List.of(WallBlock.NORTH, WallBlock.EAST, WallBlock.SOUTH, WallBlock.WEST, WallBlock.UP);
    private static final List<Property<?>> STAIRS = List.of(StairBlock.SHAPE);
    private StructuralShapes() {}

    public static BlockState constrain(BlockState before, BlockState proposal, LevelReader reader, BlockPos pos, BlockPos neighbor) {
        if (before == proposal || before.getBlock() != proposal.getBlock() || !(reader instanceof ServerLevel level)
                || !TickGate.blocks(level, pos, Action.STRUCTURAL_CHANGE)) { return proposal; }
        var properties = before.getBlock() instanceof FenceBlock ? FENCE : before.getBlock() instanceof WallBlock ? WALL : STAIRS;
        for (var property : properties) {
            if (PolicyEngine.blocksProperty(level, pos, Action.STRUCTURAL_CHANGE, neighbor, pos, property.getName())) {
                proposal = retain(before, proposal, property);
            }
        }
        return proposal;
    }

    private static <T extends Comparable<T>> BlockState retain(BlockState before, BlockState proposal, Property<T> property) {
        // Use the state supplied to this operation, never a saved snapshot or an arbitrary replacement state.
        return proposal.setValue(property, before.getValue(property));
    }
}

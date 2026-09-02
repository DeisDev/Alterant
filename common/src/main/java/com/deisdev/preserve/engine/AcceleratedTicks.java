package com.deisdev.preserve.engine;

import com.deisdev.preserve.api.Action;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Bounded dispatch at Minecraft's own tick boundary, rechecking identity between calls. */
public final class AcceleratedTicks {
    private AcceleratedTicks() {}
    public static void entity(Level level, TickingBlockEntity ticker, Runnable call) {
        var pos = ticker.getPos();
        if (TickGate.blocks(level, pos, Action.BLOCK_ENTITY_TICK)) { return; }
        if (!(level instanceof ServerLevel)) { call.run(); return; }
        var store = ((PreservationLevel) level).preserve$treatments();
        var record = store.get(pos.asLong());
        if (record == null || !record.actions().contains(Action.ACCELERATE_BLOCK_ENTITY) || record.acceleration().isEmpty()) { call.run(); return; }
        var entity = level.getBlockEntity(pos);
        int count = record.acceleration().get().invocations(false);
        store.setDirty();
        for (int i = 0; i < count; i++) {
            if (ticker.isRemoved() || !level.hasChunkAt(pos) || store.get(pos.asLong()) != record || level.getBlockEntity(pos) != entity
                    || !level.shouldTickBlocksAt(pos)) { break; }
            call.run();
        }
    }
    public static void random(ServerLevel level, BlockPos pos, BlockState initial, java.util.function.Consumer<BlockState> call) {
        if (TickGate.blocks(level, pos, Action.RANDOM_BLOCK_TICK)) { return; }
        var store = ((PreservationLevel) level).preserve$treatments();
        var record = store.get(pos.asLong());
        if (record == null || !record.actions().contains(Action.ACCELERATE_RANDOM_BLOCK) || record.acceleration().isEmpty()) { call.accept(initial); return; }
        int count = record.acceleration().get().invocations(true);
        store.setDirty();
        for (int i = 0; i < count; i++) {
            if (!level.hasChunkAt(pos) || store.get(pos.asLong()) != record) { break; }
            var state = level.getBlockState(pos);
            if (state.getBlock() != initial.getBlock() || !state.isRandomlyTicking()) { break; }
            call.accept(state);
        }
    }
}

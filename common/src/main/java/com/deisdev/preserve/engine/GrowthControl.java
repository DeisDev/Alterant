package com.deisdev.preserve.engine;

import com.deisdev.preserve.api.Action;
import com.deisdev.preserve.api.Formulation;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jspecify.annotations.Nullable;

/** Explicit native species, bounded root traversal and growth-site checks. No world scanning or generic write interception. */
public final class GrowthControl {
    private static final Map<Block, IntegerProperty> AGES = Map.of(
            Blocks.WHEAT, CropBlock.AGE, Blocks.CARROTS, CropBlock.AGE, Blocks.POTATOES, CropBlock.AGE,
            Blocks.BEETROOTS, BeetrootBlock.AGE, Blocks.SWEET_BERRY_BUSH, SweetBerryBushBlock.AGE,
            Blocks.COCOA, CocoaBlock.AGE, Blocks.NETHER_WART, NetherWartBlock.AGE);
    private GrowthControl() {}
    public static int maximumStage(BlockState state) {
        var property = AGES.get(state.getBlock()); return property == null ? -1 : property.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElseThrow();
    }
    private static int maximumHeight(BlockState state) { return state.is(Blocks.BAMBOO) ? 16 : state.is(Blocks.SUGAR_CANE) ? 3 : 0; }
    public static void validate(Level level, BlockPos pos, BlockState state, GrowthLimit limit) {
        if (limit.mode() == GrowthLimit.Mode.STAGE) {
            int max = maximumStage(state);
            if (max < 0) { throw new IllegalArgumentException("This plant does not support stage regulation"); }
            if (limit.target() > max) { throw new IllegalArgumentException("Choose a stage supported by this plant"); }
            if (state.getValue(AGES.get(state.getBlock())) > limit.target()) { throw new IllegalArgumentException("This plant is already beyond the selected limit"); }
        } else {
            int max = maximumHeight(state);
            if (max == 0) { throw new IllegalArgumentException("This plant does not support height regulation"); }
            if (limit.target() > max) { throw new IllegalArgumentException("Choose a height supported by this plant"); }
            if (!loaded(level, pos.below())) { throw new IllegalArgumentException("Target is not loaded"); }
            if (level.getBlockState(pos.below()).is(state.getBlock())) { throw new IllegalArgumentException("Apply height regulation at the root"); }
            if (height(level, pos, state.getBlock(), max) > limit.target()) { throw new IllegalArgumentException("This plant is already beyond the selected limit"); }
        }
    }
    private static int height(LevelReader level, BlockPos root, Block block, int max) {
        int height = 0;
        while (height <= max) {
            var pos = root.above(height);
            if (level.isOutsideBuildHeight(pos)) { return height; }
            if (!loaded(level, pos)) { return max + 1; }
            if (!level.getBlockState(pos).is(block)) { return height; }
            height++;
        }
        return height;
    }
    private static boolean loaded(LevelReader level, BlockPos pos) {
        return level instanceof Level world ? world.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4) : level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }
    private static @Nullable Treatment record(Level level, BlockPos pos) {
        if (!loaded(level, pos)) { return null; }
        var record = ((PreservationLevel) level).preserve$treatments().get(pos.asLong());
        return record != null && record.formulation() == Formulation.GROWTH_REGULATOR
                && (level.isClientSide() || record.blockId().equals(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()))) ? record : null;
    }
    /** Used only after the saved rule's source/target conditions match. Missing options fail closed. */
    static boolean atLimit(Level level, Treatment record, BlockPos source, BlockPos target) {
        var option = record.options().growth(); if (option.isEmpty()) { return true; }
        var limit = option.get(); var root = BlockPos.of(record.position());
        if (!loaded(level, root) || !loaded(level, source) || !loaded(level, target)) { return true; }
        var state = level.getBlockState(root);
        if (limit.mode() == GrowthLimit.Mode.STAGE) {
            var property = AGES.get(state.getBlock());
            return property == null || !source.equals(root) || !target.equals(root) || state.getValue(property) >= limit.target();
        }
        return maximumHeight(state) == 0 || target.getX() != root.getX() || target.getZ() != root.getZ()
                || target.getY() - root.getY() >= limit.target();
    }
    private static @Nullable BlockPos root(Level level, BlockPos source) {
        if (!loaded(level, source)) { return null; }
        // A vertical column shares one chunk index; an empty chunk needs no traversal.
        if (((PreservationLevel) level).preserve$treatments().chunkSize(TreatmentStore.chunkKey(source.asLong())) == 0) { return null; }
        var state = level.getBlockState(source); int max = maximumHeight(state);
        if (max == 0) { return null; }
        var current = source;
        for (int distance = 0; distance < max; distance++) {
            if (!loaded(level, current) || !level.getBlockState(current).is(state.getBlock())) { return null; }
            if (record(level, current) != null) { return current; }
            current = current.below();
        }
        return null;
    }
    public static boolean hasRootPolicy(Level level, BlockPos source) { return root(level, source) != null; }
    public static boolean blocksExtension(Level level, BlockPos source, BlockPos target) {
        var root = root(level, source);
        return root != null && PolicyEngine.blocks(level, root, Action.NATURAL_GROWTH, source, target);
    }
    /** Crop/berry/cocoa growth writes can advance several ages; constrain that proposed state before the native write. */
    public static @Nullable BlockState nextStage(Level level, BlockPos source, BlockPos target, BlockState next) {
        var record = record(level, source);
        if (record == null || !PolicyEngine.protects(level, record, Action.NATURAL_GROWTH, source, target, "")) { return next; }
        if (atLimit(level, record, source, target)) { return null; }
        var limit = record.options().growth().orElseThrow();
        var current = level.getBlockState(source); var property = AGES.get(current.getBlock());
        if (limit.mode() != GrowthLimit.Mode.STAGE || property == null || next.getBlock() != current.getBlock() || !source.equals(target)) { return null; }
        return next.setValue(property, Math.min(next.getValue(property), limit.target()));
    }
    /** Native bonemeal validation runs before consuming a dose, including dispenser bonemeal. */
    public static boolean blocksBonemeal(LevelReader reader, BlockPos source) {
        if (!(reader instanceof Level level) || !loaded(level, source)) { return false; }
        var direct = record(level, source);
        if (direct != null && direct.options().growth().map(limit -> limit.mode() == GrowthLimit.Mode.STAGE).orElse(true)) {
            return PolicyEngine.blocks(level, source, Action.NATURAL_GROWTH, source, source);
        }
        var root = root(level, source);
        if (root == null) { return false; }
        var state = level.getBlockState(root); int height = height(level, root, state.getBlock(), maximumHeight(state));
        return PolicyEngine.blocks(level, root, Action.NATURAL_GROWTH, source, root.above(height));
    }
}

package com.deisdev.preserve.mixin;

import com.deisdev.preserve.engine.GrowthControl;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BambooStalkBlock.class)
public abstract class RegulatedBambooMixin {
    @Inject(method = "growBamboo", at = @At("HEAD"), cancellable = true)
    private void preserve$beforeLeavesAndExtension(BlockState state, Level level, BlockPos pos, RandomSource random, int height, CallbackInfo info) {
        if (GrowthControl.blocksExtension(level, pos, pos.above())) { info.cancel(); }
    }
    @Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
    private void preserve$beforeDirectBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, CallbackInfo info) {
        if (GrowthControl.blocksBonemeal(level, pos)) { info.cancel(); }
    }
}

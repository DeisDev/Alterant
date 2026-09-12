package com.deisdev.preserve.mixin;

import com.deisdev.preserve.engine.GrowthControl;
import com.deisdev.preserve.engine.NaturalGrowth;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({SweetBerryBushBlock.class, CocoaBlock.class, NetherWartBlock.class})
public abstract class StageGrowthMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void preserve$beforeGrowthFeedback(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, org.spongepowered.asm.mixin.injection.callback.CallbackInfo info) {
        if (GrowthControl.blocksBonemeal(level, pos)) { info.cancel(); }
    }
    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean preserve$naturalAge(ServerLevel receiver, BlockPos target, BlockState next, int flags, Operation<Boolean> original,
                                       BlockState state, ServerLevel level, BlockPos source, RandomSource random) {
        if (NaturalGrowth.blocks(level, source, target)) { return false; }
        var limited = GrowthControl.nextStage(level, source, target, next);
        return limited != null && original.call(receiver, target, limited, flags);
    }
}

package com.deisdev.preserve.mixin;

import com.deisdev.preserve.engine.NaturalGrowth;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GrowingPlantHeadBlock.class)
public abstract class GrowingHeadMixin {
    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean preserve$growingTip(ServerLevel receiver, BlockPos target, BlockState next, Operation<Boolean> original,
                                        BlockState state, ServerLevel level, BlockPos source, RandomSource random) {
        // The direction is already resolved by the plant. Prevent placement before it converts the original head to a body.
        return !NaturalGrowth.blocks(level, source, target) && original.call(receiver, target, next);
    }
}

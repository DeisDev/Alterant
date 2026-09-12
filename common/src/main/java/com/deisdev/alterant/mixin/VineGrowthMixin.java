package com.deisdev.alterant.mixin;

import com.deisdev.alterant.engine.NaturalGrowth;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VineBlock.class)
public abstract class VineGrowthMixin {
    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean alterant$sourceSpread(ServerLevel receiver, BlockPos target, BlockState next, int flags, Operation<Boolean> original,
                                          BlockState state, ServerLevel level, BlockPos source, RandomSource random) {
        // Every branch supplies its actual destination; looking only at the destination would miss a treated source spreading outwards.
        return !NaturalGrowth.blocks(level, source, target) && original.call(receiver, target, next, flags);
    }
}

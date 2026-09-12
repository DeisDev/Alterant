package com.deisdev.preserve.mixin;

import com.deisdev.preserve.engine.NaturalGrowth;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CropBlock.class)
public abstract class CropGrowthMixin {
    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean preserve$naturalAge(ServerLevel receiver, BlockPos target, BlockState next, int flags, Operation<Boolean> original,
                                       BlockState state, ServerLevel level, BlockPos source, RandomSource random) {
        // This call site is natural growth. growCrops/performBonemeal reach their own write without an ambient cause flag.
        if (NaturalGrowth.blocks(level, source, target)) { return false; }
        var limited = com.deisdev.preserve.engine.GrowthControl.nextStage(level, source, target, next);
        return limited != null && original.call(receiver, target, limited, flags);
    }
    @WrapOperation(method = "growCrops", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean preserve$bonemealAge(net.minecraft.world.level.Level level, BlockPos target, BlockState next, int flags, Operation<Boolean> original,
                                        net.minecraft.world.level.Level receiver, BlockPos source, BlockState state) {
        var limited = com.deisdev.preserve.engine.GrowthControl.nextStage(level, source, target, next);
        return limited != null && original.call(level, target, limited, flags);
    }
}

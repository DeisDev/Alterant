package com.deisdev.alterant.mixin;

import com.deisdev.alterant.engine.GrowthControl;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({SweetBerryBushBlock.class, CocoaBlock.class})
public abstract class StageBonemealMixin {
    @WrapOperation(method = "performBonemeal", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean alterant$bonemealAge(ServerLevel receiver, BlockPos target, BlockState next, int flags, Operation<Boolean> original,
                                       ServerLevel level, RandomSource random, BlockPos source, BlockState state) {
        var limited = GrowthControl.nextStage(level, source, target, next);
        return limited != null && original.call(receiver, target, limited, flags);
    }
}

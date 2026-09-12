package com.deisdev.alterant.mixin;

import com.deisdev.alterant.api.Action;
import com.deisdev.alterant.engine.PolicyEngine;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CoralBlock;
import net.minecraft.world.level.block.CoralPlantBlock;
import net.minecraft.world.level.block.CoralFanBlock;
import net.minecraft.world.level.block.CoralWallFanBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({CoralBlock.class, CoralPlantBlock.class, CoralFanBlock.class, CoralWallFanBlock.class})
public abstract class CoralDeathMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean alterant$dryCoral(ServerLevel level, BlockPos pos, BlockState dead, int flags, Operation<Boolean> original) {
        // Only the dry-to-dead transformation is gated; support checks and waterlogged fluid scheduling still execute.
        return !PolicyEngine.blocks(level, pos, Action.ENVIRONMENTAL_CHANGE, pos, pos) && original.call(level, pos, dead, flags);
    }
}

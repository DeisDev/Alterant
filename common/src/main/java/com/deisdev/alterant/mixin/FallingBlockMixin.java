package com.deisdev.alterant.mixin;

import com.deisdev.alterant.api.Action;
import com.deisdev.alterant.engine.PolicyEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlock.class)
public abstract class FallingBlockMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void alterant$gravity(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        // This vanilla method only begins falling. Gate before it removes the block or creates an entity.
        if (PolicyEngine.blocks(level, pos, Action.GRAVITY, pos, pos.below())) { ci.cancel(); }
    }
}

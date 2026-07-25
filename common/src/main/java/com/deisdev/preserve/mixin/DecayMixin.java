package com.deisdev.preserve.mixin;

import com.deisdev.preserve.api.Action;
import com.deisdev.preserve.engine.PolicyEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LeavesBlock.class, SnowLayerBlock.class})
public abstract class DecayMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void preserve$decay(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        // These two audited methods contain only leaf decay / snow melt. Stop before drops, removal or neighbor effects.
        // Leaf distance updates, cultivation, client particles and all other dispatch methods remain independent.
        if (PolicyEngine.blocks(level, pos, Action.ENVIRONMENTAL_CHANGE, pos, pos)) { ci.cancel(); }
    }
}

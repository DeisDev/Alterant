package com.deisdev.alterant.mixin;

import com.deisdev.alterant.api.Action;
import com.deisdev.alterant.engine.PolicyEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChangeOverTimeBlock.class)
public interface OxidationMixin {
    @Inject(method = "changeOverTime", at = @At("HEAD"), cancellable = true)
    default void alterant$weathering(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        // The semantic time-change helper covers copper variants without suppressing their other behavior.
        // Axe scraping and waxing use separate explicit player operations.
        if (PolicyEngine.blocks(level, pos, Action.ENVIRONMENTAL_CHANGE, pos, pos)) { ci.cancel(); }
    }
}

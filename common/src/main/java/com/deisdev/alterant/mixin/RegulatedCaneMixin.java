package com.deisdev.alterant.mixin;

import com.deisdev.alterant.engine.GrowthControl;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SugarCaneBlock.class)
public abstract class RegulatedCaneMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void alterant$beforeExtensionOrAgeReset(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo info) {
        if (GrowthControl.blocksExtension(level, pos, pos.above())) { info.cancel(); }
    }
}

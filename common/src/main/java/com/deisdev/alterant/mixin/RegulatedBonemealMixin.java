package com.deisdev.alterant.mixin;

import com.deisdev.alterant.engine.GrowthControl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({CropBlock.class, SweetBerryBushBlock.class, CocoaBlock.class, BambooStalkBlock.class})
public abstract class RegulatedBonemealMixin {
    @Inject(method = "isValidBonemealTarget", at = @At("HEAD"), cancellable = true)
    private void alterant$beforeBonemealPayment(LevelReader level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> result) {
        if (GrowthControl.blocksBonemeal(level, pos)) { result.setReturnValue(false); }
    }
}

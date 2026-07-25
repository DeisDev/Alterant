package com.deisdev.preserve.mixin;

import com.deisdev.preserve.api.Action;
import com.deisdev.preserve.engine.PolicyEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IceBlock.class)
public abstract class IceMeltMixin {
    @Inject(method = "melt", at = @At("HEAD"), cancellable = true)
    private void preserve$melting(BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        // Environmental melt has its own boundary. Player destruction uses a separate path and remains ordinary.
        if (PolicyEngine.blocks(level, pos, Action.ENVIRONMENTAL_CHANGE, pos, pos)) { ci.cancel(); }
    }
}

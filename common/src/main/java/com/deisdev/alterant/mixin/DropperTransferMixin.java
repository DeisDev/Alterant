package com.deisdev.alterant.mixin;

import com.deisdev.alterant.engine.TransferControl;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DropperBlock.class)
public abstract class DropperTransferMixin {
    @Inject(method = "dispenseFrom", at = @At("HEAD"), cancellable = true)
    private void alterant$beforeTransfer(ServerLevel level, BlockState state, BlockPos pos, CallbackInfo ci) {
        var face = state.getValue(DropperBlock.FACING);
        if (TransferControl.blocked(level, pos, false, face) || TransferControl.blocked(level, pos.relative(face), true, face.getOpposite())) { ci.cancel(); }
    }
}

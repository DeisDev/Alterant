package com.deisdev.alterant.mixin;

import com.deisdev.alterant.engine.PreservationService;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockInput.class)
public abstract class BlockInputMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void alterant$clearCommandReplacement(ServerLevel level, BlockPos pos, int flags, CallbackInfoReturnable<Boolean> cir) {
        // Command placement can replace state or BE data without changing the block ID or BE instance.
        // A successful explicit replacement discards the old identity's work; a failed/no-op placement keeps it.
        if (cir.getReturnValue()) { PreservationService.get(level).destroyed(pos); }
    }
}

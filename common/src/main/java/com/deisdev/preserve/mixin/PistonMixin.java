package com.deisdev.preserve.mixin;

import com.deisdev.preserve.api.Action;
import com.deisdev.preserve.engine.TickGate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBaseBlock.class)
public abstract class PistonMixin {
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private static void preserve$preventMovement(BlockState state, Level level, BlockPos pos, Direction direction,
            boolean allowDestroyable, Direction connectionDirection, CallbackInfoReturnable<Boolean> cir) {
        // Reject during the resolver's validation, before moving blocks or firing removal callbacks.
        if (TickGate.blocks(level, pos, Action.PISTON_MOVEMENT)) { cir.setReturnValue(false); }
    }
}

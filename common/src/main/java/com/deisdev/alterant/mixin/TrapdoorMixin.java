package com.deisdev.alterant.mixin;

import com.deisdev.alterant.api.Action;
import com.deisdev.alterant.engine.PolicyEngine;
import com.deisdev.alterant.engine.TickGate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TrapDoorBlock.class)
public abstract class TrapdoorMixin {
    @Inject(method = "toggle", at = @At("HEAD"), cancellable = true)
    private void alterant$position(BlockState state, Level level, BlockPos pos, Player player, CallbackInfo ci) {
        // Shared only by deliberate hand/wind-charge toggles. Stop before state, sound and game-event side effects.
        if (alterant$locked(level, pos)) { ci.cancel(); }
    }

    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    private void alterant$redstonePosition(BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation,
            boolean movedByPiston, CallbackInfo ci) {
        // This audited method only changes the coupled OPEN/POWERED position. Waterlogging's updateShape still runs.
        if (alterant$locked(level, pos)) { ci.cancel(); }
    }

    @Unique private static boolean alterant$locked(Level level, BlockPos pos) {
        if (level.isClientSide()) { return TickGate.blocks(level, pos, Action.STRUCTURAL_CHANGE); }
        // The operation originates at this trapdoor. neighborChanged does not supply a signal-source position.
        return PolicyEngine.blocksProperty(level, pos, Action.STRUCTURAL_CHANGE, pos, pos, "open")
                && PolicyEngine.blocksProperty(level, pos, Action.STRUCTURAL_CHANGE, pos, pos, "powered");
    }
}

package com.deisdev.alterant.mixin.client;

import com.deisdev.alterant.api.Action;
import com.deisdev.alterant.engine.PreservationLevel;
import com.deisdev.alterant.engine.TickGate;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "unload", at = @At("HEAD"))
    private void alterant$forgetChunk(LevelChunk chunk, CallbackInfo ci) {
        ((PreservationLevel) this).alterant$clientTreatments().unload(chunk.getPos().pack());
    }

    @WrapWithCondition(method = "doAnimateTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;animateTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private boolean alterant$blockAnimation(Block block, BlockState state, Level level, BlockPos pos, RandomSource random) {
        // This level's cache contains only synchronized server decisions. Rendering/world time remains untouched.
        return !TickGate.blocks(level, pos, Action.CLIENT_TICK);
    }

    @WrapWithCondition(method = "doAnimateTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;animateTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private boolean alterant$fluidAnimation(FluidState state, Level level, BlockPos pos, RandomSource random) {
        return !TickGate.blocks(level, pos, Action.CLIENT_TICK);
    }
}

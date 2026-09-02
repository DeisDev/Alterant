package com.deisdev.preserve.mixin;

import com.deisdev.preserve.api.Action;
import com.deisdev.preserve.engine.PreservationLevel;
import com.deisdev.preserve.engine.PreservationService;
import com.deisdev.preserve.engine.TickGate;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void preserve$resumeBounded(java.util.function.BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        // Return deferred identities only when their predecessor has left the native scheduler.
        PreservationService.get((ServerLevel) (Object) this).tickResumptions();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void preserve$load(CallbackInfo ci) {
        var level = (ServerLevel) (Object) this;
        ((PreservationLevel) level).preserve$setService(new PreservationService(level));
    }

    @Inject(method = "startTickingChunk", at = @At("RETURN"))
    private void preserve$reconcileLoadedChunk(LevelChunk chunk, CallbackInfo ci) {
        // Saved ticks are unpacked at this boundary; reconciliation earlier would miss them.
        PreservationService.get((ServerLevel) (Object) this).chunkReady(chunk);
    }

    @Inject(method = "tickChunk", at = @At("HEAD"))
    private void preserve$elapseSerums(LevelChunk chunk, int tickSpeed, CallbackInfo ci) {
        PreservationService.get((ServerLevel) (Object) this).tickSerums(chunk);
    }

    @WrapOperation(method = "tickChunk", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private void preserve$randomBlock(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, Operation<Void> original) {
        // Per-position dispatch; unrelated random ticks in the same chunk still execute.
        com.deisdev.preserve.engine.AcceleratedTicks.random(level, pos, state, current -> original.call(current, level, pos, random));
    }

    @WrapWithCondition(method = "tickChunk", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private boolean preserve$randomFluid(FluidState state, ServerLevel level, BlockPos pos, RandomSource random) {
        return !TickGate.blocks(level, pos, Action.RANDOM_FLUID_TICK);
    }

    @Inject(method = "tickBlock", at = @At("HEAD"), cancellable = true)
    private void preserve$scheduledBlock(BlockPos pos, Block block, CallbackInfo ci) {
        // Normally removed from the queue on application; defense at the actual dispatch boundary as well.
        if (TickGate.blocks((ServerLevel) (Object) this, pos, Action.SCHEDULED_BLOCK_TICK)) { ci.cancel(); }
    }

    @WrapOperation(method = "tickPrecipitation", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/biome/Biome;shouldSnow(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean preserve$snow(Biome biome, LevelReader level, BlockPos pos, Operation<Boolean> original) {
        // Skip before snow stacking pushes entities or writes state; the neighboring precipitation target still runs.
        return !TickGate.blocks((ServerLevel) (Object) this, pos, Action.PRECIPITATION) && original.call(biome, level, pos);
    }

    @WrapOperation(method = "tickPrecipitation", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/biome/Biome;shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean preserve$freeze(Biome biome, LevelReader level, BlockPos pos, Operation<Boolean> original) {
        return !TickGate.blocks((ServerLevel) (Object) this, pos, Action.PRECIPITATION) && original.call(biome, level, pos);
    }

    @WrapWithCondition(method = "tickPrecipitation", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;handlePrecipitation(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/biome/Biome$Precipitation;)V"))
    private boolean preserve$precipitation(Block block, BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
        return !TickGate.blocks(level, pos, Action.PRECIPITATION);
    }

    @Inject(method = "tickFluid", at = @At("HEAD"), cancellable = true)
    private void preserve$scheduledFluid(BlockPos pos, Fluid fluid, CallbackInfo ci) {
        if (TickGate.blocks((ServerLevel) (Object) this, pos, Action.SCHEDULED_FLUID_TICK)) { ci.cancel(); }
    }

    @Inject(method = "doBlockEvent", at = @At("HEAD"), cancellable = true)
    private void preserve$blockEvent(BlockEventData event, CallbackInfoReturnable<Boolean> cir) {
        // Events are mutating interactions, separate from retained scheduled work; they are not replayed.
        if (TickGate.blocks((ServerLevel) (Object) this, event.pos(), Action.BLOCK_EVENT)) { cir.setReturnValue(false); }
    }
}

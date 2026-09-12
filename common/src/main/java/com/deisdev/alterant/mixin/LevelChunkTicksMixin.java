package com.deisdev.alterant.mixin;

import com.deisdev.alterant.engine.PreservationService;
import com.deisdev.alterant.engine.TickContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunkTicks.class)
public abstract class LevelChunkTicksMixin<T> implements TickContainer {
    @Unique private PreservationService alterant$service;
    @Unique private boolean alterant$fluid;

    @Override public void alterant$bind(PreservationService service, boolean fluid) {
        alterant$service = service;
        alterant$fluid = fluid;
    }

    @Override public void alterant$unbind() { alterant$service = null; }

    @Inject(method = "schedule", at = @At("HEAD"), cancellable = true)
    private void alterant$retainDirectRequests(ScheduledTick<T> tick, CallbackInfo ci) {
        // LevelChunk exposes this container directly. Capture before dedup/insertion, including cached handles.
        if (alterant$service != null && alterant$service.retain(tick, alterant$fluid)) { ci.cancel(); }
    }

    @Inject(method = "hasScheduledTick", at = @At("HEAD"), cancellable = true)
    private void alterant$includeRetained(BlockPos pos, T type, CallbackInfoReturnable<Boolean> cir) {
        if (alterant$service != null && alterant$service.hasRetained(pos, type, alterant$fluid)) { cir.setReturnValue(true); }
    }
}

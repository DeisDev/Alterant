package com.deisdev.preserve.mixin;

import com.deisdev.preserve.engine.PreservationService;
import com.deisdev.preserve.engine.TickContainer;
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
    @Unique private PreservationService preserve$service;
    @Unique private boolean preserve$fluid;

    @Override public void preserve$bind(PreservationService service, boolean fluid) {
        preserve$service = service;
        preserve$fluid = fluid;
    }

    @Override public void preserve$unbind() { preserve$service = null; }

    @Inject(method = "schedule", at = @At("HEAD"), cancellable = true)
    private void preserve$retainDirectRequests(ScheduledTick<T> tick, CallbackInfo ci) {
        // LevelChunk exposes this container directly. Capture before dedup/insertion, including cached handles.
        if (preserve$service != null && preserve$service.retain(tick, preserve$fluid)) { ci.cancel(); }
    }

    @Inject(method = "hasScheduledTick", at = @At("HEAD"), cancellable = true)
    private void preserve$includeRetained(BlockPos pos, T type, CallbackInfoReturnable<Boolean> cir) {
        if (preserve$service != null && preserve$service.hasRetained(pos, type, preserve$fluid)) { cir.setReturnValue(true); }
    }
}

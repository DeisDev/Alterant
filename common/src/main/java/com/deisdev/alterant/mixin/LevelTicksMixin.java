package com.deisdev.alterant.mixin;

import com.deisdev.alterant.engine.PreservationService;
import com.deisdev.alterant.engine.TickScheduler;
import com.deisdev.alterant.engine.TickContainer;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelTicks.class)
public abstract class LevelTicksMixin<T> implements TickScheduler<T> {
    @Shadow @Final private Long2ObjectMap<LevelChunkTicks<T>> allContainers;
    @Shadow @Final private Long2LongMap nextTickForContainer;
    @Shadow @Final private Queue<ScheduledTick<T>> toRunThisTick;
    @Shadow @Final private Set<ScheduledTick<?>> toRunThisTickSet;
    @Unique private PreservationService alterant$service;
    @Unique private boolean alterant$fluid;

    @Override
    public void alterant$bind(PreservationService service, boolean fluid) {
        alterant$service = service;
        alterant$fluid = fluid;
    }

    @Inject(method = "addContainer", at = @At("RETURN"))
    private void alterant$bindContainer(ChunkPos pos, LevelChunkTicks<T> container, CallbackInfo ci) {
        ((TickContainer) container).alterant$bind(alterant$service, alterant$fluid);
    }

    @Inject(method = "removeContainer", at = @At("HEAD"))
    private void alterant$releaseContainer(ChunkPos pos, CallbackInfo ci) {
        var container = allContainers.get(pos.pack());
        if (container != null) { ((TickContainer) container).alterant$unbind(); }
    }

    @WrapWithCondition(method = "runCollectedTicks", at = @At(value = "INVOKE",
            target = "Ljava/util/function/BiConsumer;accept(Ljava/lang/Object;Ljava/lang/Object;)V"))
    private boolean alterant$retainBeforeDispatch(java.util.function.BiConsumer<BlockPos, T> output,
            Object pos, Object type, @Local ScheduledTick<T> tick) {
        // Last execution boundary: never silently lose a callback inserted through another integration.
        return alterant$service == null || !alterant$service.retain(tick, alterant$fluid, true);
    }

    @Inject(method = "schedule", at = @At("HEAD"), cancellable = true)
    private void alterant$retainIncoming(ScheduledTick<T> tick, CallbackInfo ci) {
        // Capture once at insertion, including new requests made by neighbors while the target is frozen.
        if (alterant$service != null && alterant$service.retain(tick, alterant$fluid)) { ci.cancel(); }
    }

    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "schedule", at = @At("HEAD"), argsOnly = true)
    private ScheduledTick<T> alterant$accelerateIncoming(ScheduledTick<T> tick) {
        return alterant$service == null ? tick : alterant$service.accelerateScheduled(tick, alterant$fluid);
    }

    @Inject(method = "hasScheduledTick", at = @At("HEAD"), cancellable = true)
    private void alterant$includeRetained(BlockPos pos, T type, CallbackInfoReturnable<Boolean> cir) {
        // Consumers must still see retained identities, otherwise they can create duplicate restart requests.
        if (alterant$service != null && alterant$service.hasRetained(pos, type, alterant$fluid)) { cir.setReturnValue(true); }
    }

    @Override
    public List<Pending<T>> alterant$take(BlockPos pos) {
        // Called on apply/chunk readiness, never per tick. Include already-collected, not-yet-executed work.
        var result = new ArrayList<Pending<T>>();
        long chunkKey = ChunkPos.pack(pos);
        var container = allContainers.get(chunkKey);
        if (container != null) {
            container.getAll().filter(tick -> tick.pos().equals(pos)).forEach(tick -> result.add(new Pending<>(tick, false)));
            container.removeIf(tick -> tick.pos().equals(pos));
            var next = container.peek();
            if (next == null) { nextTickForContainer.remove(chunkKey); }
            else { nextTickForContainer.put(chunkKey, next.triggerTick()); }
        }
        toRunThisTick.removeIf(tick -> {
            if (!tick.pos().equals(pos)) { return false; }
            result.add(new Pending<>(tick, true));
            toRunThisTickSet.remove(tick);
            return true;
        });
        result.sort((left, right) -> ScheduledTick.DRAIN_ORDER.compare(left.tick(), right.tick()));
        return List.copyOf(result);
    }
}

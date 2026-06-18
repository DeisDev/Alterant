package com.deisdev.preserve.mixin;

import com.deisdev.preserve.engine.PreservationService;
import com.deisdev.preserve.engine.TickScheduler;
import com.deisdev.preserve.engine.TickContainer;
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
    @Unique private PreservationService preserve$service;
    @Unique private boolean preserve$fluid;

    @Override
    public void preserve$bind(PreservationService service, boolean fluid) {
        preserve$service = service;
        preserve$fluid = fluid;
    }

    @Inject(method = "addContainer", at = @At("RETURN"))
    private void preserve$bindContainer(ChunkPos pos, LevelChunkTicks<T> container, CallbackInfo ci) {
        ((TickContainer) container).preserve$bind(preserve$service, preserve$fluid);
    }

    @Inject(method = "removeContainer", at = @At("HEAD"))
    private void preserve$releaseContainer(ChunkPos pos, CallbackInfo ci) {
        var container = allContainers.get(pos.pack());
        if (container != null) { ((TickContainer) container).preserve$unbind(); }
    }

    @WrapWithCondition(method = "runCollectedTicks", at = @At(value = "INVOKE",
            target = "Ljava/util/function/BiConsumer;accept(Ljava/lang/Object;Ljava/lang/Object;)V"))
    private boolean preserve$retainBeforeDispatch(java.util.function.BiConsumer<BlockPos, T> output,
            Object pos, Object type, @Local ScheduledTick<T> tick) {
        // Last execution boundary: never silently lose a callback inserted through another integration.
        return preserve$service == null || !preserve$service.retain(tick, preserve$fluid);
    }

    @Inject(method = "schedule", at = @At("HEAD"), cancellable = true)
    private void preserve$retainIncoming(ScheduledTick<T> tick, CallbackInfo ci) {
        // Capture once at insertion, including new requests made by neighbors while the target is frozen.
        if (preserve$service != null && preserve$service.retain(tick, preserve$fluid)) { ci.cancel(); }
    }

    @Inject(method = "hasScheduledTick", at = @At("HEAD"), cancellable = true)
    private void preserve$includeRetained(BlockPos pos, T type, CallbackInfoReturnable<Boolean> cir) {
        // Consumers must still see retained identities, otherwise they can create duplicate restart requests.
        if (preserve$service != null && preserve$service.hasRetained(pos, type, preserve$fluid)) { cir.setReturnValue(true); }
    }

    @Override
    public List<ScheduledTick<T>> preserve$take(BlockPos pos) {
        // Called on apply/chunk readiness, never per tick. Include already-collected, not-yet-executed work.
        var result = new ArrayList<ScheduledTick<T>>();
        long chunkKey = ChunkPos.pack(pos);
        var container = allContainers.get(chunkKey);
        if (container != null) {
            container.getAll().filter(tick -> tick.pos().equals(pos)).forEach(result::add);
            container.removeIf(tick -> tick.pos().equals(pos));
            var next = container.peek();
            if (next == null) { nextTickForContainer.remove(chunkKey); }
            else { nextTickForContainer.put(chunkKey, next.triggerTick()); }
        }
        toRunThisTick.removeIf(tick -> {
            if (!tick.pos().equals(pos)) { return false; }
            result.add(tick);
            toRunThisTickSet.remove(tick);
            return true;
        });
        result.sort(ScheduledTick.DRAIN_ORDER);
        return List.copyOf(result);
    }
}

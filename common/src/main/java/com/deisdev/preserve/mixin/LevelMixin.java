package com.deisdev.preserve.mixin;

import com.deisdev.preserve.engine.AcceleratedTicks;
import com.deisdev.preserve.engine.PreservationLevel;
import com.deisdev.preserve.engine.PreservationService;
import com.deisdev.preserve.engine.TreatmentStore;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Level.class)
public abstract class LevelMixin implements PreservationLevel {
    @Unique private TreatmentStore preserve$treatments = new TreatmentStore();
    @Unique private PreservationService preserve$service;
    @Unique private com.deisdev.preserve.network.ClientTreatments preserve$clientTreatments;

    @Override public TreatmentStore preserve$treatments() { return preserve$treatments; }
    @Override public void preserve$setTreatments(TreatmentStore store) { preserve$treatments = store; }
    @Override public PreservationService preserve$service() { return preserve$service; }
    @Override public void preserve$setService(PreservationService service) { preserve$service = service; }
    @Override public com.deisdev.preserve.network.ClientTreatments preserve$clientTreatments() {
        if (preserve$clientTreatments == null) { preserve$clientTreatments = new com.deisdev.preserve.network.ClientTreatments(preserve$treatments); }
        return preserve$clientTreatments;
    }

    @WrapOperation(method = "tickBlockEntities", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    private void preserve$gateTicker(TickingBlockEntity ticker, Operation<Void> original) {
        // Guard invocation, not registration: cached/rebound and previously registered modded tickers are included.
        AcceleratedTicks.entity((Level) (Object) this, ticker, () -> original.call(ticker));
    }
}

package com.deisdev.alterant.mixin;

import com.deisdev.alterant.engine.AcceleratedTicks;
import com.deisdev.alterant.engine.PreservationLevel;
import com.deisdev.alterant.engine.PreservationService;
import com.deisdev.alterant.engine.TreatmentStore;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Level.class)
public abstract class LevelMixin implements PreservationLevel {
    @Unique private TreatmentStore alterant$treatments = new TreatmentStore();
    @Unique private PreservationService alterant$service;
    @Unique private com.deisdev.alterant.network.ClientTreatments alterant$clientTreatments;

    @Override public TreatmentStore alterant$treatments() { return alterant$treatments; }
    @Override public void alterant$setTreatments(TreatmentStore store) { alterant$treatments = store; }
    @Override public PreservationService alterant$service() { return alterant$service; }
    @Override public void alterant$setService(PreservationService service) { alterant$service = service; }
    @Override public com.deisdev.alterant.network.ClientTreatments alterant$clientTreatments() {
        if (alterant$clientTreatments == null) { alterant$clientTreatments = new com.deisdev.alterant.network.ClientTreatments(alterant$treatments); }
        return alterant$clientTreatments;
    }

    @WrapOperation(method = "tickBlockEntities", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    private void alterant$gateTicker(TickingBlockEntity ticker, Operation<Void> original) {
        // Guard invocation, not registration: cached/rebound and previously registered modded tickers are included.
        AcceleratedTicks.entity((Level) (Object) this, ticker, () -> original.call(ticker));
    }
}

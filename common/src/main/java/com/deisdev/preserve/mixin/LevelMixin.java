package com.deisdev.preserve.mixin;

import com.deisdev.preserve.api.Action;
import com.deisdev.preserve.engine.PreservationLevel;
import com.deisdev.preserve.engine.PreservationService;
import com.deisdev.preserve.engine.TickGate;
import com.deisdev.preserve.engine.TreatmentStore;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Level.class)
public abstract class LevelMixin implements PreservationLevel {
    @Unique private TreatmentStore preserve$treatments = new TreatmentStore();
    @Unique private PreservationService preserve$service;

    @Override public TreatmentStore preserve$treatments() { return preserve$treatments; }
    @Override public void preserve$setTreatments(TreatmentStore store) { preserve$treatments = store; }
    @Override public PreservationService preserve$service() { return preserve$service; }
    @Override public void preserve$setService(PreservationService service) { preserve$service = service; }

    @WrapWithCondition(method = "tickBlockEntities", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    private boolean preserve$gateTicker(TickingBlockEntity ticker) {
        // Guard invocation, not registration: cached/rebound and previously registered modded tickers are included.
        return !TickGate.blocks((Level) (Object) this, ticker.getPos(), Action.BLOCK_ENTITY_TICK);
    }
}

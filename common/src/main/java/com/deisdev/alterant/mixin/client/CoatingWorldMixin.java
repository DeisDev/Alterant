package com.deisdev.alterant.mixin.client;

import com.deisdev.alterant.client.coating.CoatingLevel;
import com.deisdev.alterant.client.coating.CoatingSectionCache;
import com.deisdev.alterant.engine.PreservationLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientLevel.class)
public abstract class CoatingWorldMixin implements CoatingLevel {
    @Unique private CoatingSectionCache alterant$coatingCache;
    @Override public CoatingSectionCache alterant$coatings() {
        if (alterant$coatingCache == null) {
            var level = (PreservationLevel)this;
            alterant$coatingCache = new CoatingSectionCache(level.alterant$treatments(),level.alterant$clientTreatments());
        }
        return alterant$coatingCache;
    }
}

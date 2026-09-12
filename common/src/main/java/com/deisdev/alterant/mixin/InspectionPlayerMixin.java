package com.deisdev.alterant.mixin;

import com.deisdev.alterant.network.InspectionPlayer;
import com.deisdev.alterant.network.RequestThrottle;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayer.class)
public abstract class InspectionPlayerMixin implements InspectionPlayer {
    @Unique private final RequestThrottle alterant$throttle = new RequestThrottle();
    @Override public RequestThrottle alterant$inspectionThrottle() { return alterant$throttle; }
}

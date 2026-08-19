package com.deisdev.preserve.mixin;

import com.deisdev.preserve.network.InspectionPlayer;
import com.deisdev.preserve.network.RequestThrottle;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayer.class)
public abstract class InspectionPlayerMixin implements InspectionPlayer {
    @Unique private final RequestThrottle preserve$throttle = new RequestThrottle();
    @Override public RequestThrottle preserve$inspectionThrottle() { return preserve$throttle; }
}

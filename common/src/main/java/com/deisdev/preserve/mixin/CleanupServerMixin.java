package com.deisdev.preserve.mixin;

import com.deisdev.preserve.engine.CleanupJob;
import com.deisdev.preserve.engine.CleanupServer;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class CleanupServerMixin implements CleanupServer {
    @Unique private final CleanupJob preserve$cleanup = new CleanupJob((MinecraftServer) (Object) this);
    @Unique private final AtomicLong preserve$saveFailures = new AtomicLong();
    @Override public CleanupJob preserve$cleanup() { return preserve$cleanup; }
    @Override public long preserve$saveFailures() { return preserve$saveFailures.get(); }
    @ModifyExpressionValue(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;pauseWhenEmptySeconds()I"))
    private int preserve$finishConsoleCleanup(int seconds) { return preserve$cleanup.running() ? 0 : seconds; }
    @Inject(method = "tickServer", at = @At("TAIL"))
    private void preserve$cleanupTick(java.util.function.BooleanSupplier hasTimeLeft, CallbackInfo ci) { preserve$cleanup.tick(); }
    @Inject(method = "stopServer", at = @At("HEAD"))
    private void preserve$releaseCleanupTickets(CallbackInfo ci) { preserve$cleanup.close(); }
    @Inject(method = "reportChunkSaveFailure", at = @At("HEAD"))
    private void preserve$recordSaveFailure(Throwable error, net.minecraft.world.level.chunk.storage.RegionStorageInfo storage,
                                           net.minecraft.world.level.ChunkPos pos, CallbackInfo ci) { preserve$saveFailures.incrementAndGet(); }
}

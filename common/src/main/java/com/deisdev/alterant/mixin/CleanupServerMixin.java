package com.deisdev.alterant.mixin;

import com.deisdev.alterant.engine.CleanupJob;
import com.deisdev.alterant.engine.CleanupServer;
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
    @Unique private final CleanupJob alterant$cleanup = new CleanupJob((MinecraftServer) (Object) this);
    @Unique private final AtomicLong alterant$saveFailures = new AtomicLong();
    @Override public CleanupJob alterant$cleanup() { return alterant$cleanup; }
    @Override public long alterant$saveFailures() { return alterant$saveFailures.get(); }
    @ModifyExpressionValue(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;pauseWhenEmptySeconds()I"))
    private int alterant$finishConsoleCleanup(int seconds) { return alterant$cleanup.running() ? 0 : seconds; }
    @Inject(method = "tickServer", at = @At("TAIL"))
    private void alterant$cleanupTick(java.util.function.BooleanSupplier hasTimeLeft, CallbackInfo ci) { alterant$cleanup.tick(); }
    @Inject(method = "stopServer", at = @At("HEAD"))
    private void alterant$releaseCleanupTickets(CallbackInfo ci) { alterant$cleanup.close(); }
    @Inject(method = "reportChunkSaveFailure", at = @At("HEAD"))
    private void alterant$recordSaveFailure(Throwable error, net.minecraft.world.level.chunk.storage.RegionStorageInfo storage,
                                           net.minecraft.world.level.ChunkPos pos, CallbackInfo ci) { alterant$saveFailures.incrementAndGet(); }
}

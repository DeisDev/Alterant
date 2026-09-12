package com.deisdev.alterant.mixin.client;

import com.deisdev.alterant.engine.PreservationLevel;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheMixin {
    @Shadow @Final private ClientLevel level;

    @Inject(method = {"updateViewCenter", "updateViewRadius"}, at = @At("RETURN"))
    private void alterant$trimView(CallbackInfo ci) {
        // A view jump makes old chunks inaccessible before (or without) individual unload callbacks.
        var cache = (ClientChunkCache) (Object) this;
        ((PreservationLevel) level).alterant$clientTreatments().retainChunks(key -> {
            var chunk = ChunkPos.unpack(key);
            return cache.hasChunk(chunk.x(), chunk.z());
        });
    }
}

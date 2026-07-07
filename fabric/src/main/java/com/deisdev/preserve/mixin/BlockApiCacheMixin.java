package com.deisdev.preserve.mixin;

import com.deisdev.preserve.transfer.FabricTransfers;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.impl.lookup.block.BlockApiCacheImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockApiCacheImpl.class)
public abstract class BlockApiCacheMixin {
    @ModifyReturnValue(method = "find", at = @At("RETURN"))
    private Object preserve$guard(Object result) {
        // Fabric's cached lookup invokes providers directly, bypassing BlockApiLookupImpl.find.
        var cache = (BlockApiCache<?, ?>) this;
        return FabricTransfers.wrap(cache.getLookup(), result, cache.getLevel(), cache.getPos());
    }
}

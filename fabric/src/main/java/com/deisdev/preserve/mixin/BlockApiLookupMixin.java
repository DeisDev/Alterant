package com.deisdev.preserve.mixin;

import com.deisdev.preserve.transfer.FabricTransfers;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.impl.lookup.block.BlockApiLookupImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockApiLookupImpl.class)
public abstract class BlockApiLookupMixin {
    @ModifyReturnValue(method = "find", at = @At("RETURN"))
    private Object preserve$guard(Object result, Level level, BlockPos pos, BlockState state, BlockEntity entity, Object context) {
        // Wrap even while untreated, so previously returned handles honor a later coating.
        return FabricTransfers.wrap((BlockApiLookup<?, ?>) this, result, level, pos);
    }
}

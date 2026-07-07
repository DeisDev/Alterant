package com.deisdev.preserve.mixin;

import com.deisdev.preserve.engine.TransferGuard;
import com.deisdev.preserve.transfer.GuardedEnergyHandler;
import com.deisdev.preserve.transfer.GuardedResourceHandler;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockCapability.class)
public abstract class BlockCapabilityMixin {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @ModifyReturnValue(method = "getCapability", at = @At("RETURN"))
    private Object preserve$guard(Object result, Level level, BlockPos pos, BlockState state, BlockEntity entity, Object context) {
        if (result == null) { return null; }
        // Wrap on every supported query, before a consumer can cache the handle. No per-tick invalidation is needed.
        Object capability = this;
        if (capability == Capabilities.Item.BLOCK || capability == Capabilities.Fluid.BLOCK) {
            return new GuardedResourceHandler((ResourceHandler) result, new TransferGuard(level, pos));
        }
        if (capability == Capabilities.Energy.BLOCK) {
            return new GuardedEnergyHandler((EnergyHandler) result, new TransferGuard(level, pos));
        }
        return result;
    }
}

package com.deisdev.alterant.mixin;

import com.deisdev.alterant.engine.TransferGuard;
import com.deisdev.alterant.transfer.GuardedEnergyHandler;
import com.deisdev.alterant.transfer.GuardedResourceHandler;
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
    private Object alterant$guard(Object result, Level level, BlockPos pos, BlockState state, BlockEntity entity, Object context) {
        if (result == null) { return null; }
        // Wrap on every supported query, before a consumer can cache the handle. No per-tick invalidation is needed.
        Object capability = this;
        if (capability == Capabilities.Item.BLOCK || capability == Capabilities.Fluid.BLOCK) {
            return new GuardedResourceHandler((ResourceHandler) result, new TransferGuard(level, pos, context instanceof net.minecraft.core.Direction side ? side : null, entity));
        }
        if (capability == Capabilities.Energy.BLOCK) {
            return new GuardedEnergyHandler((EnergyHandler) result, new TransferGuard(level, pos, null, entity));
        }
        return result;
    }
}

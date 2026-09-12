package com.deisdev.alterant.transfer;

import com.deisdev.alterant.engine.TransferGuard;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class FabricTransfers {
    private static final boolean HAS_ENERGY = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("team_reborn_energy");
    private FabricTransfers() {}
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Object wrap(BlockApiLookup<?, ?> lookup, Object result, Level level, BlockPos pos, Object context, net.minecraft.world.level.block.entity.BlockEntity entity) {
        if (result != null && (lookup == ItemStorage.SIDED || lookup == FluidStorage.SIDED)) {
            return GuardedStorage.wrap((Storage) result, new TransferGuard(level, pos, context instanceof net.minecraft.core.Direction side ? side : null, entity));
        }
        if (result != null && HAS_ENERGY) { return TeamRebornEnergy.wrap(lookup, result, level, pos, entity); }
        return result;
    }
}

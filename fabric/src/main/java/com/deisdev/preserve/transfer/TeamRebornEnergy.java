package com.deisdev.preserve.transfer;

import com.deisdev.preserve.engine.TransferGuard;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import team.reborn.energy.api.EnergyStorage;

/** Loaded only when Team Reborn Energy is present. Covers its sided block lookup, including cached handles. */
final class TeamRebornEnergy {
    private TeamRebornEnergy() {}
    static Object wrap(BlockApiLookup<?, ?> lookup, Object result, Level level, BlockPos pos) {
        return lookup == EnergyStorage.SIDED ? new Guarded((EnergyStorage) result, new TransferGuard(level, pos)) : result;
    }
    private static final class Guarded implements EnergyStorage {
        private final EnergyStorage delegate;
        private final TransferGuard guard;
        private Guarded(EnergyStorage delegate, TransferGuard guard) { this.delegate = delegate; this.guard = guard; }
        @Override public long insert(long amount, TransactionContext transaction) {
            StoragePreconditions.notNegative(amount);
            return guard.allowsMutation() ? delegate.insert(amount, transaction) : 0;
        }
        @Override public long extract(long amount, TransactionContext transaction) {
            StoragePreconditions.notNegative(amount);
            return guard.allowsMutation() ? delegate.extract(amount, transaction) : 0;
        }
        // Preserve connection metadata: the API expects a neighbor update when these declarations change.
        @Override public boolean supportsInsertion() { return delegate.supportsInsertion(); }
        @Override public boolean supportsExtraction() { return delegate.supportsExtraction(); }
        @Override public long getAmount() { return delegate.getAmount(); }
        @Override public long getCapacity() { return delegate.getCapacity(); }
    }
}

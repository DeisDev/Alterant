package com.deisdev.preserve.transfer;

import com.deisdev.preserve.engine.TransferGuard;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class GuardedEnergyHandler implements EnergyHandler {
    private final EnergyHandler delegate;
    private final TransferGuard guard;
    public GuardedEnergyHandler(EnergyHandler delegate, TransferGuard guard) { this.delegate = delegate; this.guard = guard; }
    @Override public long getAmountAsLong() { return delegate.getAmountAsLong(); }
    @Override public long getCapacityAsLong() { return delegate.getCapacityAsLong(); }
    @Override public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        return guard.allowsEnergyMutation() ? delegate.insert(amount, transaction) : 0;
    }
    @Override public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        return guard.allowsEnergyMutation() ? delegate.extract(amount, transaction) : 0;
    }
}

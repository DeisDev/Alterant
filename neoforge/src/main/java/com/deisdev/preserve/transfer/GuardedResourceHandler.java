package com.deisdev.preserve.transfer;

import com.deisdev.preserve.engine.TransferGuard;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class GuardedResourceHandler<T extends Resource> implements ResourceHandler<T> {
    private final ResourceHandler<T> delegate;
    private final TransferGuard guard;
    public GuardedResourceHandler(ResourceHandler<T> delegate, TransferGuard guard) { this.delegate = delegate; this.guard = guard; }
    @Override public int size() { return delegate.size(); }
    @Override public T getResource(int index) { return delegate.getResource(index); }
    @Override public long getAmountAsLong(int index) { return delegate.getAmountAsLong(index); }
    @Override public long getCapacityAsLong(int index, T resource) { return delegate.getCapacityAsLong(index, resource); }
    @Override public boolean isValid(int index, T resource) { return delegate.isValid(index, resource); }
    @Override public int insert(int index, T resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return guard.allowsMutation() ? delegate.insert(index, resource, amount, transaction) : 0;
    }
    @Override public int extract(int index, T resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return guard.allowsMutation() ? delegate.extract(index, resource, amount, transaction) : 0;
    }
}

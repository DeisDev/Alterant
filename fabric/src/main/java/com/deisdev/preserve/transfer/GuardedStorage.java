package com.deisdev.preserve.transfer;

import com.deisdev.preserve.engine.TransferGuard;
import com.google.common.collect.MapMaker;
import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/** All mutation paths, including cached views and slots, keep the guard. Read queries still reach the provider. */
public class GuardedStorage<T extends TransferVariant<?>> implements Storage<T> {
    private final Storage<T> delegate;
    private final TransferGuard guard;

    private GuardedStorage(Storage<T> delegate, TransferGuard guard) { this.delegate = delegate; this.guard = guard; }

    @SuppressWarnings("unchecked")
    public static <T extends TransferVariant<?>> Storage<T> wrap(Storage<T> delegate, TransferGuard guard) {
        if (delegate instanceof SlottedStorage<?> slots) { return new Slots<>((SlottedStorage<T>) slots, guard); }
        if (delegate instanceof SingleSlotStorage<?> slot) { return new Slot<>((SingleSlotStorage<T>) slot, guard); }
        return new GuardedStorage<>(delegate, guard);
    }

    // Keep connection metadata stable while a reversible coating temporarily rejects mutations.
    @Override public boolean supportsInsertion() { return delegate.supportsInsertion(); }
    @Override public boolean supportsExtraction() { return delegate.supportsExtraction(); }
    @Override public long insert(T resource, long amount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, amount);
        return guard.allowsMutation() ? delegate.insert(resource, amount, transaction) : 0;
    }
    @Override public long extract(T resource, long amount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, amount);
        return guard.allowsMutation() ? delegate.extract(resource, amount, transaction) : 0;
    }
    @Override public Iterator<StorageView<T>> iterator() {
        var iterator = delegate.iterator();
        return new Iterator<>() {
            @Override public boolean hasNext() { return iterator.hasNext(); }
            @Override public StorageView<T> next() { return new View<>(iterator.next(), guard); }
        };
    }
    @Override public long getVersion() { return (delegate.getVersion() << 1) | (guard.allowsMutation() ? 0 : 1); }

    private static final class Slots<T extends TransferVariant<?>> extends GuardedStorage<T> implements SlottedStorage<T> {
        private final SlottedStorage<T> slots;
        private final TransferGuard guard;
        private Slots(SlottedStorage<T> slots, TransferGuard guard) { super(slots, guard); this.slots = slots; this.guard = guard; }
        @Override public int getSlotCount() { return slots.getSlotCount(); }
        @Override public SingleSlotStorage<T> getSlot(int index) { return new Slot<>(slots.getSlot(index), guard); }
    }

    private static final class Slot<T extends TransferVariant<?>> extends GuardedStorage<T> implements SingleSlotStorage<T> {
        private final SingleSlotStorage<T> slot;
        private Slot(SingleSlotStorage<T> slot, TransferGuard guard) { super(slot, guard); this.slot = slot; }
        @Override public boolean isResourceBlank() { return slot.isResourceBlank(); }
        @Override public T getResource() { return slot.getResource(); }
        @Override public long getAmount() { return slot.getAmount(); }
        @Override public long getCapacity() { return slot.getCapacity(); }
        @Override public StorageView<T> getUnderlyingView() { return identity(slot.getUnderlyingView()); }
    }

    private static final class View<T extends TransferVariant<?>> implements StorageView<T> {
        private final StorageView<T> delegate;
        private final TransferGuard guard;
        private View(StorageView<T> delegate, TransferGuard guard) { this.delegate = delegate; this.guard = guard; }
        @Override public boolean isResourceBlank() { return delegate.isResourceBlank(); }
        @Override public T getResource() { return delegate.getResource(); }
        @Override public long getAmount() { return delegate.getAmount(); }
        @Override public long getCapacity() { return delegate.getCapacity(); }
        @Override public long extract(T resource, long amount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, amount);
            return guard.allowsMutation() ? delegate.extract(resource, amount, transaction) : 0;
        }
        @Override public StorageView<T> getUnderlyingView() { return identity(delegate.getUnderlyingView()); }
    }

    // Fabric uses underlying views as identity tokens to avoid self-transfer. Canonical, read-only tokens keep
    // that identity across repeated lookups without exposing an unguarded extractor. Neither map side retains worlds.
    private static final ConcurrentMap<StorageView<?>, StorageView<?>> IDENTITIES = new MapMaker().weakKeys().weakValues().makeMap();
    @SuppressWarnings("unchecked")
    private static <T extends TransferVariant<?>> StorageView<T> identity(StorageView<T> delegate) {
        return (StorageView<T>) IDENTITIES.computeIfAbsent(delegate, ignored -> new IdentityView<>(delegate));
    }
    private static final class IdentityView<T extends TransferVariant<?>> implements StorageView<T> {
        private final StorageView<T> delegate;
        private IdentityView(StorageView<T> delegate) { this.delegate = delegate; }
        @Override public boolean isResourceBlank() { return delegate.isResourceBlank(); }
        @Override public T getResource() { return delegate.getResource(); }
        @Override public long getAmount() { return delegate.getAmount(); }
        @Override public long getCapacity() { return delegate.getCapacity(); }
        @Override public long extract(T resource, long amount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, amount);
            return 0;
        }
    }
}

package com.deisdev.alterant.engine;

import java.util.List;
import java.util.Optional;

/** Immutable observation of committed removals. Delivery happens inside the service, never by redeeming a receipt. */
public record RemovalReceipt(RemovalCause cause, List<Entry> entries) {
    public record Entry(long position, Optional<RecoveryEntitlement> recovered) {}
    public RemovalReceipt { entries = List.copyOf(entries); }
}

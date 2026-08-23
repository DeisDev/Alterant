package com.deisdev.preserve.engine;

import com.deisdev.preserve.platform.Services;
import java.util.function.Supplier;
import net.minecraft.server.level.TicketType;

public final class CleanupTickets {
    public static final Supplier<TicketType> TYPE = Services.PLATFORM.registerCleanupTicket();
    private CleanupTickets() {}
    public static TicketType create() {
        // Temporary block simulation restores retained work. Never persist a ticket belonging to an uninstalled mod.
        return new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    }
    public static void init() {}
}

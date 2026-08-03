package com.deisdev.preserve;

/** Shared bootstrap. Game content is registered by each loader at its registry boundary. */
public final class Preserve {
    private Preserve() {}

    public static void init() {
        com.deisdev.preserve.item.PreserveItems.init();
        Constants.LOG.info("Preserve initialized");
    }
}

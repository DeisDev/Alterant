package com.deisdev.preserve;

/** Shared bootstrap. Game content is registered by each loader at its registry boundary. */
public final class Preserve {
    private Preserve() {}

    public static void init() {
        com.deisdev.preserve.engine.CleanupTickets.init();
        com.deisdev.preserve.item.PreserveItems.init();
        com.deisdev.preserve.basin.PreserveBlocks.init();
        com.deisdev.preserve.item.PreserveMenus.init();
        com.deisdev.preserve.recipe.PreserveRecipes.init();
        if (com.deisdev.preserve.platform.Services.PLATFORM.isModLoaded("openpartiesandclaims")) {
            com.deisdev.preserve.integration.OpenPacPermission.register();
        }
        Constants.LOG.info("Preserve initialized");
    }
}

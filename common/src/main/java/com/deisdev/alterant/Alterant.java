package com.deisdev.alterant;

/** Shared bootstrap. Game content is registered by each loader at its registry boundary. */
public final class Alterant {
    private Alterant() {}

    public static void init() {
        com.deisdev.alterant.engine.CleanupTickets.init();
        com.deisdev.alterant.item.AlterantItems.init();
        com.deisdev.alterant.basin.AlterantBlocks.init();
        com.deisdev.alterant.item.AlterantMenus.init();
        com.deisdev.alterant.recipe.AlterantRecipes.init();
        if (com.deisdev.alterant.platform.Services.PLATFORM.isModLoaded("openpartiesandclaims")) {
            com.deisdev.alterant.integration.OpenPacPermission.register();
        }
        Constants.LOG.info("Alterant initialized");
    }
}

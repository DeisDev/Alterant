package com.deisdev.alterant.item;

import com.deisdev.alterant.platform.Services;
import java.util.function.Supplier;
import net.minecraft.world.inventory.MenuType;

public final class AlterantMenus {
    public static final Supplier<MenuType<com.deisdev.alterant.basin.ReclaimingBasinMenu>> BASIN = Services.PLATFORM.registerMenu("reclaiming_basin", com.deisdev.alterant.basin.ReclaimingBasinMenu::new);
    public static final Supplier<MenuType<ReclamationMenu>> RECLAMATION = Services.PLATFORM.registerMenu("reclamation", ReclamationMenu::new);
    public static final Supplier<MenuType<GrowthLimitMenu>> GROWTH_LIMIT = Services.PLATFORM.registerMenu("growth_limit", GrowthLimitMenu::new);
    public static final Supplier<MenuType<TransferPolicyMenu>> TRANSFER_POLICY = Services.PLATFORM.registerMenu("transfer_policy", TransferPolicyMenu::new);
    private AlterantMenus() {}
    public static void init() {}
}

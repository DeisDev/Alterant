package com.deisdev.preserve.item;

import com.deisdev.preserve.platform.Services;
import java.util.function.Supplier;
import net.minecraft.world.inventory.MenuType;

public final class PreserveMenus {
    public static final Supplier<MenuType<com.deisdev.preserve.basin.ReclaimingBasinMenu>> BASIN = Services.PLATFORM.registerMenu("reclaiming_basin", com.deisdev.preserve.basin.ReclaimingBasinMenu::new);
    public static final Supplier<MenuType<ReclamationMenu>> RECLAMATION = Services.PLATFORM.registerMenu("reclamation", ReclamationMenu::new);
    public static final Supplier<MenuType<GrowthLimitMenu>> GROWTH_LIMIT = Services.PLATFORM.registerMenu("growth_limit", GrowthLimitMenu::new);
    public static final Supplier<MenuType<TransferPolicyMenu>> TRANSFER_POLICY = Services.PLATFORM.registerMenu("transfer_policy", TransferPolicyMenu::new);
    private PreserveMenus() {}
    public static void init() {}
}

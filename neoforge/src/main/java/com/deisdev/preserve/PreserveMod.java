package com.deisdev.preserve;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public final class PreserveMod {

    public PreserveMod(IEventBus eventBus) {
        Preserve.init();
    }
}

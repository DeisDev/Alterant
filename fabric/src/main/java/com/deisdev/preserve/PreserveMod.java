package com.deisdev.preserve;

import net.fabricmc.api.ModInitializer;

public final class PreserveMod implements ModInitializer {

    @Override
    public void onInitialize() {
        Preserve.init();
    }
}

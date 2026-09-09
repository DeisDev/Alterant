package com.deisdev.preserve.integration.jade;

import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;

public final class PreserveJade implements IWailaPlugin {
    @Override public void register(IWailaCommonRegistration registration) { registration.registerBlockDataProvider(JadeSerumData.INSTANCE, Block.class); }
    @Override public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(com.deisdev.preserve.client.JadeTreatmentProvider.INSTANCE, Block.class);
    }
}

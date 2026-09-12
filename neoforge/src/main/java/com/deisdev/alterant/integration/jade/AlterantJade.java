package com.deisdev.alterant.integration.jade;

import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class AlterantJade implements IWailaPlugin {
    @Override public void register(IWailaCommonRegistration registration) { registration.registerBlockDataProvider(JadeSerumData.INSTANCE, Block.class); }
    @Override public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(com.deisdev.alterant.client.JadeTreatmentProvider.INSTANCE, Block.class);
    }
}

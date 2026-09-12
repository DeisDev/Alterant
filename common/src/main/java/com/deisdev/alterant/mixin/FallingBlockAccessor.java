package com.deisdev.alterant.mixin;

import net.minecraft.world.level.block.FallingBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FallingBlock.class)
public interface FallingBlockAccessor {
    @Invoker("getDelayAfterPlace") int alterant$fallDelay();
}

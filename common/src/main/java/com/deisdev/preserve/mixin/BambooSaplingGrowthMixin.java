package com.deisdev.preserve.mixin;

import com.deisdev.preserve.engine.NaturalGrowth;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BambooSaplingBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BambooSaplingBlock.class)
public abstract class BambooSaplingGrowthMixin {
    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BambooSaplingBlock;growBamboo(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void preserve$naturalSprout(BambooSaplingBlock block, Level level, BlockPos source, Operation<Void> original) {
        // Prevent the first segment and resulting sapling conversion together, while the bone-meal caller remains ordinary.
        if (!NaturalGrowth.blocks(level, source, source.above())) { original.call(block, level, source); }
    }
}

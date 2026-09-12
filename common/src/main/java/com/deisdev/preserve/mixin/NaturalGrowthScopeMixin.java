package com.deisdev.preserve.mixin;

import com.deisdev.preserve.engine.NaturalGrowth;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({CropBlock.class, BambooStalkBlock.class, BambooSaplingBlock.class, VineBlock.class, GrowingPlantHeadBlock.class,
        net.minecraft.world.level.block.SweetBerryBushBlock.class, net.minecraft.world.level.block.CocoaBlock.class, net.minecraft.world.level.block.NetherWartBlock.class})
public abstract class NaturalGrowthScopeMixin {
    @WrapMethod(method = "randomTick")
    private void preserve$growthAttempt(BlockState state, ServerLevel level, BlockPos source, RandomSource random, Operation<Void> original) {
        // The callback supplies an explicit cause and source. The scope is removed even if a mod callback throws.
        NaturalGrowth.run(level, source, () -> original.call(state, level, source, random));
    }
}

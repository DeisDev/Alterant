package com.deisdev.preserve.mixin;

import com.deisdev.preserve.engine.NaturalGrowth;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({CropBlock.class, BambooStalkBlock.class, GrowingPlantHeadBlock.class})
public abstract class CropEventsMixin {
    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/CommonHooks;canCropGrow(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Z"))
    private boolean preserve$beforeGrowthEvent(Level receiver, BlockPos eventPos, BlockState eventState, boolean chance, Operation<Boolean> original,
                                               BlockState state, ServerLevel level, BlockPos source, RandomSource random) {
        // NeoForge reports the source for bamboo/crops, and the resolved destination for growing heads.
        var target = (Object) this instanceof BambooStalkBlock ? source.above() : eventPos;
        return !NaturalGrowth.blocks(level, source, target) && original.call(receiver, eventPos, eventState, chance);
    }
    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/CommonHooks;fireCropGrowPost(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void preserve$growthResult(Level receiver, BlockPos eventPos, BlockState eventState, Operation<Void> original,
                                      BlockState state, ServerLevel level, BlockPos source, RandomSource random) {
        // A pre-event listener can change conditional policy inputs. If the later write guard stops growth, do not report success.
        if (!NaturalGrowth.wasBlocked(level, source)) { original.call(receiver, eventPos, eventState); }
    }
}

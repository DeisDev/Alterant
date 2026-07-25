package com.deisdev.preserve.mixin;

import com.deisdev.preserve.api.Action;
import com.deisdev.preserve.engine.PolicyEngine;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FarmlandBlock.class)
public abstract class FarmlandDryingMixin {
    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean preserve$moistureLoss(ServerLevel level, BlockPos pos, BlockState next, int flags, Operation<Boolean> original) {
        // Both drying and hydration use setBlock here. Increasing moisture is deliberately retained.
        if (next.getValue(FarmlandBlock.MOISTURE) < level.getBlockState(pos).getValue(FarmlandBlock.MOISTURE)
                && PolicyEngine.blocks(level, pos, Action.ENVIRONMENTAL_CHANGE, pos, pos)) { return false; }
        return original.call(level, pos, next, flags);
    }
    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FarmlandBlock;turnToDirt(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void preserve$dryDirt(Entity entity, BlockState state, Level level, BlockPos pos, Operation<Void> original) {
        // Capture the drying caller before entity pushes and game events. Trampling/support loss call the helper separately.
        if (!PolicyEngine.blocks(level, pos, Action.ENVIRONMENTAL_CHANGE, pos, pos)) { original.call(entity, state, level, pos); }
    }
}

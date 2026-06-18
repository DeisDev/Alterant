package com.deisdev.preserve.mixin;

import com.deisdev.preserve.engine.PreservationLevel;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @Shadow @Final private Level level;

    @WrapOperation(method = "setBlockState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState preserve$clearReplacement(LevelChunkSection section, int x, int y, int z,
            BlockState state, Operation<BlockState> original, BlockPos pos, BlockState requested, int flags) {
        BlockState old = original.call(section, x, y, z, state);
        // The write succeeded; clear before onPlace/removal callbacks can inspect or schedule for the new occupant.
        if (old.getBlock() != state.getBlock()) {
            var access = (PreservationLevel) level;
            if (access.preserve$service() != null) { access.preserve$service().destroyed(pos); }
            else { access.preserve$treatments().remove(pos.asLong()); }
        }
        return old;
    }
}

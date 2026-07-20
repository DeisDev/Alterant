package com.deisdev.preserve.mixin;

import com.deisdev.preserve.engine.PreservationLevel;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @Shadow @Final private Level level;

    @ModifyExpressionValue(method = "setBlockEntity", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object preserve$clearEntityReplacement(Object previous, net.minecraft.world.level.block.entity.BlockEntity next) {
        // Ordinary initial load has no previous entry. A successful live BE replacement is a new occupant.
        if (previous != null && previous != next) { preserve$discard(next.getBlockPos()); }
        return previous;
    }

    @ModifyExpressionValue(method = "removeBlockEntity", at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object preserve$clearRemovedEntity(Object removed, BlockPos pos) {
        // Chunk unload uses clearAllBlockEntities, not this live removal path, so saved coatings survive unload.
        if (removed != null) { preserve$discard(pos); }
        return removed;
    }

    private void preserve$discard(BlockPos pos) {
        var service = ((PreservationLevel) level).preserve$service();
        if (service != null) { service.destroyed(pos); }
    }

    @Inject(method = "unregisterTickContainerFromLevel", at = @At("HEAD"))
    private void preserve$pauseUnloadedResumptions(ServerLevel serverLevel, CallbackInfo ci) {
        // Like vanilla SavedTick, remaining delay stops elapsing while a chunk is unloaded.
        var service = ((PreservationLevel) serverLevel).preserve$service();
        if (service != null) { service.chunkUnloaded(((LevelChunk) (Object) this).getPos()); }
    }

    @WrapOperation(method = "setBlockState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState preserve$clearReplacement(LevelChunkSection section, int x, int y, int z,
            BlockState state, Operation<BlockState> original, BlockPos pos, BlockState requested, int flags) {
        BlockState old = original.call(section, x, y, z, state);
        // The write succeeded; clear before onPlace/removal callbacks can inspect or schedule for the new occupant.
        if (old.getBlock() != state.getBlock()) {
            var access = (PreservationLevel) level;
            if (access.preserve$service() != null) { access.preserve$service().destroyed(pos); }
            // Clients receive authoritative removals. A batched block update can follow an apply delta;
            // clearing locally here would erase a valid coating and its revision without another snapshot.
        }
        return old;
    }
}

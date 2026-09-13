package com.deisdev.alterant.mixin.client;

import com.deisdev.alterant.client.coating.CoatingLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Terrain's own invalidation includes block neighbors, light packets, resource/quality changes and model updates. */
@Mixin(LevelExtractor.class)
public abstract class CoatingInvalidationMixin {
    @Shadow private ClientLevel level;
    @Inject(method="setSectionDirty(IIIZ)V",at=@At("HEAD"))
    private void alterant$dirty(int x,int y,int z,boolean playerChanged,CallbackInfo ci) {
        if (level != null) { ((CoatingLevel)level).alterant$coatings().dirty(SectionPos.asLong(x,y,z)); }
    }
    // Sodium replaces these callers and no longer routes their updates through setSectionDirty.
    @Inject(method="setBlockDirty(Lnet/minecraft/core/BlockPos;Z)V",at=@At("HEAD"))
    private void alterant$blockDirty(BlockPos pos,boolean playerChanged,CallbackInfo ci) {
        alterant$dirtyBlocks(pos.getX(),pos.getY(),pos.getZ(),pos.getX(),pos.getY(),pos.getZ());
    }
    @Inject(method="setBlocksDirty",at=@At("HEAD"))
    private void alterant$blocksDirty(int minX,int minY,int minZ,int maxX,int maxY,int maxZ,CallbackInfo ci) {
        alterant$dirtyBlocks(minX,minY,minZ,maxX,maxY,maxZ);
    }
    @Inject(method="setSectionDirtyWithNeighbors",at=@At("HEAD"))
    private void alterant$neighborsDirty(int x,int y,int z,CallbackInfo ci) {
        alterant$dirtySections(x-1,y-1,z-1,x+1,y+1,z+1);
    }
    @Unique
    private void alterant$dirtyBlocks(int minX,int minY,int minZ,int maxX,int maxY,int maxZ) {
        alterant$dirtySections(SectionPos.blockToSectionCoord(minX-1),SectionPos.blockToSectionCoord(minY-1),SectionPos.blockToSectionCoord(minZ-1),
                SectionPos.blockToSectionCoord(maxX+1),SectionPos.blockToSectionCoord(maxY+1),SectionPos.blockToSectionCoord(maxZ+1));
    }
    @Unique
    private void alterant$dirtySections(int minX,int minY,int minZ,int maxX,int maxY,int maxZ) {
        if (level == null) { return; }
        var coatings = ((CoatingLevel)level).alterant$coatings();
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            for (int y = Math.max(minY,level.getMinSectionY()); y <= Math.min(maxY,level.getMaxSectionY()); y++) {
                coatings.dirty(SectionPos.asLong(x,y,z));
            }
        }
    }
    @Inject(method="allChanged",at=@At("HEAD"))
    private void alterant$allChanged(CallbackInfo ci) {
        if (level != null) { ((CoatingLevel)level).alterant$coatings().clearMeshes(); }
    }
}

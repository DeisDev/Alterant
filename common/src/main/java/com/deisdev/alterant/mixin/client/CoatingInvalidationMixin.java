package com.deisdev.alterant.mixin.client;

import com.deisdev.alterant.client.coating.CoatingLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
    @Inject(method="allChanged",at=@At("HEAD"))
    private void alterant$allChanged(CallbackInfo ci) {
        if (level != null) { ((CoatingLevel)level).alterant$coatings().clearMeshes(); }
    }
}

package com.deisdev.alterant.mixin.client;

import com.deisdev.alterant.client.coating.CoatingLevel;
import com.deisdev.alterant.client.coating.CoatingSurfaceProfiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class CoatingTagsMixin {
    @Inject(method="handleUpdateTags",at=@At("RETURN"))
    private void alterant$tags(ClientboundUpdateTagsPacket packet, CallbackInfo ci) {
        CoatingSurfaceProfiles.tagsChanged();
        var level = Minecraft.getInstance().level;
        if (level != null) { ((CoatingLevel)level).alterant$coatings().clearMeshes(); }
    }
}

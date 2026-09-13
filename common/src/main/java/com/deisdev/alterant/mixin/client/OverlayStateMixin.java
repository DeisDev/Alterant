package com.deisdev.alterant.mixin.client;

import com.deisdev.alterant.client.OverlayRenderState;
import com.deisdev.alterant.client.ToolOverlay;
import java.util.List;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderState.class)
public abstract class OverlayStateMixin implements OverlayRenderState {
    @Unique private com.deisdev.alterant.client.coating.CoatingRenderState alterant$coatings = com.deisdev.alterant.client.coating.CoatingRenderState.EMPTY;
    @Override public com.deisdev.alterant.client.coating.CoatingRenderState alterant$coatings() { return alterant$coatings; }
    @Override public void alterant$coatings(com.deisdev.alterant.client.coating.CoatingRenderState value) { alterant$coatings = value; }
    @Unique private List<ToolOverlay.Mark> alterant$marks = List.of();
    @Unique private com.deisdev.alterant.client.SerumCard.Card alterant$card;
    @Override public com.deisdev.alterant.client.SerumCard.Card alterant$serumCard() { return alterant$card; }
    @Override public void alterant$serumCard(com.deisdev.alterant.client.SerumCard.Card card) { alterant$card = card; }
    @Override public List<ToolOverlay.Mark> alterant$overlay() { return alterant$marks; }
    @Override public void alterant$overlay(List<ToolOverlay.Mark> marks) { alterant$marks = List.copyOf(marks); }
    @Inject(method = "reset", at = @At("TAIL"))
    private void alterant$clear(CallbackInfo ci) { alterant$marks = List.of(); alterant$card = null; alterant$coatings = com.deisdev.alterant.client.coating.CoatingRenderState.EMPTY; }
}

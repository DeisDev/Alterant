package com.deisdev.preserve.mixin.client;

import com.deisdev.preserve.client.OverlayRenderState;
import com.deisdev.preserve.client.ToolOverlay;
import java.util.List;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderState.class)
public abstract class OverlayStateMixin implements OverlayRenderState {
    @Unique private List<ToolOverlay.Mark> preserve$marks = List.of();
    @Override public List<ToolOverlay.Mark> preserve$overlay() { return preserve$marks; }
    @Override public void preserve$overlay(List<ToolOverlay.Mark> marks) { preserve$marks = List.copyOf(marks); }
    @Inject(method = "reset", at = @At("TAIL"))
    private void preserve$clear(CallbackInfo ci) { preserve$marks = List.of(); }
}

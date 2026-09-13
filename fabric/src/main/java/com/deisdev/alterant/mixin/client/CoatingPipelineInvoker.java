package com.deisdev.alterant.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderPipelines.class)
public interface CoatingPipelineInvoker {
    @Invoker("register")
    static RenderPipeline alterant$register(RenderPipeline pipeline) { throw new AssertionError(); }
}

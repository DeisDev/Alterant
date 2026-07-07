package com.deisdev.preserve.mixin;

import com.deisdev.preserve.engine.ContainerParts;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CompoundContainer.class)
public abstract class CompoundContainerMixin implements ContainerParts {
    @Shadow @Final private Container container1;
    @Shadow @Final private Container container2;
    @Override public Container preserve$first() { return container1; }
    @Override public Container preserve$second() { return container2; }
}

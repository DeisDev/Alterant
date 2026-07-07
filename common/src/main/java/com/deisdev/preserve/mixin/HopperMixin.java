package com.deisdev.preserve.mixin;

import com.deisdev.preserve.engine.InteractionGate;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperMixin {
    @Inject(method = "tryMoveInItem", at = @At("HEAD"), cancellable = true)
    private static void preserve$insertion(Container from, Container into, ItemStack stack, int slot, Direction side,
                                         CallbackInfoReturnable<ItemStack> cir) {
        // Vanilla hoppers and droppers reach this boundary before mutating the destination stack.
        if (InteractionGate.blocked(from) || InteractionGate.blocked(into)) { cir.setReturnValue(stack); }
    }

    @Inject(method = "tryTakeInItemFromSlot", at = @At("HEAD"), cancellable = true)
    private static void preserve$extraction(Hopper into, Container from, int slot, Direction side, CallbackInfoReturnable<Boolean> cir) {
        // Guard before removing from the source: a later rejection cannot undo every transfer side effect.
        if (InteractionGate.blocked(from) || InteractionGate.blocked(into)) { cir.setReturnValue(false); }
    }
}

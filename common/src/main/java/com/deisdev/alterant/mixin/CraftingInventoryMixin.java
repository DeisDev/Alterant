package com.deisdev.alterant.mixin;

import com.deisdev.alterant.recipe.FullJarCrafting;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class CraftingInventoryMixin {
    @Inject(method = "isUsableForCrafting", at = @At("HEAD"), cancellable = true)
    private static void alterant$selectFullJar(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // The Temporal placement scope ends before returning to ordinary inventory or recipe operations.
        if (!FullJarCrafting.allows(stack)) { cir.setReturnValue(false); }
    }
}

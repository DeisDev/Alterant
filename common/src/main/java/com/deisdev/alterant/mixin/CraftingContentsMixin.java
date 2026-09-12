package com.deisdev.alterant.mixin;

import com.deisdev.alterant.recipe.FullJarCrafting;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StackedItemContents.class)
public abstract class CraftingContentsMixin {
    @Inject(method = "accountStack(Lnet/minecraft/world/item/ItemStack;I)V", at = @At("HEAD"), cancellable = true)
    private void alterant$countFullJars(ItemStack stack, int maximum, CallbackInfo ci) {
        // Cover stacks already in the crafting grid as well as inventory counts, before item data is discarded.
        if (!FullJarCrafting.allows(stack)) { ci.cancel(); }
    }
}

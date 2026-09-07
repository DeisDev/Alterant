package com.deisdev.preserve.mixin.client;

import com.deisdev.preserve.client.ItemBorderColors;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Item Borders 1.3.2 (26.2): customize only its automatic fallback, preserving config precedence. */
@Pseudo
@Mixin(targets = "com.anthonyhilyard.itemborders.config.ItemBordersConfig", remap = false)
public abstract class ItemBordersMixin {
    @ModifyExpressionValue(method = "getBorderColorForItem", at = @At(value = "INVOKE",
            target = "Lcom/anthonyhilyard/prism/item/ItemColors;getColorForItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/network/chat/TextColor;)Lnet/minecraft/network/chat/TextColor;"))
    private TextColor preserve$automaticBorder(TextColor original, ItemStack stack, HolderLookup.Provider provider) {
        return ItemBorderColors.automaticColor(stack, original);
    }
}

package com.deisdev.alterant.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** Native interaction hook after RightClickBlock authorization, before placement snapshot capture. */
public final class NeoForgeShapingStylusItem extends ShapingStylusItem {
    public NeoForgeShapingStylusItem(Properties properties) { super(properties); }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return context.getPlayer() != null && context.getPlayer().getCooldowns().isOnCooldown(stack) ? InteractionResult.FAIL : useOn(context);
    }
}

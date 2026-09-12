package com.deisdev.alterant.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** Removal is a tool interaction, after native authorization and outside placement rollback. */
public final class NeoForgeScraperItem extends ScraperItem {
    public NeoForgeScraperItem(Properties properties) { super(properties); }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return context.getPlayer() != null && context.getPlayer().getCooldowns().isOnCooldown(stack) ? InteractionResult.FAIL : useOn(context);
    }
}

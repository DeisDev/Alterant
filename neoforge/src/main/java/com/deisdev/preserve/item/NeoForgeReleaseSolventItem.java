package com.deisdev.preserve.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** Dissolving may resume shapes and machines; those changes are not block placement snapshots. */
public final class NeoForgeReleaseSolventItem extends ReleaseSolventItem {
    public NeoForgeReleaseSolventItem(Properties properties) { super(properties); }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return context.getPlayer() != null && context.getPlayer().getCooldowns().isOnCooldown(stack) ? InteractionResult.FAIL : useOn(context);
    }
}

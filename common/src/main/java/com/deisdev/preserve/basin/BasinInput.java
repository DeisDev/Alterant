package com.deisdev.preserve.basin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record BasinInput(ItemStack residue, ItemStack additive) implements RecipeInput {
    @Override public ItemStack getItem(int slot) { return switch (slot) { case 0 -> residue; case 1 -> additive; default -> throw new IndexOutOfBoundsException(slot); }; }
    @Override public int size() { return 2; }
}

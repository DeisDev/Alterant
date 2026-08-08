package com.deisdev.preserve.recipe;

import com.deisdev.preserve.item.CompoundItem;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

/** Vanilla recipe-book counts and selection use item identity; narrow their shared inventory paths to this recipe. */
public final class FullJarCrafting {
    private static final ScopedValue<Boolean> TEMPORAL = ScopedValue.newInstance();
    private FullJarCrafting() {}
    public static <T> T placing(Recipe<?> recipe, Supplier<T> operation) {
        boolean temporal = recipe instanceof TemporalRecipe;
        if (!TEMPORAL.isBound() && !temporal) { return operation.get(); }
        return ScopedValue.where(TEMPORAL, temporal).call(operation::get);
    }
    public static boolean allows(ItemStack stack) {
        return !TEMPORAL.isBound() || !TEMPORAL.get() || !(stack.getItem() instanceof CompoundItem jar) || jar.isFull(stack);
    }
}

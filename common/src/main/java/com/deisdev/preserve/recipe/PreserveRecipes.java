package com.deisdev.preserve.recipe;

import com.deisdev.preserve.platform.Services;
import java.util.function.Supplier;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class PreserveRecipes {
    public static final Supplier<RecipeSerializer<TemporalRecipe>> TEMPORAL = Services.PLATFORM.registerRecipeSerializer("temporal_stasis", () -> TemporalRecipe.SERIALIZER);
    public static final Supplier<RecipeSerializer<SerumUpgradeRecipe>> SERUM_UPGRADE = Services.PLATFORM.registerRecipeSerializer("serum_upgrade", () -> SerumUpgradeRecipe.SERIALIZER);
    private PreserveRecipes() {}
    public static void init() {}
}

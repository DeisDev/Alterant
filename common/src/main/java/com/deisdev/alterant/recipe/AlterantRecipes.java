package com.deisdev.alterant.recipe;

import com.deisdev.alterant.platform.Services;
import java.util.function.Supplier;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class AlterantRecipes {
    public static final Supplier<net.minecraft.world.item.crafting.RecipeType<com.deisdev.alterant.basin.ReclaimingRecipe>> RECLAIMING_TYPE = Services.PLATFORM.registerRecipeType("reclaiming");
    public static final Supplier<net.minecraft.world.item.crafting.RecipeBookCategory> RECLAIMING_CATEGORY = Services.PLATFORM.registerRecipeCategory("reclaiming");
    public static final Supplier<RecipeSerializer<com.deisdev.alterant.basin.ReclaimingRecipe>> RECLAIMING = Services.PLATFORM.registerRecipeSerializer("reclaiming", () -> com.deisdev.alterant.basin.ReclaimingRecipe.SERIALIZER);
    public static final Supplier<RecipeSerializer<TemporalRecipe>> TEMPORAL = Services.PLATFORM.registerRecipeSerializer("temporal_stasis", () -> TemporalRecipe.SERIALIZER);
    public static final Supplier<RecipeSerializer<SerumUpgradeRecipe>> SERUM_UPGRADE = Services.PLATFORM.registerRecipeSerializer("serum_upgrade", () -> SerumUpgradeRecipe.SERIALIZER);
    private AlterantRecipes() {}
    public static void init() {}
}

package com.deisdev.alterant.mixin;

import com.deisdev.alterant.recipe.FullJarCrafting;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.List;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlaceRecipe.class)
public abstract class RecipeBookMixin {
    @WrapMethod(method = "placeRecipe(Lnet/minecraft/recipebook/ServerPlaceRecipe$CraftingMenuAccess;IILjava/util/List;Ljava/util/List;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/crafting/RecipeHolder;ZZ)Lnet/minecraft/world/inventory/RecipeBookMenu$PostPlaceAction;")
    private static RecipeBookMenu.PostPlaceAction alterant$fullJars(ServerPlaceRecipe.CraftingMenuAccess<?> menu, int width, int height,
            List<Slot> grid, List<Slot> clear, Inventory inventory, RecipeHolder<?> recipe, boolean maximum, boolean drop,
            Operation<RecipeBookMenu.PostPlaceAction> original) {
        // Scope both availability counting and actual selection, before the native placer clears or fills slots.
        return FullJarCrafting.placing(recipe.value(), () -> original.call(menu, width, height, grid, clear, inventory, recipe, maximum, drop));
    }
}

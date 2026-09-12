package com.deisdev.alterant.basin;

import com.deisdev.alterant.item.ReclamationJarItem;
import com.deisdev.alterant.item.ResidueFamily;
import com.deisdev.alterant.recipe.AlterantRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.*;
import net.minecraft.world.level.Level;

/** Counted batches consume exact families, without expanding counters into ordinary stacks. */
public record ReclaimingRecipe(ResidueFamily family, int count, Optional<Ingredient> additive, int additiveCount,
                              ItemStackTemplate result, int duration) implements Recipe<BasinInput> {
    public static final com.mojang.serialization.MapCodec<ReclaimingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResidueFamily.CODEC.fieldOf("family").forGetter(ReclaimingRecipe::family),
            Codec.intRange(1, 256).fieldOf("count").forGetter(ReclaimingRecipe::count),
            Ingredient.CODEC.optionalFieldOf("additive").forGetter(ReclaimingRecipe::additive),
            Codec.intRange(1, 64).optionalFieldOf("additive_count", 1).forGetter(ReclaimingRecipe::additiveCount),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(ReclaimingRecipe::result),
            Codec.intRange(1, 12000).fieldOf("duration").forGetter(ReclaimingRecipe::duration)
    ).apply(i, ReclaimingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReclaimingRecipe> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
    public static final RecipeSerializer<ReclaimingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
    public ReclaimingRecipe {
        if (count < 1 || count > 256 || additiveCount < 1 || additiveCount > 64 || duration < 1 || duration > 12000
                || result.count() < 1 || result.count() > 64) { throw new IllegalArgumentException("Invalid reclaiming batch"); }
    }
    public int available(ItemStack stack) { return ReclamationJarItem.valid(stack) ? ReclamationJarItem.contents(stack).count(family) : stack.is(family.item()) ? stack.getCount() : 0; }
    @Override public boolean matches(BasinInput input, Level level) {
        return available(input.residue()) >= count && additive.map(value -> value.test(input.additive()) && input.additive().getCount() >= additiveCount).orElse(true);
    }
    @Override public ItemStack assemble(BasinInput input) { return result.create(); }
    @Override public boolean showNotification() { return true; }
    @Override public String group() { return ""; }
    @Override public RecipeSerializer<ReclaimingRecipe> getSerializer() { return AlterantRecipes.RECLAIMING.get(); }
    @Override public RecipeType<ReclaimingRecipe> getType() { return AlterantRecipes.RECLAIMING_TYPE.get(); }
    @Override public PlacementInfo placementInfo() {
        // Discovery can describe the loose-material route. Physical jar balances are selected in the basin's own menu.
        var ingredients = new ArrayList<Ingredient>();
        var residue = Ingredient.of(family.item());
        for (int n = 0; n < count; n++) { ingredients.add(residue); }
        additive.ifPresent(value -> { for (int n = 0; n < additiveCount; n++) { ingredients.add(value); } });
        return PlacementInfo.create(ingredients);
    }
    @Override public RecipeBookCategory recipeBookCategory() { return AlterantRecipes.RECLAIMING_CATEGORY.get(); }
    @Override public List<RecipeDisplay> display() {
        var ingredients = new ArrayList<SlotDisplay>();
        for (int remaining = count; remaining > 0; remaining -= 64) { ingredients.add(new SlotDisplay.ItemStackSlotDisplay(new ItemStackTemplate(family.item(), Math.min(64, remaining)))); }
        additive.ifPresent(value -> { for (int n = 0; n < additiveCount; n++) { ingredients.add(value.display()); } });
        return List.of(new ShapelessCraftingRecipeDisplay(ingredients, new SlotDisplay.ItemStackSlotDisplay(result), new SlotDisplay.ItemSlotDisplay(AlterantBlocks.BASIN_ITEM.get())));
    }
}

package com.deisdev.preserve.recipe;

import com.deisdev.preserve.api.Formulation;
import com.deisdev.preserve.item.CompoundItem;
import com.deisdev.preserve.item.PreserveItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

/** A full input dose becomes one full upgraded dose; its bottle is retained in the output. */
public final class SerumUpgradeRecipe extends NormalCraftingRecipe {
    private final Formulation inputSerum;
    private final Formulation resultSerum;
    public static final MapCodec<SerumUpgradeRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(recipe -> recipe.bookInfo),
            Formulation.CODEC.fieldOf("input").forGetter(recipe -> recipe.inputSerum),
            Formulation.CODEC.fieldOf("result").forGetter(recipe -> recipe.resultSerum),
            Ingredient.CODEC.listOf(0, 8).fieldOf("additives").forGetter(recipe -> recipe.additives)
    ).apply(i, SerumUpgradeRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, SerumUpgradeRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, recipe -> recipe.bookInfo,
            ByteBufCodecs.STRING_UTF8.map(name -> Formulation.valueOf(name), Formulation::name), recipe -> recipe.inputSerum,
            ByteBufCodecs.STRING_UTF8.map(name -> Formulation.valueOf(name), Formulation::name), recipe -> recipe.resultSerum,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list(8)), recipe -> recipe.additives, SerumUpgradeRecipe::new);
    public static final RecipeSerializer<SerumUpgradeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
    private final List<Ingredient> additives;

    public SerumUpgradeRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, Formulation inputSerum, Formulation resultSerum, List<Ingredient> additives) {
        super(commonInfo, bookInfo);
        if (additives.size() > 8 || !switch (resultSerum) {
            case REFINED_TIME_SERUM, ENDURING_TIME_SERUM -> inputSerum == Formulation.TIME_SERUM;
            case OVERCHARGED_TIME_SERUM -> inputSerum == Formulation.REFINED_TIME_SERUM;
            default -> false;
        }) { throw new IllegalArgumentException("Invalid serum upgrade"); }
        this.inputSerum = inputSerum;
        this.resultSerum = resultSerum;
        this.additives = List.copyOf(additives);
    }
    @Override public RecipeSerializer<SerumUpgradeRecipe> getSerializer() { return SERIALIZER; }
    @Override protected PlacementInfo createPlacementInfo() {
        var ingredients = new ArrayList<Ingredient>();
        ingredients.add(Ingredient.of(PreserveItems.compound(inputSerum)));
        ingredients.addAll(additives);
        return PlacementInfo.create(ingredients);
    }
    @Override public boolean matches(CraftingInput input, Level level) { return valid(input); }
    @Override public ItemStack assemble(CraftingInput input) { return valid(input) ? PreserveItems.compound(resultSerum).getDefaultInstance() : ItemStack.EMPTY; }

    private boolean valid(CraftingInput input) {
        if (input.ingredientCount() != 1 + additives.size()) { return false; }
        boolean found = false;
        var materials = new ArrayList<ItemStack>();
        for (var stack : input.items()) {
            if (stack.isEmpty()) { continue; }
            if (stack.getItem() instanceof CompoundItem jar) {
                if (jar.formulation() != inputSerum || !jar.isFull(stack) || found) { return false; }
                found = true;
            } else { materials.add(stack); }
        }
        if (!found || materials.size() != additives.size()) { return false; }
        // Bounded matching preserves loader data-aware ingredient tests, including overlapping tags.
        int combinations = 1 << additives.size();
        boolean[] reachable = new boolean[combinations];
        reachable[0] = true;
        for (int mask = 0; mask < combinations - 1; mask++) {
            if (!reachable[mask]) { continue; }
            var stack = materials.get(Integer.bitCount(mask));
            for (int ingredient = 0; ingredient < additives.size(); ingredient++) {
                int bit = 1 << ingredient;
                if ((mask & bit) == 0 && additives.get(ingredient).test(stack)) { reachable[mask | bit] = true; }
            }
        }
        return reachable[combinations - 1];
    }

    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        if (!valid(input)) { return NonNullList.withSize(input.size(), ItemStack.EMPTY); }
        var result = CraftingRecipe.defaultCraftingReminder(input);
        for (int slot = 0; slot < input.size(); slot++) {
            if (input.getItem(slot).getItem() instanceof CompoundItem jar) {
                // The input dose's bottle becomes the upgraded dose.
                result.set(slot, ItemStack.EMPTY);
            }
        }
        return result;
    }

    @Override public List<RecipeDisplay> display() {
        var inputs = new ArrayList<SlotDisplay>();
        inputs.add(new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(PreserveItems.compound(inputSerum).getDefaultInstance())));
        additives.forEach(ingredient -> inputs.add(ingredient.display()));
        return List.of(new ShapelessCraftingRecipeDisplay(inputs,
                new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(PreserveItems.compound(resultSerum).getDefaultInstance())),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }
}

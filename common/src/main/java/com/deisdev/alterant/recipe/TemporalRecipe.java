package com.deisdev.alterant.recipe;

import com.deisdev.alterant.api.Formulation;
import com.deisdev.alterant.item.CompoundItem;
import com.deisdev.alterant.item.AlterantItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.EnumSet;
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

/** Packs may replace additive costs; the three full base jars and net container accounting are invariant. */
public final class TemporalRecipe extends NormalCraftingRecipe {
    private static final List<Formulation> BASES = List.of(Formulation.GROWTH_INHIBITOR, Formulation.PRESERVING_SEALANT, Formulation.STRUCTURAL_STASIS);
    public static final MapCodec<TemporalRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(recipe -> recipe.bookInfo),
            Ingredient.CODEC.listOf(0, 6).fieldOf("additives").forGetter(recipe -> recipe.additives)
    ).apply(i, TemporalRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, TemporalRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, recipe -> recipe.bookInfo,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list(6)), recipe -> recipe.additives, TemporalRecipe::new);
    public static final RecipeSerializer<TemporalRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
    private final List<Ingredient> additives;

    public TemporalRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, List<Ingredient> additives) {
        super(commonInfo, bookInfo);
        if (additives.size() > 6) { throw new IllegalArgumentException("Temporal crafting fits within nine slots"); }
        this.additives = List.copyOf(additives);
    }
    @Override public RecipeSerializer<TemporalRecipe> getSerializer() { return SERIALIZER; }
    @Override protected PlacementInfo createPlacementInfo() {
        var ingredients = new ArrayList<Ingredient>();
        BASES.forEach(formulation -> ingredients.add(Ingredient.of(AlterantItems.compound(formulation))));
        ingredients.addAll(additives);
        return PlacementInfo.create(ingredients);
    }
    @Override public boolean matches(CraftingInput input, Level level) { return valid(input); }
    @Override public ItemStack assemble(CraftingInput input) { return valid(input) ? AlterantItems.TEMPORAL_STASIS.get().getDefaultInstance() : ItemStack.EMPTY; }

    private boolean valid(CraftingInput input) {
        if (input.ingredientCount() != 3 + additives.size()) { return false; }
        var found = EnumSet.noneOf(Formulation.class);
        var materials = new ArrayList<ItemStack>();
        for (var stack : input.items()) {
            if (stack.isEmpty()) { continue; }
            if (stack.getItem() instanceof CompoundItem jar) {
                if (!BASES.contains(jar.formulation()) || !jar.isFull(stack) || !found.add(jar.formulation())) { return false; }
            } else { materials.add(stack); }
        }
        if (found.size() != 3 || materials.size() != additives.size()) { return false; }
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
                // The structural jar's container becomes the output; the other two return their bottles.
                result.set(slot, jar.formulation() == Formulation.STRUCTURAL_STASIS ? ItemStack.EMPTY : new ItemStack(Items.GLASS_BOTTLE));
            }
        }
        return result;
    }

    @Override public List<RecipeDisplay> display() {
        var inputs = new ArrayList<SlotDisplay>();
        for (var formulation : BASES) {
            SlotDisplay jar = new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(AlterantItems.compound(formulation).getDefaultInstance()));
            if (formulation != Formulation.STRUCTURAL_STASIS) { jar = new SlotDisplay.WithRemainder(jar, new SlotDisplay.ItemSlotDisplay(Items.GLASS_BOTTLE)); }
            inputs.add(jar);
        }
        additives.forEach(ingredient -> inputs.add(ingredient.display()));
        return List.of(new ShapelessCraftingRecipeDisplay(inputs,
                new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(AlterantItems.TEMPORAL_STASIS.get().getDefaultInstance())),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }
}

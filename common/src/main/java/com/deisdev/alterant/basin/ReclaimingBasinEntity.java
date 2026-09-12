package com.deisdev.alterant.basin;

import com.deisdev.alterant.item.*;
import com.deisdev.alterant.recipe.AlterantRecipes;
import java.util.Optional;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A local three-slot workstation. No materials are reserved across ticks or consumed before batch completion. */
public final class ReclaimingBasinEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    private static final int[] INPUT = {0}, ADDITIVE = {1}, OUTPUT = {2};
    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private ResourceKey<Recipe<?>> selected;
    private ReclaimingRecipe working;
    private ItemStack workingInput = ItemStack.EMPTY, workingAdditive = ItemStack.EMPTY;
    private int progress;
    public ReclaimingBasinEntity(BlockPos pos, BlockState state) { super(AlterantBlocks.BASIN_ENTITY.get(), pos, state); }
    @Override public int getContainerSize() { return 3; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> value) { items = value; }
    @Override protected Component getDefaultName() { return Component.translatable("block.alterant.reclaiming_basin"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new ReclaimingBasinMenu(id, inventory, this); }
    public ResourceKey<Recipe<?>> selected() { return selected; }
    public int progress() { return progress; }
    public int duration() { return working == null ? 0 : working.duration(); }
    public boolean select(ResourceKey<Recipe<?>> recipe) {
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread() || isRemoved()) { return false; }
        if (server.recipeAccess().byKey(recipe).filter(holder -> holder.value() instanceof ReclaimingRecipe).isEmpty()) { return false; }
        if (!recipe.equals(selected)) { selected = recipe; reset(); setChanged(); }
        return true;
    }
    public static Optional<ResidueFamily> looseFamily(ItemStack stack) {
        for (var family : ResidueFamily.values()) { if (stack.is(family.item())) { return Optional.of(family); } }
        return Optional.empty();
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 ? ReclamationJarItem.valid(stack) || looseFamily(stack).isPresent() : slot == 1 && !ReclamationJarItem.valid(stack) && looseFamily(stack).isEmpty();
    }
    @Override public int[] getSlotsForFace(Direction side) { return (side == Direction.UP ? INPUT : side == Direction.DOWN ? OUTPUT : ADDITIVE).clone(); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return !ReclamationJarItem.valid(stack) && (side == Direction.UP ? slot == 0 : side != Direction.DOWN && slot == 1) && canPlaceItem(slot, stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return side == Direction.DOWN && slot == 2; }
    private void reset() { progress = 0; working = null; workingInput = ItemStack.EMPTY; workingAdditive = ItemStack.EMPTY; }
    public void tick(ServerLevel server) {
        if (items.get(0).isEmpty()) { if (progress != 0) { reset(); setChanged(); } visual(server, null, false); return; }
        if (selected == null) {
            // A loose input has an unambiguous stock default. A mixed jar waits for an explicit menu selection.
            looseFamily(items.get(0)).ifPresent(family -> selected = ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath("alterant", "reclaim/" + family.getSerializedName())));
            if (selected != null) { setChanged(); }
        }
        var holder = selected == null ? Optional.<RecipeHolder<?>>empty() : server.recipeAccess().byKey(selected);
        var recipe = holder.isPresent() && holder.get().value() instanceof ReclaimingRecipe found ? found : null;
        if (recipe == null) { if (working != null) { reset(); setChanged(); } visual(server, null, false); return; }
        var input = new BasinInput(items.get(0), items.get(1));
        if (!recipe.matches(input, server)) { if (working != null) { reset(); setChanged(); } visual(server, recipe, false); return; }
        var additive = recipe.additive().isPresent() ? items.get(1) : ItemStack.EMPTY;
        if (working == null || !working.equals(recipe) || !sameMaterial(workingInput, items.get(0)) || !sameMaterial(workingAdditive, additive)) {
            progress = 0; working = recipe;
            workingInput = items.get(0).copyWithCount(1); workingAdditive = additive.copyWithCount(1); setChanged();
        }
        var result = recipe.assemble(input);
        if (!fits(result)) { visual(server, recipe, false); return; }
        visual(server, recipe, true);
        progress++; setChanged();
        if (progress < recipe.duration()) { return; }
        // Recheck the exact recipe and every batch input/output immediately before the native inventory writes.
        if (!recipe.matches(new BasinInput(items.get(0), items.get(1)), server) || !fits(result)) { return; }
        var residue = items.get(0).copy(); var fresh = items.get(1).copy();
        if (ReclamationJarItem.valid(residue)) { ReclamationJarItem.setContents(residue, ReclamationJarItem.contents(residue).spend(recipe.family(), recipe.count())); }
        else { residue.shrink(recipe.count()); }
        if (recipe.additive().isPresent()) { fresh.shrink(recipe.additiveCount()); }
        var output = items.get(2).isEmpty() ? result : items.get(2).copyWithCount(items.get(2).getCount() + result.getCount());
        items.set(0, residue); items.set(1, fresh); items.set(2, output); reset(); setChanged();
        visual(server, recipe, false);
    }
    private static boolean sameMaterial(ItemStack first, ItemStack second) {
        return first.isEmpty() ? second.isEmpty() : !second.isEmpty() && ItemStack.isSameItemSameComponents(first, second);
    }
    private boolean fits(ItemStack result) {
        var output = items.get(2);
        return !result.isEmpty() && (output.isEmpty() ? result.getCount() <= getMaxStackSize(result)
                : ItemStack.isSameItemSameComponents(output, result) && output.getCount() + result.getCount() <= getMaxStackSize(output));
    }
    private void visual(ServerLevel server, ReclaimingRecipe recipe, boolean active) {
        var family = recipe == null ? looseFamily(items.get(0)).orElse(null) : recipe.family();
        int amount = family == null ? 0 : ReclamationJarItem.valid(items.get(0)) ? ReclamationJarItem.contents(items.get(0)).count(family)
                : items.get(0).is(family.item()) ? items.get(0).getCount() : 0;
        var state = getBlockState();
        var next = state.setValue(ReclaimingBasinBlock.FAMILY, amount == 0 ? 0 : family.ordinal() + 1)
                .setValue(ReclaimingBasinBlock.FILL, amount == 0 ? 0 : amount < 32 ? 1 : 2).setValue(ReclaimingBasinBlock.ACTIVE, active);
        if (state != next) { server.setBlock(worldPosition, next, Block.UPDATE_CLIENTS); }
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input); items = NonNullList.withSize(3, ItemStack.EMPTY); ContainerHelper.loadAllItems(input, items);
        selected = input.read("selected_recipe", Recipe.KEY_CODEC).orElse(null);
        working = input.read("working_recipe", ReclaimingRecipe.CODEC.codec()).orElse(null);
        progress = Math.max(0, Math.min(input.getIntOr("progress", 0), working == null ? 0 : working.duration()));
        workingInput = input.read("working_input", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        workingAdditive = input.read("working_additive", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output); ContainerHelper.saveAllItems(output, items);
        if (selected != null) { output.store("selected_recipe", Recipe.KEY_CODEC, selected); }
        if (working != null) { output.store("working_recipe", ReclaimingRecipe.CODEC.codec(), working); }
        output.putInt("progress", progress);
        if (!workingInput.isEmpty()) { output.store("working_input", ItemStack.CODEC, workingInput); }
        if (!workingAdditive.isEmpty()) { output.store("working_additive", ItemStack.CODEC, workingAdditive); }
    }
}

package com.deisdev.alterant.basin;

import com.deisdev.alterant.item.*;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.*;

public final class ReclaimingBasinMenu extends AbstractContainerMenu {
    private final Container inventory;
    private final ReclaimingBasinEntity basin;
    private final SimpleContainer choicesDisplay = new SimpleContainer(4);
    private final ContainerData data = new SimpleContainerData(5);
    private Collection<RecipeHolder<?>> recipeSource;
    private List<RecipeHolder<?>> choices = List.of();
    private int familyBits = -1, page;
    public ReclaimingBasinMenu(int id, Inventory playerInventory) { this(id, playerInventory, new SimpleContainer(3)); }
    public ReclaimingBasinMenu(int id, Inventory playerInventory, Container inventory) {
        super(AlterantMenus.BASIN.get(), id); this.inventory = inventory;
        basin = inventory instanceof ReclaimingBasinEntity entity ? entity : null;
        addSlot(new Slot(inventory, 0, 26, 30) { @Override public boolean mayPlace(ItemStack stack) { return ReclamationJarItem.valid(stack) || ReclaimingBasinEntity.looseFamily(stack).isPresent(); } });
        addSlot(new Slot(inventory, 1, 56, 30) { @Override public boolean mayPlace(ItemStack stack) { return inventory.canPlaceItem(1, stack); } });
        addSlot(new Slot(inventory, 2, 134, 30) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
        for (int index = 0; index < 4; index++) {
            addSlot(new Slot(choicesDisplay, index, 35 + 30 * index, 60) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player player) { return false; }
                @Override public boolean isActive() { return data.get(3) > 1; }
            });
        }
        addStandardInventorySlots(playerInventory, 8, 94); addDataSlots(data); refresh();
    }
    public int progress() { return data.get(0); }
    public int duration() { return data.get(1); }
    public int selectedIcon() { return data.get(2); }
    public int choices() { return data.get(3); }
    public int page() { return data.get(4); }
    private void refresh() {
        if (basin == null || !(basin.getLevel() instanceof ServerLevel level)) { return; }
        var source = level.recipeAccess().getRecipes(); int bits = 0; var stack = inventory.getItem(0);
        for (var family : ResidueFamily.values()) {
            if (stack.is(family.item()) || ReclamationJarItem.valid(stack) && ReclamationJarItem.contents(stack).count(family) > 0) { bits |= 1 << family.ordinal(); }
        }
        if (source != recipeSource || bits != familyBits) {
            recipeSource = source; familyBits = bits;
            final int allowed = bits;
            choices = source.stream().filter(holder -> holder.value() instanceof ReclaimingRecipe recipe && (allowed & (1 << recipe.family().ordinal())) != 0)
                    .sorted(Comparator.comparing(holder -> holder.id().identifier().toString())).toList();
            page = Math.min(page, Math.max(0, (choices.size() - 1) / 4));
            if (choices.size() == 1 && basin.selected() == null) { basin.select(choices.getFirst().id()); }
        }
        data.set(0, basin.progress()); data.set(1, basin.duration()); data.set(2, -1); data.set(3, choices.size()); data.set(4, page);
        for (int icon = 0; icon < 4; icon++) {
            int index = page * 4 + icon; var display = ItemStack.EMPTY;
            if (index < choices.size()) {
                var holder = choices.get(index); var recipe = (ReclaimingRecipe) holder.value(); display = recipe.result().create();
                var lore = new ArrayList<Component>();
                lore.add(Component.translatable("menu.alterant.basin.residue", recipe.count(), Component.translatable("item.alterant." + recipe.family().getSerializedName())));
                if (recipe.additive().isPresent()) {
                    var names = Component.empty();
                    recipe.additive().get().items().limit(3).forEach(item -> {
                        if (!names.getSiblings().isEmpty()) { names.append(Component.translatable("text.alterant.alternative_separator")); }
                        names.append(item.value().getDefaultInstance().getHoverName());
                    });
                    lore.add(Component.translatable("menu.alterant.basin.additive", recipe.additiveCount(), names));
                }
                lore.add(Component.translatable("menu.alterant.basin.duration", recipe.duration() / 20.0));
                display.set(DataComponents.LORE, new ItemLore(lore));
                if (holder.id().equals(basin.selected())) { data.set(2, icon); }
            }
            if (!ItemStack.matches(choicesDisplay.getItem(icon), display)) { choicesDisplay.setItem(icon, display); }
        }
    }
    @Override public void broadcastChanges() { refresh(); super.broadcastChanges(); }
    @Override public boolean stillValid(Player player) { return inventory.stillValid(player); }
    @Override public void clicked(int slot, int button, ContainerInput input, Player player) {
        if (!stillValid(player)) { return; }
        if (slot >= 3 && slot < 7) {
            if (basin != null && input == ContainerInput.PICKUP && button == 0) {
                refresh(); int choice = page * 4 + slot - 3;
                if (choice < choices.size()) { basin.select(choices.get(choice).id()); refresh(); }
            }
            return;
        }
        super.clicked(slot, button, input, player);
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (basin == null || !stillValid(player) || (button != 0 && button != 1)) { return false; }
        refresh(); page = Math.max(0, Math.min(Math.max(0, (choices.size() - 1) / 4), page + (button == 0 ? -1 : 1))); refresh(); return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) {
        if (!stillValid(player) || slot < 0 || slot >= slots.size() || slot >= 3 && slot < 7) { return ItemStack.EMPTY; }
        var source = slots.get(slot); if (!source.hasItem()) { return ItemStack.EMPTY; }
        var stack = source.getItem(); var original = stack.copy();
        if (slot < 3) { if (!moveItemStackTo(stack, 7, slots.size(), true)) { return ItemStack.EMPTY; } }
        else if (ReclamationJarItem.valid(stack) || ReclaimingBasinEntity.looseFamily(stack).isPresent()) {
            if (!moveItemStackTo(stack, 0, 1, false)) { return ItemStack.EMPTY; }
        } else if (!moveItemStackTo(stack, 1, 2, false)) { return ItemStack.EMPTY; }
        if (stack.isEmpty()) { source.setByPlayer(ItemStack.EMPTY); } else { source.setChanged(); }
        source.onTake(player, stack); return original;
    }
}

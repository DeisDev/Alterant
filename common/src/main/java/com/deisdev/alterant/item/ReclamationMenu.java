package com.deisdev.alterant.item;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Display slots are views into the bound jar, never a second inventory of redeemable items. */
public final class ReclamationMenu extends AbstractContainerMenu {
    private final Inventory inventory;
    private final int sourceSlot;
    private final ItemStack jar;
    private final SimpleContainer display = new SimpleContainer(4);
    private final ContainerData counts = new SimpleContainerData(4);

    public ReclamationMenu(int id, Inventory inventory) { this(id, inventory, -1); }
    public ReclamationMenu(int id, Inventory inventory, int sourceSlot) {
        super(AlterantMenus.RECLAMATION.get(), id);
        this.inventory = inventory; this.sourceSlot = sourceSlot;
        jar = sourceSlot < 0 ? ItemStack.EMPTY : inventory.getItem(sourceSlot);
        for (int family = 0; family < 4; family++) {
            addSlot(new Slot(display, family, 35 + family * 30, 24) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player player) { return false; }
            });
        }
        addStandardInventorySlots(inventory, 8, 68);
        addDataSlots(counts);
        refresh();
    }
    public int count(int family) { return counts.get(family); }
    private void refresh() {
        if (sourceSlot < 0) { return; }
        var contents = ReclamationJarItem.contents(jar);
        for (var family : ResidueFamily.values()) {
            int value = contents.count(family); counts.set(family.ordinal(), value);
            display.setItem(family.ordinal(), value == 0 ? ItemStack.EMPTY : new ItemStack(family.item(), Math.min(64, value)));
        }
    }
    @Override public boolean stillValid(Player player) {
        return sourceSlot < 0 || player == inventory.player && player.isAlive() && !player.isSpectator()
                && inventory.getItem(sourceSlot) == jar && ReclamationJarItem.valid(jar);
    }
    @Override public void broadcastChanges() { refresh(); super.broadcastChanges(); }
    @Override public void clicked(int slot, int button, ContainerInput input, Player player) {
        if (!stillValid(player)) { return; }
        if (slot >= 0 && slot < 4) {
            if (sourceSlot < 0 || player.level().isClientSide()) { return; }
            if (input == ContainerInput.QUICK_MOVE) { quickMoveStack(player, slot); }
            else if (input == ContainerInput.PICKUP && (button == 0 || button == 1)) { extract(slot, button == 1); }
            return;
        }
        super.clicked(slot, button, input, player);
    }
    private void extract(int slot, boolean half) {
        var family = ResidueFamily.values()[slot]; var contents = ReclamationJarItem.contents(jar); var carried = getCarried();
        var residue = new ItemStack(family.item());
        if (!carried.isEmpty() && !ItemStack.isSameItemSameComponents(carried, residue)) { return; }
        int available = Math.min(64, contents.count(family));
        int amount = Math.min(half ? (available + 1) / 2 : available, 64 - carried.getCount());
        if (amount <= 0) { return; }
        ReclamationJarItem.setContents(jar, contents.spend(family, amount));
        setCarried(residue.copyWithCount(carried.getCount() + amount)); inventory.setChanged(); refresh();
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) {
        if (!stillValid(player) || slot < 0 || slot >= slots.size()) { return ItemStack.EMPTY; }
        if (slot < 4) {
            if (sourceSlot < 0 || player.level().isClientSide()) { return ItemStack.EMPTY; }
            var family = ResidueFamily.values()[slot]; var contents = ReclamationJarItem.contents(jar);
            int available = Math.min(64, contents.count(family));
            if (available == 0) { return ItemStack.EMPTY; }
            var moving = new ItemStack(family.item(), available);
            moveItemStackTo(moving, 4, slots.size(), true);
            int moved = available - moving.getCount();
            if (moved == 0) { return ItemStack.EMPTY; }
            ReclamationJarItem.setContents(jar, contents.spend(family, moved)); inventory.setChanged(); refresh();
            // One click transfers one stack; returning empty prevents the native repeat loop from draining the whole jar.
            return ItemStack.EMPTY;
        }
        var source = slots.get(slot); var stack = source.getItem(); var original = stack.copy();
        if (slot < 31 ? !moveItemStackTo(stack, 31, 40, false) : !moveItemStackTo(stack, 4, 31, false)) { return ItemStack.EMPTY; }
        if (stack.isEmpty()) { source.setByPlayer(ItemStack.EMPTY); } else { source.setChanged(); }
        return original;
    }
}

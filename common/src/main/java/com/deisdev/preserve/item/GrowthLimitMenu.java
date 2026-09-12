package com.deisdev.preserve.item;

import com.deisdev.preserve.engine.GrowthLimit;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** A configuration menu bound to one physical held jar. Buttons never create items or consume compound. */
public final class GrowthLimitMenu extends AbstractContainerMenu {
    private final Inventory inventory;
    private final int sourceSlot;
    private final ItemStack jar;
    private final ContainerData values = new SimpleContainerData(2);
    public GrowthLimitMenu(int id, Inventory inventory) { this(id, inventory, -1); }
    public GrowthLimitMenu(int id, Inventory inventory, int sourceSlot) {
        super(PreserveMenus.GROWTH_LIMIT.get(), id);
        this.inventory = inventory; this.sourceSlot = sourceSlot; jar = sourceSlot < 0 ? ItemStack.EMPTY : inventory.getItem(sourceSlot);
        addStandardInventorySlots(inventory, 8, 100); addDataSlots(values); refresh();
    }
    public GrowthLimit.Mode mode() { return values.get(0) == 1 ? GrowthLimit.Mode.HEIGHT : GrowthLimit.Mode.STAGE; }
    public int target() { return values.get(1); }
    private void refresh() {
        if (sourceSlot < 0) { return; }
        var limit = jar.getOrDefault(PreserveItems.GROWTH_LIMIT.get(), GrowthLimit.DEFAULT);
        values.set(0, limit.mode().ordinal()); values.set(1, limit.target());
    }
    @Override public void broadcastChanges() { refresh(); super.broadcastChanges(); }
    @Override public boolean stillValid(Player player) {
        return sourceSlot < 0 || player == inventory.player && player.isAlive() && !player.isSpectator()
                && inventory.getItem(sourceSlot) == jar && PreserveItems.GROWTH_REGULATOR.get().remaining(jar) > 0;
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (sourceSlot < 0 || player.level().isClientSide() || !stillValid(player)) { return false; }
        GrowthLimit limit;
        if (button == 0 || button == 1) { limit = new GrowthLimit(1, button == 0 ? GrowthLimit.Mode.STAGE : GrowthLimit.Mode.HEIGHT, 3); }
        else {
            int target = button - 10;
            if (target < (mode() == GrowthLimit.Mode.HEIGHT ? 1 : 0) || target > (mode() == GrowthLimit.Mode.HEIGHT ? 16 : 7)) { return false; }
            limit = new GrowthLimit(1, mode(), target);
        }
        jar.set(PreserveItems.GROWTH_LIMIT.get(), limit); inventory.setChanged(); refresh(); broadcastChanges(); return true;
    }
    @Override public void clicked(int slot, int button, ContainerInput input, Player player) {
        if (stillValid(player)) { super.clicked(slot, button, input, player); }
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) {
        if (!stillValid(player) || slot < 0 || slot >= slots.size()) { return ItemStack.EMPTY; }
        var source = slots.get(slot); var stack = source.getItem(); var original = stack.copy();
        if (slot < 27 ? !moveItemStackTo(stack, 27, 36, false) : !moveItemStackTo(stack, 0, 27, false)) { return ItemStack.EMPTY; }
        if (stack.isEmpty()) { source.setByPlayer(ItemStack.EMPTY); } else { source.setChanged(); }
        return original;
    }
}

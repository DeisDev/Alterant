package com.deisdev.alterant.item;

import com.deisdev.alterant.engine.TransferPolicy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Physical held-jar configuration. World coatings retain the policy captured when paid. */
public final class TransferPolicyMenu extends AbstractContainerMenu {
    private final Inventory inventory;
    private final int sourceSlot;
    private final ItemStack jar;
    private final ContainerData values = new SimpleContainerData(2);
    public TransferPolicyMenu(int id, Inventory inventory) { this(id, inventory, -1); }
    public TransferPolicyMenu(int id, Inventory inventory, int sourceSlot) {
        super(AlterantMenus.TRANSFER_POLICY.get(), id);
        this.inventory = inventory; this.sourceSlot = sourceSlot;
        jar = sourceSlot < 0 ? ItemStack.EMPTY : inventory.getItem(sourceSlot);
        addStandardInventorySlots(inventory, 8, 128); addDataSlots(values); refresh();
    }
    public TransferPolicy policy() {
        return new TransferPolicy(1, TransferPolicy.Mode.values()[Math.clamp(values.get(0), 0, 2)], Math.clamp(values.get(1), 1, 63));
    }
    private void refresh() {
        if (sourceSlot < 0) { return; }
        var policy = jar.getOrDefault(AlterantItems.TRANSFER_POLICY.get(), TransferPolicy.DEFAULT);
        values.set(0, policy.mode().ordinal()); values.set(1, policy.faces());
    }
    @Override public void broadcastChanges() { refresh(); super.broadcastChanges(); }
    @Override public boolean stillValid(Player player) {
        return sourceSlot < 0 || player == inventory.player && player.isAlive() && !player.isSpectator()
                && inventory.getItem(sourceSlot) == jar && AlterantItems.TRANSFER_SEAL.get().remaining(jar) > 0;
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (sourceSlot < 0 || player.level().isClientSide() || !stillValid(player)) { return false; }
        var old = policy(); TransferPolicy next;
        if (button >= 0 && button < 3) { next = new TransferPolicy(1, TransferPolicy.Mode.values()[button], old.faces()); }
        else if (button == 20) { next = new TransferPolicy(1, old.mode(), 63); }
        else if (button >= 10 && button < 16) {
            int mask = old.faces() ^ 1 << (button - 10);
            if (mask == 0) { return false; }
            next = new TransferPolicy(1, old.mode(), mask);
        } else { return false; }
        jar.set(AlterantItems.TRANSFER_POLICY.get(), next); inventory.setChanged(); broadcastChanges(); return true;
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
